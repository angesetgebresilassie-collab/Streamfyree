package com.streamfyree.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class YumaColorScheme(
    val glassBackground: Color,
    val glassBorder: Color,
    val cardBackgroundOpaque: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color
)

val YumaDarkPalette = YumaColorScheme(
    glassBackground = Color(0x1AFFFFFF), // 10% white for translucent glass
    glassBorder = Color(0x1FFFFFFF),     // 12% white subtle border
    cardBackgroundOpaque = Color(0xFF1C1C1E),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xA6F8FAFC),
    primaryAccent = Color(0xFF6366F1),   // Yuma Indigo
    secondaryAccent = Color(0xFFA855F7) // Yuma Purple
)

val LocalYumaColors = staticCompositionLocalOf { YumaDarkPalette }

private val DarkColorScheme = darkColorScheme(
    primary = YumaDarkPalette.primaryAccent,
    secondary = YumaDarkPalette.secondaryAccent,
    background = Color(0xFF090A0F),
    surface = Color(0xFF141622),
    onPrimary = Color.White,
    onBackground = YumaDarkPalette.textPrimary,
    onSurface = YumaDarkPalette.textPrimary
)

@Composable
fun YumaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalYumaColors provides YumaDarkPalette
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}

fun Modifier.glassCard(
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = YumaDarkPalette.glassBorder,
    backgroundColor: Color = YumaDarkPalette.glassBackground
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(
        width = borderWidth,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )
