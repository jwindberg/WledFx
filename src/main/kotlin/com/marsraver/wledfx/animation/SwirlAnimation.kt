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
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Swirl animation - Audio-reactive swirling pixels with mirrored patterns.
 */
class SwirlAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<RgbColor>> = emptyArray()
    private var currentPalette: Palette? = null

    private var speed: Int = 128
    private var intensity: Int = 64
    private var custom1: Int = 16
    private var fadeAmount: Int = 4

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    @Volatile
    private var volumeSmth: Float = 0.0f
    @Volatile
    private var volumeRaw: Int = 0
    private var smoothedVolumeRaw: Int = 0
    private val volumeLock = Any()
    private var audioScope: CoroutineScope? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        synchronized(volumeLock) {
            volumeSmth = 0.0f
            volumeRaw = 0
            smoothedVolumeRaw = 0
        }
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.rmsFlow().collectLatest { level ->
                    synchronized(volumeLock) {
                        volumeSmth = level.rms.toFloat()
                        smoothedVolumeRaw = (smoothedVolumeRaw * 19 + level.level) / 20
                        volumeRaw = smoothedVolumeRaw
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val (rawVolume, smoothVolume) = synchronized(volumeLock) { volumeRaw to volumeSmth }

        if (rawVolume < NOISE_FLOOR) {
            fadeToBlack(15)
            if (custom1 > 0) applyBlur(custom1)
            return true
        }

        fadeToBlack(fadeAmount)
        if (custom1 > 0) applyBlur(custom1)

        val timeMs = now / 1_000_000
        var freq1 = (27 * speed) / 255
        var freq2 = (41 * speed) / 255
        if (freq1 < 1) freq1 = 1
        if (freq2 < 1) freq2 = 1

        val i = beatsin8(freq1, BORDER_WIDTH, combinedWidth - BORDER_WIDTH, timeMs)
        val j = beatsin8(freq2, BORDER_WIDTH, combinedHeight - BORDER_WIDTH, timeMs)
        val ni = combinedWidth - 1 - i
        val nj = combinedHeight - 1 - j

        val baseBrightness = 200
        val audioBoost = rawVolume / 2
        var brightness = (baseBrightness + audioBoost).coerceAtMost(255)
        brightness = brightness.coerceAtLeast(150)

        val paletteOffset = smoothVolume * 4.0f

        val color1 = colorFromPalette((timeMs / 11 + paletteOffset).toInt(), brightness)
        addPixelColor(i, j, color1)

        val color2 = colorFromPalette((timeMs / 13 + paletteOffset).toInt(), brightness)
        addPixelColor(j, i, color2)

        val color3 = colorFromPalette((timeMs / 17 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, nj, color3)

        val color4 = colorFromPalette((timeMs / 29 + paletteOffset).toInt(), brightness)
        addPixelColor(nj, ni, color4)

        val color5 = colorFromPalette((timeMs / 37 + paletteOffset).toInt(), brightness)
        addPixelColor(i, nj, color5)

        val color6 = colorFromPalette((timeMs / 41 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, j, color6)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Swirl"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
        audioScope?.cancel()
        audioScope = null
        synchronized(volumeLock) {
            volumeSmth = 0f
            volumeRaw = 0
            smoothedVolumeRaw = 0
        }
    }

    private fun beatsin8(frequency: Int, minVal: Int, maxVal: Int, timebase: Long): Int {
        val phase = timebase * frequency / 255.0
        val sine = sin(phase)
        val range = maxVal - minVal
        return minVal + ((sine + 1.0) * range / 2.0).roundToInt()
    }

    private fun fadeToBlack(amount: Int) {
        when {
            amount <= 0 -> return
            amount >= 255 -> {
                for (x in 0 until combinedWidth) {
                    for (y in 0 until combinedHeight) {
                        pixelColors[x][y] = RgbColor.BLACK
                    }
                }
            }
            else -> {
                val factor = (255 - amount) / 255.0
                for (x in 0 until combinedWidth) {
                    for (y in 0 until combinedHeight) {
                        pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
                    }
                }
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                            val color = pixelColors[nx][ny]
                            sumR += color.r
                            sumG += color.g
                            sumB += color.b
                            count++
                        }
                    }
                }
                val current = pixelColors[x][y]
                val avgR = if (count > 0) sumR / count else current.r
                val avgG = if (count > 0) sumG / count else current.g
                val avgB = if (count > 0) sumB / count else current.b
                temp[x][y] = RgbColor(
                    (current.r * (255 - amount) + avgR * amount) / 255,
                    (current.g * (255 - amount) + avgG * amount) / 255,
                    (current.b * (255 - amount) + avgB * amount) / 255
                )
            }
        }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + rgb.r).coerceAtMost(255),
                (current.g + rgb.g).coerceAtMost(255),
                (current.b + rgb.b).coerceAtMost(255)
            )
        }
    }

    private fun colorFromPalette(colorIndexValue: Int, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            var colorIndex = colorIndexValue % 256
            if (colorIndex < 0) colorIndex += 256
            val paletteIndex = (colorIndex / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            var colorIndex = colorIndexValue % 256
            if (colorIndex < 0) colorIndex += 256
            val h = colorIndex / 255.0f * 360.0f
            val s = 1.0f
            val v = brightness / 255.0f
            return ColorUtils.hsvToRgb(h, s, v)
        }
    }

    companion object {
        private const val BORDER_WIDTH = 2
        private const val NOISE_FLOOR = 100
    }
}

