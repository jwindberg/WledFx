package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Juggles animation - Bouncing balls with audio reactivity
 * By: Andrew Tuline
 */
class JugglesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    
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
        startTimeNs = System.nanoTime()
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade out by 224 (6.25%)
        fadeOut(224)
        
        // Get loudness (0-1024) and convert to brightness (0-255)
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val mySampleAgc = (loudness / 4).coerceIn(0, 255)  // Map 0-1024 to 0-255
        
        val segmentLength = combinedWidth * combinedHeight
        val numBalls = (intensity / 32 + 1).coerceAtLeast(1)
        
        // Create bouncing balls
        for (i in 0 until numBalls) {
            // Calculate position using beatsin16_t
            // beatsin16_t(speed/4 + i*2, 0, SEGLEN-1)
            val frequency = speed / 4 + i * 2
            val position = beatsin16_t(frequency, 0, segmentLength - 1, timeMs)
            
            // Convert 1D position to 2D coordinates
            val x = position % combinedWidth
            val y = position / combinedWidth
            
            // Get color from palette: now/4 + i*2
            val colorIndex = ((timeMs / 4 + i * 2) % 256).toInt()
            val color = colorFromPalette(colorIndex, true, 0)
            
            // Blend with background color (black) using audio brightness
            val blendedColor = ColorUtils.blend(RgbColor.BLACK, color, mySampleAgc)
            setPixelColor(x, y, blendedColor)
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Juggles"

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

    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    /**
     * beatsin16_t - Sine wave that beats at a given frequency (16-bit range)
     * Returns value oscillating between min and max
     */
    private fun beatsin16_t(frequency: Int, min: Int, max: Int, timeMs: Long): Int {
        if (frequency <= 0) return min
        val periodMs = (60000.0 / frequency).toLong()
        val phase = ((timeMs % periodMs) * 2.0 * PI / periodMs).toDouble()
        val sine = sin(phase)
        val normalized = (sine + 1.0) / 2.0
        return (min + (normalized * (max - min))).roundToInt()
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

