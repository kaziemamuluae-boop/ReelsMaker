package com.example.ui.frame

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FrameStyle
import com.example.ui.common.TopHeaderBar
import com.example.ui.common.VideoCanvasPreview
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DarkCrimson
import com.example.ui.theme.DarkGraySurface
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.LightBorder
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PremiumRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorViewModel

@Composable
fun FrameScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit
) {
    val projectState by viewModel.projectState.collectAsState()
    val frame = projectState.frameLayer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: VIDEO FRAME
        TopHeaderBar(
            title = "VIDEO FRAME",
            onBackClick = onBackClick
        )

        // Live Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.95f)
                .background(PitchBlack),
            contentAlignment = Alignment.Center
        ) {
            VideoCanvasPreview(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Frame Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.05f)
                .background(ElevatedCard)
                .border(0.5.dp, LightBorder)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Frame Style Presets
            Text(text = "FRAME STYLE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrameStyle.entries.forEach { style ->
                    val isSelected = frame.style == style
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DarkCrimson else DarkGraySurface)
                            .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.updateFrameLayer(
                                    frame.copy(
                                        style = style,
                                        isVisible = style != FrameStyle.NONE
                                    )
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = style.name.replace("_", " "),
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (frame.style != FrameStyle.NONE) {
                // 2. Thickness Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "BORDER THICKNESS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${frame.thicknessDp.toInt()} dp", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = frame.thicknessDp,
                        onValueChange = { viewModel.updateFrameLayer(frame.copy(thicknessDp = it)) },
                        valueRange = 2f..24f,
                        colors = SliderDefaults.colors(thumbColor = PremiumRed, activeTrackColor = PremiumRed, inactiveTrackColor = LightBorder)
                    )
                }

                // 3. Corner Radius Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "CORNER RADIUS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${frame.cornerRadiusDp.toInt()} dp", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = frame.cornerRadiusDp,
                        onValueChange = { viewModel.updateFrameLayer(frame.copy(cornerRadiusDp = it)) },
                        valueRange = 0f..40f,
                        colors = SliderDefaults.colors(thumbColor = PremiumRed, activeTrackColor = PremiumRed, inactiveTrackColor = LightBorder)
                    )
                }

                // 4. Color Presets
                Text(text = "PRIMARY FRAME COLOR", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val colors = listOf(
                        0xFFE50914L to "Premium Red",
                        0xFFFFD700L to "Gold",
                        0xFFFFFFFFL to "White",
                        0xFF00E5FFL to "Cyan",
                        0xFFFF007FL to "Neon Magenta",
                        0xFF76FF03L to "Lime",
                        0xFF000000L to "Black"
                    )
                    colors.forEach { (argb, name) ->
                        val isSelected = frame.primaryColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toULong()))
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else LightBorder, CircleShape)
                                .clickable { viewModel.updateFrameLayer(frame.copy(primaryColorArgb = argb)) }
                        )
                    }
                }
            }

            // Apply Button
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .testTag("frame_done_button")
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("APPLY FRAME", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
