package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.min

/**
 * FreqWave - Frequency Wave visualization
 */
class FreqWaveAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private var lastSecondHand: Int = -1
    
    private var custom1: Int = 0 
    private var custom2: Int = 31
    private var custom3: Int = 10 
    
    private val MAX_FREQUENCY = 11025.0f

    override fun getName(): String = "FreqWave"
    override fun isAudioReactive(): Boolean = true
    override fun supportsPalette(): Boolean = false

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
            
            val sensitivity = mapf(custom3.toFloat(), 1f, 31f, 1f, 10f)
            val pixVal = min(255.0f, volumeSmth * paramIntensity / 256.0f * sensitivity)
            val intensityValue = mapf(pixVal, 0f, 255f, 0f, 100f) / 100.0f
            
            var color = RgbColor.BLACK
            
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
                val brightness = min(255.0f, 255.0f * intensityValue).toInt()
                color = ColorUtils.hsvToRgb(hue, 240, brightness)
            }
            
            for (y in 0 until height) {
                val mid = width / 2
                pixelColors[mid][y] = color
                for (x in width - 1 downTo mid + 1) pixelColors[x][y] = pixelColors[x - 1][y]
                for (x in 0 until mid) pixelColors[x][y] = pixelColors[x + 1][y]
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
    
    private fun mapf(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
