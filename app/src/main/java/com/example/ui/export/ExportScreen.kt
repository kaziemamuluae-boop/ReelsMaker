package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.ExportOptions
import com.example.media.ExportProgress
import com.example.model.AspectRatioPreset
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
fun ExportScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit,
    onNavigateToExported: () -> Unit
) {
    val projectState by viewModel.projectState.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()

    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableIntStateOf(30) }
    var selectedQuality by remember { mutableStateOf("High") }

    val effectiveDurationMs = (projectState.trimEndMs - projectState.trimStartMs).coerceAtLeast(1000L)
    val durationSec = effectiveDurationMs / 1000

    // Calculate dimensions based on aspect ratio
    val (targetWidth, targetHeight) = when (projectState.aspectRatio) {
        AspectRatioPreset.RATIO_9_16 -> when (selectedResolution) {
            "480p" -> Pair(480, 854)
            "720p" -> Pair(720, 1280)
            "1440p" -> Pair(1440, 2560)
            "4K" -> Pair(2160, 3840)
            else -> Pair(1080, 1920)
        }
        AspectRatioPreset.RATIO_16_9 -> when (selectedResolution) {
            "480p" -> Pair(854, 480)
            "720p" -> Pair(1280, 720)
            "1440p" -> Pair(2560, 1440)
            "4K" -> Pair(3840, 2160)
            else -> Pair(1920, 1080)
        }
        AspectRatioPreset.RATIO_1_1 -> when (selectedResolution) {
            "480p" -> Pair(480, 480)
            "720p" -> Pair(720, 720)
            "1440p" -> Pair(1440, 1440)
            "4K" -> Pair(2160, 2160)
            else -> Pair(1080, 1080)
        }
        else -> Pair(1080, 1920)
    }

    val bitrate = when (selectedQuality) {
        "Low" -> 4_000_000
        "Medium" -> 8_000_000
        "High" -> 14_000_000
        else -> 20_000_000
    }

    val estimatedSizeBytes = (bitrate / 8L) * durationSec
    val estimatedSizeMb = String.format("%.1f MB", estimatedSizeBytes / (1024f * 1024f))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: EXPORT VIDEO
        TopHeaderBar(
            title = "EXPORT VIDEO",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isExporting) {
                // Active Export Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PremiumRed, DarkCrimson)))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val progressObj = exportProgress as? ExportProgress.Progress
                        val fraction = progressObj?.percentage ?: 0.1f
                        val msg = progressObj?.statusMessage ?: "Encoding MP4..."

                        Text(
                            text = "EXPORTING REEL",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = PremiumRed,
                            trackColor = PitchBlack
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${(fraction * 100).toInt()}%",
                            color = AccentGold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = msg,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { viewModel.cancelExport() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumRed)
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Export")
                        }
                    }
                }
            } else if (exportProgress is ExportProgress.Success) {
                // Success Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF))))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("EXPORT COMPLETED!", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Saved to Movies/Reels Maker directory", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToExported,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
                        ) {
                            Text("VIEW EXPORTED VIDEOS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Configuration Controls
                // 1. Resolution
                Text(text = "RESOLUTION", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("480p", "720p", "1080p", "1440p", "4K").forEach { res ->
                        val isSelected = selectedResolution == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCrimson else DarkGraySurface)
                                .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedResolution = res }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Framerate
                Text(text = "FRAMERATE (FPS)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(24, 30, 60).forEach { fps ->
                        val isSelected = selectedFps == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCrimson else DarkGraySurface)
                                .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedFps = fps }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$fps FPS",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Quality Preset
                Text(text = "ENCODING QUALITY", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low", "Medium", "High", "Ultra").forEach { q ->
                        val isSelected = selectedQuality == q
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCrimson else DarkGraySurface)
                                .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedQuality = q }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = q,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. Output Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LightBorder, PitchBlack)))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "EXPORT SPECIFICATION", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Output Dimensions", color = TextSecondary, fontSize = 12.sp)
                            Text("${targetWidth}x${targetHeight} (${projectState.aspectRatio.displayName})", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Clip Duration", color = TextSecondary, fontSize = 12.sp)
                            Text("${durationSec / 60}m ${durationSec % 60}s", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated File Size", color = TextSecondary, fontSize = 12.sp)
                            Text("~$estimatedSizeMb", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Save Location", color = TextSecondary, fontSize = 12.sp)
                            Text("Movies/Reels Maker", color = CrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Start Export Button
                Button(
                    onClick = {
                        viewModel.updateExportOptions(
                            ExportOptions(
                                resolutionName = selectedResolution,
                                targetWidth = targetWidth,
                                targetHeight = targetHeight,
                                fps = selectedFps,
                                quality = selectedQuality,
                                bitrate = bitrate
                            )
                        )
                        viewModel.startExport()
                    },
                    modifier = Modifier
                        .testTag("start_export_button")
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START VIDEO EXPORT", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}
