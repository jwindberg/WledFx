package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Frizzles animation - Color frizzles bouncing across the matrix with blur trails.
 */
class FrizzlesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var lastUpdateNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
        }
        lastUpdateNs = now

        fadeToBlack(16)

        val loops = 8
        val timeMs = now / 1_000_000
        for (i in loops downTo 1) {
            val freqBase = 12
            val x = beatsin8(freqBase + i, BORDER, combinedWidth - 1 - BORDER, timeMs)
            val y = beatsin8(15 - i, BORDER, combinedHeight - 1 - BORDER, timeMs)
            val hue = beatsin8(freqBase, 0, 255, timeMs)
            val color = getColorFromHue(hue, 255)
            addPixelColor(x, y, color)

            if (combinedWidth > 24 || combinedHeight > 24) {
                addPixelColor(x + 1, y, color)
                addPixelColor(x - 1, y, color)
                addPixelColor(x, y + 1, color)
                addPixelColor(x, y - 1, color)
            }
        }

        applyBlur(16)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Frizzles"

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        val factor = amount.coerceIn(0, 255)
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var weight = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                            val w = if (dx == 0 && dy == 0) 4 else 1
                            val color = pixelColors[nx][ny]
                            sumR += color.r * w
                            sumG += color.g * w
                            sumB += color.b * w
                            weight += w
                        }
                    }
                }
                val current = pixelColors[x][y]
                val averageR = if (weight == 0) current.r else sumR / weight
                val averageG = if (weight == 0) current.g else sumG / weight
                val averageB = if (weight == 0) current.b else sumB / weight
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + averageR * factor) / 255,
                    (current.g * (255 - factor) + averageG * factor) / 255,
                    (current.b * (255 - factor) + averageB * factor) / 255
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
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) return
        val current = pixelColors[x][y]
        pixelColors[x][y] = RgbColor(
            (current.r + rgb.r).coerceAtMost(255),
            (current.g + rgb.g).coerceAtMost(255),
            (current.b + rgb.b).coerceAtMost(255)
        )
    }

    private fun beatsin8(frequency: Int, low: Int, high: Int, timeMs: Long): Int {
        val freq = frequency.coerceAtLeast(1)
        val period = 1_000.0 / freq
        val angle = (timeMs % period) / period * 2 * PI
        val sine = sin(angle)
        val mid = (low + high) / 2.0
        val amplitude = (high - low) / 2.0
        return (mid + sine * amplitude).roundToInt().coerceIn(low, high)
    }

    private fun getColorFromHue(hue: Int, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(hue, 255, brightness)
        }
    }

    companion object {
        private const val BORDER = 0
    }
}
