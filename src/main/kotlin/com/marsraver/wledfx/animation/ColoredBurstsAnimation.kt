package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.*

/**
 * Colored Bursts animation - rainbow rays radiating from random centers.
 */
class ColoredBurstsAnimation : BaseAnimation() {

    private var time: Int = 0

    override fun getName(): String = "Colored Bursts"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        time = 0
    }

    override fun update(now: Long): Boolean {
        time++
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val t = time * 0.05
        val centerX = width / 2.0 + cos(t * 0.5) * width * 0.3
        val centerY = height / 2.0 + sin(t * 0.3) * height * 0.3

        val dx = x - centerX
        val dy = y - centerY
        val distance = hypot(dx, dy)

        if (distance < 1) {
            val intensity = 128 + (time % 128)
            return RgbColor(intensity, intensity, intensity)
        }

        var angle = Math.toDegrees(atan2(dy, dx))
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

        val maxDist = hypot(width.toDouble(), height.toDouble())
        var value = (1.0 - (distance / maxDist) * 0.8).coerceAtLeast(0.0)
        val pulse = 0.5 + 0.5 * sin(time * 0.1)
        value *= pulse

        val palette = paramPalette?.colors
        if (palette != null && palette.isNotEmpty()) {
            val hue = (rayIndex * rayWidth + time * 2) % 360
            val paletteIndex = ((hue / 360.0) * palette.size).toInt().coerceIn(0, palette.size - 1)
            val baseColor = palette[paletteIndex]
            val factor = value.coerceIn(0.0, 1.0)
            return ColorUtils.scaleBrightness(baseColor, factor)
        } else {
            val hue = (rayIndex * rayWidth + time * 2) % 360
            return ColorUtils.hsvToRgb(hue.toFloat(), 1.0f, value.toFloat())
        }
    }
}
