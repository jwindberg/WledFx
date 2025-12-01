package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.Palette

import kotlin.math.*

/**
 * Breathe animation - Does the "standby-breathing" of well known i-Devices.
 * Smooth pulsing brightness effect.
 */
class BreatheAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private var startTime: Long = 0L

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
        startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) {
            startTime = now
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return RgbColor.BLACK
        }
        
        val speed = 128 // Default speed (0-255)
        val timeMs = (System.currentTimeMillis() - (startTime / 1_000_000L))
        
        // Counter calculation matching original
        var counter = ((timeMs * ((speed shr 3) + 10)) and 0xFFFF).toInt()
        counter = (counter shr 2) + (counter shr 4) // 0-16384 + 0-2048
        
        var varValue = 0
        if (counter < 16384) {
            if (counter > 8192) {
                counter = 8192 - (counter - 8192)
            }
            // sin16_t returns -32768 to 32767, divide by 103 gives approximately 0-255 range
            val sineValue = sin16(counter)
            varValue = sineValue / 103
        }
        
        val lum = 30 + varValue // Luminance varies from 30 to ~255
        
        // Calculate pixel index for color selection
        val pixelIndex = y * combinedWidth + x
        val paletteIndex = (pixelIndex % currentPalette.size).coerceIn(0, currentPalette.size - 1)
        val paletteColor = currentPalette[paletteIndex]
        
        // Apply luminance to palette color
        val brightness = lum.coerceIn(0, 255) / 255.0
        return RgbColor(
            (paletteColor.r * brightness).toInt().coerceIn(0, 255),
            (paletteColor.g * brightness).toInt().coerceIn(0, 255),
            (paletteColor.b * brightness).toInt().coerceIn(0, 255)
        )
    }

    override fun getName(): String = "Breathe"

    /**
     * sin16 - 16-bit sine function
     * Returns value from -32768 to 32767
     */
    private fun sin16(angle: Int): Int {
        // Convert angle to 0-65535 range
        val normalizedAngle = (angle % 65536)
        // Convert to radians (0-65535 maps to 0-2π)
        val radians = normalizedAngle * 2.0 * PI / 65536.0
        val sine = sin(radians)
        // Map from -1..1 to -32768..32767
        return (sine * 32767.0).toInt().coerceIn(-32768, 32767)
    }
}

