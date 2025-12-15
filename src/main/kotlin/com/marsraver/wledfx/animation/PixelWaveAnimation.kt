package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * PixelWave animation - Wave expanding from center based on audio
 * By: Andrew Tuline
 */
class PixelWaveAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // Internal state
    private var secondHand: Int = 0
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        secondHand = 0
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 64
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000
        
        // Fade out all pixels slightly each frame
        fadeOut(240)
        
        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        val newSecondHand = ((timeMicros / speedFactor / 500 + 1) % 16).toInt()
        
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        
        if (newSecondHand != secondHand) {
            secondHand = newSecondHand
            
            // Calculate pixel brightness
            val pixBri = ((volume * paramIntensity / 64) + 50).coerceIn(50, 255)
            
            // Get color from palette based on time
            val colorIndex = (timeMs % 256).toInt()
            val color = colorFromPalette(colorIndex, true, 0)
            
            // Set center pixel
            val centerX = width / 2
            val centerY = height / 2
            val centerColor = ColorUtils.blend(RgbColor.BLACK, color, pixBri)
            setPixelColor(centerX, centerY, centerColor)
        }
        
        // Expand wave
        expandWaveFromCenter()
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "PixelWave"
    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun expandWaveFromCenter() {
        val centerX = width / 2
        val centerY = height / 2
        
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                temp[x][y] = pixelColors[x][y]
            }
        }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (x == centerX && y == centerY) continue
                
                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt((dx * dx + dy * dy).toDouble())
                
                if (distance < 0.5) continue
                
                val stepSize = 1.0 / distance.coerceAtLeast(1.0)
                val sourceX = centerX + (dx * (1.0 - stepSize)).roundToInt()
                val sourceY = centerY + (dy * (1.0 - stepSize)).roundToInt()
                
                if (sourceX in 0 until width && sourceY in 0 until height) {
                    pixelColors[x][y] = temp[sourceX][sourceY]
                } else {
                    pixelColors[x][y] = RgbColor.BLACK
                }
            }
        }
    }
    
    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
