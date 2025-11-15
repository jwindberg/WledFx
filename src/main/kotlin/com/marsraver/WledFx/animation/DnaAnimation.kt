package com.marsraver.WledFx.animation

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.abs

/**
 * DNA animation - dual sine waves forming a helix pattern.
 */
class DnaAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<IntArray>>
    private lateinit var tempBuffer: Array<Array<IntArray>>
    private var lastUpdateNs: Long = 0L

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        tempBuffer = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth == 0 || combinedHeight == 0) return true

        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }

        fadeToBlack(FADE_AMOUNT)

        val timeMs = now / 1_000_000
        val rows = combinedHeight
        val center = (combinedWidth - 1) / 2.0
        val amplitude = (combinedWidth - 2) / 2.0

        for (row in 0 until rows) {
            val angle = timeMs / 240.0 + row * ROW_PHASE
            val wobble = sin(angle * 0.5) * amplitude * 0.15

            val x1 = (center + sin(angle) * amplitude * 0.55 + wobble).roundToInt().coerceIn(0, combinedWidth - 1)
            val x2 = (center + sin(angle + Math.PI) * amplitude * 0.55 - wobble).roundToInt().coerceIn(0, combinedWidth - 1)

            val hueBase = (timeMs / 25 + row * 14).toInt() and 0xFF
            val hueOpposite = (hueBase + 128) and 0xFF

            val brightness1 = (160 + sin(angle * 1.3) * 95).roundToInt().coerceIn(60, 255)
            val brightness2 = (160 + sin((angle + Math.PI) * 1.3) * 95).roundToInt().coerceIn(60, 255)

            val colorA = hsvToRgb(hueBase, 200, brightness1)
            val colorB = hsvToRgb(hueOpposite, 200, brightness2)

            drawLine(x1, x2, row, blendColors(colorA, colorB), addDot = false)
            drawPixel(x1, row, colorA)
            drawPixel(x2, row, colorB)
        }

        blur2d(18)
        mirrorVertical()

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val color = pixelColors[x][y].clone()
            color[0] = color[0].coerceIn(0, 255)
            color[1] = color[1].coerceIn(0, 255)
            color[2] = color[2].coerceIn(0, 255)
            color
        } else intArrayOf(0, 0, 0)
    }

    override fun getName(): String = "DNA"

    fun cleanup() {
        // nothing to release
    }

    private fun drawPixel(x: Int, y: Int, rgb: IntArray) {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) return
        pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
        pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
        pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
        pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
        pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
    }

    private fun blendColors(a: IntArray, b: IntArray): IntArray {
        return intArrayOf(
            (a[0] + b[0]) / 2,
            (a[1] + b[1]) / 2,
            (a[2] + b[2]) / 2,
        )
    }

    private fun drawLine(x0: Int, x1: Int, y: Int, rgb: IntArray, addDot: Boolean) {
        val start = x0.coerceIn(0, combinedWidth - 1)
        val end = x1.coerceIn(0, combinedWidth - 1)
        val steps = abs(end - start) + 1
        for (step in 0 until steps) {
            val t = step / (steps - 1.0).coerceAtLeast(1.0)
            val x = ((1 - t) * start + t * end).roundToInt().coerceIn(0, combinedWidth - 1)
            val scale = (160 + t * 95).roundToInt().coerceIn(0, 255)
            val scaled = intArrayOf(rgb[0] * scale / 255, rgb[1] * scale / 255, rgb[2] * scale / 255)
            addPixelColor(x, y, scaled)
        }
        if (addDot) {
            addPixelColor(start, y, intArrayOf(255, 255, 255))
            addPixelColor(end, y, intArrayOf(30, 144, 255))
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

    private fun mirrorVertical() {
        val half = combinedHeight / 2
        for (y in 0 until half) {
            val opposite = combinedHeight - 1 - y
            for (x in 0 until combinedWidth) {
                pixelColors[x][opposite][0] = pixelColors[x][y][0]
                pixelColors[x][opposite][1] = pixelColors[x][y][1]
                pixelColors[x][opposite][2] = pixelColors[x][y][2]
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

    private fun beatsin(speed: Int, amplitude: Int, phaseOffset: Int): Int {
        val angle = (System.currentTimeMillis() / speed.toDouble() + phaseOffset / 256.0) * 2 * PI / 30.0
        val sine = sin(angle)
        val mid = amplitude / 2.0
        val range = amplitude / 2.0
        return (mid + sine * range).roundToInt().coerceIn(0, amplitude)
    }

    companion object {
        private const val FADE_AMOUNT = 200
        private const val SPEED = 30
        private const val ROW_PHASE = 0.35
        private const val BRIGHTNESS_OFFSET = 256 / 14
    }
}
