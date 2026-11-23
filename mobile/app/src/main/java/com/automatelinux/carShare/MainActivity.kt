package com.automatelinux.carShare

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Thin Android launcher — all UI lives in the shared commonMain App() composable.
class MainActivity : ComponentActivity() {
    private val rubik = FontFamily(
        Font(R.font.rubik_400, FontWeight.Normal),
        Font(R.font.rubik_500, FontWeight.Medium),
        Font(R.font.rubik_600, FontWeight.SemiBold),
        Font(R.font.rubik_700, FontWeight.Bold),
        Font(R.font.rubik_800, FontWeight.ExtraBold),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark whatever the phone's day/night setting, so force light bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            App(font = rubik)
        }
    }
}
