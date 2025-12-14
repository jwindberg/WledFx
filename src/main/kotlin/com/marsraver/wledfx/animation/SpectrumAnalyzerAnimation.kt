package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.*

/**
 * Spectrum Analyzer Bars animation - Classic audio visualizer
 * Displays vertical bars representing frequency bands from the audio spectrum
 */
class SpectrumAnalyzerAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    
    // Peak tracking for smooth falling effect
    private lateinit var peakHeights: FloatArray
    private lateinit var peakFallSpeeds: FloatArray
    
    // Configuration
    private val gravity = 0.3f // How fast peaks fall
    private val minBarHeight = 1 // Minimum bar height in pixels

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
        
        // Initialize peak tracking arrays
        peakHeights = FloatArray(combinedWidth) { 0f }
        peakFallSpeeds = FloatArray(combinedWidth) { 0f }
        
        // Initialize audio meters
        // Use number of bands equal to width for best mapping
        fftMeter = FftMeter(bands = combinedWidth.coerceIn(8, 64))
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Clear the display
        clearDisplay()
        
        // Get FFT bands and loudness
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(combinedWidth)
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        
        // Calculate brightness multiplier from loudness (0-1024 -> 0.3-1.0)
        val brightnessMultiplier = (loudness / 1024.0 * 0.7 + 0.3).coerceIn(0.3, 1.0)
        
        // Map bands to columns
        val numBands = bands.size
        
        for (x in 0 until combinedWidth) {
            // Map column to frequency band
            val bandIndex = (x * numBands / combinedWidth).coerceIn(0, numBands - 1)
            val bandValue = bands[bandIndex]
            
            // Convert band value (0-255) to bar height
            val targetHeight = (bandValue / 255.0 * combinedHeight).toFloat()
            
            // Apply peak falling with gravity
            if (targetHeight > peakHeights[x]) {
                // New peak - jump to it immediately
                peakHeights[x] = targetHeight
                peakFallSpeeds[x] = 0f
            } else {
                // Peak is falling - apply gravity
                peakFallSpeeds[x] += gravity
                peakHeights[x] = (peakHeights[x] - peakFallSpeeds[x]).coerceAtLeast(targetHeight)
            }
            
            val barHeight = peakHeights[x].toInt().coerceIn(minBarHeight, combinedHeight)
            
            // Draw the bar for this column
            drawBar(x, barHeight, brightnessMultiplier, timeMs)
        }
        
        return true
    }
    
    private fun clearDisplay() {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = RgbColor.BLACK
            }
        }
    }
    
    private fun drawBar(x: Int, height: Int, brightnessMultiplier: Double, timeMs: Long) {
        if (height <= 0) return
        
        for (y in 0 until height.coerceAtMost(combinedHeight)) {
            // Calculate color based on height position
            // Bottom = warm colors (red/orange), Top = cool colors (blue/purple)
            val heightRatio = y.toDouble() / combinedHeight
            
            // Map height to color index (0-255)
            val colorIndex = (heightRatio * 255).toInt()
            
            // Get color from palette or use gradient
            val baseColor = if (currentPalette != null) {
                colorFromPalette(colorIndex, false, 0)
            } else {
                // Default gradient: red -> yellow -> green -> cyan -> blue
                when {
                    heightRatio < 0.25 -> {
                        // Red to Yellow
                        val t = heightRatio / 0.25
                        ColorUtils.blend(RgbColor(255, 0, 0), RgbColor(255, 255, 0), (t * 255).toInt())
                    }
                    heightRatio < 0.5 -> {
                        // Yellow to Green
                        val t = (heightRatio - 0.25) / 0.25
                        ColorUtils.blend(RgbColor(255, 255, 0), RgbColor(0, 255, 0), (t * 255).toInt())
                    }
                    heightRatio < 0.75 -> {
                        // Green to Cyan
                        val t = (heightRatio - 0.5) / 0.25
                        ColorUtils.blend(RgbColor(0, 255, 0), RgbColor(0, 255, 255), (t * 255).toInt())
                    }
                    else -> {
                        // Cyan to Blue
                        val t = (heightRatio - 0.75) / 0.25
                        ColorUtils.blend(RgbColor(0, 255, 255), RgbColor(0, 0, 255), (t * 255).toInt())
                    }
                }
            }
            
            // Apply brightness multiplier
            val finalColor = ColorUtils.scaleBrightness(baseColor, brightnessMultiplier)
            
            // Set pixel (inverted Y so bars grow upward)
            val displayY = combinedHeight - 1 - y
            pixelColors[x][displayY] = finalColor
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Spectrum Analyzer"

    override fun isAudioReactive(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
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
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}
