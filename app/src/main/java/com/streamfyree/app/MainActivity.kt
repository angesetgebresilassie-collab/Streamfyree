package com.streamfyree.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streamfyree.app.ui.DebugLogOverlay
import com.streamfyree.app.ui.StreamfyreeScreen
import com.streamfyree.app.ui.YumaTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val colors = darkColorScheme(
            primary = Color(0xFFD8BEA6),
            onPrimary = Color(0xFF15110E),
            secondary = Color(0xFFCEB49D),
            onSecondary = Color(0xFF17120F),
            tertiary = Color(0xFFE8D6C7),
            background = Color(0xFF090807),
            surface = Color(0xFF12100F),
            surfaceVariant = Color(0xFF1B1714),
            onBackground = Color(0xFFF1E9E1),
            onSurface = Color(0xFFF1E9E1),
            onSurfaceVariant = Color(0xFFB9ADA3)
        )

        val base = Typography()
        val typography = base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
            headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
            headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
            headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
            titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
            titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
            labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
        )

        setContent {
            YumaTheme {
                val vm: MusicViewModel = viewModel()
                Box {
                    StreamfyreeScreen(vm)
                    DebugLogOverlay(vm)
                }
            }
        }
    }
}
