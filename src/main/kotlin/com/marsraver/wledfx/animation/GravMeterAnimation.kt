package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.LoudnessMeter
import com.marsraver.wledfx.math.MathUtils

/**
 * GravMeter - Gravity-based VU meter with noise texture
 */
class GravMeterAnimation : BaseAnimation() {

    private data class GravityState(
        var topLED: Int = 0,
        var gravityCounter: Int = 0
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    private val gravityStates = mutableListOf<GravityState>()
    private var loudnessMeter: LoudnessMeter? = null

    override fun getName(): String = "GravMeter"
    override fun isAudioReactive(): Boolean = true
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        gravityStates.clear()
        for (y in 0 until height) {
            gravityStates.add(GravityState())
        }
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade out
        fadeOut(249)
        
        // Volume
        val volumeSmth = (loudnessMeter?.getCurrentLoudness() ?: 0) / 1024.0f * 255.0f
        
        val segmentSampleAvg = volumeSmth * paramIntensity / 255.0f
        val adjustedSampleAvg = segmentSampleAvg * 0.25f 
        
        val mySampleAvg = MathUtils.mapf(adjustedSampleAvg * 2.0f, 0.0f, 64.0f, 0.0f, (width - 1).toFloat())
        val tempsamp = mySampleAvg.toInt().coerceIn(0, width - 1)
        
        val gravity = (8 - paramSpeed / 32).coerceAtLeast(1)
        
        for (y in 0 until height) {
            val gravcen = gravityStates[y]
            
            // Draw bars
            for (i in 0 until tempsamp) {
                // Using MathUtils.inoise8
                val index = MathUtils.inoise8(((i * segmentSampleAvg) + timeMs), (5000 + i * segmentSampleAvg).toFloat())
                val paletteColor = getColorFromPaletteWithBrightness(index, true, 0)
                val blendFactor = (segmentSampleAvg * 8).coerceIn(0.0f, 255.0f) / 255.0
                val color = ColorUtils.scaleBrightness(paletteColor, blendFactor)
                pixelColors[i][y] = color
            }
            
            // Update gravity
            if (tempsamp >= gravcen.topLED) {
                gravcen.topLED = tempsamp
            } else if (gravcen.gravityCounter % gravity == 0) {
                gravcen.topLED--
            }
            
            // Draw top LED
            if (gravcen.topLED > 0 && gravcen.topLED < width) {
                val topColor = getColorFromPaletteWithBrightness((timeMs % 256).toInt(), true, 0)
                pixelColors[gravcen.topLED][y] = topColor
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

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }
    
    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    private fun getColorFromPaletteWithBrightness(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        if (brightness > 0 && brightness < 255) {
            return ColorUtils.scaleBrightness(base, brightness / 255.0)
        }
        return base
    }
}
