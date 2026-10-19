package com.example.model

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class AspectRatioPreset(val displayName: String, val ratio: Float, val widthRatio: Int, val heightRatio: Int) {
    RATIO_9_16("9:16 (Reels/TikTok)", 9f / 16f, 9, 16),
    RATIO_16_9("16:9 (Landscape)", 16f / 9f, 16, 9),
    RATIO_1_1("1:1 (Square)", 1f, 1, 1),
    RATIO_4_5("4:5 (Portrait)", 4f / 5f, 4, 5),
    RATIO_4_3("4:3 (Standard)", 4f / 3f, 4, 3),
    RATIO_21_9("21:9 (Cinematic)", 21f / 9f, 21, 9),
    CUSTOM("Custom", 1f, 0, 0);

    companion object {
        fun fromName(name: String): AspectRatioPreset {
            return entries.find { it.name == name } ?: RATIO_9_16
        }
    }
}

enum class FitMode {
    FIT, FILL, CROP, ZOOM
}

enum class TextAnimationType {
    NONE, FADE_IN, FADE_OUT, SLIDE, ZOOM, POP, TYPEWRITER, BOUNCE
}

enum class FontPreset(val fontName: String) {
    SYSTEM_BOLD("Bold Display"),
    SYSTEM_REGULAR("Clean Sans"),
    SYSTEM_LIGHT("Minimal Light"),
    SERIF_CINEMATIC("Cinematic Serif"),
    MONOSPACE_MODERN("Modern Mono"),
    HANDWRITTEN("Handwritten Style"),
    BENGALI_FRIENDLY("Bengali / Unicode Friendly")
}

data class VideoMetadata(
    val uriString: String = "",
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 30f,
    val fileSize: Long = 0L,
    val mimeType: String = "video/mp4",
    val rotation: Int = 0
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (durationMs % 1000) / 100
            return String.format("%02d:%02d.%d", minutes, seconds, millis)
        }

    val fileSizeFormatted: String
        get() {
            return when {
                fileSize >= 1024 * 1024 -> String.format("%.1f MB", fileSize.toDouble() / (1024 * 1024))
                fileSize >= 1024 -> String.format("%.1f KB", fileSize.toDouble() / 1024)
                else -> "$fileSize B"
            }
        }
}

data class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Double tap to edit",
    val fontPreset: FontPreset = FontPreset.SYSTEM_BOLD,
    val fontSizeSp: Float = 26f,
    val fontColorArgb: Long = 0xFFFFFFFF,
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val backgroundColorArgb: Long = 0x00000000,
    val outlineColorArgb: Long = 0xFF000000,
    val outlineWidth: Float = 2f,
    val hasOutline: Boolean = true,
    val shadowColorArgb: Long = 0x88000000,
    val shadowRadius: Float = 6f,
    val opacity: Float = 1.0f,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1.2f,
    val alignment: String = "CENTER", // LEFT, CENTER, RIGHT
    val animationType: TextAnimationType = TextAnimationType.POP,
    val animationDurationMs: Long = 600L,
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val posXFraction: Float = 0.5f, // 0.0 to 1.0 relative to canvas
    val posYFraction: Float = 0.5f,
    val rotationDeg: Float = 0f,
    val scale: Float = 1.0f,
    val isVisible: Boolean = true
)

enum class LogoPositionPreset {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, CUSTOM
}

data class LogoLayer(
    val id: String = UUID.randomUUID().toString(),
    val imageUriString: String = "",
    val positionPreset: LogoPositionPreset = LogoPositionPreset.TOP_RIGHT,
    val posXFraction: Float = 0.82f,
    val posYFraction: Float = 0.12f,
    val scale: Float = 0.6f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 0.95f,
    val marginDp: Float = 16f,
    val applyFullVideo: Boolean = true,
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val isVisible: Boolean = true
)

enum class FrameStyle {
    NONE,
    SIMPLE,
    ROUNDED,
    DOUBLE_LINE,
    NEON_GLOW,
    CINEMATIC_LETTERBOX,
    GRADIENT_BORDER,
    DECORATIVE_RETRO,
    MINIMAL_SHARP,
    CUSTOM_PNG
}

data class FrameLayer(
    val style: FrameStyle = FrameStyle.NONE,
    val thicknessDp: Float = 8f,
    val primaryColorArgb: Long = 0xFFE50914,
    val secondaryColorArgb: Long = 0xFFFF2A4D,
    val cornerRadiusDp: Float = 16f,
    val opacity: Float = 1.0f,
    val hasGlow: Boolean = true,
    val customImageUri: String = "",
    val isVisible: Boolean = true
)

data class ClipSegment(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val isSelected: Boolean = true
)

data class ProjectEditState(
    val projectId: String = UUID.randomUUID().toString(),
    val projectName: String = "Reels Project",
    val originalVideoUri: String = "",
    val videoMetadata: VideoMetadata = VideoMetadata(),
    // Trim
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    // Auto Split segments
    val autoSplitIntervalSec: Int = 0, // 0 = disabled
    val keepIncompleteSegment: Boolean = true,
    val splitSegments: List<ClipSegment> = emptyList(),
    // Canvas & Aspect Ratio
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    val fitMode: FitMode = FitMode.FIT,
    val videoOffsetX: Float = 0f, // pan
    val videoOffsetY: Float = 0f,
    val videoZoom: Float = 1.0f,
    val videoRotation: Float = 0f,
    // Auto Reframe Face/Subject
    val autoReframeActive: Boolean = false,
    val smartSubjectCenterX: Float = 0.5f,
    // Layers
    val textLayers: List<TextLayer> = emptyList(),
    val logoLayers: List<LogoLayer> = emptyList(),
    val frameLayer: FrameLayer = FrameLayer(),
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastEditedAt: Long = System.currentTimeMillis(),
    val thumbnailUri: String = ""
)
