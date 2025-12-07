package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * Gravcntric animation - Audio-reactive bars expanding from center with gravity effect (centered variant)
 * By: Andrew Tuline
 */
class GravCentricAnimation : LedAnimation {

    private data class GravityState(
        var topLED: Int = 0,
        var gravityCounter: Int = 0
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    
    private val gravityStates = mutableListOf<GravityState>()
    
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

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
        
        gravityStates.clear()
        for (y in 0 until combinedHeight) {
            gravityStates.add(GravityState())
        }
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade out by 253
        fadeOut(253)
        
        // Get loudness (0-1024) and convert to volume-like value
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        // Scale 0-1024 to 0-255 (same as GravCenterAnimation)
        val volume = (loudness / 1024.0f) * 255.0f
        
        val segmentSampleAvg = volume * intensity / 255.0f * 0.125f
        val mySampleAvg = mapf(segmentSampleAvg * 2.0f, 0.0f, 32.0f, 0.0f, combinedWidth / 2.0f)
        val tempsamp = mySampleAvg.coerceIn(0.0f, combinedWidth / 2.0f).toInt()
        
        val gravity = (8 - speed / 32).coerceAtLeast(1)
        val offset = 1
        
        for (y in 0 until combinedHeight) {
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
                
                val centerX = combinedWidth / 2
                setPixelColor(centerX + i, y, color)
                setPixelColor(centerX - 1 - i, y, color)
            }
            
            // Draw top LED indicator (gray)
            if (gravcen.topLED >= 0) {
                val centerX = combinedWidth / 2
                val grayColor = RgbColor(128, 128, 128)  // Gray
                setPixelColor(centerX + gravcen.topLED, y, grayColor)
                setPixelColor(centerX - 1 - gravcen.topLED, y, grayColor)
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

    override fun getName(): String = "GravCentric"

    override fun isAudioReactive(): Boolean = true

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
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
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}

