package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor

/**
 * Theater Chase
 * Movie theater style marching lights.
 */
class TheaterChaseAnimation : BaseAnimation() {
    
    private var color: RgbColor = RgbColor.RED
    private var startTimeNs: Long = 0L
    
    override fun getName(): String = "Theater Chase"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true // Works on matrix effectively as 1D
    override fun supportsColor(): Boolean = true
    override fun supportsPalette(): Boolean = false
    override fun setColor(color: RgbColor) { this.color = color }

    override fun onInit() {
        startTimeNs = System.nanoTime()
        paramSpeed = 128
    }
    
    override fun update(now: Long): Boolean {
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index !in 0 until pixelCount) return RgbColor.BLACK
        
        val time = (System.nanoTime() - startTimeNs) / 1_000_000_000.0
        val speedFactor = 2.0 + (paramSpeed / 10.0)
        val step = (time * speedFactor).toInt()
        
        if ((index + step) % 3 == 0) {
            return color
        }
        return RgbColor.BLACK
    }
}
