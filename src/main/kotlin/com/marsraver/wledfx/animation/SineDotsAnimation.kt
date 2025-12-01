package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * SinDots animation - trailing sine-based dots with blur.
 */
class SinDotsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var tempBuffer: Array<Array<RgbColor>>
    private var lastUpdateTime: Long = 0
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
        lastUpdateTime = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        val deltaMs = (now - lastUpdateTime) / 1_000_000.0
        lastUpdateTime = now

        fadeToBlack(15)

        val t1 = (now / 20_000_000L).toInt() and 0xFF
        val t2 = sin8(t1) / 2

        for (i in 0 until DOT_COUNT) {
            var x = (sin8(t1 + i * 20) * combinedWidth / 255.0).roundToInt()
            var y = (sin8(t2 + i * 20) * combinedHeight / 255.0).roundToInt()
            if (x >= combinedWidth) x = combinedWidth - 1
            if (y >= combinedHeight) y = combinedHeight - 1

            val hue = (i * 255 / DOT_COUNT) and 0xFF
            val rgb = hsvToRgb(hue, 255, 255)
            addPixelColor(x, y, rgb)
        }

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

    override fun getName(): String = "SinDots"

    override fun cleanup() {
        // No resources to release.
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

    private fun sin8(value: Int): Int {
        val normalized = value and 0xFF
        val radians = normalized / 256.0 * 2.0 * PI
        return ((sin(radians) + 1.0) * 127.5).roundToInt().coerceIn(0, 255)
    }

    companion object {
        private const val DOT_COUNT = 13
    }
}

