package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoMetadataExtractor {

    suspend fun extractMetadata(context: Context, uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var width = 0
        var height = 0
        var rotation = 0
        var fps = 30f
        var mimeType = "video/mp4"
        var fileSize = 0L

        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val mimeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            width = widthStr?.toIntOrNull() ?: 1080
            height = heightStr?.toIntOrNull() ?: 1920
            rotation = rotationStr?.toIntOrNull() ?: 0
            if (mimeStr != null) mimeType = mimeStr

            // If video is rotated 90 or 270, swap width and height
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            // Estimate FPS using MediaExtractor
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/")) {
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            fps = format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                        }
                        if (format.containsKey(MediaFormat.KEY_CAPTURE_RATE)) {
                            fps = format.getFloat(MediaFormat.KEY_CAPTURE_RATE)
                        }
                        break
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                fps = 30f
            }

            // File Size
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            } else if (uri.scheme == "file" && uri.path != null) {
                fileSize = File(uri.path!!).length()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }

        VideoMetadata(
            uriString = uri.toString(),
            durationMs = durationMs,
            width = width,
            height = height,
            fps = fps,
            fileSize = fileSize,
            mimeType = mimeType,
            rotation = rotation
        )
    }

    suspend fun extractThumbnail(context: Context, uri: Uri, atTimeMs: Long = 1000L): String = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val timeUs = atTimeMs * 1000
            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (bitmap != null) {
                val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                return@withContext Uri.fromFile(thumbFile).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
        return@withContext ""
    }
}
