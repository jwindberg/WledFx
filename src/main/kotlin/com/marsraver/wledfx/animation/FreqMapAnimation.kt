package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter
import kotlin.math.log10

/**
 * FreqMap - Maps FFT Major Peak frequency to LED position
 * By: Andrew Tuline
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 6996:
 * Maps the dominant frequency to a specific LED position,
 * creating a frequency spectrum visualization.
 */
class FreqMapAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var speed: Int = 128
    private var intensity: Int = 128
    
    private val MAX_FREQ_LOG10 = 4.04238f

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
        
        fftMeter = FftMeter(bands = 16)
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        // Get audio data
        var fftMajorPeak = fftMeter?.getMajorPeakFrequency() ?: 1.0f
        // Use a simple magnitude approximation from peak frequency
        val myMagnitude = if (fftMajorPeak > 100) 128.0f else 64.0f
        
        if (fftMajorPeak < 1) fftMajorPeak = 1.0f
        
        // Fade out based on speed
        val fadeoutDelay = (256 - speed) / 32
        if (fadeoutDelay <= 1 || ((now / 1_000_000) % fadeoutDelay == 0L)) {
            fadeOut(speed)
        }
        
        // Map frequency to position (log scale)
        // log10 frequency range is from 1.78 (60Hz) to MAX_FREQ_LOG10
        var locn = ((log10(fftMajorPeak) - 1.78f) * combinedWidth / (MAX_FREQ_LOG10 - 1.78f)).toInt()
        if (locn < 0) locn = 0
        if (locn >= combinedWidth) locn = combinedWidth - 1
        
        // Map frequency to color index
        var pixCol = ((log10(fftMajorPeak) - 1.78f) * 255.0f / (MAX_FREQ_LOG10 - 1.78f)).toInt()
        if (fftMajorPeak < 61.0f) pixCol = 0
        
        val bright = myMagnitude.toInt().coerceIn(0, 255)
        
        // Get color from palette
        val paletteColor = colorFromPalette(intensity + pixCol, true, 0)
        val blendColor = ColorUtils.scaleBrightness(paletteColor, bright / 255.0)
        
        // Apply to all rows at the calculated position
        for (y in 0 until combinedHeight) {
            pixelColors[locn][y] = blendColor
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

    override fun getName(): String = "FreqMap"

    override fun isAudioReactive(): Boolean = true

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }
    
    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
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
            return ColorUtils.hsvToRgb(index % 256, 255, if (brightness > 0) brightness else 255)
        }
    }
}
