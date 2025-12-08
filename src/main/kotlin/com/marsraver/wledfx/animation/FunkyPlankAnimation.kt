package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter

/**
 * Funky Plank animation - 2D scrolling FFT visualization.
 * Written by ??? Adapted by Will Tatam.
 * 
 * Displays FFT frequency bands as horizontal bars at the top of the matrix,
 * which scroll down over time. Each band's FFT value determines both hue and brightness.
 */
class FunkyPlankAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<RgbColor>> = emptyArray()

    private var speed: Int = 128
    private var custom1: Int = 255  // Number of bands (1-16), default 255 = 16 bands
    private var noiseThreshold: Int = 10  // Minimum FFT value to display (below this = black)
    
    private var currentPalette: Palette? = null
    private var fftMeter: FftMeter? = null
    private var lastSecondHand: Int = -1
    private var startTimeNs: Long = 0L

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        lastSecondHand = -1
        startTimeNs = System.nanoTime()
        
        // Initialize FFT meter with 16 bands
        fftMeter = FftMeter(bands = 16)
    }

    override fun update(now: Long): Boolean {
        // Calculate number of bands from custom1 (1-16)
        val numBands = map(custom1, 0, 255, 1, 16).coerceIn(1, 16)

        // Timing logic: micros()/(256-speed)/500+1 % 64
        // Convert to nanoseconds: (now - startTimeNs) / 1000 = microseconds
        val micros = (now - startTimeNs) / 1_000L
        val speedDivisor = (256 - speed).coerceAtLeast(1)
        val secondHand = ((micros / speedDivisor / 500 + 1) % 64).toInt()

        // Update top row when timing changes
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand

            // Get normalized FFT bands (0-255)
            val fftBands = fftMeter?.getNormalizedBands() ?: IntArray(16)

            // Draw one pixel per column, cycling through colors
            for (x in 0 until combinedWidth) {
                // Map column position to FFT band (cycle through 16 bands)
                val bandIndex = x % 16
                val fftValue = fftBands.getOrElse(bandIndex) { 0 }
                
                // If FFT value is below threshold, draw black
                val rgb = if (fftValue < noiseThreshold) {
                    RgbColor.BLACK
                } else {
                    // Get color from palette using column index modulo numBands to cycle colors
                    val colorIndex = x % numBands
                    val baseColor = colorFromPalette(colorIndex, numBands)
                    
                    // Map FFT value to brightness (10-255) and scale the color
                    val brightness = map(fftValue, noiseThreshold, 255, 10, 255).coerceIn(10, 255)
                    ColorUtils.scaleBrightness(baseColor, brightness / 255.0)
                }
                
                setPixelColor(x, 0, rgb)
            }
        }

        // Scroll display down by one row
        // Process from bottom to top to avoid overwriting
        for (i in combinedHeight - 1 downTo 1) {
            for (j in 0 until combinedWidth) {
                pixelColors[j][i] = pixelColors[j][i - 1]
            }
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

    override fun getName(): String = "Funky Plank"

    override fun isAudioReactive(): Boolean = true

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set custom1 parameter (number of bands: 1-16)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
    }

    /**
     * Get custom1 parameter
     */
    fun getCustom1(): Int {
        return custom1
    }

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).toInt().coerceIn(toLow, toHigh)
    }

    private fun colorFromPalette(barIndex: Int, totalBands: Int): RgbColor {
        val palette = this.currentPalette
        if (palette != null && palette.colors.isNotEmpty()) {
            // Map bar index (0 to totalBands-1) to palette position (0.0-1.0)
            // Distribute bars evenly across the palette
            val position = if (totalBands <= 1) {
                0.0
            } else {
                (barIndex.toDouble() / (totalBands - 1).toDouble()).coerceIn(0.0, 1.0)
            }
            return palette.getColorAt(position)
        } else {
            // Fallback to HSV if no palette is set
            val hue = (barIndex * 360.0f / totalBands.toFloat().coerceAtLeast(1.0f)).coerceIn(0.0f, 360.0f)
            return ColorUtils.hsvToRgb(hue, 1.0f, 1.0f)
        }
    }
}

