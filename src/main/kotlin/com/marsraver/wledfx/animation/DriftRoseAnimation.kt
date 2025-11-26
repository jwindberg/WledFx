package com.marsraver.wledfx.animation

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
    private lateinit var pixelColors: Array<Array<IntArray>>
    private lateinit var tempBuffer: Array<Array<IntArray>>

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        tempBuffer = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
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

    override fun getName(): String = "Drift Rose"

    fun cleanup() {
        // nothing to release
    }

    private fun drawPixelXYF(x: Double, y: Double, rgb: IntArray) {
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

            pixelColors[xx][yy][0] = (pixelColors[xx][yy][0] + rgb[0] * weight).roundToInt().coerceAtMost(255)
            pixelColors[xx][yy][1] = (pixelColors[xx][yy][1] + rgb[1] * weight).roundToInt().coerceAtMost(255)
            pixelColors[xx][yy][2] = (pixelColors[xx][yy][2] + rgb[2] * weight).roundToInt().coerceAtMost(255)
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
                    tempBuffer[x][y][channel] = (pixelColors[x][y][channel] * (255 - amount) + (sum / weight) * amount) / 255
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
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    private fun beatsin8(speed: Int, minValue: Double, maxValue: Double, timeSeconds: Double): Double {
        val amplitude = (maxValue - minValue) / 2.0
        val mid = minValue + amplitude
        val frequency = speed / 16.0
        val sine = sin(2 * PI * frequency * timeSeconds)
        return mid + sine * amplitude
    }
}
