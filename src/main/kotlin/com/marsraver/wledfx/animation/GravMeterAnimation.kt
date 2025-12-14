package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * GravMeter - Gravity-based VU meter with noise texture
 * By: Andrew Tuline
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 6571:
 * Audio-reactive VU meter with gravity effect and Perlin noise coloring.
 * Completes the "Grav" family of effects.
 */
class GravMeterAnimation : LedAnimation {

    private data class GravityState(
        var topLED: Int = 0,
        var gravityCounter: Int = 0
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var speed: Int = 128
    private var intensity: Int = 128
    
    private val gravityStates = mutableListOf<GravityState>()
    private var loudnessMeter: LoudnessMeter? = null

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? = currentPalette

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        gravityStates.clear()
        for (y in 0 until combinedHeight) {
            gravityStates.add(GravityState())
        }
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade out by 249 (25%)
        fadeOut(249)
        
        // Get loudness and convert to volume
        val volumeSmth = (loudnessMeter?.getCurrentLoudness() ?: 0) / 1024.0f * 255.0f
        
        val segmentSampleAvg = volumeSmth * intensity / 255.0f
        val adjustedSampleAvg = segmentSampleAvg * 0.25f // divide by 4 for sensitivity compensation
        
        val mySampleAvg = mapf(adjustedSampleAvg * 2.0f, 0.0f, 64.0f, 0.0f, (combinedWidth - 1).toFloat())
        val tempsamp = mySampleAvg.toInt().coerceIn(0, combinedWidth - 1)
        
        val gravity = (8 - speed / 32).coerceAtLeast(1)
        
        for (y in 0 until combinedHeight) {
            val gravcen = gravityStates[y]
            
            // Draw bars with noise-based colors
            for (i in 0 until tempsamp) {
                val index = inoise8(i * segmentSampleAvg + timeMs, (5000 + i * segmentSampleAvg).toFloat())
                val paletteColor = colorFromPalette(index, true, 0)
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
            
            // Draw top LED indicator
            if (gravcen.topLED > 0 && gravcen.topLED < combinedWidth) {
                val topColor = colorFromPalette((timeMs % 256).toInt(), true, 0)
                pixelColors[gravcen.topLED][y] = topColor
            }
            
            gravcen.gravityCounter = (gravcen.gravityCounter + 1) % gravity
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

    override fun getName(): String = "GravMeter"

    override fun isAudioReactive(): Boolean = true

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }
    
    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    private fun mapf(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
        val fromRange = fromHigh - fromLow
        val toRange = toHigh - toLow
        if (fromRange == 0.0f) return toLow
        val scaled = (value - fromLow) / fromRange
        return toLow + scaled * toRange
    }
    
    // Simplified Perlin noise for 8-bit output
    private fun inoise8(x: Float, y: Float): Int {
        val xi = x.toInt()
        val yi = y.toInt()
        val hash = ((xi * 2654435761L + yi * 2246822519L) and 0xFFFFFFFF).toInt()
        return (hash and 0xFF)
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
            return ColorUtils.hsvToRgb(index % 256, 255, if (brightness > 0) brightness else 255)
        }
    }
}
