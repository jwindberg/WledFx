package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

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
        time = 0
    }

    override fun update(now: Long): Boolean {
        time++
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val t = time * 0.05
        val centerX = combinedWidth / 2.0 + kotlin.math.cos(t * 0.5) * combinedWidth * 0.3
        val centerY = combinedHeight / 2.0 + kotlin.math.sin(t * 0.3) * combinedHeight * 0.3

        val dx = x - centerX
        val dy = y - centerY
        val distance = hypot(dx, dy)

        if (distance < 1) {
            val intensity = 128 + (time % 128)
            return RgbColor(intensity, intensity, intensity)
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
            return RgbColor.BLACK
        }

        val maxDist = hypot(combinedWidth.toDouble(), combinedHeight.toDouble())
        var value = (1.0 - (distance / maxDist) * 0.8).coerceAtLeast(0.0)
        val pulse = 0.5 + 0.5 * sin(time * 0.1)
        value *= pulse

        // Use palette if available, otherwise use HSV
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val hue = (rayIndex * rayWidth + time * 2) % 360
            val paletteIndex = ((hue / 360.0) * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val factor = value.coerceIn(0.0, 1.0)
            return ColorUtils.scaleBrightness(baseColor, factor)
        } else {
            val hue = (rayIndex * rayWidth + time * 2) % 360
            val saturation = 1.0
            return hsvToRgb(hue, saturation, value)
        }
    }

    override fun getName(): String = "Colored Bursts"

    private fun hsvToRgb(h: Double, s: Double, v: Double): RgbColor {
        return ColorUtils.hsvToRgb(h.toFloat(), s.toFloat(), v.toFloat())
    }
}

