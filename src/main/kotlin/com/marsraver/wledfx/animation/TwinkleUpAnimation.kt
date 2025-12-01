package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.sin

/**
 * TwinkleUp animation - A very short twinkle routine with fade-in and dual controls.
 * By Andrew Tuline
 */
class TwinkleUpAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private var currentColor: RgbColor = RgbColor.BLACK // SEGCOLOR(1)
    private var intensity: Int = 128  // Controls how many pixels are lit (higher = more)
    private var speed: Int = 128      // Speed control (0-255)
    
    private var startTimeNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun supportsColor(): Boolean = true

    override fun setColor(color: RgbColor) {
        currentColor = color
    }

    override fun getColor(): RgbColor? {
        return currentColor
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        this.currentColor = RgbColor.BLACK  // Default background to black
        startTimeNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) {
            startTimeNs = now
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return RgbColor.BLACK
        }

        val segmentLength = combinedWidth * combinedHeight
        val pixelIndex = y * combinedWidth + x
        
        // Simulate the exact random sequence from the original code
        // Each pixel uses 3 sequential random8() calls:
        // 1. ranstart = random8()
        // 2. random8() for intensity check
        // 3. random8() for palette color
        // We need to simulate the PRNG state as if we've called random8() 3*pixelIndex times
        
        // Initialize PRNG with seed 535 (as in original)
        var prngState = 535L
        
        // Advance PRNG to the correct position for this pixel
        // Each pixel needs 3 random calls, so advance by 3 * pixelIndex
        for (i in 0 until pixelIndex * 3) {
            prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        }
        
        // Get ranstart (first random8() for this pixel)
        prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        val ranstart = (prngState and 0xFF).toInt()
        
        // Calculate brightness: sin8_t(ranstart + 16 * strip.now/(256-SEGMENT.speed))
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000L
        val speedFactor = (256 - speed).coerceAtLeast(1)
        val phase = ranstart + (16 * timeMs / speedFactor).toInt()
        var pixBri = ColorUtils.sin8(phase)
        
        // Get second random8() for intensity check
        prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        val randomValue = (prngState and 0xFF).toInt()
        if (randomValue > intensity) {
            pixBri = 0
        }
        
        // Get third random8() for palette color: color_from_palette(random8()+strip.now/100, ...)
        prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        val randomPalette = (prngState and 0xFF).toInt()
        val timeValue = (timeMs / 100L).toInt()
        val palettePhase = (randomPalette + timeValue) % 256
        val paletteColor = getColorFromPalette(palettePhase)
        
        // Blend: color_blend(SEGCOLOR(1), palette_color, pixBri)
        return colorBlend(currentColor, paletteColor, pixBri)
    }

    override fun getName(): String = "TwinkleUp"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }


    /**
     * Get color from palette
     */
    private fun getColorFromPalette(colorIndex: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIdx = (colorIndex % currentPalette.size).coerceIn(0, currentPalette.size - 1)
            return currentPalette[paletteIdx]
        } else {
            // Default rainbow
            return ColorUtils.hsvToRgb(colorIndex, 255, 255)
        }
    }

    /**
     * Blend two colors together
     */
    private fun colorBlend(color1: RgbColor, color2: RgbColor, blendAmount: Int): RgbColor {
        return ColorUtils.blend(color1, color2, blendAmount)
    }
}

