package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor

/**
 * Solid Color Animation
 * Simply displays a solid color across the entire strip.
 */
class SolidAnimation : BaseAnimation() {
    
    private var color: RgbColor = RgbColor.RED
    
    override fun getName(): String = "Solid"
    override fun is1D(): Boolean = true

    override fun is2D(): Boolean = false // Can be 2D really, but acts as 1D fill
    override fun supportsColor(): Boolean = true
    override fun supportsPalette(): Boolean = false
    override fun supportsSpeed(): Boolean = false
    override fun setColor(color: RgbColor) { this.color = color }

    override fun onInit() {
        // No init
    }
    
    override fun update(now: Long): Boolean {
        // Static
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return color
    }
}
