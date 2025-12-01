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
import kotlin.math.min

/**
 * Funky Plank animation - Audio-reactive scrolling bars
 * Written by ???, Adapted by Will Tatam
 */
class FunkyPlankAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var custom1: Int = 128  // Number of bands
    
    private var secondHand: Int = 0
    private var lastSecondHand: Int = -1
    
    @Volatile
    private var fftResult: ByteArray = ByteArray(16)
    private val audioLock = Any()
    private var audioScope: CoroutineScope? = null
    private var startTimeNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun getDefaultPaletteName(): String? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        synchronized(audioLock) {
            fftResult = ByteArray(16)
        }
        
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.spectrumFlow().collectLatest { spectrum ->
                    synchronized(audioLock) {
                        // Convert spectrum bands to 8-bit values (0-255)
                        val numBands = min(spectrum.bands.size, 16)
                        for (i in 0 until numBands) {
                            // Scale to 0-255 range
                            val bandVal = spectrum.bands[i].toFloat()
                            val clamped = if (bandVal < 0.0f) 0.0f else if (bandVal > 1.0f) 1.0f else bandVal
                            val bandValue = (clamped * 255.0f).toInt().coerceIn(0, 255)
                            fftResult[i] = bandValue.coerceIn(0, 255).toByte()
                        }
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000
        
        // Calculate secondHand: micros()/(256-speed)/500+1 % 64
        val speedFactor = (256 - speed).coerceAtLeast(1)
        secondHand = ((timeMicros / speedFactor / 500 + 1) % 64).toInt()
        
        val fft = synchronized(audioLock) { fftResult.copyOf() }
        
        // Only update when secondHand changes
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            
            // Calculate number of bands: map(custom1, 0, 255, 1, 16)
            val numbBands = map(custom1, 0, 255, 1, 16)
            var barWidth = combinedWidth / numbBands
            var bandInc = 1
            
            if (barWidth == 0) {
                // Matrix narrower than fft bands
                barWidth = 1
                bandInc = numbBands / combinedWidth
            }
            
            // Display values
            var b = 0
            for (band in 0 until numbBands step bandInc) {
                val bandIndex = band % 16
                val bandValue = (fft[bandIndex].toInt() and 0xFF)
                
                // Add threshold to filter out background noise (only show if above ~20% of max)
                val noiseThreshold = 50  // Only show if band value is above 50 (out of 255)
                if (bandValue < noiseThreshold) {
                    // Skip this band, leave it black
                    b++
                    continue
                }
                
                // Normalize after threshold
                val normalizedValue = ((bandValue - noiseThreshold).toFloat() / (255 - noiseThreshold)).coerceIn(0.0f, 1.0f)
                
                // Use square root curve to compress dynamic range (prevent peaking)
                val curvedValue = kotlin.math.sqrt(normalizedValue)
                
                // Use band index to vary color across bands - map to full 0-255 range for palette
                val colorIndex = (bandIndex * 256 / 16) % 256  // Spread bands across full color range
                // Map curved value to brightness range (reduced max to prevent peaking)
                val v = map((curvedValue * 255.0f).toInt(), 0, 255, 30, 200)  // Reduced max brightness
                
                for (w in 0 until barWidth) {
                    val xpos = (barWidth * b) + w
                    if (xpos < combinedWidth) {
                        val color = colorFromPalette(colorIndex, true, v)
                        setPixelColor(xpos, 0, color)
                    }
                }
                b++
            }
            
            // Update the display: shift down
            for (i in combinedHeight - 1 downTo 1) {
                for (j in 0 until combinedWidth) {
                    pixelColors[j][i] = pixelColors[j][i - 1]
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

    override fun getName(): String = "Funky Plank"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set custom1 (Number of bands)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
    }

    override fun cleanup() {
        audioScope?.cancel()
        audioScope = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).toInt()
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val palette = currentPalette
        if (palette != null && palette.colors.isNotEmpty()) {
            val adjustedIndex = if (wrap) index else index % 256
            val paletteIndex = ((adjustedIndex % 256) / 256.0 * palette.colors.size).toInt().coerceIn(0, palette.colors.size - 1)
            val baseColor = palette.colors[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, brightness)
        }
    }
}

