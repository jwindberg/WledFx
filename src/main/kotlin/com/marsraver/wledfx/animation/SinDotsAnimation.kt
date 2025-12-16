package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.roundToInt

/**
 * SinDots animation - trailing sine-based dots with blur.
 */
class SinDotsAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var tempBuffer: Array<Array<RgbColor>>
    private var lastUpdateTime: Long = 0

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        tempBuffer = Array(width) { Array(height) { RgbColor.BLACK } }
        lastUpdateTime = System.nanoTime()
        paramSpeed = 128
    }

    override fun update(now: Long): Boolean {
        // Delta time not strictly used for motion, since motion is just based on `now`
        val deltaMs = (now - lastUpdateTime) / 1_000_000.0
        lastUpdateTime = now

        fadeToBlack(15)

        val t1 = (now / 20_000_000L).toInt() and 0xFF
        val t2 = MathUtils.sin8(t1) / 2

        for (i in 0 until DOT_COUNT) {
            var x = (MathUtils.sin8(t1 + i * 20) * width / 255.0).roundToInt()
            var y = (MathUtils.sin8(t2 + i * 20) * height / 255.0).roundToInt()
            if (x >= width) x = width - 1
            if (y >= height) y = height - 1

            val hue = (i * 255 / DOT_COUNT) and 0xFF
            // Use BaseAnimation palette
            val rgb = getColorFromPalette(hue)
            addPixelColor(x, y, rgb)
        }

        blur2d(16)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "SineDots"

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

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun blur2d(amount: Int) {
        if (amount <= 0) return
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

        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = tempBuffer[x][y]
            }
        }
    }

    companion object {
        private const val DOT_COUNT = 13
    }
}
