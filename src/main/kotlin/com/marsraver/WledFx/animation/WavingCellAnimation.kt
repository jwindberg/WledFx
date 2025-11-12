package com.marsraver.WledFx.animation

import com.marsraver.WledFx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Waving Cell animation - heat palette waves animated with sinusoidal motion.
 * Audio RMS drives palette energy and bloom.
 */
class WavingCellAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var timeValue: Double = 0.0

    @Volatile
    private var smoothedLevel: Double = 0.0
    private val audioLock = Any()
    private var audioScope: CoroutineScope? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        timeValue = 0.0
        synchronized(audioLock) {
            smoothedLevel = 0.0
        }
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.rmsFlow().collectLatest { level ->
                    synchronized(audioLock) {
                        smoothedLevel = smoothedLevel * 0.85 + level.level * 0.15
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        // AnimationTimer supplies nanoseconds; convert to milliseconds before scaling.
        timeValue = now / 1_000_000.0 / 100.0
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) {
            return intArrayOf(0, 0, 0)
        }

        val level = synchronized(audioLock) { smoothedLevel }.coerceIn(0.0, 255.0)
        val energy = 0.4 + (level / 255.0) * 1.2
        val heatBoost = (level / 255.0) * 90.0

        val t = timeValue
        val inner = sin8(y * 5 + t * 5.0 * energy)
        val wave = sin8(x * 10 + inner * energy)
        val vertical = cos8(y * 10.0 * energy)
        var index = wave * energy + vertical * (0.7 + energy * 0.3) + t + heatBoost
        index = wrapToPaletteRange(index)
        val color = colorFromHeatPalette(index)
        val brightnessScale = (0.6 + level / 255.0 * 0.8).coerceIn(0.6, 1.4)
        color[0] = (color[0] * brightnessScale).roundToInt().coerceIn(0, 255)
        color[1] = (color[1] * brightnessScale).roundToInt().coerceIn(0, 255)
        color[2] = (color[2] * brightnessScale).roundToInt().coerceIn(0, 255)
        return color
    }

    override fun getName(): String = "Waving Cell"

    fun cleanup() {
        audioScope?.cancel()
        audioScope = null
        synchronized(audioLock) {
            smoothedLevel = 0.0
        }
    }

    private fun sin8(theta: Double): Double {
        var angle = theta % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        return (sin(radians) + 1.0) * 127.5
    }

    private fun cos8(theta: Double): Double {
        var angle = theta % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        return (cos(radians) + 1.0) * 127.5
    }

    private fun wrapToPaletteRange(value: Double): Double {
        var result = value % 256.0
        if (result < 0) result += 256.0
        return result
    }

    private fun colorFromHeatPalette(indexValue: Double): IntArray {
        val index = indexValue.coerceIn(0.0, 255.0)
        var lower = HEAT_PALETTE.first()
        var upper = HEAT_PALETTE.last()

        for (entry in HEAT_PALETTE) {
            if (entry.position <= index) {
                lower = entry
            }
            if (entry.position >= index) {
                upper = entry
                break
            }
        }

        if (lower === upper) {
            return lower.color.clone()
        }

        val range = upper.position - lower.position
        val fraction = if (range <= 0.0) 0.0 else (index - lower.position) / range
        val r = lerp(lower.color[0], upper.color[0], fraction)
        val g = lerp(lower.color[1], upper.color[1], fraction)
        val b = lerp(lower.color[2], upper.color[2], fraction)
        return intArrayOf(r, g, b)
    }

    private fun lerp(start: Int, end: Int, fraction: Double): Int {
        val value = start + (end - start) * fraction
        return value.roundToInt().coerceIn(0, 255)
    }

    private data class PaletteEntry(val position: Double, val color: IntArray)

    companion object {
        private val HEAT_PALETTE = listOf(
            PaletteEntry(0.0, intArrayOf(0, 0, 0)),
            PaletteEntry(48.0, intArrayOf(48, 0, 0)),
            PaletteEntry(96.0, intArrayOf(128, 16, 0)),
            PaletteEntry(160.0, intArrayOf(255, 80, 0)),
            PaletteEntry(224.0, intArrayOf(255, 200, 0)),
            PaletteEntry(255.0, intArrayOf(255, 255, 255)),
        )
    }
}

