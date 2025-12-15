package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * Lissajous animation - Curved patterns using sine and cosine
 * By: Andrew Tuline
 */
class LissajousAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var custom1: Int = 0   // Blur amount
    private var custom3: Int = 0   // Rotation speed
    private var check1: Boolean = false  // Smear mode
    
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade to black by intensity
        fadeToBlackBy(paramIntensity)
        
        // Calculate phase: (now * (1 + custom3)) / 32
        val phase = ((timeMs * (1 + custom3)) / 32).toInt()
        
        // Draw 256 points along the Lissajous curve
        for (i in 0 until 256) {
            // Calculate x and y positions using MathUtils.sin8 and cos8
            val xlocn = MathUtils.sin8(phase / 2 + (i * paramSpeed) / 32)
            val ylocn = MathUtils.cos8(phase / 2 + i * 2)
            
            // Map to screen coordinates with proper rounding
            val x = if (width < 2) {
                1
            } else {
                (MathUtils.map(2 * xlocn, 0, 511, 0, 2 * (width - 1)) + 1) / 2
            }
            
            val y = if (height < 2) {
                1
            } else {
                (MathUtils.map(2 * ylocn, 0, 511, 0, 2 * (height - 1)) + 1) / 2
            }
            
            // Get color from palette: now/100 + i
            val colorIndex = ((timeMs / 100 + i) % 256).toInt()
            val color = colorFromPalette(colorIndex, true, 0)
            
            setPixelColor(x, y, color)
        }
        
        // Apply blur: custom1 >> (1 + check1 * 3), check1
        val blurAmount = custom1 shr (1 + if (check1) 3 else 0)
        if (blurAmount > 0) {
            applyBlur(blurAmount, check1)
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

    override fun getName(): String = "Lissajous"
    override fun supportsIntensity(): Boolean = true

    fun setCustom1(value: Int) { this.custom1 = value.coerceIn(0, 255) }
    fun setCustom3(value: Int) { this.custom3 = value.coerceIn(0, 255) }
    fun setCheck1(enabled: Boolean) { this.check1 = enabled }
    fun getCheck1(): Boolean { return check1 }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeToBlackBy(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun applyBlur(amount: Int, smear: Boolean) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
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
                
                if (smear) {
                    // Smear mode: blend with average
                    temp[x][y] = RgbColor(
                        (current.r * (255 - factor) + avgR * factor) / 255,
                        (current.g * (255 - factor) + avgG * factor) / 255,
                        (current.b * (255 - factor) + avgB * factor) / 255
                    )
                } else {
                    // Regular blur
                    temp[x][y] = RgbColor(
                        (current.r * (255 - factor) + avgR * factor) / 255,
                        (current.g * (255 - factor) + avgG * factor) / 255,
                        (current.b * (255 - factor) + avgB * factor) / 255
                    )
                }
            }
        }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
