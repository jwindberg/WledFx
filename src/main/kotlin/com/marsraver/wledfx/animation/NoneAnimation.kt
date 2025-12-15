package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * A blank animation that displays nothing (all LEDs off).
 * This is used as a "None" option in the animation selector.
 */
class NoneAnimation : BaseAnimation() {
    
    override fun getName(): String = "None"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    
    override fun onInit() {
        // Nothing to init
    }
    
    override fun update(now: Long): Boolean {
        // Keep running but do nothing
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return RgbColor.BLACK
    }
    
    override fun supportsColor(): Boolean = false
    override fun supportsPalette(): Boolean = false
    override fun supportsSpeed(): Boolean = false
    override fun supportsIntensity(): Boolean = false
}
