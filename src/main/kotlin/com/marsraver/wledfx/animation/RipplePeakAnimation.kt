package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.math.MathUtils
import java.util.Random
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.log10

/**
 * Ripple Peak animation - Audio-reactive ripples that expand from points when peaks are detected
 * By: Andrew Tuline
 */
class RipplePeakAnimation : BaseAnimation() {

    private data class Ripple(
        var posX: Int,
        var posY: Int,
        var state: Int,  // 254=inactive, 255=initialize, 0-16=expanding
        var color: Int
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val ripples = mutableListOf<Ripple>()
    
    // Internal parameters
    private var custom2: Int = 0      // Volume threshold (min)
    
    private var fftMeter: FftMeter? = null
    private var startTimeNs: Long = 0L
    private val random = Random()

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        ripples.clear()
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
        
        // Use 32 bands for peak detection
        fftMeter = FftMeter(bands = 32)
    }

    override fun update(now: Long): Boolean {
        // Fade out
        fadeOut(240)
        fadeOut(240)
        
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val maxValue = bands.maxOrNull() ?: 0
        val totalMagnitude = bands.sum()
        val fftPeak = fftMeter?.getMajorPeakFrequency() ?: 0.0f
        
        val threshold = if (custom2 > 0) custom2 else 50
        val peak = maxValue > threshold
        
        // Max ripples based on intensity
        val maxRipples = (paramIntensity / 16).coerceAtLeast(1)
        
        while (ripples.size > maxRipples) {
            ripples.removeAt(0)
        }
        
        // Process ripples
        for (i in ripples.indices) {
            val ripple = ripples[i]
            
            when (ripple.state) {
                254 -> { /* Inactive */ }
                
                255 -> { // Initialize
                    ripple.posX = random.nextInt(width)
                    ripple.posY = random.nextInt(height)
                    
                    if (fftPeak > 1.0f) {
                        ripple.color = (log10(fftPeak.toDouble()) * 128.0).toInt().coerceIn(0, 255)
                    } else {
                        ripple.color = random.nextInt(256)
                    }
                    ripple.state = 0
                }
                
                0 -> { // Initial pixel
                    val color = colorFromPalette(ripple.color, false, 0)
                    setPixelColor(ripple.posX, ripple.posY, color)
                    ripple.state++
                }
                
                16 -> { // End of ripple
                    ripple.state = 254
                }
                
                else -> { // Expand
                    val brightness = (2 * 255 / ripple.state).coerceIn(0, 255)
                    val baseColor = colorFromPalette(ripple.color, false, 0)
                    val brightnessFactor = brightness / 255.0
                    val blendedColor = ColorUtils.scaleBrightness(baseColor, brightnessFactor)
                    
                    val radius = ripple.state.toDouble()
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val distance = hypot((x - ripple.posX).toDouble(), (y - ripple.posY).toDouble())
                            val diff = abs(distance - radius)
                            if (diff < 0.5) {
                                addPixelColor(x, y, blendedColor)
                            }
                        }
                    }
                    ripple.state++
                }
            }
        }
        
        if (peak) {
            val inactiveRipple = ripples.find { it.state == 254 }
            if (inactiveRipple != null) {
                inactiveRipple.state = 255
            } else if (ripples.size < maxRipples) {
                ripples.add(Ripple(0, 0, 255, 0))
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

    override fun getName(): String = "Ripple Peak"
    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true

    fun setCustom2(value: Int) { this.custom2 = value.coerceIn(0, 255) }

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + color.r).coerceAtMost(255),
                (current.g + color.g).coerceAtMost(255),
                (current.b + color.b).coerceAtMost(255)
            )
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
