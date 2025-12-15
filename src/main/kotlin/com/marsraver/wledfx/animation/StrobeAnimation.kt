package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * Strobe Animation
 * Flashing light with short duty cycle (strobe effect).
 */
class StrobeAnimation : BaseAnimation() {
    
    private var startTimeNs: Long = 0L
    
    override fun getName(): String = "Strobe"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true // Doesn't matter, global effect
    
    // Default color white if not set
    override fun onInit() {
        startTimeNs = System.nanoTime()
        // If paramColor hasn't been set externally yet, defaulting to white is fine (handled in base)
    }
    
    override fun update(now: Long): Boolean {
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val now = System.nanoTime()
        val time = (now - startTimeNs) / 1_000_000_000.0
        
        // Strobe freq: 1Hz to 20Hz
        val freq = 1.0 + (paramSpeed / 10.0) // 1 to 26 Hz
        val period = 1.0 / freq
        
        // Duty cycle: 10% on
        val onTime = period * 0.1
        val phase = time % period
        
        return if (phase < onTime) {
            paramColor
        } else {
            RgbColor.BLACK
        }
    }
}
