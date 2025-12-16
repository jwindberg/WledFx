package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.math.MathUtils

/**
 * BPM animation - Colored stripes pulsing at a defined Beats-Per-Minute (BPM).
 */
class BpmAnimation : BaseAnimation() {
    
    // We don't strictly need startTime as beatsin8 uses system time / millis internally if provided
    // MathUtils.beatsin8 takes timeMs
    private var startTime: Long = 0L

    override fun getName(): String = "BPM"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) startTime = now
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val palette = paramPalette?.colors ?: return RgbColor.BLACK
        
        // paramSpeed controls BPM
        // original was val speed = 32 (default)
        // paramSpeed is 0-255. 
        // 32 is slow. Let's use paramSpeed directly? 
        // Original logic: val speed = 32. 
        // Let's use paramSpeed but maybe scale it? 
        // If paramSpeed is 128 (default in Base), that might be fast.
        // WLED standard is usually direct mapping for BPM effects but typically centralized.
        // Let's use paramSpeed directly.
        
        val timeMs = System.currentTimeMillis() - startTime
        val stp = ((timeMs / 20) and 0xFF).toInt()
        
        // Use MathUtils.beatsin8
        // Note: original beat8 used System.currentTimeMillis() inside, 
        // but MathUtils.beatsin8 takes timeMs for consistent frame timing.
        val beat = MathUtils.beatsin8(paramSpeed, 64, 255, timeMs)

        val pixelIndex = y * width + x
        val colorIndex = (stp + (pixelIndex * 2)) % 256
        val paletteIndex = (colorIndex % palette.size).coerceIn(0, palette.size - 1)
        val baseColor = palette[paletteIndex]
        
        val brightnessOffset = (beat - stp + (pixelIndex * 10)) % 256
        val brightnessFactor = brightnessOffset / 255.0
        
        return RgbColor(
            (baseColor.r * brightnessFactor).toInt().coerceIn(0, 255),
            (baseColor.g * brightnessFactor).toInt().coerceIn(0, 255),
            (baseColor.b * brightnessFactor).toInt().coerceIn(0, 255)
        )
    }
}
