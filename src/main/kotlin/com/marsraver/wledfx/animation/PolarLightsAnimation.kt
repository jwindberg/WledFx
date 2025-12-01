package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Polar Lights animation - Aurora-like effect using Perlin noise
 * By: Kostyantyn Matviyevskyy, Modified by: Andrew Tuline & @dedehai
 */
class PolarLightsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var flipPalette: Boolean = false
    
    private var step: Long = 0L
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
        step = 0L
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        // Calculate adjustHeight based on rows: map(rows, 8, 32, 28, 12)
        val adjustHeight = map(combinedHeight, 8, 32, 28.0f, 12.0f)
        
        // Calculate adjScale based on cols: map(cols, 8, 64, 310, 63)
        val adjScale = map(combinedWidth, 8, 64, 310, 63)
        
        // Calculate _scale based on intensity: map(intensity, 0, 255, 30, adjScale)
        val _scale = map(intensity, 0, 255, 30, adjScale)
        
        // Calculate _speed based on speed: map(speed, 0, 255, 128, 16)
        val _speed = map(speed, 0, 255, 128, 16)
        
        // Increment step for animation
        step++
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                // Calculate perlin noise value
                // perlin8((step%2) + x * _scale, y * 16 + step % 16, step / _speed)
                val noiseX = (step % 2).toDouble() + x * _scale
                val noiseY = y * 16.0 + (step % 16)
                val noiseZ = step.toDouble() / _speed
                val perlinVal = perlin8(noiseX, noiseY, noiseZ)
                
                // Calculate distance from center (rows/2)
                val centerY = combinedHeight / 2.0f
                val distanceFromCenter = abs(centerY - y.toFloat())
                val heightAdjustment = distanceFromCenter * adjustHeight
                
                // qsub8: quantized subtract (subtract with underflow protection)
                val palindex = qsub8(perlinVal, heightAdjustment.roundToInt())
                
                // Flip palette if check1 is enabled
                val finalPalIndex = if (flipPalette) 255 - palindex else palindex
                val palbrightness = palindex
                
                // Get color from palette
                val color = colorFromPalette(finalPalIndex, false, palbrightness)
                setPixelColor(x, y, color)
            }
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

    override fun getName(): String = "Polar Lights"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set flip palette mode (check1)
     */
    fun setFlipPalette(enabled: Boolean) {
        this.flipPalette = enabled
    }

    /**
     * Get flip palette mode
     */
    fun getFlipPalette(): Boolean {
        return flipPalette
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    /**
     * perlin8 - 2D/3D Perlin noise function returning 0-255
     * Simplified implementation for polar lights
     */
    private fun perlin8(x: Double, y: Double, z: Double): Int {
        val scaledX = x / 128.0
        val scaledY = y / 128.0
        val scaledZ = z / 128.0
        val noise = perlinNoise(scaledX, scaledY, scaledZ)
        return ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0).roundToInt()
    }

    /**
     * Perlin noise implementation (simplified version)
     */
    private fun perlinNoise(x: Double, y: Double, z: Double): Double {
        // Simple hash-based noise for performance
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val zi = floor(z).toInt()
        
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
        
        // Hash function for gradients
        fun hash(n: Int): Int {
            var h = n
            h = ((h shl 13) xor h)
            h = h * (h * h * 15731 + 789221) + 1376312589
            return h and 0x7fffffff
        }
        
        // Get gradients for 8 corners of cube
        val g000 = hash(xi + hash(yi + hash(zi)))
        val g001 = hash(xi + hash(yi + hash(zi + 1)))
        val g010 = hash(xi + hash(yi + 1 + hash(zi)))
        val g011 = hash(xi + hash(yi + 1 + hash(zi + 1)))
        val g100 = hash(xi + 1 + hash(yi + hash(zi)))
        val g101 = hash(xi + 1 + hash(yi + hash(zi + 1)))
        val g110 = hash(xi + 1 + hash(yi + 1 + hash(zi)))
        val g111 = hash(xi + 1 + hash(yi + 1 + hash(zi + 1)))
        
        // Smooth interpolation
        fun fade(t: Double): Double {
            return t * t * t * (t * (t * 6 - 15) + 10)
        }
        
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)
        
        // Dot products
        fun dot(g: Int, x: Double, y: Double, z: Double): Double {
            val grad = (g % 12).toDouble()
            val dx = if (grad < 4) 1.0 else if (grad < 8) -1.0 else 0.0
            val dy = if (grad < 4 || grad >= 8) 0.0 else if (grad < 6) 1.0 else -1.0
            val dz = if (grad < 2 || (grad >= 4 && grad < 6) || grad >= 8) 0.0 else if (grad < 4) 1.0 else -1.0
            return dx * x + dy * y + dz * z
        }
        
        val x00 = dot(g000, xf, yf, zf) * (1 - u) + dot(g100, xf - 1, yf, zf) * u
        val x10 = dot(g010, xf, yf - 1, zf) * (1 - u) + dot(g110, xf - 1, yf - 1, zf) * u
        val x01 = dot(g001, xf, yf, zf - 1) * (1 - u) + dot(g101, xf - 1, yf, zf - 1) * u
        val x11 = dot(g011, xf, yf - 1, zf - 1) * (1 - u) + dot(g111, xf - 1, yf - 1, zf - 1) * u
        
        val y0 = x00 * (1 - v) + x10 * v
        val y1 = x01 * (1 - v) + x11 * v
        
        return y0 * (1 - w) + y1 * w
    }

    /**
     * qsub8 - Quantized subtract (subtract with underflow protection)
     * Returns max(0, a - b)
     */
    private fun qsub8(a: Int, b: Int): Int {
        return (a - b).coerceAtLeast(0).coerceIn(0, 255)
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).roundToInt()
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Float, toHigh: Float): Float {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).toFloat()
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
            val brightnessFactor = brightness.coerceIn(0, 255) / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, brightness.coerceIn(0, 255))
        }
    }
}

