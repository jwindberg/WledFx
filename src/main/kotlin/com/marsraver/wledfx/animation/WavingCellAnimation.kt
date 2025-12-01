package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.AudioPipeline
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
    private var currentPalette: Palette? = null

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

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) {
            return RgbColor.BLACK
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
        val color = colorFromPalette(index)
        val brightnessScale = (0.6 + level / 255.0 * 0.8).coerceIn(0.6, 1.4)
        return ColorUtils.scaleBrightness(color, brightnessScale)
    }

    override fun getName(): String = "Waving Cell"

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun cleanup() {
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

    private fun colorFromPalette(indexValue: Double): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            // Use the selected palette
            val index = (indexValue.coerceIn(0.0, 255.0) / 255.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            return currentPalette[index]
        } else {
            // Fallback to heat palette if no palette is set
            return colorFromHeatPalette(indexValue)
        }
    }

    private fun colorFromHeatPalette(indexValue: Double): RgbColor {
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
            return lower.color
        }

        val range = upper.position - lower.position
        val fraction = if (range <= 0.0) 0.0 else (index - lower.position) / range
        val r = lerp(lower.color.r, upper.color.r, fraction)
        val g = lerp(lower.color.g, upper.color.g, fraction)
        val b = lerp(lower.color.b, upper.color.b, fraction)
        return RgbColor(r, g, b)
    }

    private fun lerp(start: Int, end: Int, fraction: Double): Int {
        val value = start + (end - start) * fraction
        return value.roundToInt().coerceIn(0, 255)
    }

    private data class PaletteEntry(val position: Double, val color: RgbColor)

    companion object {
        private val HEAT_PALETTE = listOf(
            PaletteEntry(0.0, RgbColor.BLACK),
            PaletteEntry(48.0, RgbColor(48, 0, 0)),
            PaletteEntry(96.0, RgbColor(128, 16, 0)),
            PaletteEntry(160.0, RgbColor(255, 80, 0)),
            PaletteEntry(224.0, RgbColor(255, 200, 0)),
            PaletteEntry(255.0, RgbColor.WHITE),
        )
    }
}

