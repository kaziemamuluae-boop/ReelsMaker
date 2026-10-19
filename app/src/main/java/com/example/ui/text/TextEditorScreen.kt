package com.example.ui.text

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FontPreset
import com.example.model.TextAnimationType
import com.example.model.TextLayer
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
fun TextEditorScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit
) {
    val projectState by viewModel.projectState.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()

    val activeLayer = projectState.textLayers.find { it.id == selectedLayerId }
        ?: projectState.textLayers.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: TEXT EDITOR
        TopHeaderBar(
            title = "TEXT EDITOR",
            onBackClick = onBackClick,
            actions = {
                IconButton(
                    onClick = { viewModel.addTextLayer("New Text") },
                    modifier = Modifier
                        .testTag("add_text_layer_button")
                        .size(36.dp)
                        .background(DarkCrimson, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Text Layer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        // Live Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f)
                .background(PitchBlack),
            contentAlignment = Alignment.Center
        ) {
            VideoCanvasPreview(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer Selector Pills if multiple layers exist
        if (projectState.textLayers.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedCard)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                projectState.textLayers.forEachIndexed { index, layer ->
                    val isSelected = layer.id == activeLayer?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PremiumRed else DarkGraySurface)
                            .clickable { viewModel.selectLayer(layer.id) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Layer ${index + 1}: ${layer.text.take(10)}",
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (activeLayer == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.addTextLayer("Add Reels Title") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ ADD TEXT LAYER", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Text Layer Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(ElevatedCard)
                    .border(0.5.dp, LightBorder)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Text Input Field
                OutlinedTextField(
                    value = activeLayer.text,
                    onValueChange = { newText ->
                        viewModel.updateTextLayer(activeLayer.copy(text = newText))
                    },
                    modifier = Modifier
                        .testTag("text_input_field")
                        .fillMaxWidth(),
                    label = { Text("Edit Text Overlay") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PremiumRed,
                        unfocusedBorderColor = LightBorder,
                        focusedLabelColor = CrimsonRed,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = PremiumRed
                    ),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.deleteTextLayer(activeLayer.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Text", tint = CrimsonRed)
                        }
                    }
                )

                // 1. Font Selection Presets
                Text(text = "FONT STYLE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontPreset.entries.forEach { font ->
                        val isSelected = activeLayer.fontPreset == font
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCrimson else DarkGraySurface)
                                .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateTextLayer(activeLayer.copy(fontPreset = font)) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = font.fontName,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 2. Formatting Toggles (Bold, Italic, Alignment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatToggleIcon(
                        icon = Icons.Default.FormatBold,
                        isActive = activeLayer.isBold,
                        onClick = { viewModel.updateTextLayer(activeLayer.copy(isBold = !activeLayer.isBold)) }
                    )

                    FormatToggleIcon(
                        icon = Icons.Default.FormatItalic,
                        isActive = activeLayer.isItalic,
                        onClick = { viewModel.updateTextLayer(activeLayer.copy(isItalic = !activeLayer.isItalic)) }
                    )

                    FormatToggleIcon(
                        icon = Icons.Default.FormatAlignLeft,
                        isActive = activeLayer.alignment == "LEFT",
                        onClick = { viewModel.updateTextLayer(activeLayer.copy(alignment = "LEFT")) }
                    )

                    FormatToggleIcon(
                        icon = Icons.Default.FormatAlignCenter,
                        isActive = activeLayer.alignment == "CENTER",
                        onClick = { viewModel.updateTextLayer(activeLayer.copy(alignment = "CENTER")) }
                    )

                    FormatToggleIcon(
                        icon = Icons.Default.FormatAlignRight,
                        isActive = activeLayer.alignment == "RIGHT",
                        onClick = { viewModel.updateTextLayer(activeLayer.copy(alignment = "RIGHT")) }
                    )
                }

                // 3. Font Size Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "FONT SIZE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${activeLayer.fontSizeSp.toInt()} sp", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = activeLayer.fontSizeSp,
                        onValueChange = { viewModel.updateTextLayer(activeLayer.copy(fontSizeSp = it)) },
                        valueRange = 14f..64f,
                        colors = SliderDefaults.colors(thumbColor = PremiumRed, activeTrackColor = PremiumRed, inactiveTrackColor = LightBorder)
                    )
                }

                // 4. Color Palette Presets
                Text(text = "TEXT COLOR", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val colors = listOf(
                        0xFFFFFFFF to "White",
                        0xFFFF2A4D to "Crimson",
                        0xFFFFD700 to "Gold",
                        0xFF00E5FF to "Cyan",
                        0xFF76FF03 to "Neon Green",
                        0xFFFF007F to "Hot Pink",
                        0xFF000000 to "Black"
                    )
                    colors.forEach { (argb, name) ->
                        val isSelected = activeLayer.fontColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else LightBorder, CircleShape)
                                .clickable { viewModel.updateTextLayer(activeLayer.copy(fontColorArgb = argb)) }
                        )
                    }
                }

                // 5. Outline / Stroke Toggle & Background Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Outline Toggle
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkGraySurface, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Outline", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = activeLayer.hasOutline,
                            onCheckedChange = { viewModel.updateTextLayer(activeLayer.copy(hasOutline = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PremiumRed)
                        )
                    }

                    // Background Box Toggle
                    val hasBg = activeLayer.backgroundColorArgb != 0x00000000L
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkGraySurface, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Backdrop", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = hasBg,
                            onCheckedChange = {
                                val newBg = if (it) 0xCC000000L else 0x00000000L
                                viewModel.updateTextLayer(activeLayer.copy(backgroundColorArgb = newBg))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PremiumRed)
                        )
                    }
                }

                // 6. Text Animation Presets
                Text(text = "TEXT ANIMATION", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextAnimationType.entries.forEach { anim ->
                        val isSelected = activeLayer.animationType == anim
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCrimson else DarkGraySurface)
                                .border(1.dp, if (isSelected) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateTextLayer(activeLayer.copy(animationType = anim)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = anim.name.replace("_", " "),
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 7. Timing / Duration Controls
                val durationMs = projectState.videoMetadata.durationMs.coerceAtLeast(1000L)
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("DISPLAY DURATION ON TIMELINE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${activeLayer.startMs / 1000}s → ${activeLayer.endMs / 1000}s", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = activeLayer.endMs.toFloat(),
                        onValueChange = {
                            val newEnd = it.toLong().coerceIn(activeLayer.startMs + 500L, durationMs)
                            viewModel.updateTextLayer(activeLayer.copy(endMs = newEnd))
                        },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = PremiumRed, activeTrackColor = PremiumRed, inactiveTrackColor = LightBorder)
                    )
                }

                // Apply Button
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .testTag("text_done_button")
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("APPLY TEXT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun FormatToggleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) DarkCrimson else DarkGraySurface)
            .border(1.dp, if (isActive) PremiumRed else LightBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.White else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
