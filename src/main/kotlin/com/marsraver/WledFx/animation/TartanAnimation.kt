package com.marsraver.WledFx.animation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tartan animation - animated plaid pattern using sine-based oscillations.
 */
class TartanAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var horizontalScale: Double = 1.0
    private var hueBase: Int = 0
    private var hueAccumulatorMs: Double = 0.0
    private var lastUpdateNanos: Long = 0L

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        offsetX = 0.0
        offsetY = 0.0
        horizontalScale = 1.0
        hueBase = 0
        hueAccumulatorMs = 0.0
        lastUpdateNanos = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now
        }
        val deltaMs = (now - lastUpdateNanos) / 1_000_000.0
        hueAccumulatorMs += deltaMs
        while (hueAccumulatorMs >= 8.0) {
            hueBase = (hueBase + 1) and 0xFF
            hueAccumulatorMs -= 8.0
        }
        lastUpdateNanos = now

        val timeSeconds = now / 1_000_000_000.0
        offsetX = beatsin(timeSeconds, 0.25, -180.0, 180.0)
        offsetY = beatsin(timeSeconds, 0.18, -180.0, 180.0, phaseOffsetSeconds = 6.0)
        horizontalScale = beatsin(timeSeconds, 0.35, 0.5, 4.0)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) {
            return intArrayOf(0, 0, 0)
        }

        val hueX = (x * horizontalScale + offsetY + hueBase).toFloat()
        val brightnessX = sin8(x * 18.0 + offsetX)
        val colorX = hsvToRgb(hueX, 200, brightnessX)

        val hueY = (y * 2.0 + offsetX + hueBase).toFloat()
        val brightnessY = sin8(y * 18.0 + offsetY)
        val colorY = hsvToRgb(hueY, 200, brightnessY)

        return intArrayOf(
            (colorX[0] + colorY[0]).coerceAtMost(255),
            (colorX[1] + colorY[1]).coerceAtMost(255),
            (colorX[2] + colorY[2]).coerceAtMost(255),
        )
    }

    override fun getName(): String = "Tartan"

    fun cleanup() {
        // No resources to release; provided for interface parity.
    }

    private fun beatsin(
        timeSeconds: Double,
        frequencyHz: Double,
        low: Double,
        high: Double,
        phaseOffsetSeconds: Double = 0.0,
    ): Double {
        val angle = 2.0 * Math.PI * (frequencyHz * (timeSeconds + phaseOffsetSeconds))
        val sine = sin(angle)
        return low + (high - low) * (sine + 1.0) / 2.0
    }

    private fun sin8(input: Double): Int {
        var angle = input % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * Math.PI
        val sine = sin(radians)
        return ((sine + 1.0) * 127.5).roundToInt().coerceIn(0, 255)
    }

    private fun hsvToRgb(hue: Float, saturation: Int, value: Int): IntArray {
        val h = (hue % 256 + 256) % 256
        val s = saturation.coerceIn(0, 255) / 255.0f
        val v = value.coerceIn(0, 255) / 255.0f

        if (s <= 0f) {
            val gray = (v * 255).roundToInt().coerceIn(0, 255)
            return intArrayOf(gray, gray, gray)
        }

        val hueSection = h / 42.666668f
        val i = hueSection.toInt()
        val f = hueSection - i

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

