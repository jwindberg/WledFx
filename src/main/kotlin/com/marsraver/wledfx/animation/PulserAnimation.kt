package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * 2D Pulser animation - Pulsing vertical line that moves across the grid
 * By: ldirko, modified by: Andrew Tuline
 */
class PulserAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade to black: 8 - (intensity>>5)
        val fadeAmount = 8 - (paramIntensity shr 5)
        fadeToBlack(fadeAmount.coerceAtLeast(1))
        
        // Calculate time value: strip.now / (18 - speed / 16)
        val a = (timeMs / (18 - paramSpeed / 16).coerceAtLeast(1)).toLong()
        
        // X position: (a / 14) % cols
        val x = ((a / 14) % width).toInt()
        
        // Y position
        val sinSum = MathUtils.sin8((a * 5).toInt()) + MathUtils.sin8((a * 4).toInt()) + MathUtils.sin8((a * 2).toInt())
        val y = MathUtils.map(sinSum, 0, 765, height - 1, 0)
        
        // Get color from palette
        val colorIndex = MathUtils.map(y, 0, height - 1, 0, 255)
        val color = colorFromPalette(colorIndex, 255, true)
        
        // Set pixel
        setPixelColor(x, y, color)
        
        // Apply blur: intensity>>4
        val blurAmount = paramIntensity shr 4
        applyBlur(blurAmount)
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Pulser"
    override fun supportsIntensity(): Boolean = true

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                // 3x3 blur kernel
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val color = pixelColors[nx][ny]
                            sumR += color.r
                            sumG += color.g
                            sumB += color.b
                            count++
                        }
                    }
                }
                
                val current = pixelColors[x][y]
                val avgR = if (count > 0) sumR / count else current.r
                val avgG = if (count > 0) sumG / count else current.g
                val avgB = if (count > 0) sumB / count else current.b
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + avgR * factor) / 255,
                    (current.g * (255 - factor) + avgG * factor) / 255,
                    (current.b * (255 - factor) + avgB * factor) / 255
                )
            }
        }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun colorFromPalette(index: Int, brightness: Int, linearBlend: Boolean): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
