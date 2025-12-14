package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.log10

/**
 * FreqMatrix - 2D Frequency Matrix visualization
 * By: Andreas Pleschung
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 7029:
 * Maps FFT major peak frequency to color and shifts pixels upward,
 * creating a scrolling frequency visualization matrix.
 */
class FreqMatrixAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private var lastSecondHand: Int = -1
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 0  // Low bin
    private var custom2: Int = 31 // High bin
    private var custom3: Int = 10 // Sensitivity
    
    private val MAX_FREQUENCY = 11025.0f
    private val MAX_FREQ_LOG10 = 4.04238f

    override fun supportsPalette(): Boolean = false // Uses HSV color mapping

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
        loudnessMeter = LoudnessMeter()
        lastSecondHand = -1
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val micros = (now - startTimeNs) / 1_000L
        val secondHand = ((micros / (256 - speed) / 500) % 16).toInt()
        
        if (lastSecondHand != secondHand) {
            lastSecondHand = secondHand
            
            // Get audio data
            var fftMajorPeak = fftMeter?.getMajorPeakFrequency() ?: 1.0f
            val volumeSmth = (loudnessMeter?.getCurrentLoudness() ?: 0) / 1024.0f * 255.0f
            
            // Calculate sensitivity and pixel value
            val sensitivity = mapValue(custom3, 0, 31, 1, 10)
            var pixVal = (volumeSmth * intensity * sensitivity) / 256.0f
            if (pixVal > 255) pixVal = 255.0f
            
            val intensityValue = mapValue(pixVal.toInt(), 0, 255, 0, 100) / 100.0f
            
            var color = RgbColor.BLACK
            
            if (fftMajorPeak > MAX_FREQUENCY) fftMajorPeak = 1.0f
            
            // Map frequency to color
            if (fftMajorPeak < 80) {
                color = RgbColor.BLACK
            } else {
                val upperLimit = 80 + 42 * custom2
                val lowerLimit = 80 + 3 * custom1
                val hue = if (lowerLimit != upperLimit) {
                    mapValue(fftMajorPeak.toInt(), lowerLimit, upperLimit, 0, 255)
                } else {
                    fftMajorPeak.toInt() and 0xFF
                }
                val brightness = (255 * intensityValue).toInt().coerceIn(0, 255)
                color = ColorUtils.hsvToRgb(hue, 240, brightness)
            }
            
            // Shift pixels up (for 2D) or left (for 1D row)
            // For each column, shift pixels up
            for (x in 0 until combinedWidth) {
                for (y in combinedHeight - 1 downTo 1) {
                    pixelColors[x][y] = pixelColors[x][y - 1]
                }
                // Set bottom row to new color
                pixelColors[x][0] = color
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

    override fun getName(): String = "FreqMatrix"

    override fun isAudioReactive(): Boolean = true

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
