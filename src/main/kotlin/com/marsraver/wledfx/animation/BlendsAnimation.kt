package com.marsraver.wledfx.animation
import com.marsraver.wledfx.palette.Palette

import kotlin.math.*

/**
 * Blends animation - Smoothly blends random colors across the palette.
 * Modified, originally by Mark Kriegsman
 */
class BlendsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    
    private lateinit var pixelColors: Array<Array<IntArray>>
    private var startTime: Long = 0L

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
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) {
            startTime = now
        }
        
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return true
        }
        
        val intensity = 128 // Default intensity (0-255)
        val blendSpeed = mapRange(intensity.toDouble(), 0.0, 255.0, 5.0, 30.0).toInt() // Slower blending
        val speed = 32 // Default speed (0-255) - reduced for slower animation
        val shiftSpeed = (speed shr 3) + 1
        val timeMs = (now - startTime) / 1_000_000L
        val shift = ((timeMs * shiftSpeed) / 512).toInt() // Increased divisor to slow down shift
        
        for (y in 0 until combinedHeight) {
            for (x in 0 until combinedWidth) {
                val pixelIndex = y * combinedWidth + x
                val currentShift = shift + (pixelIndex * 3)
                
                // Use quadwave8 function: creates a smooth wave pattern
                val waveValue = quadwave8(((pixelIndex + 1) * 16).toInt())
                val paletteIndex = ((currentShift + waveValue) % 256) % currentPalette.size
                val targetColor = currentPalette[paletteIndex]
                
                // Blend current color towards target color
                val currentColor = pixelColors[x][y]
                pixelColors[x][y] = colorBlend(currentColor, targetColor, blendSpeed)
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y].clone()
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Blends"

    /**
     * Quadwave8 - creates a smooth wave pattern from 0-255
     * Similar to sin wave but with different curve
     */
    private fun quadwave8(x: Int): Int {
        val normalized = (x % 256) / 255.0
        // Create a quadratic wave: goes up, then down
        val wave = if (normalized < 0.5) {
            4 * normalized * normalized
        } else {
            4 * (1 - normalized) * (1 - normalized)
        }
        return (wave * 255).toInt().coerceIn(0, 255)
    }

    /**
     * Blend two colors together
     * blendSpeed: higher = faster blend (0-255)
     */
    private fun colorBlend(color1: IntArray, color2: IntArray, blendSpeed: Int): IntArray {
        val blendFactor = blendSpeed.coerceIn(0, 255) / 255.0
        
        return intArrayOf(
            ((color1[0] * (1.0 - blendFactor)) + (color2[0] * blendFactor)).toInt().coerceIn(0, 255),
            ((color1[1] * (1.0 - blendFactor)) + (color2[1] * blendFactor)).toInt().coerceIn(0, 255),
            ((color1[2] * (1.0 - blendFactor)) + (color2[2] * blendFactor)).toInt().coerceIn(0, 255)
        )
    }

    private fun mapRange(value: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return ((value - inMin) * (outMax - outMin) / (inMax - inMin)) + outMin
    }
}

