package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * Frizzles animation - Color frizzles bouncing across the matrix with blur trails.
 */
class FrizzlesAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var lastUpdateNs: Long = 0L

    override fun getName(): String = "Frizzles"
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNs == 0L) lastUpdateNs = now
        
        fadeToBlack(16)

        val loops = 8
        val timeMs = now / 1_000_000
        for (i in loops downTo 1) {
            val freqBase = 12
            val x = MathUtils.beatsin8(freqBase + i, 0, width - 1, timeMs)
            val y = MathUtils.beatsin8(15 - i, 0, height - 1, timeMs)
            val hue = MathUtils.beatsin8(freqBase, 0, 255, timeMs)
            val color = getColorFromHue(hue, 255)
            addPixelColor(x, y, color)

            if (width > 24 || height > 24) {
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
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        val factor = amount.coerceIn(0, 255)
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0; var sumG = 0; var sumB = 0; var weight = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val w = if (dx == 0 && dy == 0) 4 else 1
                            val color = pixelColors[nx][ny]
                            sumR += color.r * w; sumG += color.g * w; sumB += color.b * w
                            weight += w
                        }
                    }
                }
                val current = pixelColors[x][y]
                val avgR = if (weight == 0) current.r else sumR / weight
                val avgG = if (weight == 0) current.g else sumG / weight
                val avgB = if (weight == 0) current.b else sumB / weight
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + avgR * factor) / 255,
                    (current.g * (255 - factor) + avgG * factor) / 255,
                    (current.b * (255 - factor) + avgB * factor) / 255
                )
            }
        }
        for (x in 0 until width) for (y in 0 until height) pixelColors[x][y] = temp[x][y]
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x !in 0 until width || y !in 0 until height) return
        val current = pixelColors[x][y]
        pixelColors[x][y] = RgbColor(
            (current.r + rgb.r).coerceAtMost(255),
            (current.g + rgb.g).coerceAtMost(255),
            (current.b + rgb.b).coerceAtMost(255)
        )
    }

    private fun getColorFromHue(hue: Int, brightness: Int): RgbColor {
        val baseColor = getColorFromPalette(hue)
        val brightnessFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
    }
}
