package com.example.ui.selection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.media.VideoMetadataExtractor
import com.example.model.VideoMetadata
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
import kotlinx.coroutines.launch

@Composable
fun VideoSelectionScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit,
    onVideoSelected: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var detectedMetadata by remember { mutableStateOf<VideoMetadata?>(null) }
    var detectedThumb by remember { mutableStateOf<String?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            coroutineScope.launch {
                detectedMetadata = VideoMetadataExtractor.extractMetadata(context, uri)
                detectedThumb = VideoMetadataExtractor.extractThumbnail(context, uri, 1000L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .navigationBarsPadding()
    ) {
        // Fixed Screen Title: SELECT VIDEO
        TopHeaderBar(
            title = "SELECT VIDEO",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Import Button
            Button(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier
                    .testTag("choose_video_button")
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
            ) {
                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("BROWSE GALLERY / FILES", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (detectedMetadata != null && selectedUri != null) {
                // Video Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElevatedCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PremiumRed, DarkCrimson)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (detectedThumb != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PitchBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = detectedThumb,
                                    contentDescription = "Selected Video Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text(
                            text = "DETECTED MEDIA SPECS",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val meta = detectedMetadata!!
                        val aspect = if (meta.height > 0) String.format("%.2f", meta.width.toFloat() / meta.height.toFloat()) else "Unknown"

                        MetadataRow(icon = Icons.Default.Schedule, label = "Duration", value = meta.durationFormatted)
                        MetadataRow(icon = Icons.Default.AspectRatio, label = "Resolution", value = "${meta.width} x ${meta.height} ($aspect)")
                        MetadataRow(icon = Icons.Default.Speed, label = "Framerate", value = "${meta.fps.toInt()} FPS")
                        MetadataRow(icon = Icons.Default.Storage, label = "File Size", value = meta.fileSizeFormatted)
                        MetadataRow(icon = Icons.Default.Movie, label = "MIME Format", value = meta.mimeType)
                    }
                }

                // Proceed Button
                Button(
                    onClick = {
                        selectedUri?.let { uri ->
                            viewModel.initializeNewProjectWithVideo(uri)
                            onVideoSelected()
                        }
                    },
                    modifier = Modifier
                        .testTag("proceed_to_editor_button")
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PROCEED TO EDITOR", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(DarkGraySurface, RoundedCornerShape(16.dp))
                        .border(1.dp, LightBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Select any MP4, MOV, WebM video from your device to automatically detect resolution, FPS, and start non-destructive editing.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = TextSecondary, fontSize = 12.sp)
        }
        Text(text = value, color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
