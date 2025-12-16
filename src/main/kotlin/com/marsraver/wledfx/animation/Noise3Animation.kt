package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.NoiseUtils

/**
 * Noise 3
 * Lava Lamp / Void effect.
 * Maps Noise -> Palette Index AND Brightness.
 */
class Noise3Animation : BaseAnimation() {
    
    private var scale: Int = 128
    private var timePos: Long = 0
    
    override fun getName(): String = "Noise 3"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    
    override fun update(now: Long): Boolean {
        timePos += (paramSpeed / 4) + 1
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val s = (scale / 4) + 1
        val noiseVal: Int
        
        if (width > 1 && height > 1) {
             noiseVal = NoiseUtils.inoise16(x * s * 10, y * s * 10, (timePos / 256).toInt())
        } else {
             val index = getPixelIndex(x, y)
             noiseVal = NoiseUtils.inoise16(index * s * 10, (timePos / 256).toInt(), 0)
        }
        
        // Noise 3: Index based on noise, Brightness based on noise curve
        // This creates "blobs" of color floating in black/dark areas
        val val8 = (noiseVal shr 8)
        val index = (val8 + (timePos / 32)).toInt()
        
        // Map brightness: Peak at 128, tapering to edges
        // Simple sine-like curve or triangle wave using abs
        // abs(val8 - 128) * 2 = 0..255 (V shape)
        // Invert to get Peak in middle: 255 - ...
        val bri = 255 - kotlin.math.abs(val8 - 128) * 2
        
        // Get base color
        val base = getColorFromPalette(index and 0xFF)
        
        // Apply brightness curve
        return ColorUtils.scaleBrightness(base, bri / 255.0)
    }
}
