package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.math.NoiseUtils

/**
 * Noise 1
 * Standard Palette Noise.
 * Maps Noise -> Palette Index.
 */
class Noise1Animation : BaseAnimation() {
    
    private var scale: Int = 128
    private var timePos: Long = 0
    
    override fun getName(): String = "Noise 1"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    
    override fun update(now: Long): Boolean {
        // Use paramSpeed from BaseAnimation
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
        
        val paletteIndex = (noiseVal shr 8).coerceIn(0, 255)
        return getColorFromPalette(paletteIndex)
    }
}
