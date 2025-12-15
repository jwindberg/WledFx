package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * Blends animation - Smoothly blends random colors across the palette.
 */
class BlendsAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTime: Long = 0L

    override fun getName(): String = "Blends"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
         pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
         startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) startTime = now
        
        val palette = paramPalette?.colors
        if (palette == null || palette.isEmpty()) return true
        
        val blendSpeed = MathUtils.map(paramIntensity, 0, 255, 5, 30)
        val shiftSpeed = (paramSpeed shr 3) + 1
        val timeMs = (now - startTime) / 1_000_000L
        val shift = ((timeMs * shiftSpeed) / 512).toInt()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixelIndex = y * width + x
                val currentShift = shift + (pixelIndex * 3)
                
                val waveValue = quadwave8(((pixelIndex + 1) * 16))
                val paletteIndex = ((currentShift + waveValue) % 256) % palette.size
                val targetColor = palette[paletteIndex]
                
                val currentColor = pixelColors[x][y]
                pixelColors[x][y] = colorBlend(currentColor, targetColor, blendSpeed)
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
    
    private fun quadwave8(x: Int): Int {
        val normalized = (x % 256) / 255.0
        val wave = if (normalized < 0.5) {
            4 * normalized * normalized
        } else {
            4 * (1 - normalized) * (1 - normalized)
        }
        return (wave * 255).toInt().coerceIn(0, 255)
    }

    private fun colorBlend(color1: RgbColor, color2: RgbColor, blendSpeed: Int): RgbColor {
        return ColorUtils.blend(color1, color2, blendSpeed)
    }
}
