package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * TwinkleUp animation - A very short twinkle routine with fade-in and dual controls.
 * By Andrew Tuline
 */
class TwinkleUpAnimation : BaseAnimation() {

    private var startTimeNs: Long = 0L

    override fun supportsColor(): Boolean = true

    override fun onInit() {
        startTimeNs = 0L
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) {
            startTimeNs = now
        }
        // No per-frame update needed, state is derived from time in getPixelColor
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val currentPalette = getPalette()?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return RgbColor.BLACK
        }

        val pixelIndex = y * width + x
        
        // Simulate the exact random sequence from the original code
        var prngState = 535L
        
        // Advance PRNG to the correct position for this pixel
        // Each pixel needs 3 random calls, so advance by 3 * pixelIndex
        for (i in 0 until pixelIndex * 3) {
            prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        }
        
        // Get ranstart
        prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        val ranstart = (prngState and 0xFF).toInt()
        
        // Calculate brightness
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000L
        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        val phase = ranstart + (16 * timeMs / speedFactor).toInt()
        var pixBri = MathUtils.sin8(phase)
        
        // Get second random8() for intensity check
        prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        val randomValue = (prngState and 0xFF).toInt()
        
        // Intensity check
        if (randomValue > paramIntensity) {
            pixBri = 0
        }
        
        // Get third random8() for palette color
        prngState = ((prngState * 1103515245L) + 12345L) and 0x7FFFFFFFL
        val randomPalette = (prngState and 0xFF).toInt()
        val timeValue = (timeMs / 100L).toInt()
        val palettePhase = (randomPalette + timeValue) % 256
        
        // Use BaseAnimation palette
        val paletteColor = getColorFromPalette(palettePhase)
        
        // Blend: color_blend(SEGCOLOR(1), palette_color, pixBri)
        return ColorUtils.blend(paramColor, paletteColor, pixBri)
    }

    override fun getName(): String = "TwinkleUp"
    override fun supportsIntensity(): Boolean = true
}
