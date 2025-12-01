package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Black Hole animation - Orbiting stars around a central point.
 */
class BlackHoleAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<RgbColor>> = emptyArray()
    private var currentPalette: Palette? = null

    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 128
    private var custom2: Int = 128
    private var custom3: Int = 128
    private var solid: Boolean = false
    private var blur: Boolean = false

    private var startTime: Long = 0

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
        startTime = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val timeMs = System.currentTimeMillis() - startTime
        val fadeAmount = max(2, (16 + (speed shr 3)) / 4)
        fadeToBlack(fadeAmount)

        val t = timeMs / 128

        for (i in 0 until 8) {
            val phaseOffsetX = if (i % 2 == 1) 128 else 0
            val phaseOffsetY = if (i % 2 == 1) 192 else 64
            val x = beatsin8(custom1 shr 3, 0, combinedWidth - 1, phaseOffsetX + (t * i).toInt(), t)
            val y = beatsin8(intensity shr 3, 0, combinedHeight - 1, phaseOffsetY + (t * i).toInt(), t)

            val paletteIndex = i * 32
            val brightness = if (solid) 0 else 255
            val color = colorFromPalette(paletteIndex, false, brightness)
            addPixelColor(x, y, color)
        }

        val innerFreq = custom2 shr 3
        for (i in 0 until 4) {
            val phaseOffsetX = if (i % 2 == 1) 128 else 0
            val phaseOffsetY = if (i % 2 == 1) 192 else 64
            val minX = combinedWidth / 4
            val maxX = combinedWidth - 1 - combinedWidth / 4
            val minY = combinedHeight / 4
            val maxY = combinedHeight - 1 - combinedHeight / 4

            val x = beatsin8(innerFreq, minX, maxX, phaseOffsetX + (t * i).toInt(), t)
            val y = beatsin8(innerFreq, minY, maxY, phaseOffsetY + (t * i).toInt(), t)

            val paletteIndex = 255 - i * 64
            val brightness = if (solid) 0 else 255
            val color = colorFromPalette(paletteIndex, false, brightness)
            addPixelColor(x, y, color)
        }

        val centerX = combinedWidth / 2
        val centerY = combinedHeight / 2
        setPixelColor(centerX, centerY, RgbColor.WHITE)

        if (blur) {
            val blurAmount = 16
            val useSmear = combinedWidth * combinedHeight < 100
            applyBlur(blurAmount, useSmear)
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Black Hole"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    private fun fadeToBlack(amount: Int) {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val current = pixelColors[x][y]
                pixelColors[x][y] = RgbColor(
                    max(0, current.r - amount),
                    max(0, current.g - amount),
                    max(0, current.b - amount)
                )
            }
        }
    }

    private fun applyBlur(amount: Int, @Suppress("UNUSED_PARAMETER") smear: Boolean) {
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

        pixelColors = temp
    }

    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = rgb
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

    private fun beatsin8(frequency: Int, min: Int, max: Int, phaseOffset: Int, timebase: Long): Int {
        val phase = timebase * frequency / 255.0 + phaseOffset / 255.0 * 2 * Math.PI
        val sine = sin(phase)
        val range = max - min
        return min + ((sine + 1.0) * range / 2.0).roundToInt()
    }

    private fun colorFromPalette(hue: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            // Use the actual palette - map hue index (0-255) to palette array
            val adjustedHue = if (wrap) hue else hue % 256
            val paletteIndex = ((adjustedHue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            // Apply brightness scaling
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fall back to HSV if no palette is set
            val adjustedHue = if (wrap) hue else hue % 256
            return ColorUtils.hsvToRgb(adjustedHue, 255, brightness)
        }
    }
}

