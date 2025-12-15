package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

/**
 * Comet (Lighthouse) - Continuous comet with fading trail
 */
class CometAnimation : BaseAnimation() {
    
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var lastIndex: Int = 0
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Comet"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        lastIndex = 0
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        // speed handled via paramSpeed
        val counter = ((now - startTimeNs) / 1_000_000L) * ((paramSpeed shr 2) + 1) and 0xFFFF
        val index = ((counter * width) shr 16).toInt() % width
        
        // Intensity controls fade rate
        fadeOut(paramIntensity)
        
        if (index > lastIndex) {
            for (i in lastIndex..index) drawColumn(i)
        } else if (index < lastIndex && index < 10) {
            for (i in 0..index) drawColumn(i)
        }
        drawColumn(index)
        
        lastIndex = index
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun fadeOut(fadeAmount: Int) {
        val fadeFactor = fadeAmount / 255.0
        for (y in 0 until height) {
            for (x in 0 until width) {
                // scaleColor logic
                val current = pixelColors[x][y]
                val factor = 1.0 - fadeFactor
                pixelColors[x][y] = RgbColor(
                    (current.r * factor).toInt().coerceIn(0, 255),
                    (current.g * factor).toInt().coerceIn(0, 255),
                    (current.b * factor).toInt().coerceIn(0, 255)
                )
            }
        }
    }
    
    private fun drawColumn(x: Int) {
        if (x < 0 || x >= width) return
        val color = getColorFromPalette(x * 255 / width, 255) // map position to hue/palette
        for (y in 0 until height) {
            pixelColors[x][y] = color
        }
    }
    
    private fun getColorFromPalette(index: Int, brightness: Int): RgbColor {
        val base = getColorFromPalette(index) // BaseAnimation helper
        if (brightness < 255) return ColorUtils.scaleBrightness(base, brightness/255.0)
        return base
    }
}
