package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Lissajous animation - Curved patterns using sine and cosine
 * By: Andrew Tuline
 */
class LissajousAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 0   // Blur amount
    private var custom3: Int = 0   // Rotation speed
    private var check1: Boolean = false  // Smear mode
    
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
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade to black by intensity
        fadeToBlackBy(intensity)
        
        // Calculate phase: (now * (1 + custom3)) / 32
        val phase = ((timeMs * (1 + custom3)) / 32).toInt()
        
        // Draw 256 points along the Lissajous curve
        for (i in 0 until 256) {
            // Calculate x and y positions using sin8_t and cos8_t
            val xlocn = sin8_t(phase / 2 + (i * speed) / 32)
            val ylocn = cos8_t(phase / 2 + i * 2)
            
            // Map to screen coordinates with proper rounding
            val x = if (combinedWidth < 2) {
                1
            } else {
                (map(2 * xlocn, 0, 511, 0, 2 * (combinedWidth - 1)) + 1) / 2
            }
            
            val y = if (combinedHeight < 2) {
                1
            } else {
                (map(2 * ylocn, 0, 511, 0, 2 * (combinedHeight - 1)) + 1) / 2
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
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Lissajous"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set custom1 (Blur amount)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
    }

    /**
     * Set custom3 (Rotation speed)
     */
    fun setCustom3(value: Int) {
        this.custom3 = value.coerceIn(0, 255)
    }

    /**
     * Set check1 (Smear mode)
     */
    fun setCheck1(enabled: Boolean) {
        this.check1 = enabled
    }

    /**
     * Get check1
     */
    fun getCheck1(): Boolean {
        return check1
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeToBlackBy(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    /**
     * sin8_t - Sine function for 8-bit input (0-255 maps to 0-2π)
     */
    private fun sin8_t(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val sine = sin(radians)
        return ((sine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * cos8_t - Cosine function for 8-bit input (0-255 maps to 0-2π)
     */
    private fun cos8_t(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val cosine = cos(radians)
        return ((cosine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * Apply blur to the pixel grid
     */
    private fun applyBlur(amount: Int, smear: Boolean) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                // 3x3 blur kernel
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
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
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).roundToInt()
    }

    /**
     * Get color from palette
     */
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
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}

