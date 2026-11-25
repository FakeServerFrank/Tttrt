package com.carrusel.optimizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OptimizerScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var shz by remember { mutableStateOf(ShizukuHelper.state()) }
    LaunchedEffect(Unit) {
        while (true) {
            shz = ShizukuHelper.state()
            delay(1000)
        }
    }
    val games = remember { ShizukuHelper.findGames(ctx) }
    val titles = Optimizer.TITLES
    val states = remember {
        mutableStateListOf<StepState>().also { l -> titles.forEach { _ -> l.add(StepState.Pending) } }
    }
    var running by remember { mutableStateOf(false) }
    val done = states.count { it == StepState.Done }
    val pct = done * 100 / titles.size

    Column(Modifier.fillMaxSize().background(Palette.Bg)) {
        Text(
            "Carrusel",
            color = Palette.Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 10.dp)
        )
        TabRow(selectedTabIndex = 0, containerColor = Palette.Bg, contentColor = Palette.Ink) {
            Tab(
                selected = true,
                onClick = {},
                selectedContentColor = Palette.Ink,
                text = { Text("Optimización") }
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(8.dp).height(8.dp)
                        .background(if (shz == ShizukuState.Ready) Palette.Ink else Palette.Alert)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    when (shz) {
                        ShizukuState.Ready -> "Shizuku conectado"
                        ShizukuState.NeedsPermission -> "Shizuku necesita permiso"
                        ShizukuState.NotRunning -> "Shizuku no está activo"
                    },
                    color = Palette.Steel,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                "${games.size} juegos detectados",
                color = Palette.Steel,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Listo para juegos", color = Palette.Ink, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Text("$pct%", color = Palette.Ink, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(Palette.Dim)) {
                Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(Palette.Ink))
            }
            titles.forEachIndexed { i, title ->
                val st = states[i]
                val symbol = when (st) {
                    StepState.Pending -> "□"
                    StepState.Running -> "▣"
                    StepState.Done -> "■"
                    StepState.Failed -> "✕"
                }
                val color = when (st) {
                    StepState.Pending -> Palette.Steel
                    StepState.Failed -> Palette.Alert
                    else -> Palette.Ink
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(symbol, color = color, fontSize = 16.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(title, color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }
            }
            val label = when {
                running -> "Optimizando…"
                shz == ShizukuState.NotRunning -> "Abrir Shizuku"
                shz == ShizukuState.NeedsPermission -> "Conceder permiso"
                pct == 100 -> "Optimizado al 100%"
                else -> "Optimizar para juegos"
            }
            Button(
                onClick = {
                    when (shz) {
                        ShizukuState.NotRunning -> {
                            val intent = ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            if (intent != null) ctx.startActivity(intent)
                        }
                        ShizukuState.NeedsPermission -> {
                            try {
                                ShizukuHelper.requestPermission()
                            } catch (e: Throwable) {
                            }
                        }
                        ShizukuState.Ready -> {
                            if (!running) {
                                scope.launch {
                                    running = true
                                    for (i in states.indices) states[i] = StepState.Pending
                                    for (i in titles.indices) {
                                        states[i] = StepState.Running
                                        val ok = Optimizer.runStep(i, games)
                                        states[i] = if (ok) StepState.Done else StepState.Failed
                                    }
                                    running = false
                                }
                            }
                        }
                    }
                },
                enabled = !running,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.Ink,
                    contentColor = Palette.Bg,
                    disabledContainerColor = Palette.Dim,
                    disabledContentColor = Palette.Ink
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(label, fontWeight = FontWeight.Medium)
            }
        }
    }
}
