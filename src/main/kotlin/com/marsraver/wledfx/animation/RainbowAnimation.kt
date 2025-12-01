package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

/**
 * Rainbow animation - Cycles all LEDs through a rainbow
 */
class RainbowAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var speed: Int = 128
    private var intensity: Int = 128
    
    private var startTimeNs: Long = 0L

    override fun supportsPalette(): Boolean = false

    override fun setPalette(palette: Palette) {
        // Not used
    }

    override fun getPalette(): Palette? {
        return null
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Calculate counter: (strip.now * ((SEGMENT.speed >> 2) +2)) & 0xFFFF then >> 8
        val speedFactor = (speed shr 2) + 2
        val counter = ((timeMs * speedFactor).toLong() and 0xFFFF).toInt()
        val hue = counter shr 8
        
        // Get color from color wheel
        val rainbowColor = colorWheel(hue)
        
        // Fill entire grid with color
        val fillColor = if (intensity < 128) {
            // Blend with white based on intensity
            val blendAmount = 128 - intensity
            ColorUtils.blend(rainbowColor, RgbColor.WHITE, blendAmount)
        } else {
            rainbowColor
        }
        
        // Fill all pixels
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = fillColor
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Rainbow"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * color_wheel - Converts a position (0-255) to a rainbow color
     * Equivalent to FastLED's color_wheel function
     */
    private fun colorWheel(pos: Int): RgbColor {
        val hue = (pos % 256 + 256) % 256
        // Full saturation and brightness for rainbow
        return ColorUtils.hsvToRgb(hue, 255, 255)
    }
}

