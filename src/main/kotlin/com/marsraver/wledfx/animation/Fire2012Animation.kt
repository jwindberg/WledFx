package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Fire 2012 (1D)
 * Classic 1D Fire simulation by Mark Kriegsman.
 */
class Fire2012Animation : BaseAnimation() {
    
    private lateinit var heat: IntArray
    
    // Parameters
    // Speed -> Cooling
    // Intensity -> Sparking?
    
    override fun getName(): String = "Fire 2012"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false // Strict 1D effect mapped to 2D
    
    // Default palette support from BaseAnimation.
    
    override fun onInit() {
        // heat array sized to pixelCount
        heat = IntArray(pixelCount)
    }
    
    override fun update(now: Long): Boolean {
        if (pixelCount == 0) return true
        
        // Map Speed/Intensity to Sparking/Cooling
        // Speed: Higher = Faster fire = Less cooling? Or More cooling?
        // Original code: Speed = Cooling rate.
        // Let's keep it simple: Cooling = paramSpeed
        // Sparking = paramIntensity (default 128)
        
        val cooling = (paramSpeed * 0.4).toInt().coerceAtLeast(20)
        val sparking = paramIntensity.coerceIn(0, 255)
        
        // Step 1. Cool down
        for (i in 0 until pixelCount) {
             val cooldown = Random.nextInt(0, ((cooling * 10) / pixelCount) + 2)
             heat[i] = (heat[i] - cooldown).coerceAtLeast(0)
        }
        
        // Step 2. Drift up
        for (k in pixelCount - 1 downTo 2) {
             heat[k] = (heat[k - 1] + heat[k - 2] + heat[k - 2]) / 3
        }
        
        // Step 3. Ignite
        if (Random.nextInt(0, 255) < sparking) {
             val y = Random.nextInt(0, 7)
             if (y < pixelCount) {
                 heat[y] = (heat[y] + Random.nextInt(160, 255)).coerceAtMost(255)
             }
        }
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) {
             val h = heat[index]
             // TODO: Support Palette Mapping if set?
             // Original used heatColor(t).
             // WLED usually maps heat 0-255 to Palette Index 0-240.
             
             if (paramPalette != null) {
                 // Map heat 0-255 to palette
                 return getColorFromPalette(h)
             }
             return ColorUtils.heatColor(h)
        }
        return RgbColor.BLACK
    }
}
