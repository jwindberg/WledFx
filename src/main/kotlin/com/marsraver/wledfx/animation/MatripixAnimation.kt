package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * Matripix animation - Shifting pixels with audio-reactive brightness
 * By: Andrew Tuline
 */
class MatripixAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 64
    
    private var secondHand: Int = 0
    private var lastSecondHand: Int = -1
    
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        secondHand = 0
        lastSecondHand = -1
        startTimeNs = System.nanoTime()
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000
        
        // Calculate secondHand: micros()/(256-speed)/500 % 16
        val speedFactor = (256 - speed).coerceAtLeast(1)
        secondHand = ((timeMicros / speedFactor / 500) % 16).toInt()
        
        // Get loudness (0-1024) and convert to 0-255 range
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        
        // Only update when secondHand changes
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            
            // Calculate pixel brightness: volumeRaw * intensity / 64
            val pixBri = (volume * intensity / 64).coerceIn(0, 255)
            
            // Get color from palette based on time
            val colorIndex = (timeMs % 256).toInt()
            val newColor = colorFromPalette(colorIndex, true, 0)
            
            // Shift pixels left for each row
            for (y in 0 until combinedHeight) {
                // Shift left: pixels[i] = pixels[i+1]
                for (x in 0 until combinedWidth - 1) {
                    pixelColors[x][y] = pixelColors[x + 1][y]
                }
                
                // Add new pixel at the end (right side) with audio brightness
                val blendedColor = ColorUtils.blend(RgbColor.BLACK, newColor, pixBri)
                pixelColors[combinedWidth - 1][y] = blendedColor
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

    override fun getName(): String = "Matripix"

    override fun isAudioReactive(): Boolean = true

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    /**
     * Get color from palette
     */
    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}

