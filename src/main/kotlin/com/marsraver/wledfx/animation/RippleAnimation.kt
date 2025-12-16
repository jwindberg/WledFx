package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import java.util.Random
import kotlin.math.*

/**
 * Ripple animation - Expanding circular ripples that randomly spawn
 */
class RippleAnimation : BaseAnimation() {

    private data class Ripple(
        var state: Int,      // 0 = inactive, 1-255 = active
        var color: Int,       // Color index
        var posX: Int,        // X position 
        var posY: Int         // Y position 
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val ripples = mutableListOf<Ripple>()
    private var custom1: Int = 0      // Blur amount (if we want to expose it?)
    private var overlay: Boolean = false  // check2
    
    // Note: random is already available in kotlin.random.Random but Ripple used java.util.Random, switching to Kotlin's for consistency
    private val random = kotlin.random.Random.Default
    private var frameCount: Long = 0L

    override fun getName(): String = "Ripple"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        ripples.clear()
        frameCount = 0L
        
        val maxRipples = min(1 + (width * height) / 4, 100)
        for (i in 0 until maxRipples) {
            ripples.add(Ripple(0, 0, 0, 0))
        }
    }

    override fun update(now: Long): Boolean {
        frameCount++
        
        // Background
        if (custom1 > 0 || overlay) {
            fadeOut(250)
        } else {
            fill(RgbColor.BLACK)
        }
        
        val maxRipples = ripples.size
        
        for (i in 0 until maxRipples) {
            val ripple = ripples[i]
            
            if (ripple.state > 0) {
                // Draw wave
                val rippleDecay = (paramSpeed shr 4) + 1 
                val rippleOriginX = ripple.posX
                val rippleOriginY = ripple.posY
                val col = getColorFromPaletteWithBrightness(ripple.color, 255)
                
                val propagation = ((ripple.state / rippleDecay - 1) * (paramSpeed + 1))
                val propI = propagation shr 8
                val propF = propagation and 0xFF
                
                val amp = if (ripple.state < 17) {
                    MathUtils.triwave8((ripple.state - 1) * 8)
                } else {
                    map(ripple.state, 17, 255, 255, 2)
                }
                
                val propI2D = propI / 2
                // Reuse MathUtils
                val mag = MathUtils.scale8(MathUtils.sin8(propF shr 2), amp)
                
                if (propI2D > 0) {
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
                // Random spawn
                // Use paramIntensity
                val threshold = paramIntensity shr 3
                val randomValue = random.nextInt(10000 + frameCount.toInt())
                
                if (randomValue <= threshold) {
                    ripple.state = 1
                    ripple.posX = random.nextInt(width)
                    ripple.posY = random.nextInt(height)
                    ripple.color = random.nextInt(256)
                }
            }
        }
        
        if (custom1 > 0) {
            applyBlur(custom1 shr 1)
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun fill(color: RgbColor) {
        for (x in 0 until width) {
            for (y in 0 until height) {
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
        val maxX = min(width - 1, centerX + radius)
        val minY = max(0, centerY - radius)
        val maxY = min(height - 1, centerY + radius)
        
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
                    if (abs(distance - radius) <= 1) {
                        addPixelColor(x, y, color)
                    }
                }
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
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

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).roundToInt()
    }
    
    private fun getColorFromPaletteWithBrightness(index: Int, brightness: Int): RgbColor {
        return getColorFromPalette(index)
        // Original supported brightness scaling, but getColorFromPalette returns full brightness.
        // We can scale it.
        // Actually getColorFromPalette returns the raw palette color.
        // Let's implement brightness scaling helper if needed or just use ColorUtils.scaleBrightness
        // Base class helper: getColorFromPalette(index)
    }
}
