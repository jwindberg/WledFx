package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * Scan Animation
 * A single dot moving back and forth (like Larson Scanner but no trail/fade).
 */
class ScanAnimation : BaseAnimation() {
    
    private var startTimeNs: Long = 0L
    private var color: RgbColor = RgbColor.RED
    
    override fun getName(): String = "Scan"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    override fun supportsColor(): Boolean = true
    override fun setColor(color: RgbColor) { this.color = color }

    override fun onInit() {
        startTimeNs = System.nanoTime()
        paramSpeed = 128
    }
    
    override fun update(now: Long): Boolean { return true }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Use BaseAnimation's getPixelIndex for standardized 1D mapping if available?
        // But for now, let's keep the manual serpentine to match exact behavior unless we want to change it.
        // Usually, 1D effects on 2D grids behave as a single strip.
        val index = getPixelIndex(x, y) // BaseAnimation helper
        
        if (index !in 0 until pixelCount) return RgbColor.BLACK
        
        val time = (System.nanoTime() - startTimeNs) / 1_000_000_000.0
        val speedFactor = 0.5 + (paramSpeed / 128.0) * 3.0
        val period = 1.0 / speedFactor 
        val phase = (time % period) / period 
        
        val pos = if (phase < 0.5) {
            phase * 2
        } else {
            (1.0 - phase) * 2
        }
        
        val activeIndex = (pos * (pixelCount - 1)).toInt().coerceIn(0, pixelCount - 1)
        
        // With large counts, one pixel is hard to see.
        // Let's make the dot 3 pixels wide?
        if (index >= activeIndex - 1 && index <= activeIndex + 1) {
            return color
        }
        return RgbColor.BLACK
    }
}
