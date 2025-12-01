package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

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
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var lastUpdateNs: Long = 0L
    private var hueOffset: Int = 0
    private var currentPalette: Palette? = null

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

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "DNA Spiral"

    override fun cleanup() {
        // nothing to dispose
    }

    private fun drawLine(x0: Int, x1: Int, y: Int, rgb: RgbColor, addDot: Boolean, gradient: Boolean) {
        val clampedY = y.coerceIn(0, combinedHeight - 1)
        val start = x0.coerceIn(0, combinedWidth - 1)
        val end = x1.coerceIn(0, combinedWidth - 1)
        val steps = abs(end - start) + 1
        for (step in 0 until steps) {
            val t = step / (steps - 1.0).coerceAtLeast(1.0)
            val x = lerp(start, end, t).roundToInt().coerceIn(0, combinedWidth - 1)
            val scale = if (gradient) (t * 255).roundToInt().coerceIn(0, 255) else 255
            val scaled = ColorUtils.scaleBrightness(rgb, scale / 255.0)
            addPixelColor(x, clampedY, scaled)
        }

        if (addDot) {
            val head = start.coerceIn(0, combinedWidth - 1)
            val tail = end.coerceIn(0, combinedWidth - 1)
            addPixelColor(head, clampedY, RgbColor(72, 61, 139)) // DarkSlateGray
            addPixelColor(tail, clampedY, RgbColor.WHITE)
        }
    }

    private fun fadeDown() {
        val factor = 120.0 / 256.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
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

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = value / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(hue, saturation, value)
        }
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

