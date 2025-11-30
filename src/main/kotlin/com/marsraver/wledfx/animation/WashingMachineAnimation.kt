package com.marsraver.wledfx.animation
import com.marsraver.wledfx.palette.Palette

import kotlin.math.PI
import kotlin.math.sin

/**
 * Washing Machine animation - Rotating waves forward, then pause, then backward.
 * By Stefan Seegel
 */
class WashingMachineAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private var intensity: Int = 128  // Controls wave frequency
    private var speed: Int = 128      // Speed control (0-255)
    
    private var step: Long = 0L
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
        step = 0L
        startTimeNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) {
            startTimeNs = now
            return true
        }

        // Calculate speed using tristate_square8
        // tristate_square8 creates: forward (high), pause (mid), backward (low), pause (mid)
        val timeMs = (now - startTimeNs) / 1_000_000L
        val tristateSpeed = tristateSquare8(timeMs shr 7, 90, 15)
        
        // Accumulate step: step += (speed * 2048) / (512 - SEGMENT.speed)
        val speedFactor = (512 - speed).coerceAtLeast(1)
        step += (tristateSpeed * 2048L) / speedFactor

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return intArrayOf(0, 0, 0)
        }

        val segmentLength = combinedWidth * combinedHeight
        val pixelIndex = y * combinedWidth + x
        
        // Calculate color: sin8_t(((intensity / 25 + 1) * 255 * i / SEGLEN) + (step >> 7))
        val intensityFactor = (intensity / 25 + 1)
        val phase = (intensityFactor * 255 * pixelIndex / segmentLength) + (step shr 7).toInt()
        val col = sin8(phase)
        
        // Get color from palette
        val paletteIndex = (col % currentPalette.size).coerceIn(0, currentPalette.size - 1)
        return currentPalette[paletteIndex]
    }

    override fun getName(): String = "Washing Machine"

    /**
     * tristate_square8 - Creates a three-state square wave
     * Pattern: forward (high), pause (mid), backward (low), pause (mid), repeat
     * @param time time value (shifted right by 7)
     * @param highValue value during forward phase
     * @param lowValue value during backward phase
     * @return speed value (high, mid, or low)
     */
    private fun tristateSquare8(time: Long, highValue: Int, lowValue: Int): Int {
        // Create a cycle: 0-63 forward, 64-127 pause, 128-191 backward, 192-255 pause
        val cycle = (time % 256).toInt()
        return when {
            cycle < 64 -> highValue      // Forward
            cycle < 128 -> (highValue + lowValue) / 2  // Pause (mid)
            cycle < 192 -> lowValue     // Backward
            else -> (highValue + lowValue) / 2  // Pause (mid)
        }
    }

    /**
     * sin8 - Sine wave mapped to 0-255 range
     * Equivalent to FastLED's sin8 function
     */
    private fun sin8(angle: Int): Int {
        // Normalize angle to 0-255 range
        val normalized = (angle % 256 + 256) % 256
        // Convert to radians (0-255 maps to 0-2π)
        val radians = (normalized / 255.0) * 2 * PI
        // Calculate sine (-1 to 1) and map to 0-255
        val sine = sin(radians)
        val result = ((sine + 1.0) / 2.0 * 255.0).toInt()
        return result.coerceIn(0, 255)
    }
}

