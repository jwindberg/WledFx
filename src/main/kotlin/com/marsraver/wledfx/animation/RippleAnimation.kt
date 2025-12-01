package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.Random

/**
 * Ripple animation - Expanding circular ripples that randomly spawn
 */
class RippleAnimation : LedAnimation {

    private data class Ripple(
        var state: Int,      // 0 = inactive, 1-255 = active
        var color: Int,       // Color index
        var posX: Int,        // X position (high byte of pos in original)
        var posY: Int         // Y position (low byte of pos in original)
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val ripples = mutableListOf<Ripple>()
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 0      // Blur amount
    private var overlay: Boolean = false  // check2 - overlay mode (fade out instead of fill)
    
    private val random = Random()
    private var frameCount: Long = 0L

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
        ripples.clear()
        frameCount = 0L
        
        val maxRipples = min(1 + (combinedWidth * combinedHeight) / 4, 100)
        for (i in 0 until maxRipples) {
            ripples.add(Ripple(0, 0, 0, 0))
        }
    }

    override fun update(now: Long): Boolean {
        frameCount++
        
        // Background handling
        if (custom1 > 0 || overlay) {
            // Fade out
            fadeOut(250)
        } else {
            // Fill with black background (SEGCOLOR(1) in original is configurable, but black looks better)
            fill(RgbColor.BLACK)
        }
        
        val maxRipples = ripples.size
        val segmentLength = combinedWidth * combinedHeight
        
        // Process each ripple
        for (i in 0 until maxRipples) {
            val ripple = ripples[i]
            
            if (ripple.state > 0) {
                // Draw wave
                val rippleDecay = (speed shr 4) + 1  // Faster decay if faster propagation
                val rippleOriginX = ripple.posX
                val rippleOriginY = ripple.posY
                val col = colorFromPalette(ripple.color, false, false, 255)
                
                // Propagation calculation
                val propagation = ((ripple.state / rippleDecay - 1) * (speed + 1))
                val propI = propagation shr 8
                val propF = propagation and 0xFF
                
                // Amplitude calculation
                val amp = if (ripple.state < 17) {
                    triwave8((ripple.state - 1) * 8)
                } else {
                    map(ripple.state, 17, 255, 255, 2)
                }
                
                // For 2D: draw circle
                val propI2D = propI / 2
                val mag = scale8(sin8_t(propF shr 2), amp)
                
                if (propI2D > 0) {
                    // Blend ripple color with background (black when not in overlay mode)
                    val bgColor = if (custom1 > 0 || overlay) {
                        getPixelColor(rippleOriginX + propI2D, rippleOriginY)
                    } else {
                        RgbColor.BLACK
                    }
                    drawCircle(rippleOriginX, rippleOriginY, propI2D, 
                              ColorUtils.blend(bgColor, col, mag), 
                              true)
                }
                
                ripple.state += rippleDecay
                if (ripple.state > 254) {
                    ripple.state = 0
                }
            } else {
                // Randomly create new wave
                // hw_random16(IBN + 10000) <= (SEGMENT.intensity >> (SEGMENT.is2D()*3))
                // For 2D, intensity is shifted right by 3 bits
                val threshold = intensity shr 3
                val randomValue = random.nextInt(10000 + frameCount.toInt())
                
                if (randomValue <= threshold) {
                    ripple.state = 1
                    ripple.posX = random.nextInt(combinedWidth)
                    ripple.posY = random.nextInt(combinedHeight)
                    ripple.color = random.nextInt(256)
                }
            }
        }
        
        // Apply blur if specified
        if (custom1 > 0) {
            applyBlur(custom1 shr 1)
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

    override fun getName(): String = "Ripple"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun fill(color: RgbColor) {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = color
            }
        }
    }

    private fun drawCircle(centerX: Int, centerY: Int, radius: Int, color: RgbColor, filled: Boolean) {
        if (radius <= 0) {
            setPixelColor(centerX, centerY, color)
            return
        }
        
        val minX = max(0, centerX - radius)
        val maxX = min(combinedWidth - 1, centerX + radius)
        val minY = max(0, centerY - radius)
        val maxY = min(combinedHeight - 1, centerY + radius)
        
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val dx = x - centerX
                val dy = y - centerY
                val distance = hypot(dx.toDouble(), dy.toDouble()).roundToInt()
                
                if (filled) {
                    if (distance <= radius) {
                        addPixelColor(x, y, color)
                    }
                } else {
                    // Draw circle outline (within 1 pixel of radius)
                    if (abs(distance - radius) <= 1) {
                        addPixelColor(x, y, color)
                    }
                }
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + color.r).coerceAtMost(255),
                (current.g + color.g).coerceAtMost(255),
                (current.b + color.b).coerceAtMost(255)
            )
        }
    }

    private fun applyBlur(amount: Int) {
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
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + avgR * factor) / 255,
                    (current.g * (255 - factor) + avgG * factor) / 255,
                    (current.b * (255 - factor) + avgB * factor) / 255
                )
            }
        }
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    /**
     * triwave8 - Triangular wave function (0-255 input, 0-255 output)
     */
    private fun triwave8(input: Int): Int {
        val inVal = input and 0xFF
        return if (inVal < 128) {
            inVal * 2
        } else {
            255 - (inVal - 128) * 2
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
     * scale8 - Scale a value by another value (scale8(value, scale) = (value * scale) / 256)
     */
    private fun scale8(value: Int, scale: Int): Int {
        return ((value and 0xFF) * (scale and 0xFF)) / 256
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).roundToInt()
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, blend: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, brightness)
        }
    }
}

