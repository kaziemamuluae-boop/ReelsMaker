package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProjectEditState
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
import kotlin.math.roundToInt

@Composable
fun TimelineComponent(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val projectState by viewModel.projectState.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val currentPositionMs by viewModel.playerManager.currentPositionMs.collectAsState()
    val durationMs = projectState.videoMetadata.durationMs.coerceAtLeast(1000L)

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkGraySurface)
            .padding(vertical = 6.dp)
    ) {
        // Player Playback Controls & Time Display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Time position string
            val currentSec = currentPositionMs / 1000
            val curMin = currentSec / 60
            val curRemSec = currentSec % 60
            val curMs = (currentPositionMs % 1000) / 100

            val totalSec = durationMs / 1000
            val totMin = totalSec / 60
            val totRemSec = totalSec % 60

            val formattedTime = String.format("%02d:%02d.%d / %02d:%02d", curMin, curRemSec, curMs, totMin, totRemSec)

            Text(
                text = formattedTime,
                color = AccentGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Playback buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.playerManager.fastSeek(-5000L) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 5s",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.playerManager.togglePlayPause() },
                    modifier = Modifier
                        .testTag("timeline_play_pause_button")
                        .size(40.dp)
                        .background(PremiumRed, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.playerManager.fastSeek(5000L) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 5s",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick Split at playhead & Zoom controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        // Trim to current playhead as new start or end
                        if (currentPositionMs > projectState.trimStartMs) {
                            viewModel.updateTrimRange(projectState.trimStartMs, currentPositionMs)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Cut At Playhead",
                        tint = CrimsonRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(3.0f) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Timeline Multi-track Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(105.dp)
                .background(PitchBlack)
                .border(0.5.dp, LightBorder)
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            val baseWidthPx = constraints.maxWidth.toFloat()
            val totalTimelineWidthPx = (baseWidthPx * zoomLevel).coerceAtLeast(baseWidthPx)

            Box(
                modifier = Modifier
                    .width((totalTimelineWidthPx / density).dp)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // TRACK 1: Video Track with Trim range and drag handles
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElevatedCard)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val clickMs = ((offset.x / totalTimelineWidthPx) * durationMs).toLong()
                                    viewModel.playerManager.seekTo(clickMs.coerceIn(0L, durationMs))
                                }
                            }
                    ) {
                        // Active Trimmed Segment Highlight
                        val startFraction = (projectState.trimStartMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        val endFraction = (projectState.trimEndMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        val trimWidthPx = (endFraction - startFraction) * totalTimelineWidthPx
                        val trimStartPx = startFraction * totalTimelineWidthPx

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(trimStartPx.roundToInt(), 0) }
                                .width((trimWidthPx / density).dp)
                                .fillMaxHeight()
                                .background(DarkCrimson.copy(alpha = 0.6f))
                                .border(1.5.dp, PremiumRed, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "VIDEO CLIP",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 6.dp)
                            )
                        }

                        // Start Trim Handle (Draggable)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(trimStartPx.roundToInt() - 10, 0) }
                                .width(20.dp)
                                .fillMaxHeight()
                                .background(PremiumRed, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        val deltaMs = ((dragAmount / totalTimelineWidthPx) * durationMs).toLong()
                                        val newStart = (projectState.trimStartMs + deltaMs).coerceIn(0L, projectState.trimEndMs - 500L)
                                        viewModel.updateTrimRange(newStart, projectState.trimEndMs)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.width(2.dp).height(14.dp).background(Color.White))
                        }

                        // End Trim Handle (Draggable)
                        val trimEndPx = endFraction * totalTimelineWidthPx
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(trimEndPx.roundToInt() - 10, 0) }
                                .width(20.dp)
                                .fillMaxHeight()
                                .background(PremiumRed, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        val deltaMs = ((dragAmount / totalTimelineWidthPx) * durationMs).toLong()
                                        val newEnd = (projectState.trimEndMs + deltaMs).coerceIn(projectState.trimStartMs + 500L, durationMs)
                                        viewModel.updateTrimRange(projectState.trimStartMs, newEnd)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.width(2.dp).height(14.dp).background(Color.White))
                        }
                    }

                    // TRACK 2: Text Layers Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeepBlack)
                    ) {
                        for (layer in projectState.textLayers) {
                            val startFrac = (layer.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                            val endFrac = (layer.endMs.toFloat() / durationMs).coerceIn(0f, 1f)
                            val leftPx = startFrac * totalTimelineWidthPx
                            val wPx = ((endFrac - startFrac) * totalTimelineWidthPx).coerceAtLeast(20f)

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(leftPx.roundToInt(), 0) }
                                    .width((wPx / density).dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF800020), RoundedCornerShape(4.dp))
                                    .border(1.dp, CrimsonRed, RoundedCornerShape(4.dp))
                                    .clickable { viewModel.selectLayer(layer.id) }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "T: ${layer.text}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // TRACK 3: Logo / Watermark Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeepBlack)
                    ) {
                        for (logo in projectState.logoLayers) {
                            val startFrac = if (logo.applyFullVideo) 0f else (logo.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                            val endFrac = if (logo.applyFullVideo) 1f else (logo.endMs.toFloat() / durationMs).coerceIn(0f, 1f)
                            val leftPx = startFrac * totalTimelineWidthPx
                            val wPx = ((endFrac - startFrac) * totalTimelineWidthPx).coerceAtLeast(20f)

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(leftPx.roundToInt(), 0) }
                                    .width((wPx / density).dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF331111), RoundedCornerShape(3.dp))
                                    .border(1.dp, Color(0xFFFF6B81), RoundedCornerShape(3.dp))
                                    .clickable { viewModel.selectLayer(logo.id) }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "LOGO WATERMARK",
                                    color = TextSecondary,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Global Playhead Vertical Red Needle
                val playheadFrac = (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                val playheadPx = playheadFrac * totalTimelineWidthPx

                Box(
                    modifier = Modifier
                        .offset { IntOffset(playheadPx.roundToInt() - 6, 0) }
                        .width(12.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val deltaMs = ((dragAmount / totalTimelineWidthPx) * durationMs).toLong()
                                val newPos = (currentPositionMs + deltaMs).coerceIn(0L, durationMs)
                                viewModel.playerManager.seekTo(newPos)
                            }
                        }
                ) {
                    // Needle Top Head
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(12.dp)
                            .background(PremiumRed, CircleShape)
                    )
                    // Needle Line
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(PremiumRed)
                    )
                }
            }
        }
    }
}
