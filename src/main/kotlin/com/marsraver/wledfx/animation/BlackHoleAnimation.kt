package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.max

/**
 * Black Hole animation - Orbiting stars around a central point.
 */
class BlackHoleAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // custom1/2/3 were used. Let's map them to Speed/Intensity or fixed values
    // Original used custom1, 2, 3 as parameters
    // We'll map: paramSpeed -> speed
    // paramIntensity -> intensity
    // custom1 -> paramSpeed? Or just default? Original custom1=128
    // custom2 -> 128
    
    // We will use paramSpeed and paramIntensity for the main drivers.
    
    private var solid: Boolean = false
    private var blur: Boolean = false
    private var startTime: Long = 0

    override fun getName(): String = "Black Hole"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTime = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val timeMs = System.currentTimeMillis() - startTime
        val fadeAmount = max(2, (16 + (paramSpeed shr 3)) / 4)
        fadeToBlack(fadeAmount)

        val t = timeMs / 128
        val custom1 = 128 // Default or exposed via other mechanism? BaseAnimation only has 2 params + color/palette.
        val custom2 = 128

        for (i in 0 until 8) {
            val phaseOffsetX = if (i % 2 == 1) 128 else 0
            val phaseOffsetY = if (i % 2 == 1) 192 else 64
            val x = MathUtils.beatsin8(custom1 shr 3, 0, width - 1, t, phaseOffsetX + (t * i).toInt())
            val y = MathUtils.beatsin8(paramIntensity shr 3, 0, height - 1, t, phaseOffsetY + (t * i).toInt())

            val paletteIndex = i * 32
            val brightness = if (solid) 0 else 255
            val color = getColorFromPaletteWithBrightness(paletteIndex, false, brightness)
            addPixelColor(x, y, color)
        }

        val innerFreq = custom2 shr 3
        for (i in 0 until 4) {
            val phaseOffsetX = if (i % 2 == 1) 128 else 0
            val phaseOffsetY = if (i % 2 == 1) 192 else 64
            val minX = width / 4
            val maxX = width - 1 - width / 4
            val minY = height / 4
            val maxY = height - 1 - height / 4

            val x = MathUtils.beatsin8(innerFreq, minX, maxX, t, phaseOffsetX + (t * i).toInt())
            val y = MathUtils.beatsin8(innerFreq, minY, maxY, t, phaseOffsetY + (t * i).toInt())

            val paletteIndex = 255 - i * 64
            val brightness = if (solid) 0 else 255
            val color = getColorFromPaletteWithBrightness(paletteIndex, false, brightness)
            addPixelColor(x, y, color)
        }

        val centerX = width / 2
        val centerY = height / 2
        setPixelColor(centerX, centerY, RgbColor.WHITE)

        if (blur) {
            val blurAmount = 16
            val useSmear = width * height < 100
            applyBlur(blurAmount, useSmear)
        }
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
                val current = pixelColors[x][y]
                pixelColors[x][y] = ColorUtils.scaleBrightness(current, factor)
            }
        }
    }

    private fun applyBlur(amount: Int, smear: Boolean) {
        if (amount <= 0) return
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }

        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0

                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val color = pixelColors[nx][ny]
                            sumR += color.r
                            sumG += color.g
                            sumB += color.b
                            count++
                        }
                    }
                }

                val current = pixelColors[x][y]
                val blurredR = if (count > 0) sumR / count else current.r
                val blurredG = if (count > 0) sumG / count else current.g
                val blurredB = if (count > 0) sumB / count else current.b
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - amount) + blurredR * amount) / 255,
                    (current.g * (255 - amount) + blurredG * amount) / 255,
                    (current.b * (255 - amount) + blurredB * amount) / 255
                )
            }
        }
        // Copy back
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = rgb
        }
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
    
    private fun getColorFromPaletteWithBrightness(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        if (brightness > 0 && brightness < 255) {
             return ColorUtils.scaleBrightness(base, brightness / 255.0)
        }
        if (brightness == 0) return RgbColor.BLACK
        return base
    }
}
