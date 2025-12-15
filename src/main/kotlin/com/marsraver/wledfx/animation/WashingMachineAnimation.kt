package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

/**
 * Washing Machine animation - Rotating waves forward, then pause, then backward.
 * By Stefan Seegel
 */
class WashingMachineAnimation : BaseAnimation() {

    private var step: Long = 0L
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Washing Machine"
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        step = 0L
        startTimeNs = 0L
        paramSpeed = 28 // Original default was 28
        paramIntensity = 128 // Original default
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
        
        // Speed factor: lower speed values = slower animation (inverted relationship in original code?)
        // Original: speed (0-255). 
        // val speedFactor = (256 - speed).coerceAtLeast(1)
        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        
        step += (tristateSpeed * 128L) / speedFactor

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val segmentLength = width * height
        val pixelIndex = y * width + x
        
        // Calculate color: sin8_t(((intensity / 25 + 1) * 255 * i / SEGLEN) + (step >> 7))
        val intensityFactor = (paramIntensity / 25 + 1)
        val phase = (intensityFactor * 255 * pixelIndex / segmentLength) + (step shr 7).toInt()
        val col = sin8(phase)
        
        return getColorFromPalette((col and 0xFF))
    }

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
     */
    private fun sin8(angle: Int): Int {
        return com.marsraver.wledfx.color.ColorUtils.sin8(angle)
    }
}
