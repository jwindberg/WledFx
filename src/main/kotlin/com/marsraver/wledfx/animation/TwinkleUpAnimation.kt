package com.marsraver.wledfx.animation
import com.marsraver.wledfx.palette.Palette

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
    private var currentColor: IntArray = intArrayOf(0, 0, 0) // SEGCOLOR(1)
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

    override fun setColor(r: Int, g: Int, b: Int) {
        currentColor = intArrayOf(r, g, b)
    }

    override fun getColor(): IntArray {
        return currentColor.clone()
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        this.currentColor = intArrayOf(0, 0, 0)  // Default background to black
        startTimeNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) {
            startTimeNs = now
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return intArrayOf(0, 0, 0)
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
        var pixBri = sin8(phase)
        
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

    /**
     * sin8 - Sine wave mapped to 0-255 range
     */
    private fun sin8(angle: Int): Int {
        val normalized = (angle % 256 + 256) % 256
        val radians = (normalized / 255.0) * 2 * PI
        val sine = sin(radians)
        val result = ((sine + 1.0) / 2.0 * 255.0).toInt()
        return result.coerceIn(0, 255)
    }

    /**
     * Get color from palette
     */
    private fun getColorFromPalette(colorIndex: Int): IntArray {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIdx = (colorIndex % currentPalette.size).coerceIn(0, currentPalette.size - 1)
            return currentPalette[paletteIdx]
        } else {
            // Default rainbow
            return hsvToRgb(colorIndex, 255, 255)
        }
    }

    /**
     * Blend two colors together
     */
    private fun colorBlend(color1: IntArray, color2: IntArray, blendAmount: Int): IntArray {
        val blend = blendAmount.coerceIn(0, 255) / 255.0
        val invBlend = 1.0 - blend
        return intArrayOf(
            ((color1[0] * invBlend + color2[0] * blend)).toInt().coerceIn(0, 255),
            ((color1[1] * invBlend + color2[1] * blend)).toInt().coerceIn(0, 255),
            ((color1[2] * invBlend + color2[2] * blend)).toInt().coerceIn(0, 255)
        )
    }

    /**
     * Convert HSV to RGB
     */
    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): IntArray {
        val h = (hue % 256 + 256) % 256
        val s = saturation.coerceIn(0, 255) / 255.0
        val v = value.coerceIn(0, 255) / 255.0

        if (s <= 0.0) {
            val gray = (v * 255).toInt()
            return intArrayOf(gray, gray, gray)
        }

        val hSection = h / 42.6666667
        val i = hSection.toInt()
        val f = hSection - i

        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))

        val (r, g, b) = when (i % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }

        return intArrayOf(
            (r * 255).toInt().coerceIn(0, 255),
            (g * 255).toInt().coerceIn(0, 255),
            (b * 255).toInt().coerceIn(0, 255)
        )
    }
}

