package com.marsraver.wledfx.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Simple loudness meter, similar to Arduino's loudness sensor.
 * Returns a value from 0 (quiet) to 1024 (loudest).
 * Maintains a history of up to [HISTORY_SIZE] values.
 * 
 * Usage:
 * ```
 * val meter = LoudnessMeter()
 * val loudness = meter.getLoudness()  // Returns 0-1024 (doesn't update history)
 * val current = meter.getCurrentLoudness()  // Returns 0-1024 and adds to history
 * ```
 */
class LoudnessMeter {
    companion object {
        /**
         * Maximum number of loudness values to keep in history.
         * Adjust this value to change how many recent readings are used for normalization.
         */
        private const val HISTORY_SIZE = 600
    }
    
    @Volatile
    private var currentLoudness: Int = 0
    
    private val audioLock = Any()
    private val historyLock = Any()
    private val history = ArrayDeque<Int>(HISTORY_SIZE)  // Circular buffer for up to HISTORY_SIZE values
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isStarted = false

    init {
        start()
    }

    /**
     * Starts monitoring audio in the background.
     * Called automatically on construction.
     */
    private fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            AudioPipeline.rmsFlow().collectLatest { audioLevel ->
                // Convert RMS (0.0 to ~0.015) to 0-1024 range
                // RMS values are typically 0.0001 to 0.015 for normal audio
                // Scale to 0-1024: multiply by a large factor and clamp
                val scaled = (audioLevel.rms * 70000.0).coerceIn(0.0, 1024.0).toInt()
                
                synchronized(audioLock) {
                    currentLoudness = scaled
                }
            }
        }
    }

    /**
     * Gets the current loudness level without updating history.
     * 
     * @return A value from 0 (quiet) to 1024 (loudest)
     */
    fun getLoudness(): Int {
        return synchronized(audioLock) {
            currentLoudness
        }
    }

    /**
     * Gets the current loudness level and adds it to the history.
     * Maintains a history of up to [HISTORY_SIZE] values, removing the oldest when full.
     * Normalizes the value based on the min/max of the current history,
     * then maps it to the full range [0, 1024] for easy use.
     * This makes the value responsive to relative changes in recent audio.
     * 
     * @return A mapped value from 0 (quietest in recent history) to 1024 (loudest in recent history)
     */
    fun getCurrentLoudness(): Int {
        val loudness = synchronized(audioLock) {
            currentLoudness
        }
        
        val normalizedValue = synchronized(historyLock) {
            // Find min and max of current history (before adding new value)
            val min = if (history.isNotEmpty()) history.minOrNull() ?: 0 else 0
            val max = if (history.isNotEmpty()) history.maxOrNull() ?: 1024 else 1024
            
            // Normalize: map current value from [min, max] range to [256, 768] safe range
            // This way:
            // - When current is at the max of recent history → maps to 768 (high but not maxed)
            // - When current is at the min of recent history → maps to 256 (low but not zero)
            // - Values in between scale proportionally
            // - This finds ups and downs relative to recent audio without hitting extremes
            val normalized = if (history.size >= 2 && max > min) {
                // Calculate where current value sits in the [min, max] range (0.0 to 1.0)
                val positionInRange = if (max > min) {
                    ((loudness - min).toDouble() / (max - min)).coerceIn(0.0, 1.0)
                } else {
                    0.5  // If min == max, center it
                }
                
                // Map that position to the safe range [256, 768]
                val safeMin = 256
                val safeMax = 768
                (safeMin + positionInRange * (safeMax - safeMin)).toInt()
            } else {
                // Not enough history yet, return a centered safe value
                512  // Middle of safe range
            }
            
            // Add original loudness to end of history
            history.addLast(loudness)
            
            // Remove oldest if we exceed HISTORY_SIZE values
            if (history.size > HISTORY_SIZE) {
                history.removeFirst()
            }
            
            normalized
        }
        
        // Map the normalized value (256-768) back to full range (0-1024)
        // Arduino-style map: map(value, fromLow, fromHigh, toLow, toHigh)
        return map(normalizedValue, 256, 768, 0, 1024)
    }
    
    /**
     * Arduino-style map function.
     * Maps a value from one range to another proportionally.
     * 
     * @param value The value to map
     * @param fromLow Lower bound of the input range
     * @param fromHigh Upper bound of the input range
     * @param toLow Lower bound of the output range
     * @param toHigh Upper bound of the output range
     * @return The mapped value
     */
    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        
        val normalized = (value - fromLow) / fromRange
        return (toLow + normalized * toRange).toInt().coerceIn(toLow, toHigh)
    }

    /**
     * Gets the history of loudness values (up to [HISTORY_SIZE] most recent).
     * 
     * @return A list of loudness values from oldest to newest
     */
    fun getHistory(): List<Int> {
        return synchronized(historyLock) {
            history.toList()
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

