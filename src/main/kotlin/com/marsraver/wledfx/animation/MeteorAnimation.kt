package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.random.Random
import kotlin.math.min

/**
 * Meteor - Shooting star with fading trail
 * Original C code from WLED v0.15.3 FX.cpp line 2404
 * 
 * Features:
 * - Meteor size is 5% of strip length
 * - Trail fades with random variation
 * - Smooth mode for gradual movement
 * - Gradient coloring option
 */
class MeteorAnimation : LedAnimation {
    
    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var trail: IntArray  // Brightness values for trail
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128  // Trail fade rate
    private var smooth: Boolean = true
    private var gradient: Boolean = true
    
    private var step: Long = 0
    private var startTimeNs: Long = 0L
    
    override fun supportsPalette(): Boolean = true
    
    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }
    
    override fun getPalette(): Palette? = currentPalette
    
    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        trail = IntArray(combinedWidth) { 0 }
        startTimeNs = System.nanoTime()
    }
    
    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val meteorSize = 1 + combinedWidth / 20  // 5% of width
        
        // Calculate meteor position
        val meteorStart = if (smooth) {
            val stepByte = ((step shr 6) and 0xFF).toInt()
            (stepByte * combinedWidth) / 255
        } else {
            val counter = ((now - startTimeNs) / 1_000_000L) * ((speed shr 2) + 8)
            ((counter * combinedWidth) shr 16).toInt() % combinedWidth
        }
        
        val maxBrightness = if (!gradient) 240 else 255
        
        // Fade all LEDs with random variation
        for (i in 0 until combinedWidth) {
            if (Random.nextInt(256) <= 255 - intensity) {
                if (smooth) {
                    if (trail[i] > 0) {
                        val change = trail[i] + 4 - Random.nextInt(24)  // -20 to +4
                        trail[i] = change.coerceIn(0, maxBrightness)
                    }
                } else {
                    trail[i] = (trail[i] * (128 + Random.nextInt(127))) / 255
                }
            }
        }
        
        // Draw meteor head
        for (j in 0 until meteorSize) {
            val index = (meteorStart + j) % combinedWidth
            trail[index] = maxBrightness
        }
        
        // Apply trail to all rows
        for (y in 0 until combinedHeight) {
            for (x in 0 until combinedWidth) {
                val brightness = trail[x]
                val color = if (gradient) {
                    // Gradient mode: use position for color
                    colorFromPalette(x * 255 / combinedWidth, brightness)
                } else {
                    // Non-gradient: use brightness for color
                    colorFromPalette(brightness, brightness)
                }
                pixelColors[x][y] = color
            }
        }
        
        step += speed + 1
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    override fun getName(): String = "Meteor"
    
    override fun supportsSpeed(): Boolean = true
    
    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }
    
    override fun getSpeed(): Int = speed
    
    private fun colorFromPalette(index: Int, brightness: Int): RgbColor {
        val palette = currentPalette?.colors
        if (palette != null && palette.isNotEmpty()) {
            val paletteIndex = (index % 256) * palette.size / 256
            val baseColor = palette[paletteIndex.coerceIn(0, palette.size - 1)]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Default: use HSV with brightness
            return ColorUtils.hsvToRgb(index % 256, 255, brightness)
        }
    }
}
