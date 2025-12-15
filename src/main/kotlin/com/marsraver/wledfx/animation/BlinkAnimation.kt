package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * Blink Animation.
 * Simply blinks the lights on and off.
 */
class BlinkAnimation : BaseAnimation() {
    
    private var lastTimeNs: Long = 0L
    private var isOn: Boolean = false
    
    override fun getName(): String = "Blink"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    override fun supportsPalette(): Boolean = false
    
    // Explicitly doesn't support palette/color if just blinking fixed white, but usually WLED blink respects Color 1 and 2
    // Let's assume Color 1 and Color 2 (Black).
    
    override fun onInit() {
        super.onInit()
        lastTimeNs = System.nanoTime()
    }
    
    override fun update(now: Long): Boolean {
        // Blink speed: 128 = 1Hz?
        // WLED logic: delay = (255 - speed) * 5ms? Or something.
        // Let's use simple logic: period in ms = (256 - speed) * 10 
        
        val periodMs = ((256 - paramSpeed) * 5L).coerceAtLeast(50L)
        val nowMs = now / 1_000_000L
        val cycle = nowMs % (periodMs * 2)
        
        isOn = cycle < periodMs
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // 1. Check if 1D or 2D logic
        // Blink is universal.
        
        if (isOn) {
            return paramColor // Default to Primary Color
        } else {
            // Secondary color? Or Black.
            // WLED Blink uses Col 1 / Col 2.
            // For now, Black.
            return RgbColor.BLACK
        }
    }
}
