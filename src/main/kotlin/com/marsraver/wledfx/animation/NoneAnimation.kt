package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.color.RgbColor

/**
 * A blank animation that displays nothing (all LEDs off).
 * This is used as a "None" option in the animation selector.
 */
class NoneAnimation : LedAnimation {
    private var width: Int = 0
    private var height: Int = 0
    
    override fun getName(): String = "None"
    
    override fun init(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
    
    override fun update(now: Long): Boolean {
        // Always return true to keep animation running
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Always return black (off)
        return RgbColor(0, 0, 0)
    }
    
    override fun cleanup() {
        // Nothing to clean up
    }
    
    override fun supportsColor(): Boolean = false
    override fun supportsPalette(): Boolean = false
    override fun supportsSpeed(): Boolean = false
    override fun supportsMultiMode(): Boolean = false
    override fun supportsCatMode(): Boolean = false
    override fun supportsTextInput(): Boolean = false
    override fun supportsSpeedFactor(): Boolean = false
}
