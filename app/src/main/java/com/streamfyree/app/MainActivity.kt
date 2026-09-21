package com.streamfyree.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.streamfyree.app.ui.StreamfyreeScreen

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary=Color(0xFFE0A662), onPrimary=Color(0xFF0A0705),
                background=Color(0xFF0A0705), surface=Color(0xFF1C1611),
                onBackground=Color(0xFFF3EAE1), onSurface=Color(0xFFF3EAE1)
            )) {
                StreamfyreeScreen(viewModel())
            }
        }
    }
}
