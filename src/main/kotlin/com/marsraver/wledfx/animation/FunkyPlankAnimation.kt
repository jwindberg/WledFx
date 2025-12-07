package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.FftMeter
import kotlin.math.min
import kotlin.math.pow

/**
 * Funky Plank animation - Audio-reactive scrolling bars
 * Written by ???, Adapted by Will Tatam
 */
class FunkyPlankAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var custom1: Int = 128  // Number of bands
    
    private var secondHand: Int = 0
    private var lastSecondHand: Int = -1
    
    private var fftMeter: FftMeter? = null
    private var startTimeNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun getDefaultPaletteName(): String? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        fftMeter = FftMeter(bands = 16)
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000
        
        // Calculate secondHand: micros()/(256-speed)/500+1 % 64
        val speedFactor = (256 - speed).coerceAtLeast(1)
        secondHand = ((timeMicros / speedFactor / 500 + 1) % 64).toInt()
        
        // Get FFT bands (0-255 values) from FftMeter
        // Use normalized bands so we get good dynamic range even when absolute levels are low
        val fftBands = fftMeter?.getNormalizedBands() ?: IntArray(16)
        
        // Only update when secondHand changes
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            
            // Calculate number of bands: map(custom1, 0, 255, 1, 16)
            val numbBands = map(custom1, 0, 255, 1, 16)
            var barWidth = combinedWidth / numbBands
            var bandInc = 1
            
            if (barWidth == 0) {
                // Matrix narrower than fft bands
                barWidth = 1
                bandInc = numbBands / combinedWidth
            }
            
            // Display values
            var b = 0
            for (band in 0 until numbBands step bandInc) {
                val bandIndex = band % 16
                val bandValue = fftBands.getOrElse(bandIndex) { 0 }
                
                // Add threshold to filter out background noise
                // Normalized bands are already scaled, so a small threshold is enough
                val noiseThreshold = 10  // Only show if band value is above 10 (out of 255)
                if (bandValue < noiseThreshold) {
                    // Skip this band, leave it black
                    b++
                    continue
                }
                
                // Normalize after threshold (0.0 - 1.0)
                val normalizedValue = ((bandValue - noiseThreshold).toFloat() / (255 - noiseThreshold)).coerceIn(0.0f, 1.0f)
                
                // Apply a gentle gamma curve to enhance contrast without crushing lows
                // gamma < 1.0 brightens low values; here we keep it subtle
                val gamma = 0.8f
                val curvedValue = normalizedValue.pow(gamma)
                
                // Use band index to vary color across bands - map to full 0-255 range for palette
                val colorIndex = (bandIndex * 256 / 16) % 256  // Spread bands across full color range
                // Map curved value to brightness range
                // Use almost full range so quiet sections are visibly dim and loud sections are bright
                val v = map((curvedValue * 255.0f).toInt(), 0, 255, 10, 255)
                
                for (w in 0 until barWidth) {
                    val xpos = (barWidth * b) + w
                    if (xpos < combinedWidth) {
                        val color = colorFromPalette(colorIndex, true, v)
                        setPixelColor(xpos, 0, color)
                    }
                }
                b++
            }
            
            // Update the display: shift down
            for (i in combinedHeight - 1 downTo 1) {
                for (j in 0 until combinedWidth) {
                    pixelColors[j][i] = pixelColors[j][i - 1]
                }
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

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set custom1 (Number of bands)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
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
        return (toLow + scaled * toRange).toInt()
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val palette = currentPalette
        if (palette != null && palette.colors.isNotEmpty()) {
            val adjustedIndex = if (wrap) index else index % 256
            val paletteIndex = ((adjustedIndex % 256) / 256.0 * palette.colors.size).toInt().coerceIn(0, palette.colors.size - 1)
            val baseColor = palette.colors[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, brightness)
        }
    }
}

