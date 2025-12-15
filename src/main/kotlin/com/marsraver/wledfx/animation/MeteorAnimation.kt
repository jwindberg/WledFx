package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Meteor - Shooting star with fading trail
 */
class MeteorAnimation : BaseAnimation() {
    
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var trail: IntArray  // Brightness values for trail
    
    // speed, intensity from BaseAnimation
    private var smooth: Boolean = true
    private var gradient: Boolean = true
    
    private var step: Long = 0
    private var startTimeNs: Long = 0L
    
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    override fun getName(): String = "Meteor"
    
    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        trail = IntArray(width) { 0 }
        startTimeNs = System.nanoTime()
    }
    
    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val meteorSize = 1 + width / 20  // 5% of width
        
        // Calculate meteor position
        val meteorStart = if (smooth) {
            val stepByte = ((step shr 6) and 0xFF).toInt()
            (stepByte * width) / 255
        } else {
            val counter = ((now - startTimeNs) / 1_000_000L) * ((paramSpeed shr 2) + 8)
            ((counter * width) shr 16).toInt() % width
        }
        
        val maxBrightness = if (!gradient) 240 else 255
        // Use paramIntensity (inherited) instead of local var
        
        // Fade all LEDs with random variation
        for (i in 0 until width) {
            if (Random.nextInt(256) <= 255 - paramIntensity) {
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
            val index = (meteorStart + j) % width
            trail[index] = maxBrightness
        }
        
        // Apply trail to all rows (simple replication across Y)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val brightness = trail[x]
                val color = if (gradient) {
                    // Gradient mode: use position for color
                    colorFromPalette(x * 255 / width, brightness)
                } else {
                    // Non-gradient: use brightness for color
                    colorFromPalette(brightness, brightness)
                }
                pixelColors[x][y] = color
            }
        }
        
        step += paramSpeed + 1
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Here we rely on the pixelColors array populated in update()
        // Could technically optimize to compute on fly to skip array 
        // but Meteor state (trail) is complex, so array is fine.
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun colorFromPalette(index: Int, brightness: Int): RgbColor {
        val palette = paramPalette?.colors
        if (palette != null && palette.isNotEmpty()) {
            val paletteIndex = (index % 256) * palette.size / 256
            val baseColor = palette[paletteIndex.coerceIn(0, palette.size - 1)]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Default: use HSV with brightness
            // BaseAnimation helper doesn't support brightness, so we use ColorUtils directly
            return ColorUtils.hsvToRgb(index % 256, 255, brightness)
        }
    }
}
