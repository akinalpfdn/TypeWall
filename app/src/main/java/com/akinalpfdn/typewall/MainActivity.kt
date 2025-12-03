package com.akinalpfdn.typewall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.akinalpfdn.typewall.data.ThemePreferences
import com.akinalpfdn.typewall.ui.screens.CanvasScreen
import com.akinalpfdn.typewall.ui.theme.TypeWallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences = remember { ThemePreferences(this) }

            TypeWallTheme(themePreferences = themePreferences) {
                CanvasScreen()
            }
        }
    }
}