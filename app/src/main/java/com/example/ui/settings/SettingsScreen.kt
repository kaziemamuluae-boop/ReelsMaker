package com.example.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SettingsScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val cacheStatus = remember { mutableStateOf("Cleaned") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: SETTINGS
        TopHeaderBar(
            title = "SETTINGS",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Defaults
            Text(text = "EXPORT PREFERENCES", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LightBorder, PitchBlack)))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingItemRow(
                        icon = Icons.Default.VideoSettings,
                        title = "Default Export Resolution",
                        value = "1080p (Full HD)"
                    )

                    SettingItemRow(
                        icon = Icons.Default.Speed,
                        title = "Default Framerate",
                        value = "30 FPS"
                    )

                    SettingItemRow(
                        icon = Icons.Default.Folder,
                        title = "Export Destination Folder",
                        value = "Movies/Reels Maker"
                    )
                }
            }

            // System Performance & Hardware
            Text(text = "PERFORMANCE & ENGINE", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LightBorder, PitchBlack)))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingItemRow(
                        icon = Icons.Default.Shield,
                        title = "Hardware Video Acceleration",
                        value = "MediaCodec AVC / HEVC (Active)"
                    )

                    SettingItemRow(
                        icon = Icons.Default.Palette,
                        title = "Visual Theme",
                        value = "Premium Red + Deep Black"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.cacheDir.deleteRecursively()
                                    cacheStatus.value = "Cache Cleared (0 MB)"
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Clear Temporary Cache", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Frees up temporary frame files", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        Text(cacheStatus.value, color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // App Information
            Text(text = "ABOUT REELS MAKER", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(LightBorder, PitchBlack)))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PremiumRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Reels Maker", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Text("Version 1.0.0 (Build 2026)", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Professional Android Video Editor & Smart Reels Creator. Engineered with Jetpack Compose, AndroidX Media3 ExoPlayer, and Hardware Acceleration.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingItemRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = value, color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
