package com.example.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.AspectRatioPreset
import com.example.model.FontPreset
import com.example.model.FrameStyle
import com.example.model.ProjectEditState
import com.example.model.TextAnimationType
import com.example.model.TextLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ExportOptions(
    val resolutionName: String = "1080p",
    val targetWidth: Int = 1080,
    val targetHeight: Int = 1920,
    val fps: Int = 30,
    val quality: String = "High", // Low, Medium, High, Custom
    val bitrate: Int = 8_000_000,
    val customOutputFolder: String = "Movies/Reels Maker"
)

sealed class ExportProgress {
    data class Progress(val percentage: Float, val statusMessage: String) : ExportProgress()
    data class Success(val outputFilePath: String, val uriString: String, val durationMs: Long, val fileSize: Long) : ExportProgress()
    data class Error(val errorMessage: String) : ExportProgress()
    object Cancelled : ExportProgress()
}

class VideoExportEngine(private val context: Context) {

    @Volatile
    private var isCancelled = false

    fun cancelExport() {
        isCancelled = true
    }

    suspend fun exportProject(
        state: ProjectEditState,
        options: ExportOptions,
        onProgress: (ExportProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        isCancelled = false
        try {
            onProgress(ExportProgress.Progress(0.05f, "Preparing video tracks..."))

            val inputUri = Uri.parse(state.originalVideoUri)
            val startTimeMs = if (state.trimStartMs > 0) state.trimStartMs else 0L
            val endTimeMs = if (state.trimEndMs > startTimeMs) state.trimEndMs else state.videoMetadata.durationMs
            val effectiveDurationMs = (endTimeMs - startTimeMs).coerceAtLeast(1000L)

            // Setup output file
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFileName = "ReelsMaker_$timeStamp.mp4"
            val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outputFile = File(outputDir, outputFileName)

            onProgress(ExportProgress.Progress(0.15f, "Decoding frames & burning overlays..."))

            // Render video with Canvas overlay synthesis and audio muxing
            val success = processVideoWithCanvas(
                inputUri = inputUri,
                outputFile = outputFile,
                state = state,
                options = options,
                startMs = startTimeMs,
                endMs = endTimeMs,
                onProgress = onProgress
            )

            if (isCancelled) {
                outputFile.delete()
                onProgress(ExportProgress.Cancelled)
                return@withContext
            }

            if (!success || !outputFile.exists() || outputFile.length() == 0L) {
                onProgress(ExportProgress.Error("Failed to encode MP4 video file."))
                return@withContext
            }

            onProgress(ExportProgress.Progress(0.92f, "Saving to Reels Maker directory..."))

            // Save to Public MediaStore / Movies directory
            val finalUri = saveToPublicStorage(outputFile, outputFileName)

            onProgress(
                ExportProgress.Success(
                    outputFilePath = outputFile.absolutePath,
                    uriString = finalUri.toString(),
                    durationMs = effectiveDurationMs,
                    fileSize = outputFile.length()
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(ExportProgress.Error("Export failed: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    private suspend fun processVideoWithCanvas(
        inputUri: Uri,
        outputFile: File,
        state: ProjectEditState,
        options: ExportOptions,
        startMs: Long,
        endMs: Long,
        onProgress: (ExportProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var muxer: MediaMuxer? = null
        var retriever: MediaMetadataRetriever? = null
        var audioExtractor: MediaExtractor? = null

        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, inputUri)

            val outWidth = options.targetWidth
            val outHeight = options.targetHeight
            val fps = options.fps.coerceIn(15, 60)
            val bitRate = options.bitrate

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Setup Video Encoder
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outWidth, outHeight)
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoEncoder.createInputSurface()
            videoEncoder.start()

            // Setup Audio extraction & copying
            var audioTrackIndexInMuxer = -1
            var audioTrackIndexInExtractor = -1
            try {
                audioExtractor = MediaExtractor()
                audioExtractor.setDataSource(context, inputUri, null)
                for (i in 0 until audioExtractor.trackCount) {
                    val format = audioExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrackIndexInExtractor = i
                        audioTrackIndexInMuxer = muxer.addTrack(format)
                        break
                    }
                }
            } catch (e: Exception) {
                // Audio might not be present or supported
                e.printStackTrace()
            }

            var videoTrackIndexInMuxer = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val totalDurationMs = (endMs - startMs).coerceAtLeast(500L)
            val frameIntervalMs = 1000L / fps
            val totalFrames = (totalDurationMs / frameIntervalMs).toInt().coerceAtLeast(1)

            // Pre-load logo bitmap if present
            val logoBitmaps = mutableMapOf<String, Bitmap>()
            for (logo in state.logoLayers) {
                if (logo.imageUriString.isNotEmpty()) {
                    try {
                        val uri = Uri.parse(logo.imageUriString)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                logoBitmaps[logo.id] = bmp
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Render frames
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isSubpixelText = true
            }

            var currentFrame = 0
            var presentationTimeUs = 0L

            for (f in 0 until totalFrames) {
                if (isCancelled) break

                val currentMs = startMs + (f * frameIntervalMs)
                val targetTimeUs = currentMs * 1000L

                // Extract frame from video
                val frameBitmap = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.frameAtTime

                // Lock Canvas from MediaCodec InputSurface
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }

                // 1. Draw Background (Deep Black)
                canvas.drawColor(AndroidColor.BLACK)

                // 2. Draw Video Frame with AspectRatio, Scaling & Pan Offset
                if (frameBitmap != null) {
                    drawVideoFrame(
                        canvas = canvas,
                        bitmap = frameBitmap,
                        outWidth = outWidth,
                        outHeight = outHeight,
                        state = state,
                        paint = paint
                    )
                }

                // 3. Draw Video Frame Border Overlay
                drawFrameOverlay(
                    canvas = canvas,
                    outWidth = outWidth,
                    outHeight = outHeight,
                    state = state,
                    paint = paint
                )

                // 4. Draw Logo Watermark Layer
                drawLogoLayers(
                    canvas = canvas,
                    outWidth = outWidth,
                    outHeight = outHeight,
                    state = state,
                    currentMs = currentMs - startMs,
                    logoBitmaps = logoBitmaps,
                    paint = paint
                )

                // 5. Draw Text Layers with Font, Style, Animation & Outline
                drawTextLayers(
                    canvas = canvas,
                    outWidth = outWidth,
                    outHeight = outHeight,
                    state = state,
                    currentMs = currentMs - startMs,
                    textPaint = textPaint
                )

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain Encoder output
                while (true) {
                    val encoderStatus = videoEncoder.dequeueOutputBuffer(bufferInfo, 1000)
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed twice")
                        }
                        val newFormat = videoEncoder.outputFormat
                        videoTrackIndexInMuxer = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (encoderStatus >= 0) {
                        val encodedData = videoEncoder.getOutputBuffer(encoderStatus)
                            ?: throw RuntimeException("Encoder outputBuffer $encoderStatus was null")

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                throw RuntimeException("Muxer not started")
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndexInMuxer, encodedData, bufferInfo)
                        }

                        videoEncoder.releaseOutputBuffer(encoderStatus, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }

                currentFrame++
                val progress = 0.15f + (0.75f * (currentFrame.toFloat() / totalFrames))
                onProgress(
                    ExportProgress.Progress(
                        percentage = progress.coerceIn(0.15f, 0.90f),
                        statusMessage = "Exporting frame $currentFrame / $totalFrames (${(progress * 100).toInt()}%)"
                    )
                )
            }

            // Signal End of Stream to Video Encoder
            videoEncoder.signalEndOfInputStream()

            // Drain remaining buffers
            var eos = false
            while (!eos && !isCancelled) {
                val encoderStatus = videoEncoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (encoderStatus >= 0) {
                    val encodedData = videoEncoder.getOutputBuffer(encoderStatus)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndexInMuxer, encodedData, bufferInfo)
                    }
                    videoEncoder.releaseOutputBuffer(encoderStatus, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true
                    }
                } else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            videoEncoder.stop()
            videoEncoder.release()
            inputSurface.release()

            // Copy audio segment if available
            if (audioTrackIndexInExtractor >= 0 && audioTrackIndexInMuxer >= 0 && muxerStarted && audioExtractor != null && !isCancelled) {
                try {
                    audioExtractor.selectTrack(audioTrackIndexInExtractor)
                    audioExtractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    val audioBuffer = ByteBuffer.allocate(256 * 1024)
                    val audioBufferInfo = MediaCodec.BufferInfo()

                    while (true) {
                        val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                        if (sampleSize < 0) break

                        val sampleTimeUs = audioExtractor.sampleTime
                        if (sampleTimeUs > endMs * 1000L) break

                        val adjustedTimeUs = (sampleTimeUs - (startMs * 1000L)).coerceAtLeast(0L)

                        audioBufferInfo.offset = 0
                        audioBufferInfo.size = sampleSize
                        audioBufferInfo.presentationTimeUs = adjustedTimeUs
                        audioBufferInfo.flags = audioExtractor.sampleFlags

                        muxer.writeSampleData(audioTrackIndexInMuxer, audioBuffer, audioBufferInfo)
                        audioExtractor.advance()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            try {
                audioExtractor?.release()
                retriever?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun drawVideoFrame(
        canvas: Canvas,
        bitmap: Bitmap,
        outWidth: Int,
        outHeight: Int,
        state: ProjectEditState,
        paint: Paint
    ) {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()

        val canvasAspect = outWidth.toFloat() / outHeight.toFloat()
        val videoAspect = srcWidth / srcHeight

        var dstWidth = outWidth.toFloat()
        var dstHeight = outHeight.toFloat()

        // Fit or fill calculation
        when (state.aspectRatio) {
            AspectRatioPreset.RATIO_9_16 -> {
                // Reel vertical canvas
                val scale = maxOf(outWidth / srcWidth, outHeight / srcHeight) * state.videoZoom
                dstWidth = srcWidth * scale
                dstHeight = srcHeight * scale
            }
            AspectRatioPreset.RATIO_16_9 -> {
                val scale = minOf(outWidth / srcWidth, outHeight / srcHeight) * state.videoZoom
                dstWidth = srcWidth * scale
                dstHeight = srcHeight * scale
            }
            AspectRatioPreset.RATIO_1_1 -> {
                val scale = maxOf(outWidth / srcWidth, outHeight / srcHeight) * state.videoZoom
                dstWidth = srcWidth * scale
                dstHeight = srcHeight * scale
            }
            else -> {
                val scale = minOf(outWidth / srcWidth, outHeight / srcHeight) * state.videoZoom
                dstWidth = srcWidth * scale
                dstHeight = srcHeight * scale
            }
        }

        // Apply auto reframe subject center if active
        var centerX = outWidth / 2f + (state.videoOffsetX * outWidth)
        var centerY = outHeight / 2f + (state.videoOffsetY * outHeight)

        if (state.autoReframeActive) {
            // center on detected subject center
            val subjectOffset = (0.5f - state.smartSubjectCenterX) * outWidth
            centerX += subjectOffset
        }

        val dstRect = RectF(
            centerX - (dstWidth / 2f),
            centerY - (dstHeight / 2f),
            centerX + (dstWidth / 2f),
            centerY + (dstHeight / 2f)
        )

        canvas.save()
        if (state.videoRotation != 0f) {
            canvas.rotate(state.videoRotation, outWidth / 2f, outHeight / 2f)
        }
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        canvas.restore()
    }

    private fun drawFrameOverlay(
        canvas: Canvas,
        outWidth: Int,
        outHeight: Int,
        state: ProjectEditState,
        paint: Paint
    ) {
        val frame = state.frameLayer
        if (!frame.isVisible || frame.style == FrameStyle.NONE) return

        val strokeWidthPx = frame.thicknessDp * (outWidth / 360f)
        val cornerRadiusPx = frame.cornerRadiusDp * (outWidth / 360f)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidthPx
        paint.color = frame.primaryColorArgb.toInt()
        paint.alpha = (frame.opacity * 255).toInt().coerceIn(0, 255)

        val halfStroke = strokeWidthPx / 2f
        val rect = RectF(
            halfStroke + 12f,
            halfStroke + 12f,
            outWidth - halfStroke - 12f,
            outHeight - halfStroke - 12f
        )

        when (frame.style) {
            FrameStyle.SIMPLE -> {
                canvas.drawRect(rect, paint)
            }
            FrameStyle.ROUNDED -> {
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
            }
            FrameStyle.DOUBLE_LINE -> {
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
                val innerRect = RectF(
                    rect.left + strokeWidthPx + 6f,
                    rect.top + strokeWidthPx + 6f,
                    rect.right - strokeWidthPx - 6f,
                    rect.bottom - strokeWidthPx - 6f
                )
                paint.strokeWidth = strokeWidthPx * 0.5f
                paint.color = frame.secondaryColorArgb.toInt()
                canvas.drawRoundRect(innerRect, (cornerRadiusPx - 6f).coerceAtLeast(0f), (cornerRadiusPx - 6f).coerceAtLeast(0f), paint)
            }
            FrameStyle.NEON_GLOW -> {
                paint.setShadowLayer(24f, 0f, 0f, frame.secondaryColorArgb.toInt())
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
                paint.clearShadowLayer()
            }
            FrameStyle.CINEMATIC_LETTERBOX -> {
                paint.style = Paint.Style.FILL
                paint.color = AndroidColor.BLACK
                val letterboxHeight = outHeight * 0.12f
                canvas.drawRect(0f, 0f, outWidth.toFloat(), letterboxHeight, paint)
                canvas.drawRect(0f, outHeight - letterboxHeight, outWidth.toFloat(), outHeight.toFloat(), paint)
            }
            FrameStyle.GRADIENT_BORDER -> {
                val gradient = android.graphics.LinearGradient(
                    0f, 0f, outWidth.toFloat(), outHeight.toFloat(),
                    frame.primaryColorArgb.toInt(), frame.secondaryColorArgb.toInt(),
                    android.graphics.Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
                paint.shader = null
            }
            else -> {
                canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
            }
        }
    }

    private fun drawLogoLayers(
        canvas: Canvas,
        outWidth: Int,
        outHeight: Int,
        state: ProjectEditState,
        currentMs: Long,
        logoBitmaps: Map<String, Bitmap>,
        paint: Paint
    ) {
        for (logo in state.logoLayers) {
            if (!logo.isVisible) continue
            if (!logo.applyFullVideo && (currentMs < logo.startMs || currentMs > logo.endMs)) continue

            val bitmap = logoBitmaps[logo.id] ?: continue

            val baseWidth = outWidth * 0.25f * logo.scale
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val baseHeight = baseWidth / aspect

            val centerX = logo.posXFraction * outWidth
            val centerY = logo.posYFraction * outHeight

            val dstRect = RectF(
                centerX - (baseWidth / 2f),
                centerY - (baseHeight / 2f),
                centerX + (baseWidth / 2f),
                centerY + (baseHeight / 2f)
            )

            paint.alpha = (logo.opacity * 255).toInt().coerceIn(0, 255)

            canvas.save()
            if (logo.rotationDeg != 0f) {
                canvas.rotate(logo.rotationDeg, centerX, centerY)
            }
            canvas.drawBitmap(bitmap, null, dstRect, paint)
            canvas.restore()
        }
    }

    private fun drawTextLayers(
        canvas: Canvas,
        outWidth: Int,
        outHeight: Int,
        state: ProjectEditState,
        currentMs: Long,
        textPaint: Paint
    ) {
        for (layer in state.textLayers) {
            if (!layer.isVisible) continue
            if (currentMs < layer.startMs || currentMs > layer.endMs) continue

            val scaleMultiplier = outWidth / 360f
            val baseTextSize = layer.fontSizeSp * scaleMultiplier * layer.scale
            textPaint.textSize = baseTextSize

            // Font typeface
            val typeface = when (layer.fontPreset) {
                FontPreset.SYSTEM_BOLD -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                FontPreset.SYSTEM_REGULAR -> Typeface.DEFAULT
                FontPreset.SYSTEM_LIGHT -> Typeface.create("sans-serif-light", Typeface.NORMAL)
                FontPreset.SERIF_CINEMATIC -> Typeface.SERIF
                FontPreset.MONOSPACE_MODERN -> Typeface.MONOSPACE
                FontPreset.HANDWRITTEN -> Typeface.create("cursive", Typeface.BOLD)
                FontPreset.BENGALI_FRIENDLY -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            textPaint.typeface = typeface

            val centerX = layer.posXFraction * outWidth
            val centerY = layer.posYFraction * outHeight

            // Calculate text animation
            var animatedAlpha = layer.opacity
            var animScale = 1.0f
            var offsetY = 0f
            val elapsedAnim = currentMs - layer.startMs
            if (elapsedAnim < layer.animationDurationMs) {
                val progress = (elapsedAnim.toFloat() / layer.animationDurationMs).coerceIn(0f, 1f)
                when (layer.animationType) {
                    TextAnimationType.FADE_IN -> animatedAlpha *= progress
                    TextAnimationType.POP, TextAnimationType.ZOOM -> animScale = 0.5f + (0.5f * progress)
                    TextAnimationType.SLIDE -> offsetY = (1f - progress) * 50f
                    TextAnimationType.BOUNCE -> {
                        val bounce = Math.sin(progress * Math.PI).toFloat() * 20f
                        offsetY = -bounce
                    }
                    else -> {}
                }
            }

            canvas.save()
            canvas.translate(centerX, centerY + offsetY)
            if (layer.rotationDeg != 0f) {
                canvas.rotate(layer.rotationDeg)
            }
            if (animScale != 1.0f) {
                canvas.scale(animScale, animScale)
            }

            // Measure multiline text
            val lines = layer.text.split("\n")
            val fontMetrics = textPaint.fontMetrics
            val lineHeight = (fontMetrics.bottom - fontMetrics.top) * layer.lineSpacing
            val totalTextHeight = lines.size * lineHeight

            var startY = -(totalTextHeight / 2f) - fontMetrics.top

            for (line in lines) {
                val textWidth = textPaint.measureText(line)
                val lineX = - (textWidth / 2f)

                // Background box if set
                if (layer.backgroundColorArgb != 0x00000000L) {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.backgroundColorArgb.toInt()
                        style = Paint.Style.FILL
                    }
                    val bgRect = RectF(
                        lineX - 12f,
                        startY + fontMetrics.top - 6f,
                        lineX + textWidth + 12f,
                        startY + fontMetrics.bottom + 6f
                    )
                    canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)
                }

                // Text Outline / Stroke
                if (layer.hasOutline) {
                    textPaint.style = Paint.Style.STROKE
                    textPaint.strokeWidth = layer.outlineWidth * scaleMultiplier
                    textPaint.color = layer.outlineColorArgb.toInt()
                    textPaint.alpha = (animatedAlpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawText(line, lineX, startY, textPaint)
                }

                // Main Text Fill
                textPaint.style = Paint.Style.FILL
                textPaint.color = layer.fontColorArgb.toInt()
                textPaint.alpha = (animatedAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawText(line, lineX, startY, textPaint)

                startY += lineHeight
            }

            canvas.restore()
        }
    }

    private suspend fun saveToPublicStorage(sourceFile: File, displayName: String): Uri = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Reels Maker")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = context.contentResolver.insert(collection, contentValues)
                ?: return@withContext Uri.fromFile(sourceFile)

            context.contentResolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(itemUri, contentValues, null, null)
            return@withContext itemUri
        } else {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val reelsDir = File(moviesDir, "Reels Maker").apply { mkdirs() }
            val destFile = File(reelsDir, displayName)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { out ->
                    input.copyTo(out)
                }
            }
            return@withContext Uri.fromFile(destFile)
        }
    }
}
