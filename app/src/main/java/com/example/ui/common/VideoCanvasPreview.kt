package com.example.ui.common

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.AspectRatioPreset
import com.example.model.FontPreset
import com.example.model.FrameStyle
import com.example.model.LogoLayer
import com.example.model.LogoPositionPreset
import com.example.model.ProjectEditState
import com.example.model.TextLayer
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkGraySurface
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PremiumRed
import com.example.viewmodel.EditorViewModel
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun VideoCanvasPreview(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
    enableDirectGestures: Boolean = true,
    onLayerSelected: ((String?) -> Unit)? = null
) {
    val projectState by viewModel.projectState.collectAsState()
    val currentPositionMs by viewModel.playerManager.currentPositionMs.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()

    val context = LocalContext.current
    val player = remember { viewModel.playerManager.getPlayer() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PitchBlack),
        contentAlignment = Alignment.Center
    ) {
        // Aspect Ratio Box Container
        val ratio = projectState.aspectRatio.ratio
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(12.dp))
                .background(PitchBlack)
                .pointerInput(enableDirectGestures) {
                    if (enableDirectGestures) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            val currentPanX = projectState.videoOffsetX + (pan.x / 1000f)
                            val currentPanY = projectState.videoOffsetY + (pan.y / 1000f)
                            val currentZoom = (projectState.videoZoom * zoom).coerceIn(0.5f, 4f)
                            viewModel.updateVideoTransform(
                                panX = currentPanX,
                                panY = currentPanY,
                                zoom = currentZoom,
                                rotation = projectState.videoRotation + rotation
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()

            // 1. ExoPlayer Video Surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (projectState.videoOffsetX * canvasWidthPx).roundToInt(),
                            (projectState.videoOffsetY * canvasHeightPx).roundToInt()
                        )
                    }
                    .scale(projectState.videoZoom)
                    .rotate(projectState.videoRotation)
            )

            // 2. Video Frame Layer (Border / Letterbox / Glow)
            val frame = projectState.frameLayer
            if (frame.isVisible && frame.style != FrameStyle.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val strokePx = frame.thicknessDp * density
                            val cornerRadiusPx = frame.cornerRadiusDp * density
                            val primaryColor = Color(frame.primaryColorArgb.toULong()).copy(alpha = frame.opacity)
                            val secondaryColor = Color(frame.secondaryColorArgb.toULong()).copy(alpha = frame.opacity)

                            when (frame.style) {
                                FrameStyle.SIMPLE -> {
                                    drawRect(
                                        color = primaryColor,
                                        style = Stroke(width = strokePx)
                                    )
                                }
                                FrameStyle.ROUNDED -> {
                                    drawRoundRect(
                                        color = primaryColor,
                                        cornerRadius = CornerRadius(cornerRadiusPx),
                                        style = Stroke(width = strokePx)
                                    )
                                }
                                FrameStyle.DOUBLE_LINE -> {
                                    drawRoundRect(
                                        color = primaryColor,
                                        cornerRadius = CornerRadius(cornerRadiusPx),
                                        style = Stroke(width = strokePx)
                                    )
                                    drawRoundRect(
                                        color = secondaryColor,
                                        topLeft = Offset(strokePx + 4f, strokePx + 4f),
                                        size = Size(size.width - (strokePx * 2) - 8f, size.height - (strokePx * 2) - 8f),
                                        cornerRadius = CornerRadius((cornerRadiusPx - 4f).coerceAtLeast(0f)),
                                        style = Stroke(width = strokePx * 0.5f)
                                    )
                                }
                                FrameStyle.NEON_GLOW -> {
                                    drawRoundRect(
                                        color = primaryColor,
                                        cornerRadius = CornerRadius(cornerRadiusPx),
                                        style = Stroke(width = strokePx)
                                    )
                                }
                                FrameStyle.CINEMATIC_LETTERBOX -> {
                                    val letterboxH = size.height * 0.12f
                                    drawRect(color = Color.Black, topLeft = Offset.Zero, size = Size(size.width, letterboxH))
                                    drawRect(color = Color.Black, topLeft = Offset(0f, size.height - letterboxH), size = Size(size.width, letterboxH))
                                }
                                FrameStyle.GRADIENT_BORDER -> {
                                    drawRoundRect(
                                        brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
                                        cornerRadius = CornerRadius(cornerRadiusPx),
                                        style = Stroke(width = strokePx)
                                    )
                                }
                                else -> {}
                            }
                        }
                )
            }

            // 3. Logo Layers
            for (logo in projectState.logoLayers) {
                val isLogoActive = logo.applyFullVideo || (currentPositionMs >= logo.startMs && currentPositionMs <= logo.endMs)
                if (logo.isVisible && isLogoActive && logo.imageUriString.isNotEmpty()) {
                    InteractiveLogoItem(
                        logo = logo,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                        isSelected = selectedLayerId == logo.id,
                        onSelect = {
                            viewModel.selectLayer(logo.id)
                            onLayerSelected?.invoke(logo.id)
                        },
                        onUpdate = { updated ->
                            viewModel.updateLogoLayer(updated)
                        },
                        onDelete = {
                            viewModel.deleteLogoLayer(logo.id)
                        }
                    )
                }
            }

            // 4. Text Layers
            for (textLayer in projectState.textLayers) {
                val isTextActive = currentPositionMs >= textLayer.startMs && currentPositionMs <= textLayer.endMs
                if (textLayer.isVisible && isTextActive) {
                    InteractiveTextItem(
                        layer = textLayer,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                        isSelected = selectedLayerId == textLayer.id,
                        onSelect = {
                            viewModel.selectLayer(textLayer.id)
                            onLayerSelected?.invoke(textLayer.id)
                        },
                        onUpdate = { updated ->
                            viewModel.updateTextLayer(updated)
                        },
                        onDelete = {
                            viewModel.deleteTextLayer(textLayer.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveTextItem(
    layer: TextLayer,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (TextLayer) -> Unit,
    onDelete: () -> Unit
) {
    val textColor = Color(layer.fontColorArgb.toULong()).copy(alpha = layer.opacity)
    val bgColor = Color(layer.backgroundColorArgb.toULong())

    val fontFamily = when (layer.fontPreset) {
        FontPreset.SERIF_CINEMATIC -> FontFamily.Serif
        FontPreset.MONOSPACE_MODERN -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val fontWeight = if (layer.isBold) FontWeight.Bold else FontWeight.Normal
    val fontStyle = if (layer.isItalic) FontStyle.Italic else FontStyle.Normal

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (layer.posXFraction * canvasWidthPx - 100).roundToInt(),
                    (layer.posYFraction * canvasHeightPx - 30).roundToInt()
                )
            }
            .rotate(layer.rotationDeg)
            .scale(layer.scale)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    val newPosX = (layer.posXFraction + (pan.x / canvasWidthPx)).coerceIn(0.05f, 0.95f)
                    val newPosY = (layer.posYFraction + (pan.y / canvasHeightPx)).coerceIn(0.05f, 0.95f)
                    val newScale = (layer.scale * zoom).coerceIn(0.4f, 3.0f)
                    val newRot = layer.rotationDeg + rotation
                    onUpdate(
                        layer.copy(
                            posXFraction = newPosX,
                            posYFraction = newPosY,
                            scale = newScale,
                            rotationDeg = newRot
                        )
                    )
                }
            }
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, PremiumRed, RoundedCornerShape(8.dp)).padding(4.dp)
                } else {
                    Modifier.padding(4.dp)
                }
            )
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = layer.text,
            color = textColor,
            fontSize = layer.fontSizeSp.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(22.dp)
                    .background(PremiumRed, CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Layer",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractiveLogoItem(
    logo: LogoLayer,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (LogoLayer) -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (logo.posXFraction * canvasWidthPx - 50).roundToInt(),
                    (logo.posYFraction * canvasHeightPx - 50).roundToInt()
                )
            }
            .rotate(logo.rotationDeg)
            .scale(logo.scale)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    val newPosX = (logo.posXFraction + (pan.x / canvasWidthPx)).coerceIn(0.05f, 0.95f)
                    val newPosY = (logo.posYFraction + (pan.y / canvasHeightPx)).coerceIn(0.05f, 0.95f)
                    val newScale = (logo.scale * zoom).coerceIn(0.3f, 2.5f)
                    val newRot = logo.rotationDeg + rotation
                    onUpdate(
                        logo.copy(
                            posXFraction = newPosX,
                            posYFraction = newPosY,
                            scale = newScale,
                            rotationDeg = newRot,
                            positionPreset = LogoPositionPreset.CUSTOM
                        )
                    )
                }
            }
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, PremiumRed, RoundedCornerShape(8.dp)).padding(4.dp)
                } else {
                    Modifier.padding(4.dp)
                }
            )
            .size(90.dp)
    ) {
        AsyncImage(
            model = logo.imageUriString,
            contentDescription = "Logo Layer",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            alpha = logo.opacity
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .background(PremiumRed, CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Logo",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
