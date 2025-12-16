package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.*

/**
 * DNA Spiral animation
 */
class DnaSpiralAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var lastUpdateNs: Long = 0L
    private var hueOffset: Int = 0
    
    private val FREQ = 6
    private val PERIOD_SECONDS = 5.5

    override fun getName(): String = "DNA Spiral"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        lastUpdateNs = 0L
        hueOffset = 0
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNs == 0L) lastUpdateNs = now
        fadeDown()

        val timeSeconds = now / 1_000_000_000.0
        val auxTime = timeSeconds * 0.85
        val rows = height
        val edgePadding = 1
        val center = (width - 1) / 2.0
        val amplitude = (width - 1 - edgePadding * 2) / 2.0

        val timeSegment = (timeSeconds / 0.008).toInt()
        for (row in 0 until rows) {
            val basePhase = row * FREQ
            val phase1 = basePhase
            val phase2 = basePhase + 128

            val p1 = center + sin(2 * PI * (timeSeconds / PERIOD_SECONDS + phase1 / 256.0)) * amplitude
            val p2 = center + sin(2 * PI * (auxTime / PERIOD_SECONDS + phase1 / 256.0 + 0.5)) * amplitude
            val q1 = center + sin(2 * PI * (timeSeconds / PERIOD_SECONDS + phase2 / 256.0)) * amplitude
            val q2 = center + sin(2 * PI * (auxTime / PERIOD_SECONDS + phase2 / 256.0 + 0.5)) * amplitude

            val x = ((p1 + p2) / 2.0).roundToInt().coerceIn(edgePadding, width - 1 - edgePadding)
            val x1 = ((q1 + q2) / 2.0).roundToInt().coerceIn(edgePadding, width - 1 - edgePadding)

            val hue = ((row * 128) / (rows.coerceAtLeast(2) - 1) + hueOffset) and 0xFF
            val color = getColorFromPalette(hue) // Use BaseAnimation palette support

            if (((row + timeSegment) and 3) != 0) {
                drawLine(x, x1, row, color, addDot = true, gradient = true)
            }
        }
        hueOffset = (hueOffset + 3) and 0xFF
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun drawLine(x0: Int, x1: Int, y: Int, rgb: RgbColor, addDot: Boolean, gradient: Boolean) {
        val clampedY = y.coerceIn(0, height - 1)
        val start = x0.coerceIn(0, width - 1)
        val end = x1.coerceIn(0, width - 1)
        val steps = abs(end - start) + 1
        for (step in 0 until steps) {
            val t = step / (steps - 1.0).coerceAtLeast(1.0)
            val x = lerp(start, end, t).roundToInt().coerceIn(0, width - 1)
            val scale = if (gradient) (t * 255).roundToInt().coerceIn(0, 255) else 255
            val scaled = ColorUtils.scaleBrightness(rgb, scale / 255.0)
            addPixelColor(x, clampedY, scaled)
        }
        if (addDot) {
            addPixelColor(start.coerceIn(0, width - 1), clampedY, RgbColor(72, 61, 139))
            addPixelColor(end.coerceIn(0, width - 1), clampedY, RgbColor.WHITE)
        }
    }

    private fun fadeDown() {
        val factor = 120.0 / 256.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        val current = pixelColors[x][y]
        pixelColors[x][y] = RgbColor(
            (current.r + rgb.r).coerceAtMost(255),
            (current.g + rgb.g).coerceAtMost(255),
            (current.b + rgb.b).coerceAtMost(255)
        )
    }

    private fun lerp(start: Int, end: Int, t: Double): Double = start + (end - start) * t
}
