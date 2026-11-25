package com.carrusel.optimizer

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class ShizukuState { NotRunning, NeedsPermission, Ready }
enum class StepState { Pending, Running, Done, Failed }

object ShizukuHelper {
    private val SAFE = Regex("^[A-Za-z0-9._]+$")

    fun state(): ShizukuState {
        return try {
            if (!Shizuku.pingBinder()) {
                ShizukuState.NotRunning
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.Ready
            } else {
                ShizukuState.NeedsPermission
            }
        } catch (e: Throwable) {
            ShizukuState.NotRunning
        }
    }

    fun requestPermission() {
        Shizuku.requestPermission(1001)
    }

    /** Ejecuta un comando de shell con los privilegios de Shizuku. Devuelve (codigo, salida). */
    suspend fun exec(cmd: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            m.isAccessible = true
            val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            Pair(code, out + err)
        } catch (e: Throwable) {
            Pair(-1, e.message ?: "error")
        }
    }

    @Suppress("DEPRECATION")
    fun findGames(ctx: Context): List<String> {
        return try {
            ctx.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    (it.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                        it.category == ApplicationInfo.CATEGORY_GAME
                }
                .map { it.packageName }
                .filter { SAFE.matches(it) && it != ctx.packageName }
        } catch (e: Throwable) {
            emptyList()
        }
    }
}

object Optimizer {
    val TITLES = listOf(
        "Cerrar apps en segundo plano",
        "Desactivar ahorro de batería",
        "Reducir animaciones del sistema",
        "Activar modo juego",
        "Compilar juegos para rendimiento"
    )

    suspend fun runStep(i: Int, games: List<String>): Boolean {
        return when (i) {
            0 -> ShizukuHelper.exec("am kill-all").first == 0
            1 -> ShizukuHelper.exec("settings put global low_power 0").first == 0
            2 -> ShizukuHelper.exec(
                "settings put global window_animation_scale 0.5; " +
                    "settings put global transition_animation_scale 0.5; " +
                    "settings put global animator_duration_scale 0.5"
            ).first == 0
            3 -> if (games.isEmpty()) true else games.map {
                ShizukuHelper.exec("cmd game mode performance $it").first == 0
            }.any { ok -> ok }
            else -> if (games.isEmpty()) true else games.map {
                ShizukuHelper.exec("cmd package compile -m speed -f $it").first == 0
            }.any { ok -> ok }
        }
    }
}
