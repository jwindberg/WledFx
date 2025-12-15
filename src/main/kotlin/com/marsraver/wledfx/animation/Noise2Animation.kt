package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.math.NoiseUtils

/**
 * Noise 2
 * Palette Noise with cycling colors.
 * Mapped to Time + Noise value.
 */
class Noise2Animation : BaseAnimation() {
    
    private var scale: Int = 128
    private var timePos: Long = 0
    
    override fun getName(): String = "Noise 2"
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
        
        // Noise 2: Index = Noise value + Time
        // Causes colors to "flow" through the noise pattern
        val colorIndex = ((noiseVal shr 8) + (timePos / 32)).toInt().coerceIn(0, 255)
        
        return getColorFromPalette(colorIndex)
    }
}
