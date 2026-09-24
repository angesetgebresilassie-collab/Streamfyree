package com.streamfyree.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    glassBackground = Color(0x1AFFFFFF),
    glassBorder = Color(0x1FFFFFFF),
    cardBackgroundOpaque = Color(0xFF1C1C1E),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xA6F8FAFC),
    primaryAccent = Color(0xFF1DB954),   // Spotify/Yuma Green
    secondaryAccent = Color(0xFF10B981)
)

val LocalYumaColors = staticCompositionLocalOf { YumaDarkPalette }

private val ProductSansFont = FontFamily.SansSerif

private val base = Typography()
val ProductSansTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = ProductSansFont),
    bodyMedium = base.bodyMedium.copy(fontFamily = ProductSansFont),
    bodySmall = base.bodySmall.copy(fontFamily = ProductSansFont),
    labelLarge = base.labelLarge.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = ProductSansFont, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = ProductSansFont)
)

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
            typography = ProductSansTypography,
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
