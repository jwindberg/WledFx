package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.*

/**
 * Android loading circle animation - A rotating arc that grows and shrinks.
 */
class AndroidAnimation : BaseAnimation() {

    private var counter: Int = 0
    private var size: Int = 2 
    private var shrinking: Boolean = false
    private var startPosition: Int = 0 
    
    private var lastUpdateTime: Long = 0L

    override fun getName(): String = "Android"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        counter = 0
        size = 2
        shrinking = false
        startPosition = 0
        lastUpdateTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = now
            return true
        }

        val elapsed = now - lastUpdateTime
        val maxRadius = min(width, height) / 2
        val circumference = (2 * PI * maxRadius).toInt().coerceAtLeast(1)
        val baseDelay = 3_000_000L + ((8L * (255 - paramSpeed)) / circumference) * 1_000_000L
        val delay = baseDelay / 100 
        
        if (elapsed < delay) return true
        
        lastUpdateTime = now

        val maxSizeDegrees = (paramIntensity * 360) / 255

        if (size > maxSizeDegrees) {
            shrinking = true
        } else if (size < 2) {
            shrinking = false
        }

        if (!shrinking) {
            if ((counter % 3) == 1) {
                startPosition = (startPosition + 1) % 360
            } else {
                size++
            }
        } else {
            startPosition = (startPosition + 1) % 360
            if ((counter % 3) != 1) {
                size--
            }
        }

        counter++
        if (startPosition >= 360) startPosition = 0

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val centerX = width / 2.0
        val centerY = height / 2.0
        
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt(dx * dx + dy * dy)
        val maxRadius = min(width, height) / 2.0
        
        // Calculate angle (0-360)
        var angle = Math.toDegrees(atan2(dy, dx))
        if (angle < 0) angle += 360.0
        
        val endPosition = (startPosition + size) % 360
        val isInArc = if (startPosition < endPosition) {
            angle >= startPosition && angle < endPosition
        } else {
            angle >= startPosition || angle < endPosition
        }
        
        if (distance <= maxRadius && isInArc) {
            return RgbColor(200, 200, 200)
        } else {
            // Palette or rainbow
            val palette = paramPalette?.colors
            if (palette != null && palette.isNotEmpty()) {
                val paletteIndex = ((angle / 360.0) * palette.size).toInt().coerceIn(0, palette.size - 1)
                return palette[paletteIndex]
            } else {
                val hue = (angle / 360.0 * 255).toInt()
                return ColorUtils.hsvToRgb(hue, 255, 128)
            }
        }
    }
}
