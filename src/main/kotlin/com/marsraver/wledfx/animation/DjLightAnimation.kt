package com.marsraver.wledfx.animation

import com.marsraver.wledfx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * DJLight animation - Audio-reactive animation that pulses from center and spreads outward.
 * Written by ??? Adapted by Will Tatam.
 * Based on original C++ code from WLED.
 */
class DjLightAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<IntArray>>
    private var startTime: Long = 0L
    private var callCount: Long = 0

    private val fftResult = IntArray(16)
    private val spectrumLock = Any()
    private var audioScope: CoroutineScope? = null
    
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
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
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
                pixelColors[x][y] = intArrayOf(0, 0, 0)
            }
        }
        
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.spectrumFlow(bands = 16).collectLatest { spectrum ->
                    synchronized(spectrumLock) {
                        val bands = spectrum.bands
                        // Smooth FFT values for better responsiveness
                        for (i in fftResult.indices) {
                            val incoming = bands.getOrNull(i) ?: 0
                        // Smooth: blend 25% old, 75% new (fast response)
                        // Use moderate amplification - dynamic range will handle sensitivity
                        fftResult[i] = ((fftResult[i] * 1 + incoming * 3) / 4 * 2.5).toInt().coerceIn(0, 255)
                        }
                    }
                }
            }
        }
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
            
            // Get FFT data snapshot
            val fftSnapshot = synchronized(spectrumLock) { fftResult.clone() }
            
            // Update dynamic range tracking (every second or so)
            if (now - lastRangeUpdate > 1_000_000_000L) { // Every 1 second
                lastRangeUpdate = now
                val currentMax = fftSnapshot.maxOrNull() ?: 0
                val currentMin = fftSnapshot.minOrNull() ?: 0
                
                // Update max/min with slow decay (allows adaptation to changing levels)
                maxFFTLevel = ((maxFFTLevel * 9 + currentMax * 1) / 10).coerceIn(1, 255)
                if (currentMin > 0) {
                    minFFTLevel = ((minFFTLevel * 9 + currentMin * 1) / 10).coerceIn(0, maxFFTLevel - 1)
                }
                
                // Ensure min < max
                if (minFFTLevel >= maxFFTLevel) {
                    minFFTLevel = maxOf(0, maxFFTLevel - 10)
                }
            }
            
            // Use dynamic range to normalize FFT values (auto-gain)
            val range = maxFFTLevel - minFFTLevel
            val rangeMin = minFFTLevel
            
            fun normalizeFFT(value: Int): Int {
                if (range <= 0) return value
                // Normalize but leave headroom (don't use full 0-255, use 0-120 for better contrast and less white)
                val normalized = ((value - rangeMin) * 120) / range
                return normalized.coerceIn(0, 120)
            }
            
            // Color from FFT: Use normalized values for dynamic range
            // Original uses /2, apply that to normalized values
            val redRaw = fftSnapshot.getOrElse(15) { 0 }
            val greenRaw = fftSnapshot.getOrElse(5) { 0 }
            val blueRaw = fftSnapshot.getOrElse(0) { 0 }
            
            val redValue = (normalizeFFT(redRaw) / 2).coerceIn(0, 255)
            val greenValue = (normalizeFFT(greenRaw) / 2).coerceIn(0, 255)
            val blueValue = (normalizeFFT(blueRaw) / 2).coerceIn(0, 255)
            
            // Add threshold - values below threshold become black for better contrast
            // Higher threshold = more black in the mix to balance whites
            val brightnessThreshold = 50
            val finalRed = if (redValue < brightnessThreshold) 0 else redValue
            val finalGreen = if (greenValue < brightnessThreshold) 0 else greenValue
            val finalBlue = if (blueValue < brightnessThreshold) 0 else blueValue
            
            // Fade: map(fftResult[4], 0, 255, 255, 4)
            // Use normalized fade band for consistent fade response
            val fadeBandRaw = fftSnapshot.getOrElse(4) { 0 }
            val fadeBand = normalizeFFT(fadeBandRaw)
            // More aggressive fade range: 240-80 (more fade = more black balance)
            val fadeAmount = mapValue(fadeBand.coerceIn(0, 255), 0, 255, 240, 80).coerceIn(80, 240)
            
            // Apply fade to color (more aggressive fade for better black balance)
            val fadeFactor = (255 - fadeAmount) / 255.0
            val fadedR = (finalRed * fadeFactor).toInt().coerceIn(0, 255)
            val fadedG = (finalGreen * fadeFactor).toInt().coerceIn(0, 255)
            val fadedB = (finalBlue * fadeFactor).toInt().coerceIn(0, 255)
            
            // Always set pixel at middle (even if black when no sound)
            val midX = mid % combinedWidth
            val midY = mid / combinedWidth
            if (midY < combinedHeight) {
                pixelColors[midX][midY] = intArrayOf(fadedR, fadedG, fadedB)
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
                    pixelColors[toX][toY] = pixelColors[fromX][fromY].clone()
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
                    pixelColors[toX][toY] = pixelColors[fromX][fromY].clone()
                }
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y].clone()
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "DJLight"
    
    fun cleanup() {
        audioScope?.cancel()
        audioScope = null
        synchronized(spectrumLock) {
            fftResult.fill(0)
        }
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
    
}
