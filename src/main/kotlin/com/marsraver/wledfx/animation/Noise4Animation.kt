package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.NoiseUtils

/**
 * Noise 4
 * Fire-like effect.
 * High contrast mapping.
 */
class Noise4Animation : BaseAnimation() {
    
    private var scale: Int = 128
    private var timePos: Long = 0
    
    override fun getName(): String = "Noise 4"
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
        
        // Noise 4: Wraps palette index rapidly
        val val8 = (noiseVal shr 8)
        val index = (val8 * 3) + (timePos / 16).toInt() // Wraps 3 times
        
        // Sharp brightness curve
        // Power function or threshold?
        // Let's use standard palette retrieval but with the fast wrapping index
        
        return getColorFromPalette(index and 0xFF)
    }
}
