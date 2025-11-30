package com.marsraver.wledfx.animation
import com.marsraver.wledfx.palette.Palette

import kotlin.math.*

/**
 * BPM animation - Colored stripes pulsing at a defined Beats-Per-Minute (BPM).
 */
class BpmAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private var startTime: Long = 0L

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
        startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) {
            startTime = now
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return intArrayOf(0, 0, 0)
        }
        
        val timeMs = System.currentTimeMillis()
        val stp = ((timeMs / 20) and 0xFF).toInt()
        
        // Speed controls BPM (default 64 BPM)
        val speed = 32 // BPM (0-255, higher = faster)
        val beat = beatsin8(speed, 64, 255)
        
        // Calculate pixel index for color selection
        val pixelIndex = y * combinedWidth + x
        val colorIndex = (stp + (pixelIndex * 2)) % 256
        val paletteIndex = (colorIndex % currentPalette.size).coerceIn(0, currentPalette.size - 1)
        val baseColor = currentPalette[paletteIndex]
        
        // Apply beat-based brightness
        val brightnessOffset = (beat - stp + (pixelIndex * 10)) % 256
        val brightnessFactor = brightnessOffset / 255.0
        
        return intArrayOf(
            (baseColor[0] * brightnessFactor).toInt().coerceIn(0, 255),
            (baseColor[1] * brightnessFactor).toInt().coerceIn(0, 255),
            (baseColor[2] * brightnessFactor).toInt().coerceIn(0, 255)
        )
    }

    override fun getName(): String = "BPM"

    /**
     * beatsin8 - Sine wave that pulses at a specific BPM
     * Returns a value between min and max that oscillates at the given BPM
     */
    private fun beatsin8(bpm: Int, min: Int, max: Int): Int {
        // Convert BPM to frequency (beats per second)
        val bps = bpm / 60.0
        val timeMs = System.currentTimeMillis()
        val timeSeconds = timeMs / 1000.0
        
        // Calculate sine wave phase
        val phase = (timeSeconds * bps * 2 * PI) % (2 * PI)
        val sine = sin(phase)
        
        // Map sine (-1 to 1) to min-max range
        val range = max - min
        val mid = (min + max) / 2.0
        val amplitude = range / 2.0
        val value = mid + (sine * amplitude)
        
        return value.toInt().coerceIn(min, max)
    }
}

