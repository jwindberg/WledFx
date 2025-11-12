package com.marsraver.WledFx.animation

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * SinDots animation - trailing sine-based dots with blur.
 */
class SinDotsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<IntArray>>
    private lateinit var tempBuffer: Array<Array<IntArray>>
    private var lastUpdateTime: Long = 0

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        tempBuffer = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
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

    override fun getName(): String = "SinDots"

    fun cleanup() {
        // No resources to release.
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
            pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
            pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
        }
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255)
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = pixelColors[x][y][0] * factor / 255
                pixelColors[x][y][1] = pixelColors[x][y][1] * factor / 255
                pixelColors[x][y][2] = pixelColors[x][y][2] * factor / 255
            }
        }
    }

    private fun blur2d(amount: Int) {
        if (amount <= 0) return
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
                    tempBuffer[x][y][channel] =
                        (pixelColors[x][y][channel] * (255 - amount) + (sum / weight) * amount) / 255
                }
            }
        }

        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = tempBuffer[x][y][0]
                pixelColors[x][y][1] = tempBuffer[x][y][1]
                pixelColors[x][y][2] = tempBuffer[x][y][2]
            }
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
            (b * 255).roundToInt().coerceIn(0, 255),
        )
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

