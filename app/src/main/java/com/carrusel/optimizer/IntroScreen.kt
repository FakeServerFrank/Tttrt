package com.carrusel.optimizer

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val DUR = 11000f
private val STAGES = listOf(
    1 to "Ensamblando agente",
    10 to "Desplegando objetivo",
    24 to "Preparando inyección",
    36 to "Evadiendo cortafuegos",
    50 to "Inyectando payload",
    66 to "Reescribiendo el kernel",
    82 to "Borrando huellas",
    95 to "Compilando interfaz",
    100 to "Acceso concedido"
)
private val FLICK = floatArrayOf(0f, 1f, .15f, 1f, .4f, 1f)
private val DYS = floatArrayOf(-16f, 10f, -4f, 22f, -26f, 4f)
private val SCREEN = Color(0xFF020504)
private val EYE = Color(0xFFEAFFF3)
private val PCOLORS = listOf(
    Color(0xFF5DFF9B), Color(0xFF3DDC84), Color(0xFFFF3D81), Color(0xFFEAFFF3), Color(0xFF8FA3AD)
)
private val MONO: Typeface = Typeface.MONOSPACE
private val MONO_B: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

private class Particle(val vx: Float, val vy: Float, val s: Float, val c: Int)

private fun Color.a(x: Float): Color = copy(alpha = (alpha * x).coerceIn(0f, 1f))
private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun easeIO(x: Float): Float {
    if (x < .5f) return 2f * x * x
    val k = -2f * x + 2f
    return 1f - k * k / 2f
}

