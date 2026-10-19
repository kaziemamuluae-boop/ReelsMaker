package com.example.ui.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FrameStyle
import com.example.ui.common.TopHeaderBar
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkCrimson
import com.example.ui.theme.DarkGraySurface
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.LightBorder
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PremiumRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorViewModel

@Composable
fun LayersScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit,
    onNavigateToTextEdit: () -> Unit,
    onNavigateToLogoEdit: () -> Unit,
    onNavigateToFrameEdit: () -> Unit
) {
    val projectState by viewModel.projectState.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: LAYERS
        TopHeaderBar(
            title = "LAYERS",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Info
            item {
                Text(
                    text = "ACTIVE LAYER STACK (TOP TO BOTTOM)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // 1. Text Layers
            items(projectState.textLayers) { textLayer ->
                val isSelected = selectedLayerId == textLayer.id
                LayerRowItem(
                    title = "Text: \"${textLayer.text}\"",
                    subtitle = "Font: ${textLayer.fontPreset.fontName} • ${(textLayer.endMs - textLayer.startMs) / 1000}s",
                    icon = Icons.Default.TextFields,
                    isSelected = isSelected,
                    isVisible = textLayer.isVisible,
                    onSelect = {
                        viewModel.selectLayer(textLayer.id)
                        onNavigateToTextEdit()
                    },
                    onToggleVisibility = {
                        viewModel.updateTextLayer(textLayer.copy(isVisible = !textLayer.isVisible))
                    },
                    onDelete = {
                        viewModel.deleteTextLayer(textLayer.id)
                    }
                )
            }

            // 2. Logo Layers
            items(projectState.logoLayers) { logoLayer ->
                val isSelected = selectedLayerId == logoLayer.id
                LayerRowItem(
                    title = "Logo / Watermark",
                    subtitle = "Position: ${logoLayer.positionPreset.name} • Scale: ${logoLayer.scale}x",
                    icon = Icons.Default.PhotoLibrary,
                    isSelected = isSelected,
                    isVisible = logoLayer.isVisible,
                    onSelect = {
                        viewModel.selectLayer(logoLayer.id)
                        onNavigateToLogoEdit()
                    },
                    onToggleVisibility = {
                        viewModel.updateLogoLayer(logoLayer.copy(isVisible = !logoLayer.isVisible))
                    },
                    onDelete = {
                        viewModel.deleteLogoLayer(logoLayer.id)
                    }
                )
            }

            // 3. Frame Layer
            if (projectState.frameLayer.style != FrameStyle.NONE) {
                val frame = projectState.frameLayer
                item {
                    LayerRowItem(
                        title = "Video Frame: ${frame.style.name.replace("_", " ")}",
                        subtitle = "Thickness: ${frame.thicknessDp}dp • Radius: ${frame.cornerRadiusDp}dp",
                        icon = Icons.Default.ViewCarousel,
                        isSelected = false,
                        isVisible = frame.isVisible,
                        onSelect = onNavigateToFrameEdit,
                        onToggleVisibility = {
                            viewModel.updateFrameLayer(frame.copy(isVisible = !frame.isVisible))
                        },
                        onDelete = {
                            viewModel.updateFrameLayer(frame.copy(style = FrameStyle.NONE, isVisible = false))
                        }
                    )
                }
            }

            // 4. Base Video Layer (Always bottom)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LightBorder, PitchBlack)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCrimson),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Base Video Track",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val meta = projectState.videoMetadata
                            Text(
                                text = "${meta.width}x${meta.height} • ${meta.durationFormatted} • ${meta.fps.toInt()} FPS",
                                color = AccentGold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    isVisible: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = ElevatedCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isSelected) listOf(PremiumRed, DarkCrimson) else listOf(LightBorder, PitchBlack)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PremiumRed else DarkGraySurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else CrimsonRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = if (isVisible) TextPrimary else TextMuted
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Layer",
                    tint = PremiumRed
                )
            }
        }
    }
}
