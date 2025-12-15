package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.LoudnessMeter
import com.marsraver.wledfx.math.MathUtils

/**
 * Gravcntric animation - Audio-reactive bars expanding from center with gravity effect (centered variant)
 * By: Andrew Tuline
 */
class GravCentricAnimation : BaseAnimation() {

    private data class GravityState(
        var topLED: Int = 0,
        var gravityCounter: Int = 0
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val gravityStates = mutableListOf<GravityState>()
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
        
        gravityStates.clear()
        for (y in 0 until height) {
            gravityStates.add(GravityState())
        }
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        fadeOut(253)
        
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f) * 255.0f
        
        val segmentSampleAvg = volume * paramIntensity / 255.0f * 0.125f
        
        val mySampleAvg = MathUtils.mapf(segmentSampleAvg * 2.0f, 0.0f, 32.0f, 0.0f, width / 2.0f)
        val tempsamp = mySampleAvg.coerceIn(0.0f, width / 2.0f).toInt()
        
        val gravity = (8 - paramSpeed / 32).coerceAtLeast(1)
        val offset = 1
        
        for (y in 0 until height) {
            val gravcen = gravityStates[y]
            
            if (tempsamp >= gravcen.topLED) {
                gravcen.topLED = tempsamp - offset
            } else if (gravcen.gravityCounter % gravity == 0) {
                gravcen.topLED--
            }
            
            // Draw bars from center (Gravcentric mode)
            for (i in 0 until tempsamp) {
                val index = (segmentSampleAvg * 24 + timeMs / 200).toInt() % 256
                val color = colorFromPalette(index, true, 0)
                
                val centerX = width / 2
                setPixelColor(centerX + i, y, color)
                setPixelColor(centerX - 1 - i, y, color)
            }
            
            if (gravcen.topLED >= 0) {
                val centerX = width / 2
                val grayColor = RgbColor(128, 128, 128)
                setPixelColor(centerX + gravcen.topLED, y, grayColor)
                setPixelColor(centerX - 1 - gravcen.topLED, y, grayColor)
            }
            
            gravcen.gravityCounter = (gravcen.gravityCounter + 1) % gravity
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

    override fun getName(): String = "GravCentric"
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
