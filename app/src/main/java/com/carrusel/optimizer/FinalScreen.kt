package com.carrusel.optimizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun FinalScreen(onOpen: () -> Unit, onReplay: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "spin")
    val ry by inf.animateFloat(
        initialValue = -32f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ry"
    )
    val rx by inf.animateFloat(
        initialValue = 10f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(3700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rx"
    )
    val pop = remember { Animatable(0.2f) }
    var showBtn by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(1700)
        showBtn = true
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f)
            },
            contentAlignment = Alignment.Center
        ) {
            val depth = 14
            for (d in depth downTo 0) {
                val f = d / depth.toFloat()
                val col: Color = if (d == 0) Palette.Ink else lerp(Palette.Droid2, Color(0xFF0E3B25), f)
                Text(
                    "On\ncarrusel",
                    color = col,
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        rotationY = ry
                        rotationX = rx
                        cameraDistance = 14f * density
                        translationX = -d * 2f * density * sin(ry * PI.toFloat() / 180f)
                        translationY = d * 1.4f * density * sin(rx * PI.toFloat() / 180f)
                    }
                )
            }
        }
        Spacer(Modifier.height(48.dp))
        AnimatedVisibility(visible = showBtn, enter = fadeIn(tween(500))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onOpen,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.Ink,
                        contentColor = Palette.Bg
                    )
                ) {
                    Text("ABRIR", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onReplay) {
                    Text("Ver de nuevo", color = Palette.Steel)
                }
            }
        }
    }
}
