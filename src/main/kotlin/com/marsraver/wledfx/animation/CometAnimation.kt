package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.Palette
import kotlin.math.min

/**
 * Comet (Lighthouse) - Continuous comet with fading trail
 * Original C code from WLED v0.15.3 FX.cpp line 1286
 * 
 * Features:
 * - Continuous movement around strip
 * - Fade rate controlled by intensity
 * - Draws line from last position to current
 * - Smooth wrapping at boundaries
 */
class CometAnimation : LedAnimation {
    
    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128  // Fade rate
    
    private var lastIndex: Int = 0
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
        startTimeNs = System.nanoTime()
        lastIndex = 0
    }
    
    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        // Calculate current position
        val counter = ((now - startTimeNs) / 1_000_000L) * ((speed shr 2) + 1) and 0xFFFF
        val index = ((counter * combinedWidth) shr 16).toInt() % combinedWidth
        
        // Fade all pixels
        fadeOut(intensity)
        
        // Draw comet from last position to current
        if (index > lastIndex) {
            // Moving forward
            for (i in lastIndex..index) {
                drawColumn(i)
            }
        } else if (index < lastIndex && index < 10) {
            // Wrapped around - draw from 0 to current
            for (i in 0..index) {
                drawColumn(i)
            }
        }
        
        // Always draw current position
        drawColumn(index)
        
        lastIndex = index
        return true
    }
    
    private fun fadeOut(fadeAmount: Int) {
        val fadeFactor = fadeAmount / 255.0
        for (y in 0 until combinedHeight) {
            for (x in 0 until combinedWidth) {
                pixelColors[x][y] = scaleColor(pixelColors[x][y], 1.0 - fadeFactor)
            }
        }
    }
    
    private fun drawColumn(x: Int) {
        if (x < 0 || x >= combinedWidth) return
        
        val color = colorFromPalette(x)
        for (y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }
    
    private fun scaleColor(color: RgbColor, factor: Double): RgbColor {
        return RgbColor(
            (color.r * factor).toInt().coerceIn(0, 255),
            (color.g * factor).toInt().coerceIn(0, 255),
            (color.b * factor).toInt().coerceIn(0, 255)
        )
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    override fun getName(): String = "Comet"
    
    override fun supportsSpeed(): Boolean = true
    
    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }
    
    override fun getSpeed(): Int = speed
    
    private fun colorFromPalette(index: Int): RgbColor {
        val palette = currentPalette?.colors
        if (palette != null && palette.isNotEmpty()) {
            val paletteIndex = (index * 255 / combinedWidth) * palette.size / 256
            return palette[paletteIndex.coerceIn(0, palette.size - 1)]
        } else {
            // Default: rainbow based on position
            return com.marsraver.wledfx.color.ColorUtils.hsvToRgb(
                (index * 255 / combinedWidth) % 256,
                255,
                255
            )
        }
    }
}
