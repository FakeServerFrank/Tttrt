package com.carrusel.optimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class Phase { Intro, Final, App }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarruselApp() }
    }
}

@Composable
fun CarruselApp() {
    var phase by remember { mutableStateOf(Phase.Intro) }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Ink,
            onPrimary = Palette.Bg,
            background = Palette.Bg,
            surface = Palette.Bg,
            onSurface = Palette.Ink
        )
    ) {
        Box(Modifier.fillMaxSize().background(Palette.Bg)) {
            when (phase) {
                Phase.Intro -> IntroScreen(onBoomDone = { phase = Phase.Final })
                Phase.Final -> FinalScreen(
                    onOpen = { phase = Phase.App },
                    onReplay = { phase = Phase.Intro }
                )
                Phase.App -> OptimizerScreen()
            }
        }
    }
}
