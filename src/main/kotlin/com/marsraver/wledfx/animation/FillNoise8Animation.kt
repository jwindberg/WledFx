package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.math.MathUtils

/**
 * Fill Noise 8
 * 8-bit Noise Animation.
 */
class FillNoise8Animation : BaseAnimation() {
    
    private var timePos: Long = 0
    
    override fun getName(): String = "Fill Noise 8"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    
    override fun onInit() {
        paramSpeed = 128
        paramIntensity = 128 // Scale
    }
    
    override fun update(now: Long): Boolean {
        timePos += (paramSpeed / 4) + 1
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val s = (paramIntensity / 4) + 1
        val noiseVal: Int
        
        // 8-bit noise
        if (width > 1 && height > 1) {
             noiseVal = MathUtils.inoise8(x * s * 10, y * s * 10, (timePos / 256).toInt())
        } else {
             val index = getPixelIndex(x, y)
             noiseVal = MathUtils.inoise8(index * s * 10, (timePos / 256).toInt(), 0)
        }
        
        return getColorFromPalette(noiseVal)
    }
    
    override fun supportsIntensity(): Boolean = true
}
