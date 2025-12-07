package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

import com.marsraver.wledfx.audio.FftMeter

/**
 * DJLight animation - Audio-reactive animation that pulses from center and spreads outward.
 * Written by ??? Adapted by Will Tatam.
 * Based on original C++ code from WLED.
 */
class DjLightAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTime: Long = 0L
    private var callCount: Long = 0
    
    private var fftMeter: FftMeter? = null
    
    private var secondHand: Int = 0
    private var lastSecondHand: Int = -1
    
    // Dynamic range tracking for auto-gain
    private var maxFFTLevel: Int = 0
    private var minFFTLevel: Int = 255
    private var lastRangeUpdate: Long = 0L

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        val segmentLength = combinedWidth * combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTime = 0L
        callCount = 0
        secondHand = 0
        lastSecondHand = -1
        maxFFTLevel = 0
        minFFTLevel = 255
        lastRangeUpdate = 0L
        
        // Fill with black on first call (matching original: if (SEGENV.call == 0))
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = RgbColor.BLACK
            }
        }
        
        // Use 16 bands for FFT; FftMeter will normalize per-band over recent history
        fftMeter = FftMeter(bands = 16)
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) {
            startTime = now
        }
        callCount++
        
        val segmentLength = combinedWidth * combinedHeight
        if (segmentLength <= 1) {
            return true
        }
        
        val mid = segmentLength / 2
        
        // Convert nanoseconds to microseconds (micros() in original)
        val micros = (now - startTime) / 1_000L
        
        // Timing calculation matching original: micros()/(256-SEGMENT.speed)/500+1 % 64
        val speed = 128 // Default speed (0-255)
        val speedFactor = (256 - speed).coerceAtLeast(1)
        val timingValue = ((micros / speedFactor / 500L) + 1) % 64
        secondHand = timingValue.toInt()
        
        // Only update when secondHand changes (matching original trigger)
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            
            // Get normalized FFT snapshot (0-255 per band, normalized over recent history)
            val fftSnapshot = fftMeter?.getNormalizedBands() ?: IntArray(16)
            
            // Color from FFT: direct use of normalized bands (no extra auto-gain)
            val redRaw = fftSnapshot.getOrElse(15) { 0 }
            val greenRaw = fftSnapshot.getOrElse(5) { 0 }
            val blueRaw = fftSnapshot.getOrElse(0) { 0 }
            
            var redValue = redRaw.coerceIn(0, 255)
            var greenValue = greenRaw.coerceIn(0, 255)
            var blueValue = blueRaw.coerceIn(0, 255)
            
            // Bias toward the dominant channel to reduce washed-out white
            val maxChannel = maxOf(redValue, greenValue, blueValue)
            if (maxChannel > 0) {
                val scaleStrong = 1.0
                val scaleWeak = 0.6
                if (redValue == maxChannel) {
                    greenValue = (greenValue * scaleWeak).toInt()
                    blueValue = (blueValue * scaleWeak).toInt()
                } else if (greenValue == maxChannel) {
                    redValue = (redValue * scaleWeak).toInt()
                    blueValue = (blueValue * scaleWeak).toInt()
                } else {
                    redValue = (redValue * scaleWeak).toInt()
                    greenValue = (greenValue * scaleWeak).toInt()
                }
            }
            
            // Add threshold - lower than before so normal music shows color
            val brightnessThreshold = 30
            val finalRed = if (redValue < brightnessThreshold) 0 else redValue
            val finalGreen = if (greenValue < brightnessThreshold) 0 else greenValue
            val finalBlue = if (blueValue < brightnessThreshold) 0 else blueValue
            
            // Fade: map(fftResult[4], 0, 255, 255, 4)
            // Use normalized fade band for consistent fade response
            val fadeBandRaw = fftSnapshot.getOrElse(4) { 0 }
            val fadeBand = fadeBandRaw
            // Slightly less aggressive fade so colors stay brighter
            val fadeAmount = mapValue(fadeBand.coerceIn(0, 255), 0, 255, 220, 100).coerceIn(100, 220)
            
            // Apply fade to color (more aggressive fade for better black balance)
            val fadeFactor = (255 - fadeAmount) / 255.0
            val fadedR = (finalRed * fadeFactor).toInt().coerceIn(0, 255)
            val fadedG = (finalGreen * fadeFactor).toInt().coerceIn(0, 255)
            val fadedB = (finalBlue * fadeFactor).toInt().coerceIn(0, 255)
            
            // Always set pixel at middle (even if black when no sound)
            val midX = mid % combinedWidth
            val midY = mid / combinedWidth
            if (midY < combinedHeight) {
                pixelColors[midX][midY] = RgbColor(fadedR, fadedG, fadedB)
            }
            
            // Shift pixels outward to stream the center pixel
            // Shift pixels left: for (int i = SEGLEN - 1; i > mid; i--) 
            //   SEGMENT.setPixelColor(i, SEGMENT.getPixelColor(i-1));
            for (i in segmentLength - 1 downTo mid + 1) {
                val fromX = (i - 1) % combinedWidth
                val fromY = (i - 1) / combinedWidth
                val toX = i % combinedWidth
                val toY = i / combinedWidth
                
                if (fromY < combinedHeight && toY < combinedHeight) {
                    pixelColors[toX][toY] = pixelColors[fromX][fromY]
                }
            }
            
            // Shift pixels right: for (int i = 0; i < mid; i++)
            //   SEGMENT.setPixelColor(i, SEGMENT.getPixelColor(i+1));
            for (i in 0 until mid) {
                val fromX = (i + 1) % combinedWidth
                val fromY = (i + 1) / combinedWidth
                val toX = i % combinedWidth
                val toY = i / combinedWidth
                
                if (fromY < combinedHeight && toY < combinedHeight) {
                    pixelColors[toX][toY] = pixelColors[fromX][fromY]
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

    override fun getName(): String = "DJLight"
    
    override fun isAudioReactive(): Boolean = true
    
    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
    
}
