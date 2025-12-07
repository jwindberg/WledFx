package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.hypot
import kotlin.math.roundToInt
import java.util.Random

/**
 * Puddles animation - Audio-reactive puddles of light
 * By: Andrew Tuline
 */
class PuddlesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 28
    private var intensity: Int = 128
    private var peakDetect: Boolean = false  // Toggle between puddles and puddlepeak modes
    private var custom1: Int = 0  // Bin selection for peak mode
    private var custom2: Int = 0  // Volume threshold for peak mode
    
    private var loudnessMeter: LoudnessMeter? = null
    private var fftMeter: FftMeter? = null
    @Volatile
    private var samplePeak: Boolean = false
    @Volatile
    private var maxVol: Int = 0
    private var startTimeNs: Long = 0L
    private val random = Random()
    private var lastPuddleTime: Long = 0L

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
        lastPuddleTime = 0L
        
        loudnessMeter = LoudnessMeter()
        // Use 32 bands for peak detection (matches original)
        fftMeter = FftMeter(bands = 32)
        
        samplePeak = false
        maxVol = 0
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Speed controls fade rate: higher speed = slower fade (puddles last longer)
        // Map speed (0-255) to fade amount: 0 = fast fade (240), 255 = slow fade (254)
        val fadeVal = map(speed, 0, 255, 240, 254)
        fadeOut(fadeVal)
        
        // Get loudness (0-1024) and convert to 0-255 range for volumeRaw
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val rawVol = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        val smoothVol = loudness / 1024.0f * 255.0f  // For size calculation
        
        // Use FftMeter to detect peaks and overall magnitude
        val fftBands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val maxValue = fftBands.maxOrNull() ?: 0
        val totalMagnitude = fftBands.sum()
        
        val threshold = 50
        val peak = maxValue > threshold
        val vol = (totalMagnitude / 16.0).toInt().coerceIn(0, 255)
        
        // Allow multiple puddles simultaneously - only limit by a very short minimum interval
        // Speed controls minimum interval: higher speed = shorter interval (more frequent)
        val minInterval = map(speed, 0, 255, 50, 10).toLong()  // 50ms at slowest, 10ms at fastest
        val timeSinceLastPuddle = timeMs - lastPuddleTime
        val canCreatePuddle = timeSinceLastPuddle >= minInterval
        
        var size = 0
        var posX = 0
        var posY = 0
        var shouldCreatePuddle = false
        
        if (peakDetect) {
            // Puddlepeak mode: only create puddles on peaks
            // Uses custom1 for bin selection and custom2 for volume threshold
            val volumeThreshold = custom2 / 2
            if (canCreatePuddle && peak && vol >= volumeThreshold) {
                shouldCreatePuddle = true
                lastPuddleTime = timeMs
                // Random starting position
                posX = random.nextInt(combinedWidth)
                posY = random.nextInt(combinedHeight)
                
                // Determine size: volumeSmth * intensity / 256 / 4 + 1
                size = (smoothVol * intensity / 256.0f / 4.0f + 1.0f).toInt().coerceAtLeast(1)
            }
        } else {
            // Regular puddles mode: create puddles when volumeRaw > 1
            if (canCreatePuddle && rawVol > 1) {
                shouldCreatePuddle = true
                lastPuddleTime = timeMs
                // Random starting position
                posX = random.nextInt(combinedWidth)
                posY = random.nextInt(combinedHeight)
                
                // Determine size: volumeRaw * intensity / 256 / 8 + 1
                size = (rawVol * intensity / 256 / 8 + 1).coerceAtLeast(1)
            }
        }
        
        // Limit size to fit in grid (for circular puddles, use radius)
        if (shouldCreatePuddle && size > 0) {
            val maxRadius = min(combinedWidth, combinedHeight) / 2
            size = min(size, maxRadius)
            
            // Flash the LEDs (create puddle)
            if (size > 0) {
                val colorIndex = (timeMs % 256).toInt()
                val color = colorFromPalette(colorIndex, false, 0)
                
                // Create circular puddle
                drawPuddle(posX, posY, size, color)
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

    override fun getName(): String = "Puddles"

    override fun isAudioReactive(): Boolean = true

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set peak detection mode (puddlepeak vs regular puddles)
     */
    override fun supportsPeakDetect(): Boolean = true

    override fun setPeakDetect(enabled: Boolean) {
        this.peakDetect = enabled
    }

    /**
     * Get peak detection mode
     */
    fun getPeakDetect(): Boolean {
        return peakDetect
    }

    /**
     * Set custom1 (bin selection for peak mode)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
    }

    /**
     * Set custom2 (volume threshold for peak mode)
     */
    fun setCustom2(value: Int) {
        this.custom2 = value.coerceIn(0, 255)
    }

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
        fftMeter?.stop()
        fftMeter = null
    }

    private fun fadeOut(amount: Int) {
        // amount is how much to keep (254 = keep 254/255 = 99.6% per frame = slow fade)
        // Higher amount = slower fade (keeps more brightness each frame)
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun drawPuddle(centerX: Int, centerY: Int, radius: Int, color: RgbColor) {
        if (radius <= 0) {
            setPixelColor(centerX, centerY, color)
            return
        }
        
        val minX = max(0, centerX - radius)
        val maxX = min(combinedWidth - 1, centerX + radius)
        val minY = max(0, centerY - radius)
        val maxY = min(combinedHeight - 1, centerY + radius)
        val radiusSq = radius * radius
        
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val dx = x - centerX
                val dy = y - centerY
                val distSq = dx * dx + dy * dy
                if (distSq <= radiusSq) {
                    setPixelColor(x, y, color)
                }
            }
        }
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
        return (toLow + scaled * toRange).roundToInt()
    }

    private fun min(a: Int, b: Int): Int = kotlin.math.min(a, b)
    private fun max(a: Int, b: Int): Int = kotlin.math.max(a, b)

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

