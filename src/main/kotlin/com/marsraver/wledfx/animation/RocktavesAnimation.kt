package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin

/**
 * Rocktaves animation - Same note from each octave is same colour
 * By: Andrew Tuline, adapted for WLED
 * Audio-reactive animation that maps frequency to musical notes
 */
class RocktavesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    @Volatile
    private var fftMajorPeak: Float = 0.0f
    @Volatile
    private var myMagnitude: Float = 0.0f
    private val audioLock = Any()
    private var audioScope: CoroutineScope? = null
    private var startTimeNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        synchronized(audioLock) {
            fftMajorPeak = 0.0f
            myMagnitude = 0.0f
        }
        
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                // Use spectrum to estimate major peak frequency
                AudioPipeline.spectrumFlow(bands = 32).collectLatest { spectrum ->
                    synchronized(audioLock) {
                        // Find the band with highest magnitude
                        var maxBand = 0
                        var maxValue = 0
                        var totalMagnitude = 0.0
                        
                        for (i in spectrum.bands.indices) {
                            val value = spectrum.bands[i]
                            totalMagnitude += value
                            if (value > maxValue) {
                                maxValue = value
                                maxBand = i
                            }
                        }
                        
                        // Estimate frequency from band (assuming 44.1kHz sample rate)
                        // Each band represents ~689 Hz (44100 / 64 bands)
                        // For 32 bands, each represents ~1378 Hz
                        val estimatedFreq = maxBand * (44100.0f / 64.0f) + (44100.0f / 128.0f)
                        fftMajorPeak = estimatedFreq.coerceIn(0.0f, 22050.0f)
                        
                        // Magnitude is the total energy - amplify for better sensitivity
                        // Original divides by 16, but we'll amplify by 2-3x for better sensitivity
                        myMagnitude = (totalMagnitude / 8.0 * 1.5).toFloat().coerceIn(0.0f, 255.0f)
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade to black
        fadeToBlack(16)
        
        val (frTemp, magnitude) = synchronized(audioLock) {
            Pair(fftMajorPeak, myMagnitude)
        }
        
        // Lower threshold for better sensitivity to quieter music
        if (magnitude < 15.0f) {
            // Too quiet, don't display anything
            return true
        }
        
        // Calculate octave count
        var octCount = 0
        var freq = frTemp
        while (freq > 249.0f && octCount < 5) {
            octCount++
            freq = freq / 2.0f
        }
        
        // Calculate volume/brightness - more sensitive to lower volumes
        var volTemp = 32.0f + magnitude * 1.5f
        if (magnitude < 15.0f) volTemp = 0.0f  // Lower threshold from 48 to 15
        if (magnitude > 144.0f) volTemp = 255.0f
        val brightness = volTemp.coerceIn(0.0f, 255.0f).toInt()
        
        // Map frequency to musical note (C3 base = 132 Hz)
        var noteFreq = freq - 132.0f
        noteFreq = abs(noteFreq * 2.1f)
        val noteIndex = noteFreq.coerceIn(0.0f, 255.0f).toInt()
        
        // Calculate pixel position using beatsin8
        // beatsin8_t(8+octCount*4, 0, 255, 0, octCount*8)
        val bpm = 8 + octCount * 4
        val phaseOffset = octCount * 8
        val beatsinValue = beatsin8(bpm, 0, 255, timeMs, phaseOffset)
        val segmentLength = combinedWidth * combinedHeight
        var i = map(beatsinValue, 0, 255, 0, segmentLength - 1)
        i = i.coerceIn(0, segmentLength - 1)
        
        // Convert 1D index to 2D coordinates
        val x = i % combinedWidth
        val y = i / combinedWidth
        
        // Get color from palette
        val color1 = RgbColor(255, 0, 0) // SEGCOLOR(1) - default red, could be made configurable
        val color2 = colorFromPalette(noteIndex, false, 0)
        val blendedColor = ColorUtils.blend(color1, color2, brightness)
        
        // Add color to pixel
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = blendedColor
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

    override fun getName(): String = "Rocktaves"

    override fun cleanup() {
        audioScope?.cancel()
        audioScope = null
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    /**
     * beatsin8 - Sine wave that oscillates at the given BPM
     */
    private fun beatsin8(bpm: Int, low: Int, high: Int, timeMs: Long, phaseOffset: Int = 0): Int {
        val beatsPerSecond = bpm / 60.0
        val radiansPerMs = beatsPerSecond * 2.0 * PI / 1000.0
        val phase = timeMs * radiansPerMs + (phaseOffset / 255.0) * 2.0 * PI
        val sine = sin(phase)
        
        val mid = (low + high) / 2.0
        val range = (high - low) / 2.0
        return (mid + sine * range).toInt().coerceIn(low, high)
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).toInt()
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}

