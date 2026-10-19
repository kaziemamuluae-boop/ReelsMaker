package com.example.ui.editor

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.model.AspectRatioPreset
import com.example.ui.common.TimelineComponent
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
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit,
    onNavigateToCrop: () -> Unit,
    onNavigateToText: () -> Unit,
    onNavigateToLogo: () -> Unit,
    onNavigateToFrame: () -> Unit,
    onNavigateToLayers: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val projectState by viewModel.projectState.collectAsState()
    val userMsg by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMsg) {
        userMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Screen Title: VIDEO EDITOR with Undo, Redo, Save, Export Actions
            TopHeaderBar(
                title = "VIDEO EDITOR",
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) TextPrimary else TextMuted
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) TextPrimary else TextMuted
                        )
                    }

                    IconButton(
                        onClick = { viewModel.saveCurrentProject() },
                        modifier = Modifier
                            .testTag("editor_save_project_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Project",
                            tint = AccentGold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onNavigateToExport,
                        modifier = Modifier
                            .testTag("editor_export_button")
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumRed),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )

            // Video Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(PitchBlack),
                contentAlignment = Alignment.Center
            ) {
                VideoCanvasPreview(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Quick Tool Bar (Horizontal scrolling tools)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedCard)
                    .border(0.5.dp, LightBorder)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolIconItem(
                    title = "CROP",
                    icon = Icons.Default.Crop,
                    onClick = onNavigateToCrop
                )

                ToolIconItem(
                    title = "TEXT",
                    icon = Icons.Default.TextFields,
                    onClick = onNavigateToText
                )

                ToolIconItem(
                    title = "LOGO",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = onNavigateToLogo
                )

                ToolIconItem(
                    title = "FRAME",
                    icon = Icons.Default.ViewCarousel,
                    onClick = onNavigateToFrame
                )

                ToolIconItem(
                    title = "LAYERS",
                    icon = Icons.Default.Layers,
                    onClick = onNavigateToLayers
                )

                ToolIconItem(
                    title = if (projectState.autoReframeActive) "REFRAME ON" else "SMART REFRAME",
                    icon = Icons.Default.AutoAwesome,
                    isActive = projectState.autoReframeActive,
                    onClick = { viewModel.toggleAutoReframe() }
                )
            }

            // Timeline Module
            TimelineComponent(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ToolIconItem(
    title: String,
    icon: ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) DarkCrimson else DarkGraySurface)
            .border(1.dp, if (isActive) PremiumRed else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isActive) Color.White else CrimsonRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = if (isActive) Color.White else TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
