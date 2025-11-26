package com.marsraver.wledfx.animation

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Square Swirl animation - layered sine-driven points with dynamic blur.
 */
class SquareSwirlAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<IntArray>>

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
    }

    override fun update(now: Long): Boolean {
        val timeSeconds = now / 1_000_000_000.0
        val blurWave = beatsin(timeSeconds, 3.0, 64, 192)
        val blurAmount = dim8Raw(blurWave)
        applyBlur(blurAmount)

        val border = max(1, min(combinedWidth, combinedHeight) / 16)
        val maxX = combinedWidth - 1 - border
        val maxY = combinedHeight - 1 - border

        val i = beatsin(timeSeconds, 91.0, border, maxX)
        val j = beatsin(timeSeconds, 109.0, border, maxY, 0.125)
        val k = beatsin(timeSeconds, 73.0, border, maxY, 0.25)

        val ms = now / 1_000_000
        val hue1 = ((ms / 29) % 256).toInt()
        val hue2 = ((ms / 41) % 256).toInt()
        val hue3 = ((ms / 73) % 256).toInt()

        addPixelColor(i, j, hsvToRgb(hue1, 200, 255))
        addPixelColor(j, k, hsvToRgb(hue2, 200, 255))
        addPixelColor(k, i, hsvToRgb(hue3, 200, 255))

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val color = pixelColors[x][y].clone()
            color[0] = color[0].coerceIn(0, 255)
            color[1] = color[1].coerceIn(0, 255)
            color[2] = color[2].coerceIn(0, 255)
            color
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Square Swirl"

    fun cleanup() {
        // No resources to release.
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (channel in 0 until 3) {
                    var sum = 0
                    var weight = 0
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                                val w = if (dx == 0 && dy == 0) 4 else 1
                                sum += pixelColors[nx][ny][channel] * w
                                weight += w
                            }
                        }
                    }
                    val average = if (weight == 0) pixelColors[x][y][channel] else sum / weight
                    temp[x][y][channel] =
                        (pixelColors[x][y][channel] * (255 - factor) + average * factor) / 255
                }
            }
        }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = temp[x][y][0]
                pixelColors[x][y][1] = temp[x][y][1]
                pixelColors[x][y][2] = temp[x][y][2]
            }
        }
    }

    private fun beatsin(
        timeSeconds: Double,
        bpm: Double,
        low: Int,
        high: Int,
        phaseOffset: Double = 0.0,
    ): Int {
        val freq = bpm / 60.0
        val angle = 2.0 * PI * (freq * timeSeconds + phaseOffset)
        val sine = sin(angle)
        val mid = (low + high) / 2.0
        val range = (high - low) / 2.0
        return (mid + sine * range).roundToInt().coerceIn(min(low, high), max(low, high))
    }

    private fun dim8Raw(value: Int): Int {
        val v = value.coerceIn(0, 255)
        return ((v * v) / 255.0).roundToInt().coerceIn(0, 255)
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
            pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
            pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
        }
    }

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): IntArray {
        val h = (hue % 256 + 256) % 256
        val s = saturation.coerceIn(0, 255) / 255.0
        val v = value.coerceIn(0, 255) / 255.0

        if (s <= 0.0) {
            val gray = (v * 255).roundToInt()
            return intArrayOf(gray, gray, gray)
        }

        val hSection = h / 42.6666667
        val i = hSection.toInt()
        val f = hSection - i

        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))

        val (r, g, b) = when (i % 6) {
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

