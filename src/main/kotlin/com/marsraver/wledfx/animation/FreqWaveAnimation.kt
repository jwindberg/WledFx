package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.min

/**
 * FreqWave - Frequency Wave visualization spreading from center
 * By: Andreas Pleschung
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 7127:
 * Maps FFT major peak to color and spreads pixels from center outward,
 * creating a wave effect based on frequency.
 */
class FreqWaveAnimation : LedAnimation {

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
    private var custom3: Int = 10 // Pre-amp/sensitivity
    
    private val MAX_FREQUENCY = 11025.0f

    override fun supportsPalette(): Boolean = false

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
            val sensitivity = mapf(custom3.toFloat(), 1f, 31f, 1f, 10f)
            val pixVal = min(255.0f, volumeSmth * intensity / 256.0f * sensitivity)
            val intensityValue = mapf(pixVal, 0f, 255f, 0f, 100f) / 100.0f
            
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
                val brightness = min(255.0f, 255.0f * intensityValue).toInt()
                color = ColorUtils.hsvToRgb(hue, 240, brightness)
            }
            
            // Apply to each row independently (spreading from center)
            for (y in 0 until combinedHeight) {
                val mid = combinedWidth / 2
                
                // Set center pixel
                pixelColors[mid][y] = color
                
                // Shift right half to the right
                for (x in combinedWidth - 1 downTo mid + 1) {
                    pixelColors[x][y] = pixelColors[x - 1][y]
                }
                
                // Shift left half to the left
                for (x in 0 until mid) {
                    pixelColors[x][y] = pixelColors[x + 1][y]
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

    override fun getName(): String = "FreqWave"

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
    
    private fun mapf(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
