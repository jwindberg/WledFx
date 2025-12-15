package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * FreqMatrix - 2D Frequency Matrix visualization
 */
class FreqMatrixAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private var lastSecondHand: Int = -1
    
    private var custom1: Int = 0  
    private var custom2: Int = 31 
    private var custom3: Int = 10 
    
    private val MAX_FREQUENCY = 11025.0f

    override fun getName(): String = "FreqMatrix"
    override fun isAudioReactive(): Boolean = true
    override fun supportsPalette(): Boolean = false // Implicitly HSV

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        fftMeter = FftMeter(bands = 16)
        loudnessMeter = LoudnessMeter()
        lastSecondHand = -1
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val micros = (now - startTimeNs) / 1_000L
        val secondHand = ((micros / (256 - paramSpeed) / 500) % 16).toInt()
        
        if (lastSecondHand != secondHand) {
            lastSecondHand = secondHand
            
            var fftMajorPeak = fftMeter?.getMajorPeakFrequency() ?: 1.0f
            val volumeSmth = (loudnessMeter?.getCurrentLoudness() ?: 0) / 1024.0f * 255.0f
            
            val sensitivity = mapValue(custom3, 0, 31, 1, 10)
            var pixVal = (volumeSmth * paramIntensity * sensitivity) / 256.0f
            if (pixVal > 255) pixVal = 255.0f
            
            val intensityValue = mapValue(pixVal.toInt(), 0, 255, 0, 100) / 100.0f
            var color: RgbColor
            
            if (fftMajorPeak > MAX_FREQUENCY) fftMajorPeak = 1.0f
            
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
            
            for (x in 0 until width) {
                for (y in height - 1 downTo 1) {
                    pixelColors[x][y] = pixelColors[x][y - 1]
                }
                pixelColors[x][0] = color
            }
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

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
