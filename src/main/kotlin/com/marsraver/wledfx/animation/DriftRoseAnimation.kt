package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Drift Rose animation - floral sine-wave pattern flowing from the center.
 */
class DriftRoseAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var tempBuffer: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null

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
        tempBuffer = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth == 0 || combinedHeight == 0) return true

        val centerX = (combinedWidth / 2.0) - 0.5
        val centerY = (combinedHeight / 2.0) - 0.5
        val radius = min(combinedWidth, combinedHeight) / 2.0
        val timeSeconds = now / 1_000_000_000.0

        for (i in 1..36) {
            val angle = Math.toRadians(i * 10.0)
            val wave = beatsin8(i, 0.0, radius * 2, timeSeconds) - radius
            val x = centerX + sin(angle) * wave
            val y = centerY + cos(angle) * wave
            val hue = (i * 10) and 0xFF
            drawPixelXYF(x, y, hsvToRgb(hue, 255, 255))
        }

        fadeToBlack(32)
        blur2d(16)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Drift Rose"

    override fun cleanup() {
        // nothing to release
    }

    private fun drawPixelXYF(x: Double, y: Double, rgb: RgbColor) {
        val floorX = x.toInt()
        val floorY = y.toInt()
        val fracX = (x - floorX).coerceIn(0.0, 1.0)
        val fracY = (y - floorY).coerceIn(0.0, 1.0)
        val invX = 1.0 - fracX
        val invY = 1.0 - fracY

        val weights = listOf(
            invX * invY,
            fracX * invY,
            invX * fracY,
            fracX * fracY
        )
        val offsets = listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)

        for ((index, weight) in weights.withIndex()) {
            val (dx, dy) = offsets[index]
            val xx = floorX + dx
            val yy = floorY + dy
            if (xx !in 0 until combinedWidth || yy !in 0 until combinedHeight) continue

            val current = pixelColors[xx][yy]
            pixelColors[xx][yy] = RgbColor(
                (current.r + rgb.r * weight).roundToInt().coerceAtMost(255),
                (current.g + rgb.g * weight).roundToInt().coerceAtMost(255),
                (current.b + rgb.b * weight).roundToInt().coerceAtMost(255)
            )
        }
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun blur2d(amount: Int) {
        if (amount <= 0) return
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
                val blurredR = if (weight > 0) sumR / weight else current.r
                val blurredG = if (weight > 0) sumG / weight else current.g
                val blurredB = if (weight > 0) sumB / weight else current.b
                tempBuffer[x][y] = RgbColor(
                    (current.r * (255 - amount) + blurredR * amount) / 255,
                    (current.g * (255 - amount) + blurredG * amount) / 255,
                    (current.b * (255 - amount) + blurredB * amount) / 255
                )
            }
        }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = tempBuffer[x][y]
            }
        }
    }

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = value / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(hue, saturation, value)
        }
    }

    private fun beatsin8(speed: Int, minValue: Double, maxValue: Double, timeSeconds: Double): Double {
        val amplitude = (maxValue - minValue) / 2.0
        val mid = minValue + amplitude
        val frequency = speed / 16.0
        val sine = sin(2 * PI * frequency * timeSeconds)
        return mid + sine * amplitude
    }
}
