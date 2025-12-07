package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * PixelWave animation - Wave expanding from center based on audio
 * By: Andrew Tuline
 */
class PixelWaveAnimation : LedAnimation {

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
        
        // Fade out all pixels slightly each frame
        fadeOut(240)
        
        // Calculate secondHand: micros()/(256-speed)/500+1 % 16
        // Higher speed = faster ticks
        val speedFactor = (256 - speed).coerceAtLeast(1)
        val newSecondHand = ((timeMicros / speedFactor / 500 + 1) % 16).toInt()
        
        // Get loudness (0-1024) and convert to 0-255 range
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        
        // Update when secondHand changes (creates new wave pulse)
        if (newSecondHand != secondHand) {
            secondHand = newSecondHand
            
            // Calculate pixel brightness: volume * intensity / 64
            // Ensure minimum brightness so wave is visible even with low audio
            val pixBri = ((volume * intensity / 64) + 50).coerceIn(50, 255)
            
            // Get color from palette based on time
            val colorIndex = (timeMs % 256).toInt()
            val color = colorFromPalette(colorIndex, true, 0)
            
            // Set center pixel with audio-reactive brightness
            val centerX = combinedWidth / 2
            val centerY = combinedHeight / 2
            val centerColor = ColorUtils.blend(RgbColor.BLACK, color, pixBri)
            setPixelColor(centerX, centerY, centerColor)
        }
        
        // Always expand wave outward from center (every frame)
        expandWaveFromCenter()
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "PixelWave"

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

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }


    /**
     * Expand wave from center - shifts pixels outward in all directions
     * Each pixel moves one step away from center, creating an expanding wave effect
     */
    private fun expandWaveFromCenter() {
        val centerX = combinedWidth / 2
        val centerY = combinedHeight / 2
        
        // Create temporary buffer to hold current state
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        
        // Copy current state
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                temp[x][y] = pixelColors[x][y]
            }
        }
        
        // Shift pixels outward from center
        // For each pixel, copy from a pixel one step closer to center
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                if (x == centerX && y == centerY) {
                    // Center pixel stays (already set in update)
                    continue
                }
                
                // Calculate direction vector from center
                val dx = x - centerX
                val dy = y - centerY
                
                // Calculate distance from center
                val distance = sqrt((dx * dx + dy * dy).toDouble())
                
                if (distance < 0.5) {
                    // Very close to center, keep as is
                    continue
                }
                
                // Get source pixel (one step closer to center)
                // Move inward by a small amount (about 1 pixel)
                val stepSize = 1.0 / distance.coerceAtLeast(1.0)
                val sourceX = centerX + (dx * (1.0 - stepSize)).roundToInt()
                val sourceY = centerY + (dy * (1.0 - stepSize)).roundToInt()
                
                if (sourceX in 0 until combinedWidth && sourceY in 0 until combinedHeight) {
                    // Copy from source (which is closer to center)
                    pixelColors[x][y] = temp[sourceX][sourceY]
                } else {
                    // Out of bounds, fade to black
                    pixelColors[x][y] = RgbColor.BLACK
                }
            }
        }
    }
    
    /**
     * Fade out all pixels by a given amount
     */
    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
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

