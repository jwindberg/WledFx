package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * Spectrogram (Waterfall) Animation
 * X-Axis: Frequency
 * Y-Axis: Time (scrolling down)
 * Color/Brightness: Intensity
 */
class SpectrogramAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var fftMeter: FftMeter? = null
    
    // Buffer for scrolling (Circular buffer or array copy?)
    // Array copy is simpler for small matrices like 32x32.
    
    override fun getName(): String = "Spectrogram"
    override fun isAudioReactive(): Boolean = true
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        // Match bands to width for 1:1 mapping
        fftMeter = FftMeter(bands = width.coerceIn(8, 64)) 
    }

    override fun update(now: Long): Boolean {
        // 1. Shift everything down
        for (y in height - 1 downTo 1) {
            for (x in 0 until width) {
                pixelColors[x][y] = pixelColors[x][y - 1]
            }
        }
        
        // 2. Get new FFT data
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(width)
        val numBands = bands.size
        
        // 3. Draw top row (y=0)
        for (x in 0 until width) {
            // Map x to band index
            val bandIndex = (x * numBands / width).coerceIn(0, numBands - 1)
            val intensity = bands[bandIndex]
            
            // Map intensity to Color
            // If we have a palette, map intensity 0-255 to palette index 0-255
            // If no palette, use HeatMap or Rainbow
            
            val color: RgbColor
            if (getPalette() != null) {
                // Map intensity to palette
                color = getColorFromPalette(intensity)
            } else {
                // Default to standard Heat Colors (Black -> Red -> Yellow -> White)
                color = ColorUtils.heatColor(intensity)
            }
            
            // Optionally scale brightness by paramIntensity?
            // Spectrograms are usually self-scaling, but let's allow dimming.
            val dimmed = ColorUtils.scaleBrightness(color, paramIntensity / 255.0)
            
            pixelColors[x][0] = dimmed
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }
}
