package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.math.MathUtils

/**
 * Breathe animation - Does the "standby-breathing" of well known i-Devices.
 * Smooth pulsing brightness effect.
 */
class BreatheAnimation : BaseAnimation() {

    private var startTime: Long = 0L

    override fun getName(): String = "Breathe"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true

    override fun onInit() {
        startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) startTime = now
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Use paramSpeed from BaseAnimation
        val timeMs = (System.currentTimeMillis() - (startTime / 1_000_000L))
        
        // Counter calculation matching original
        var counter = ((timeMs * ((paramSpeed shr 3) + 10)) and 0xFFFF).toInt()
        counter = (counter shr 2) + (counter shr 4) 
        
        var varValue = 0
        if (counter < 16384) {
            if (counter > 8192) {
                counter = 8192 - (counter - 8192)
            }
            val sineValue = MathUtils.sin16(counter)
            varValue = sineValue / 103
        }
        
        val lum = 30 + varValue 
        
        // Use getPixelIndex (BaseAnimation delegating to Mapper)
        val pixelIndex = getPixelIndex(x, y)
        
        // Use helper with paramPalette
        val pal = paramPalette?.colors
        val paletteColor = if (pal != null && pal.isNotEmpty()) {
            val paletteIndex = (pixelIndex % pal.size).coerceIn(0, pal.size - 1)
            pal[paletteIndex]
        } else {
             RgbColor.WHITE
        }
        
        val brightness = lum.coerceIn(0, 255) / 255.0
        return RgbColor(
            (paletteColor.r * brightness).toInt().coerceIn(0, 255),
            (paletteColor.g * brightness).toInt().coerceIn(0, 255),
            (paletteColor.b * brightness).toInt().coerceIn(0, 255)
        )
    }
}
