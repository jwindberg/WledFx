package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * Noise2D - Basic 2D Perlin noise pattern
 * By: Andrew Tuline
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 5507
 */
class Noise2DAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val timeMs = (now - startTimeNs) / 1_000_000
        val scale = paramIntensity + 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Use MathUtils.inoise8 3D
                val pixelHue8 = MathUtils.inoise8(x * scale, y * scale, (timeMs / (16 - paramSpeed / 16)).toInt())
                pixelColors[x][y] = colorFromPalette(pixelHue8, true, 255)
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

    override fun getName(): String = "Noise2D"
    override fun isAudioReactive(): Boolean = false
    override fun supportsIntensity(): Boolean = true

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
