package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.FftMeter
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
    
    private var fftMeter: FftMeter? = null
    @Volatile
    private var samplePeak: Boolean = false
    @Volatile
    private var fftMajorPeak: Float = 0.0f
    @Volatile
    private var maxVol: Int = 0
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
        
        samplePeak = false
        fftMajorPeak = 0.0f
        maxVol = 0
        
        // Use 32 bands for peak detection and frequency estimation
        fftMeter = FftMeter(bands = 32)
    }

    override fun update(now: Long): Boolean {
        // Fade out (called twice in original for lower frame rate)
        fadeOut(240)
        fadeOut(240)
        
        // Use FftMeter for peaks, major frequency and volume
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val maxValue = bands.maxOrNull() ?: 0
        val totalMagnitude = bands.sum()
        val fftPeak = fftMeter?.getMajorPeakFrequency() ?: 0.0f
        
        val threshold = if (custom2 > 0) custom2 else 50
        val peak = maxValue > threshold
        val vol = (totalMagnitude / 16.0).toInt().coerceIn(0, 255)
        
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
                    val baseColor = colorFromPalette(ripple.color, false, 0)
                    val brightnessFactor = brightness / 255.0
                    val blendedColor = ColorUtils.scaleBrightness(baseColor, brightnessFactor)
                    
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

    override fun isAudioReactive(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
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


