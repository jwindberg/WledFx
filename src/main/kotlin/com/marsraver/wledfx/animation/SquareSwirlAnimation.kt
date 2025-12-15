package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Square Swirl animation - layered sine-driven points with dynamic blur.
 */
class SquareSwirlAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
    }

    override fun update(now: Long): Boolean {
        val timeMs = now / 1_000_000
        
        // Apply blurring
        val blurWave = MathUtils.beatsin8(3, 64, 192, timeMs)
        val blurAmount = dim8Raw(blurWave)
        applyBlur(blurAmount)

        val border = max(1, min(width, height) / 16)
        val maxX = (width - 1 - border).coerceAtLeast(border)
        val maxY = (height - 1 - border).coerceAtLeast(border)

        val maxCoord = min(maxX, maxY)
        val i = MathUtils.beatsin8(91, border, maxCoord, timeMs)
        val j = MathUtils.beatsin8(109, border, maxCoord, timeMs)
        val k = MathUtils.beatsin8(73, border, maxCoord, timeMs)

        val hue1 = ((timeMs / 29) % 256).toInt()
        val hue2 = ((timeMs / 41) % 256).toInt()
        val hue3 = ((timeMs / 73) % 256).toInt()

        // Use hsv to rgb (with potential palette usage if we wanted, but logic here is specific colors)
        // Original uses HSV with high saturation/val.
        // We will stick to HSV unless we map hues to palette.
        // Let's use BaseAnimation palette if available, mapping hue.
        
        addPixelColor(i.coerceIn(0, width - 1), j.coerceIn(0, height - 1), getColorFromHue(hue1, 255))
        addPixelColor(j.coerceIn(0, width - 1), k.coerceIn(0, height - 1), getColorFromHue(hue2, 255))
        addPixelColor(k.coerceIn(0, width - 1), i.coerceIn(0, height - 1), getColorFromHue(hue3, 255))

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Square Swirl"

    private fun getColorFromHue(hue: Int, brightness: Int): RgbColor {
        val base = getColorFromPalette(hue)
        val brightnessFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var weight = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
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
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun dim8Raw(value: Int): Int {
        val v = value.coerceIn(0, 255)
        return ((v * v) / 255.0).roundToInt().coerceIn(0, 255)
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + rgb.r).coerceAtMost(255),
                (current.g + rgb.g).coerceAtMost(255),
                (current.b + rgb.b).coerceAtMost(255)
            )
        }
    }
}
