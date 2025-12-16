package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import com.marsraver.wledfx.math.MathUtils
import java.util.Random
import kotlin.math.min
import kotlin.math.max

/**
 * Puddles animation - Audio-reactive puddles of light
 * By: Andrew Tuline
 */
class PuddlesAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var peakDetect: Boolean = false  // Toggle between puddles and puddlepeak modes
    private var custom1: Int = 0  // Bin selection for peak mode
    private var custom2: Int = 0  // Volume threshold for peak mode
    
    private var loudnessMeter: LoudnessMeter? = null
    private var fftMeter: FftMeter? = null
    private var startTimeNs: Long = 0L
    private val random = Random()
    private var lastPuddleTime: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        lastPuddleTime = 0L
        paramSpeed = 28
        paramIntensity = 128
        
        loudnessMeter = LoudnessMeter()
        fftMeter = FftMeter(bands = 32)
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Speed controls fade rate: higher speed = slower fade (puddles last longer)
        val fadeVal = MathUtils.map(paramSpeed, 0, 255, 240, 254)
        fadeOut(fadeVal)
        
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val rawVol = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        val smoothVol = loudness / 1024.0f * 255.0f
        
        val fftBands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val maxValue = fftBands.maxOrNull() ?: 0
        val totalMagnitude = fftBands.sum()
        
        val threshold = 50
        val peak = maxValue > threshold
        val vol = (totalMagnitude / 16.0).toInt().coerceIn(0, 255)
        
        val minInterval = MathUtils.map(paramSpeed, 0, 255, 50, 10).toLong()
        val timeSinceLastPuddle = timeMs - lastPuddleTime
        val canCreatePuddle = timeSinceLastPuddle >= minInterval
        
        var size = 0
        var posX = 0
        var posY = 0
        var shouldCreatePuddle = false
        
        if (peakDetect) {
            val volumeThreshold = custom2 / 2
            if (canCreatePuddle && peak && vol >= volumeThreshold) {
                shouldCreatePuddle = true
                lastPuddleTime = timeMs
                posX = random.nextInt(width)
                posY = random.nextInt(height)
                size = (smoothVol * paramIntensity / 256.0f / 4.0f + 1.0f).toInt().coerceAtLeast(1)
            }
        } else {
            if (canCreatePuddle && rawVol > 1) {
                shouldCreatePuddle = true
                lastPuddleTime = timeMs
                posX = random.nextInt(width)
                posY = random.nextInt(height)
                size = (rawVol * paramIntensity / 256 / 8 + 1).coerceAtLeast(1)
            }
        }
        
        if (shouldCreatePuddle && size > 0) {
            val maxRadius = min(width, height) / 2
            size = min(size, maxRadius)
            
            if (size > 0) {
                val colorIndex = (timeMs % 256).toInt()
                val color = colorFromPalette(colorIndex, false, 0)
                drawPuddle(posX, posY, size, color)
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

    override fun getName(): String = "Puddles"
    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true
    override fun supportsPeakDetect(): Boolean = true

    override fun setPeakDetect(enabled: Boolean) { this.peakDetect = enabled }
    fun getPeakDetect(): Boolean { return peakDetect }
    fun setCustom1(value: Int) { this.custom1 = value.coerceIn(0, 255) }
    fun setCustom2(value: Int) { this.custom2 = value.coerceIn(0, 255) }

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
        fftMeter?.stop()
        fftMeter = null
    }

    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
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
        val maxX = min(width - 1, centerX + radius)
        val minY = max(0, centerY - radius)
        val maxY = min(height - 1, centerY + radius)
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
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
