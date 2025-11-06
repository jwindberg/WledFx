package com.marsraver.WledFx.animation

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Black Hole animation - Orbiting stars around a central point.
 */
class BlackHoleAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<IntArray>> = emptyArray()

    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 128
    private var custom2: Int = 128
    private var custom3: Int = 128
    private var solid: Boolean = false
    private var blur: Boolean = false

    private var startTime: Long = 0

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
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
        setPixelColor(centerX, centerY, intArrayOf(255, 255, 255))

        if (blur) {
            val blurAmount = 16
            val useSmear = combinedWidth * combinedHeight < 100
            applyBlur(blurAmount, useSmear)
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y].clone()
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Black Hole"

    private fun fadeToBlack(amount: Int) {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (c in 0 until 3) {
                    pixelColors[x][y][c] = max(0, pixelColors[x][y][c] - amount)
                }
            }
        }
    }

    private fun applyBlur(amount: Int, @Suppress("UNUSED_PARAMETER") smear: Boolean) {
        if (amount <= 0) return
        val temp = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }

        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (c in 0 until 3) {
                    var sum = 0
                    var count = 0

                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                                sum += pixelColors[nx][ny][c]
                                count++
                            }
                        }
                    }

                    val blurred = if (count > 0) sum / count else pixelColors[x][y][c]
                    temp[x][y][c] = (pixelColors[x][y][c] * (255 - amount) + blurred * amount) / 255
                }
            }
        }

        pixelColors = temp
    }

    private fun setPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = rgb[0].coerceIn(0, 255)
            pixelColors[x][y][1] = rgb[1].coerceIn(0, 255)
            pixelColors[x][y][2] = rgb[2].coerceIn(0, 255)
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
            pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
            pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
        }
    }

    private fun beatsin8(frequency: Int, min: Int, max: Int, phaseOffset: Int, timebase: Long): Int {
        val phase = timebase * frequency / 255.0 + phaseOffset / 255.0 * 2 * Math.PI
        val sine = sin(phase)
        val range = max - min
        return min + ((sine + 1.0) * range / 2.0).roundToInt()
    }

    private fun colorFromPalette(hue: Int, wrap: Boolean, brightness: Int): IntArray {
        val adjustedHue = if (wrap) hue else hue % 256
        val h = (adjustedHue % 256) / 255.0f * 360.0f
        val s = 1.0f
        val v = brightness / 255.0f
        return hsvToRgb(h, s, v)
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val hi = ((h / 60.0f) % 6).toInt()
        val f = h / 60.0f - hi
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)

        val (r, g, b) = when (hi) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }

        return intArrayOf(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }
}

