package com.marsraver.wledfx.animation

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Colored Bursts animation - rainbow rays radiating from random centers.
 */
class ColoredBurstsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var time: Int = 0

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        time = 0
    }

    override fun update(now: Long): Boolean {
        time++
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        val t = time * 0.05
        val centerX = combinedWidth / 2.0 + kotlin.math.cos(t * 0.5) * combinedWidth * 0.3
        val centerY = combinedHeight / 2.0 + kotlin.math.sin(t * 0.3) * combinedHeight * 0.3

        val dx = x - centerX
        val dy = y - centerY
        val distance = hypot(dx, dy)

        if (distance < 1) {
            val intensity = 128 + (time % 128)
            return intArrayOf(intensity, intensity, intensity)
        }

        var angle = Math.toDegrees(kotlin.math.atan2(dy, dx))
        if (angle < 0) angle += 360.0

        val numRays = 16.0
        val rayWidth = 360.0 / numRays
        val rayPos = (angle + time * 2) % 360
        val rayIndex = floor(rayPos / rayWidth)
        val rayOffset = (rayPos % rayWidth) / rayWidth
        val rayCenterOffset = abs(rayOffset - 0.5)

        if (rayCenterOffset > 0.3) {
            return intArrayOf(0, 0, 0)
        }

        val hue = (rayIndex * rayWidth + time * 2) % 360
        val saturation = 1.0
        val maxDist = hypot(combinedWidth.toDouble(), combinedHeight.toDouble())
        var value = (1.0 - (distance / maxDist) * 0.8).coerceAtLeast(0.0)
        val pulse = 0.5 + 0.5 * sin(time * 0.1)
        value *= pulse

        return hsvToRgb(hue, saturation, value)
    }

    override fun getName(): String = "Colored Bursts"

    private fun hsvToRgb(h: Double, s: Double, v: Double): IntArray {
        if (s <= 0.0) {
            val value = (v * 255).toInt().coerceIn(0, 255)
            return intArrayOf(value, value, value)
        }

        val c = (v * s * 255).toInt()
        val hPrime = h / 60.0
        val x = (c * (1 - abs(hPrime % 2 - 1))).toInt()
        val m = (v * 255 - c).toInt()

        val (r1, g1, b1) = when {
            hPrime < 1 -> Triple(c, x, 0)
            hPrime < 2 -> Triple(x, c, 0)
            hPrime < 3 -> Triple(0, c, x)
            hPrime < 4 -> Triple(0, x, c)
            hPrime < 5 -> Triple(x, 0, c)
            else -> Triple(c, 0, x)
        }

        return intArrayOf(
            (r1 + m).coerceIn(0, 255),
            (g1 + m).coerceIn(0, 255),
            (b1 + m).coerceIn(0, 255)
        )
    }
}

