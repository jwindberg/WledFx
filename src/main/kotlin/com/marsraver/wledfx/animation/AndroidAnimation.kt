package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.*

/**
 * Android loading circle animation - A rotating arc that grows and shrinks.
 */
class AndroidAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    
    private var counter: Int = 0
    private var size: Int = 2 // Size in degrees
    private var shrinking: Boolean = false
    private var startPosition: Int = 0 // Start angle in degrees
    
    private var lastUpdateTime: Long = 0L

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
        // Speed control: faster when speed is higher (255 - speed gives delay)
        // Original: 3 + ((8 * (255 - speed)) / SEGLEN)
        // For 2D, use circumference approximation: 2 * PI * radius
        val maxRadius = min(combinedWidth, combinedHeight) / 2
        val circumference = (2 * PI * maxRadius).toInt().coerceAtLeast(1)
        val speed = 128 // Default speed (0-255, higher = faster)
        val baseDelay = 3_000_000L + ((8L * (255 - speed)) / circumference) * 1_000_000L
        val delay = baseDelay / 100 // Scale down for smoother animation
        
        if (elapsed < delay) {
            return true
        }
        
        lastUpdateTime = now

        // Calculate intensity-based max size in degrees
        // Original uses: (intensity * SEGLEN) / 255
        // For 2D, convert to degrees: (intensity * 360) / 255
        val intensity = 128 // Default intensity (0-255)
        val maxSizeDegrees = (intensity * 360) / 255

        // Determine if we should grow or shrink
        if (size > maxSizeDegrees) {
            shrinking = true
        } else if (size < 2) {
            shrinking = false
        }

        if (!shrinking) {
            // Growing
            if ((counter % 3) == 1) {
                startPosition = (startPosition + 1) % 360
            } else {
                size++
            }
        } else {
            // Shrinking
            startPosition = (startPosition + 1) % 360
            if ((counter % 3) != 1) {
                size--
            }
        }

        counter++
        if (startPosition >= 360) {
            startPosition = 0
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val centerX = combinedWidth / 2.0
        val centerY = combinedHeight / 2.0
        
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt(dx * dx + dy * dy)
        val maxRadius = min(combinedWidth, combinedHeight) / 2.0
        
        // Calculate angle in degrees (0-360)
        var angle = Math.toDegrees(atan2(dy, dx))
        if (angle < 0) angle += 360.0
        
        // Check if this pixel is within the arc
        val endPosition = (startPosition + size) % 360
        val isInArc = if (startPosition < endPosition) {
            angle >= startPosition && angle < endPosition
        } else {
            angle >= startPosition || angle < endPosition
        }
        
        // Only draw arc within the circle radius
        if (distance <= maxRadius && isInArc) {
            // Use primary color (white/light color for Android style)
            return RgbColor(200, 200, 200)
        } else {
            // Use palette color based on position
            val currentPalette = this.currentPalette?.colors
            if (currentPalette != null && currentPalette.isNotEmpty()) {
                val paletteIndex = ((angle / 360.0) * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
                return currentPalette[paletteIndex]
            } else {
                // Default rainbow based on angle
                val hue = (angle / 360.0 * 255).toInt()
                return hsvToRgb(hue, 255, 128)
            }
        }
    }

    override fun getName(): String = "Android"

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): RgbColor {
        return ColorUtils.hsvToRgb(hue, saturation, value)
    }
}

