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
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.roundToInt
import java.util.Random

/**
 * Ripple Peak animation - Audio-reactive ripples that expand from points when peaks are detected
 * By: Andrew Tuline
 */
class RipplePeakAnimation : LedAnimation {

    private data class Ripple(
        var posX: Int,
        var posY: Int,
        var state: Int,  // 254=inactive, 255=initialize, 0-16=expanding
        var color: Int
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val ripples = mutableListOf<Ripple>()
    private var currentPalette: Palette? = null
    
    private var intensity: Int = 128  // Controls max number of ripples (intensity/16)
    private var custom1: Int = 0      // Select bin (not used in 2D adaptation)
    private var custom2: Int = 0      // Volume threshold (min)
    
    @Volatile
    private var samplePeak: Boolean = false
    @Volatile
    private var fftMajorPeak: Float = 0.0f
    @Volatile
    private var maxVol: Int = 0
    private val audioLock = Any()
    private var audioScope: CoroutineScope? = null
    private var startTimeNs: Long = 0L
    private val random = Random()

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
        ripples.clear()
        startTimeNs = System.nanoTime()
        
        synchronized(audioLock) {
            samplePeak = false
            fftMajorPeak = 0.0f
            maxVol = 0
        }
        
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                // Use spectrum to detect peaks and estimate major frequency
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
                        
                        // Estimate frequency from band
                        val estimatedFreq = (maxBand * (44100.0 / 64.0) + (44100.0 / 128.0)).toFloat()
                        fftMajorPeak = estimatedFreq.coerceIn(0.0f, 22050.0f)
                        
                        // Detect peak - if magnitude exceeds threshold
                        val threshold = if (custom2 > 0) custom2 else 50
                        samplePeak = maxValue > threshold
                        maxVol = (totalMagnitude / 16.0).toInt().coerceIn(0, 255)
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        // Fade out (called twice in original for lower frame rate)
        fadeOut(240)
        fadeOut(240)
        
        val (peak, fftPeak, vol) = synchronized(audioLock) {
            Triple(samplePeak, fftMajorPeak, maxVol)
        }
        
        val maxRipples = (intensity / 16).coerceAtLeast(1)
        
        // Limit ripples to maxRipples
        while (ripples.size > maxRipples) {
            ripples.removeAt(0)
        }
        
        // Process each ripple
        for (i in ripples.indices) {
            val ripple = ripples[i]
            
            when (ripple.state) {
                254 -> {  // Inactive mode
                    // Do nothing
                }
                
                255 -> {  // Initialize ripple variables
                    // Random position in 2D space
                    ripple.posX = random.nextInt(combinedWidth)
                    ripple.posY = random.nextInt(combinedHeight)
                    
                    // Color based on FFT_MajorPeak (log10) or random
                    if (fftPeak > 1.0f) {
                        ripple.color = (log10(fftPeak.toDouble()) * 128.0).toInt().coerceIn(0, 255)
                    } else {
                        ripple.color = random.nextInt(256)
                    }
                    
                    ripple.state = 0
                }
                
                0 -> {  // Initial pixel
                    val color = colorFromPalette(ripple.color, false, 0)
                    setPixelColor(ripple.posX, ripple.posY, color)
                    ripple.state++
                }
                
                16 -> {  // At the end of the ripples
                    ripple.state = 254  // Set to inactive
                }
                
                else -> {  // Middle of the ripples - expand outward
                    val brightness = (2 * 255 / ripple.state).coerceIn(0, 255)
                    val color = colorFromPalette(ripple.color, false, 0)
                    val color1 = RgbColor(255, 0, 0)  // SEGCOLOR(1) - default red
                    val blendedColor = ColorUtils.blend(color1, color, brightness)
                    
                    // Expand in all directions (circular ripple for 2D)
                    val radius = ripple.state.toDouble()
                    for (x in 0 until combinedWidth) {
                        for (y in 0 until combinedHeight) {
                            val distance = hypot((x - ripple.posX).toDouble(), (y - ripple.posY).toDouble())
                            val diff = abs(distance - radius)
                            if (diff < 0.5) {  // On the ripple ring
                                addPixelColor(x, y, blendedColor)
                            }
                        }
                    }
                    
                    ripple.state++
                }
            }
        }
        
        // Create new ripple when peak is detected
        if (peak) {
            // Find inactive ripple or create new one
            val inactiveRipple = ripples.find { it.state == 254 }
            if (inactiveRipple != null) {
                inactiveRipple.state = 255  // Will initialize on next update
            } else if (ripples.size < maxRipples) {
                ripples.add(Ripple(0, 0, 255, 0))  // Will initialize on next update
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

    override fun getName(): String = "Ripple Peak"

    override fun cleanup() {
        audioScope?.cancel()
        audioScope = null
    }

    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + color.r).coerceAtMost(255),
                (current.g + color.g).coerceAtMost(255),
                (current.b + color.b).coerceAtMost(255)
            )
        }
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


