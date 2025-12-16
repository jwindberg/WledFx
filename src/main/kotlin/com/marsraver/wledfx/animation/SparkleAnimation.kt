package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import kotlin.random.Random

/**
 * Sparkle Animation
 * Random pixels flash ON then immediately OFF.
 * Refactored to use standard pixelColors 2D array.
 */
class SparkleAnimation : BaseAnimation() {
    
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var color: RgbColor = RgbColor.WHITE
    
    override fun getName(): String = "Sparkle"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    override fun supportsColor(): Boolean = true
    override fun supportsPalette(): Boolean = false
    override fun setColor(color: RgbColor) { this.color = color }

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        // Default white sparkle
        paramSpeed = 128
        if (paramColor == RgbColor.BLACK) paramColor = RgbColor.WHITE
        this.color = paramColor
    }
    
    override fun update(now: Long): Boolean {
        // Clear all
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = RgbColor.BLACK
            }
        }
        
        // Add random sparkles
        // Count based on speed and total pixels
        // Original logic: 1 + speed/32
        // New logic should scale with size
        val totalPixels = width * height
        val density = (paramSpeed / 255.0) * 0.1 // up to 10% density
        val count = (totalPixels * density).toInt().coerceAtLeast(1)
        
        for (k in 0 until count) {
            val x = Random.nextInt(width)
            val y = Random.nextInt(height)
            pixelColors[x][y] = color
        }
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
}