@Composable
fun IntroScreen(onBoomDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    val boom = remember { Animatable(0f) }
    var boomStarted by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val paint = remember { android.graphics.Paint().apply { isAntiAlias = true } }
    val particles = remember {
        val rnd = Random(7)
        List(160) {
            val ang = rnd.nextFloat() * 6.283f
            val v = 2f + rnd.nextFloat() * 9f
            Particle(cos(ang) * v, sin(ang) * v - 2f, 2f + rnd.nextFloat() * 6f, it % 5)
        }
    }

    LaunchedEffect(Unit) {
        clock.animateTo(DUR, tween(DUR.toInt(), easing = LinearEasing))
    }
    LaunchedEffect(boomStarted) {
        if (boomStarted) {
            boom.animateTo(1f, tween(900))
            onBoomDone()
        }
    }

    val e = clock.value
    val b = boom.value
    val t = e / DUR
    val p = (t + 0.02f * sin(t * PI.toFloat() * 8f)).coerceIn(0f, 1f)
    val q = ((p - .34f) / .66f).coerceIn(0f, 1f)
    val pct = max(1, (p * 100f).roundToInt())
    val ready = e >= DUR

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth().alpha(1f - b)) {
            Text(
                "objetivo-7 / puerto 5555",
                color = Palette.Steel,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Text("● en vivo", color = Palette.Alert, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { canvasSize = it }
                .pointerInput(ready, canvasSize) {
                    detectTapGestures(onTap = { off ->
                        if (ready && !boomStarted && canvasSize.width > 0) {
                            val s = min(canvasSize.width / 340f, canvasSize.height / 280f)
                            val ox = (canvasSize.width - 340f * s) / 2f
                            val oy = (canvasSize.height - 280f * s) / 2f
                            val vx = (off.x - ox) / s
                            val vy = (off.y - oy) / s
                            if (vx in 225f..337f && vy in 160f..226f) boomStarted = true
                        }
                    })
                }
        ) {
            val s = min(size.width / 340f, size.height / 280f)
            withTransform({
                translate((size.width - 340f * s) / 2f, (size.height - 280f * s) / 2f)
                scale(s, s, Offset.Zero)
            }) {
                drawScene(e, q, b, ready, paint, particles)
            }
        }
        Column(Modifier.alpha(1f - b)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$pct",
                    color = if (ready) Palette.Alert else Palette.Ink,
                    fontSize = 88.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text("%", color = Palette.Steel, fontSize = 36.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).background(Palette.Dim)) {
                Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(Palette.Ink))
            }
            Spacer(Modifier.height(12.dp))
            val shown = STAGES.filter { pct >= it.first }.takeLast(4)
            Column(Modifier.height(86.dp)) {
                shown.forEachIndexed { i, st ->
                    Text(
                        "> ${st.second}",
                        color = if (i == shown.lastIndex) Palette.Ink else Palette.Dim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun DrawScope.piece(
    e: Float, b: Float, tx: Float, ty: Float, rot: Float, delay: Float, cx: Float, cy: Float,
    block: DrawScope.(Float) -> Unit
) {
    val k = ((e - 300f - delay) / 1200f).coerceIn(0f, 1f)
    val ease = 1f - (1f - k) * (1f - k) * (1f - k)
    var dx = tx * (1f - ease)
    var dy = ty * (1f - ease)
    var r = rot * (1f - ease)
    var al = 0.7f + 0.3f * ease
    if (b > 0f) {
        dx = -tx * 4f * b
        dy = -ty * 4f * b
        r = rot * 5f * b
        al = 1f - b
    }
    if (al <= 0.01f) return
    withTransform({
        translate(dx, dy)
        rotate(r, Offset(cx, cy))
    }) {
        block(al)
    }
}

private fun DrawScope.drawSyringe(dx: Float, dy: Float, q: Float, al: Float) {
    val hx = 140f + 36f * q
    withTransform({ translate(dx, dy) }) {
        val fx = hx + 33f
        if (210f - fx > 0f) {
            drawRect(Palette.Ink.a(.55f * al), Offset(fx, 143f), Size(210f - fx, 14f))
        }
        drawRect(color = Palette.Ink.a(al), topLeft = Offset(172f, 142f), size = Size(38f, 16f), style = Stroke(1.5f))
        drawLine(Palette.Steel.a(al), Offset(210f, 150f), Offset(230f, 150f), 2f)
        drawLine(Palette.Steel.a(al), Offset(hx, 150f), Offset(hx + 30f, 150f), 3f)
        drawRect(Palette.Steel.a(al), Offset(hx - 2f, 141f), Size(4f, 18f))
        drawRect(Palette.Ink.a(al), Offset(hx + 30f, 143f), Size(3f, 14f))
    }
}

private fun DrawScope.drawPhone(a: Float, e: Float, q: Float, ready: Boolean, paint: android.graphics.Paint) {
    drawRoundRect(
        color = Palette.Panel.a(a), topLeft = Offset(232f, 48f),
        size = Size(98f, 204f), cornerRadius = CornerRadius(16f)
    )
    drawRoundRect(
        color = Palette.Steel.a(a), topLeft = Offset(232f, 48f),
        size = Size(98f, 204f), cornerRadius = CornerRadius(16f), style = Stroke(1.5f)
    )
    drawRoundRect(
        color = SCREEN.a(a), topLeft = Offset(239f, 62f),
        size = Size(84f, 176f), cornerRadius = CornerRadius(6f)
    )
    clipRect(239f, 62f, 323f, 238f) {
        val h = 176f * q
        val top = 238f - h
        drawRect(Palette.Ink.a(.2f * a), Offset(239f, top), Size(84f, h))
        paint.textSize = 8f
        paint.typeface = MONO
        paint.textAlign = android.graphics.Paint.Align.LEFT
        val seed = (e / 130f).toInt()
        for (i in 0 until 14) {
            val y = 74f + i * 12f
            val rnd = java.util.Random((seed * 31 + i * 7919).toLong())
            val sb = StringBuilder("0x")
            repeat(14) { sb.append("0123456789abcdef"[rnd.nextInt(16)]) }
            paint.color = (if (y >= top) Palette.Ink else Palette.Dim).a(a).toArgb()
            drawIntoCanvas { c -> c.nativeCanvas.drawText(sb.toString(), 243f, y, paint) }
        }
        if (ready) {
            drawRect(SCREEN.a(.85f * a), Offset(239f, 62f), Size(84f, 176f))
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.typeface = MONO_B
            paint.textSize = 34f
            paint.color = Palette.Alert.a(a).toArgb()
            drawIntoCanvas { c -> c.nativeCanvas.drawText("ROOT", 281f, 124f, paint) }
            paint.typeface = MONO
            paint.textSize = 9f
            paint.color = Palette.Ink.a(a).toArgb()
            drawIntoCanvas { c -> c.nativeCanvas.drawText("acceso total", 281f, 142f, paint) }
            val pulse = if (((e / 500f).toInt() % 2) == 0) 1f else .6f
            drawRoundRect(
                color = Palette.Ink.a(a * pulse), topLeft = Offset(243f, 182f),
                size = Size(76f, 24f), cornerRadius = CornerRadius(4f)
            )
            paint.typeface = MONO_B
            paint.color = Palette.Bg.a(a).toArgb()
            drawIntoCanvas { c -> c.nativeCanvas.drawText("Disporcionar", 281f, 197.5f, paint) }
        }
    }
    drawRoundRect(
        color = Palette.Dim.a(a), topLeft = Offset(266f, 52f),
        size = Size(28f, 4f), cornerRadius = CornerRadius(2f)
    )
}

private fun DrawScope.drawScene(
    e: Float, q: Float, b: Float, ready: Boolean,
    paint: android.graphics.Paint, parts: List<Particle>
) {
    val pull = ((e - 2000f) / 1700f).coerceIn(0f, 1f)
    val u = easeIO((pull / .4f).coerceIn(0f, 1f))
    val w = easeIO(((pull - .4f) / .6f).coerceIn(0f, 1f))
    val rx = 132f + 36f * q
    val hx = if (pull <= 0f) 122f else if (pull < .4f) lerpF(122f, 118f, u) else lerpF(118f, rx, w)
    val hy = if (pull <= 0f) 160f else if (pull < .4f) lerpF(160f, 180f, u) else lerpF(180f, 150f, w)

    val sf = (e - 1700f) / 700f
    val pa = if (e < 1700f) 0f else if (sf >= 1f) 1f else FLICK[(sf * 6f).toInt().coerceIn(0, 5)]
    if (pa > 0f && b < 1f) {
        val sc = 1f + 0.5f * b
        withTransform({ scale(sc, sc, Offset(281f, 150f)) }) {
            drawPhone(pa * (1f - b), e, q, ready, paint)
        }
    }

    piece(e, b, -50f, -70f, -40f, 100f, 75f, 82f) { al ->
        val path = Path().apply {
            moveTo(39f, 100f)
            arcTo(Rect(39f, 64f, 111f, 136f), 180f, 180f, false)
            close()
        }
        drawPath(path, Palette.Droid.a(al))
    }
    piece(e, b, -30f, -90f, 70f, 350f, 55f, 64f) { al ->
        drawLine(Palette.Droid.a(al), Offset(59f, 72f), Offset(52f, 57f), 3f, StrokeCap.Round)
    }
    piece(e, b, 70f, -60f, -50f, 250f, 95f, 64f) { al ->
        drawLine(Palette.Droid.a(al), Offset(91f, 72f), Offset(98f, 57f), 3f, StrokeCap.Round)
    }
    piece(e, b, -80f, 40f, 0f, 500f, 62f, 88f) { al ->
        drawCircle(EYE.a(al), 3.5f, Offset(62f, 88f))
    }
    piece(e, b, 20f, -110f, 0f, 450f, 88f, 88f) { al ->
        drawCircle(EYE.a(al), 3.5f, Offset(88f, 88f))
    }
    piece(e, b, -50f, 60f, 25f, 0f, 75f, 139f) { al ->
        drawRoundRect(
            color = Palette.Droid.a(al), topLeft = Offset(39f, 105f),
            size = Size(72f, 68f), cornerRadius = CornerRadius(9f)
        )
    }
    piece(e, b, -70f, -40f, -80f, 200f, 28f, 132f) { al ->
        drawRoundRect(
            color = Palette.Droid.a(al), topLeft = Offset(21f, 107f),
            size = Size(14f, 50f), cornerRadius = CornerRadius(7f)
        )
    }
    piece(e, b, -20f, 110f, 50f, 300f, 60f, 183f) { al ->
        drawRoundRect(
            color = Palette.Droid.a(al), topLeft = Offset(53f, 165f),
            size = Size(14f, 36f), cornerRadius = CornerRadius(7f)
        )
    }
    piece(e, b, 60f, 90f, -40f, 400f, 90f, 183f) { al ->
        drawRoundRect(
            color = Palette.Droid.a(al), topLeft = Offset(83f, 165f),
            size = Size(14f, 36f), cornerRadius = CornerRadius(7f)
        )
    }

    if (pull > 0f && b == 0f) {
        drawSyringe(hx - rx, hy - 150f, q, (pull * 6f).coerceIn(0f, 1f))
    }

    piece(e, b, 90f, 50f, 60f, 150f, 121f, 150f) { al ->
        drawLine(Palette.Droid.a(al), Offset(121f, 114f), Offset(hx, hy), 14f, StrokeCap.Round)
        drawCircle(Palette.Droid2.a(al), 8f, Offset(hx, hy))
    }

    if (q > .03f && b == 0f) {
        for (i in 0..5) {
            val ph = ((e / 1100f) + i * 0.16f) % 1f
            drawRect(Palette.Ink.a(1f - ph), Offset(232f + 64f * ph, 148f + DYS[i] * ph), Size(5f, 3f))
        }
    }

    if (b > 0f && b < 1f) {
        val tt = b * 54f
        for (pt in parts) {
            val x = 281f + pt.vx * tt * 0.8f
            val y = 150f + pt.vy * tt * 0.8f + 0.14f * tt * tt
            drawRect(PCOLORS[pt.c].a(1f - b), Offset(x, y), Size(pt.s, pt.s))
        }
    }
}
