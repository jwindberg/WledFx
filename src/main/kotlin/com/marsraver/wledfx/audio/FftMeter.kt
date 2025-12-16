package com.marsraver.wledfx.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * FFT meter for normalized frequency spectrum data.
 * Provides consistent, normalized FFT band values (0-255) and major peak frequency.
 * Maintains history for normalization and smooths values automatically.
 * 
 * Usage:
 * ```
 * val meter = FftMeter(bands = 16)
 * val bands = meter.getBands()  // Returns IntArray of 0-255 values
 * val peakFreq = meter.getMajorPeakFrequency()  // Returns frequency in Hz
 * ```
 */
class FftMeter(
    private val bands: Int = 16,
    private val sampleRate: Float = 44100f
) {
    companion object {
        /**
         * Maximum number of spectrum snapshots to keep in history for normalization.
         */
        private const val HISTORY_SIZE = 600
        
        /**
         * Smoothing factor for band values (0.0 = no smoothing, 1.0 = no change).
         * Higher values = more smoothing (slower response).
         * Reduced from 0.75 to 0.5 for faster response to audio changes.
         */
        private const val SMOOTHING_FACTOR = 0.5
    }
    
    @Volatile
    private var currentBands: IntArray = IntArray(bands)
    
    @Volatile
    private var majorPeakFrequency: Float = 0.0f
    
    private val audioLock = Any()
    private val historyLock = Any()
    private val bandHistory = Array(bands) { ArrayDeque<Int>(HISTORY_SIZE) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isStarted = false

    init {
        start()
    }

    /**
     * Starts monitoring audio spectrum in the background.
     * Called automatically on construction.
     */
    private fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            AudioPipeline.spectrumFlow(bands = bands, sampleRate = sampleRate).collectLatest { spectrum ->
                synchronized(audioLock) {
                    // Process each band
                    for (i in 0 until bands) {
                        val rawValue = spectrum.bands.getOrNull(i) ?: 0
                        
                        // Convert to 0-255 range (spectrum bands are typically 0-255 already, but normalize)
                        val normalized = rawValue.coerceIn(0, 255)
                        
                        // Smooth the value (exponential moving average)
                        // Use faster attack (less smoothing) when values are increasing
                        val current = currentBands.getOrNull(i) ?: 0
                        val smoothing = if (normalized > current) {
                            // Faster attack when values are rising
                            0.3
                        } else {
                            // Slower decay when values are falling
                            SMOOTHING_FACTOR
                        }
                        val smoothed = (current * smoothing + normalized * (1.0 - smoothing)).toInt()
                        currentBands[i] = smoothed.coerceIn(0, 255)
                        
                        // Add to history for normalization
                        synchronized(historyLock) {
                            val history = bandHistory[i]
                            history.addLast(smoothed)
                            if (history.size > HISTORY_SIZE) {
                                history.removeFirst()
                            }
                        }
                    }
                    
                    // Calculate major peak frequency
                    var maxMag = 0
                    var maxBandIndex = 0
                    for (i in 0 until bands) {
                        if (currentBands[i] > maxMag) {
                            maxMag = currentBands[i]
                            maxBandIndex = i
                        }
                    }
                    // Convert band index to frequency: bandIndex * sampleRate / (bands * 2)
                    majorPeakFrequency = if (maxMag > 0) {
                        (maxBandIndex * sampleRate / (bands * 2.0f)).coerceAtLeast(1.0f)
                    } else {
                        0.0f
                    }
                }
            }
        }
    }

    /**
     * Gets the current FFT band values without normalization.
     * Values are smoothed but not normalized to history.
     * 
     * @return IntArray of band values from 0 (quiet) to 255 (loud) for each band
     */
    fun getBands(): IntArray {
        return synchronized(audioLock) {
            currentBands.copyOf()
        }
    }

    /**
     * Gets the current FFT band values with history-based normalization.
     * Each band is normalized based on its recent min/max values,
     * making it responsive to relative changes in frequency content.
     * 
     * @return IntArray of normalized band values from 0-255
     */
    fun getNormalizedBands(): IntArray {
        val bandsSnapshot = synchronized(audioLock) {
            currentBands.copyOf()
        }
        
        return synchronized(historyLock) {
            IntArray(bands) { bandIndex ->
                val history = bandHistory[bandIndex]
                val currentValue = bandsSnapshot[bandIndex]
                
                if (history.size < 2) {
                    // Not enough history, return current value
                    currentValue
                } else {
                    val min = history.minOrNull() ?: 0
                    val max = history.maxOrNull() ?: 255
                    
                    if (max > min) {
                        // Normalize: map from [min, max] to [0, 255]
                        val position = ((currentValue - min).toDouble() / (max - min)).coerceIn(0.0, 1.0)
                        (position * 255.0).toInt().coerceIn(0, 255)
                    } else {
                        // No variation, return middle value
                        128
                    }
                }
            }
        }
    }

    /**
     * Gets the major peak frequency in Hz.
     * This is the frequency of the band with the highest magnitude.
     * 
     * @return Frequency in Hz, or 0.0f if no audio detected
     */
    fun getMajorPeakFrequency(): Float {
        return synchronized(audioLock) {
            majorPeakFrequency
        }
    }

    /**
     * Gets a specific band value.
     * 
     * @param bandIndex The band index (0 to bands-1)
     * @return Band value from 0-255, or 0 if index is out of range
     */
    fun getBand(bandIndex: Int): Int {
        return synchronized(audioLock) {
            currentBands.getOrNull(bandIndex) ?: 0
        }
    }

    /**
     * Gets a normalized band value (history-based normalization).
     * 
     * @param bandIndex The band index (0 to bands-1)
     * @return Normalized band value from 0-255, or 0 if index is out of range
     */
    fun getNormalizedBand(bandIndex: Int): Int {
        val bandsSnapshot = synchronized(audioLock) {
            currentBands.copyOf()
        }
        
        return synchronized(historyLock) {
            val history = bandHistory.getOrNull(bandIndex) ?: return 0
            val currentValue = bandsSnapshot.getOrNull(bandIndex) ?: return 0
            
            if (history.size < 2) {
                currentValue
            } else {
                val min = history.minOrNull() ?: 0
                val max = history.maxOrNull() ?: 255
                
                if (max > min) {
                    val position = ((currentValue - min).toDouble() / (max - min)).coerceIn(0.0, 1.0)
                    (position * 255.0).toInt().coerceIn(0, 255)
                } else {
                    128
                }
            }
        }
    }

    /**
     * Stops monitoring audio and cleans up resources.
     * Call this when you're done with the meter.
     */
    fun stop() {
        scope.cancel()
        isStarted = false
    }
}

