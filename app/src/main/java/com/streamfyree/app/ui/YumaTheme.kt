package com.streamfyree.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object YumaColors {
    val Background = Color(0F, 0.04f, 0.07f, 1.0f) // #0A0B12
    val GlassCardBg = Color(0x99141622) // Translucent dark glass
    val GlassCardBorder = Color(0x33FFFFFF) // Subtle hairline glass border
    val PrimaryAccent = Color(0xFF6366F1) // Indigo/Violet YDS 2.1 primary
    val SecondaryAccent = Color(0xFFA855F7) // Purple accent
    val SurfaceElevated = Color(0xCC1E202E)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val GlassBar = Color(0xEE0D0E17)
    val CardGlow = Color(0x336366F1)
}

private val YumaDarkColorScheme = darkColorScheme(
    primary = YumaColors.PrimaryAccent,
    secondary = YumaColors.SecondaryAccent,
    background = YumaColors.Background,
    surface = Color(0xFF12141D),
    onPrimary = Color.White,
    onBackground = YumaColors.TextPrimary,
    onSurface = YumaColors.TextPrimary
)

@Composable
fun YumaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YumaDarkColorScheme,
        content = content
    )
}

fun Modifier.glassCard(
    cornerRadius: Dp = 22.dp,
    borderWidth: Dp = 0.5.dp,
    borderColor: Color = YumaColors.GlassCardBorder,
    backgroundColor: Color = YumaColors.GlassCardBg
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = (backgroundColor.alpha * 0.85f).coerceIn(0f, 1f))
            )
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            colors = listOf(
                borderColor,
                borderColor.copy(alpha = 0.1f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
