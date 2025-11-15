package com.marsraver.WledFx.animation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * DNA Spiral animation - intertwined sine-wave strands forming a rotating double helix.
 */
class DnaSpiralAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<IntArray>>
    private var lastUpdateNs: Long = 0L
    private var hueOffset: Int = 0

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        lastUpdateNs = 0L
        hueOffset = 0
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth == 0 || combinedHeight == 0) return true

        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
        }

        fadeDown()

        val timeSeconds = now / 1_000_000_000.0
        val auxTime = timeSeconds * 0.85
        val rows = combinedHeight
        val edgePadding = 1
        val center = (combinedWidth - 1) / 2.0
        val amplitude = (combinedWidth - 1 - edgePadding * 2) / 2.0

        val timeSegment = (timeSeconds / 0.008).toInt()
        for (row in 0 until rows) {
            val basePhase = row * FREQ
            val phase1 = basePhase
            val phase2 = basePhase + 128

            val p1 = center + sin(2 * PI * (timeSeconds / PERIOD_SECONDS + phase1 / 256.0)) * amplitude
            val p2 = center + sin(2 * PI * (auxTime / PERIOD_SECONDS + phase1 / 256.0 + 0.5)) * amplitude
            val q1 = center + sin(2 * PI * (timeSeconds / PERIOD_SECONDS + phase2 / 256.0)) * amplitude
            val q2 = center + sin(2 * PI * (auxTime / PERIOD_SECONDS + phase2 / 256.0 + 0.5)) * amplitude

            val x = ((p1 + p2) / 2.0).roundToInt().coerceIn(edgePadding, combinedWidth - 1 - edgePadding)
            val x1 = ((q1 + q2) / 2.0).roundToInt().coerceIn(edgePadding, combinedWidth - 1 - edgePadding)

            val hue = ((row * 128) / (rows.coerceAtLeast(2) - 1) + hueOffset) and 0xFF
            val color = hsvToRgb(hue, 255, 255)

            if (((row + timeSegment) and 3) != 0) {
                drawLine(x, x1, row, color, addDot = true, gradient = true)
            }
        }

        hueOffset = (hueOffset + 3) and 0xFF
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

    override fun getName(): String = "DNA Spiral"

    fun cleanup() {
        // nothing to dispose
    }

    private fun drawLine(x0: Int, x1: Int, y: Int, rgb: IntArray, addDot: Boolean, gradient: Boolean) {
        val clampedY = y.coerceIn(0, combinedHeight - 1)
        val start = x0.coerceIn(0, combinedWidth - 1)
        val end = x1.coerceIn(0, combinedWidth - 1)
        val steps = abs(end - start) + 1
        for (step in 0 until steps) {
            val t = step / (steps - 1.0).coerceAtLeast(1.0)
            val x = lerp(start, end, t).roundToInt().coerceIn(0, combinedWidth - 1)
            val scale = if (gradient) (t * 255).roundToInt().coerceIn(0, 255) else 255
            val scaled = intArrayOf(
                rgb[0] * scale / 255,
                rgb[1] * scale / 255,
                rgb[2] * scale / 255,
            )
            addPixelColor(x, clampedY, scaled)
        }

        if (addDot) {
            val head = start.coerceIn(0, combinedWidth - 1)
            val tail = end.coerceIn(0, combinedWidth - 1)
            addPixelColor(head, clampedY, intArrayOf(72, 61, 139)) // DarkSlateGray
            addPixelColor(tail, clampedY, intArrayOf(255, 255, 255)) // White
        }
    }

    private fun fadeDown() {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = pixelColors[x][y][0] * 120 / 256
                pixelColors[x][y][1] = pixelColors[x][y][1] * 120 / 256
                pixelColors[x][y][2] = pixelColors[x][y][2] * 120 / 256
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
        pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
        pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
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

    private fun beatsin8(speed: Int, minValue: Int, maxValue: Int, timebase: Int, phase: Int): Int {
        val time = System.currentTimeMillis() / speed.toDouble() + phase / 256.0
        val sine = sin(2 * PI * time)
        val amplitude = (maxValue - minValue) / 2.0
        return (minValue + amplitude + sine * amplitude).roundToInt().coerceIn(minValue, maxValue)
    }

    private fun beatsin8(speed: Int, minValue: Int, maxValue: Int, timebase: Int): Int {
        return beatsin8(speed, minValue, maxValue, timebase, 0)
    }

    private fun beatsin8(speed: Int, minValue: Int, maxValue: Int): Int {
        return beatsin8(speed, minValue, maxValue, 0, 0)
    }

    private fun sinePosition(timeSeconds: Double, min: Double, max: Double, phase: Int): Double {
        val center = (min + max) / 2.0
        val amplitude = (max - min) / 2.0
        val angle = 2 * PI * (timeSeconds / PERIOD_SECONDS + phase / 256.0)
        return center + sin(angle) * amplitude
    }

    private fun lerp(start: Int, end: Int, t: Double): Double = start + (end - start) * t

    companion object {
        private const val SPEED = 40
        private const val FREQ = 6
        private const val FADE_AMOUNT = 120
        private const val PERIOD_SECONDS = 5.5
    }
}

