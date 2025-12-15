package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.LoudnessMeter
import com.marsraver.wledfx.math.MathUtils

/**
 * Matripix animation - Shifting pixels with audio-reactive brightness
 * By: Andrew Tuline
 */
class MatripixAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // Internal state
    private var secondHand: Int = 0
    private var lastSecondHand: Int = -1
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        secondHand = 0
        lastSecondHand = -1
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 64
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000
        
        // Calculate secondHand: micros()/(256-speed)/500 % 16
        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        secondHand = ((timeMicros / speedFactor / 500) % 16).toInt()
        
        // Get loudness (0-1024) and convert to 0-255 range
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        
        // Only update when secondHand changes
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            
            // Calculate pixel brightness
            val pixBri = (volume * paramIntensity / 64).coerceIn(0, 255)
            
            // Get color from palette based on time
            val colorIndex = (timeMs % 256).toInt()
            val newColor = colorFromPalette(colorIndex, true, 0)
            
            // Shift pixels left for each row
            for (y in 0 until height) {
                // Shift left: pixels[i] = pixels[i+1]
                for (x in 0 until width - 1) {
                    pixelColors[x][y] = pixelColors[x + 1][y]
                }
                
                // Add new pixel at the end (right side) with audio brightness
                val blendedColor = ColorUtils.blend(RgbColor.BLACK, newColor, pixBri)
                pixelColors[width - 1][y] = blendedColor
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Matripix"
    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
