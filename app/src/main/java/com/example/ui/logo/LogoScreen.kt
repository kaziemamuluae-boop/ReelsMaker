package com.example.ui.logo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.LogoPositionPreset
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorViewModel

@Composable
fun LogoScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit
) {
    val projectState by viewModel.projectState.collectAsState()
    val activeLogo = projectState.logoLayers.firstOrNull()

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addLogoLayer(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: LOGO / WATERMARK
        TopHeaderBar(
            title = "LOGO / WATERMARK",
            onBackClick = onBackClick,
            actions = {
                if (activeLogo != null) {
                    IconButton(onClick = { viewModel.deleteLogoLayer(activeLogo.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Logo", tint = PremiumRed)
                    }
                }
            }
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

        // Controls
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
            // Import / Replace Logo Button
            Button(
                onClick = { logoPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .testTag("choose_logo_button")
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (activeLogo == null) PremiumRed else DarkGraySurface)
            ) {
                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (activeLogo == null) "IMPORT PNG LOGO / WATERMARK" else "REPLACE LOGO IMAGE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (activeLogo != null) {
                // 1. Position Presets
                Text(text = "CORNER PLACEMENT", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        Triple(LogoPositionPreset.TOP_LEFT, 0.18f, 0.12f),
                        Triple(LogoPositionPreset.TOP_RIGHT, 0.82f, 0.12f),
                        Triple(LogoPositionPreset.BOTTOM_LEFT, 0.18f, 0.88f),
                        Triple(LogoPositionPreset.BOTTOM_RIGHT, 0.82f, 0.88f),
                        Triple(LogoPositionPreset.CENTER, 0.5f, 0.5f)
                    )

                    presets.forEach { (preset, x, y) ->
                        val isSelected = activeLogo.positionPreset == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCrimson else DarkGraySurface)
                                .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateLogoLayer(
                                        activeLogo.copy(
                                            positionPreset = preset,
                                            posXFraction = x,
                                            posYFraction = y
                                        )
                                    )
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.name.replace("_", " "),
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. Opacity Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "WATERMARK OPACITY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${(activeLogo.opacity * 100).toInt()}%", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = activeLogo.opacity,
                        onValueChange = { viewModel.updateLogoLayer(activeLogo.copy(opacity = it)) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = PremiumRed, activeTrackColor = PremiumRed, inactiveTrackColor = LightBorder)
                    )
                }

                // 3. Scale Size Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "LOGO SIZE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = String.format("%.1fx", activeLogo.scale), color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = activeLogo.scale,
                        onValueChange = { viewModel.updateLogoLayer(activeLogo.copy(scale = it)) },
                        valueRange = 0.2f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = PremiumRed, activeTrackColor = PremiumRed, inactiveTrackColor = LightBorder)
                    )
                }

                // 4. Apply Full Video Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkGraySurface, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Full Video Duration", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Show logo throughout the entire video", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = activeLogo.applyFullVideo,
                        onCheckedChange = { viewModel.updateLogoLayer(activeLogo.copy(applyFullVideo = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PremiumRed)
                    )
                }
            }

            // Apply Button
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .testTag("logo_done_button")
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("APPLY LOGO", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
