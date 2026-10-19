package com.example.ui.crop

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioPreset
import com.example.model.FitMode
import com.example.ui.common.TopHeaderBar
import com.example.ui.common.VideoCanvasPreview
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
fun CropScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit
) {
    val projectState by viewModel.projectState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: CROP VIDEO
        TopHeaderBar(
            title = "CROP VIDEO",
            onBackClick = onBackClick,
            actions = {
                IconButton(
                    onClick = {
                        viewModel.updateVideoTransform(0f, 0f, 1f, 0f)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Framing",
                        tint = TextSecondary
                    )
                }
            }
        )

        // Live Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(PitchBlack),
            contentAlignment = Alignment.Center
        ) {
            VideoCanvasPreview(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                enableDirectGestures = true
            )
        }

        // Controls Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ElevatedCard)
                .border(0.5.dp, LightBorder)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Aspect Ratio Selector
            Text(
                text = "CANVAS ASPECT RATIO",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioPreset.entries.forEach { preset ->
                    val isSelected = projectState.aspectRatio == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DarkCrimson else DarkGraySurface)
                            .border(1.5.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setAspectRatio(preset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.displayName,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Fit Mode Selection
            Text(
                text = "FIT / FILL MODE",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FitMode.entries.forEach { mode ->
                    val isSelected = projectState.fitMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DarkCrimson else DarkGraySurface)
                            .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setFitMode(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Smart Auto Reframe Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkGraySurface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CrimsonRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Auto Reframe",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-center subjects & faces in vertical frame",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = projectState.autoReframeActive,
                    onCheckedChange = { viewModel.toggleAutoReframe() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PremiumRed,
                        uncheckedTrackColor = DarkGraySurface
                    )
                )
            }

            // 4. Zoom Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ZOOM SCALE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = String.format("%.2fx", projectState.videoZoom), color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = projectState.videoZoom,
                    onValueChange = { newZoom ->
                        viewModel.updateVideoTransform(
                            panX = projectState.videoOffsetX,
                            panY = projectState.videoOffsetY,
                            zoom = newZoom,
                            rotation = projectState.videoRotation
                        )
                    },
                    valueRange = 0.5f..3.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = PremiumRed,
                        activeTrackColor = PremiumRed,
                        inactiveTrackColor = LightBorder
                    )
                )
            }

            // Done Button
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .testTag("crop_done_button")
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("APPLY CROP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
