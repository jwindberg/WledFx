package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * MetaBalls animation - Blob-like effect with 3 moving points
 * By: Stefan Petrick, adapted by Andrew Tuline
 */
class MetaBallsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
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
        
        // Speed: 0.25f * (1+(speed>>6))
        val speedFactor = 0.25f * (1 + (speed shr 6))
        val timeValue = timeMs * speedFactor
        
        // Get 2 random moving points using perlin8
        val x2 = map(perlin8(timeValue, 25355.0, 685.0), 0, 255, 0, combinedWidth - 1)
        val y2 = map(perlin8(timeValue, 355.0, 11685.0), 0, 255, 0, combinedHeight - 1)
        
        val x3 = map(perlin8(timeValue, 55355.0, 6685.0), 0, 255, 0, combinedWidth - 1)
        val y3 = map(perlin8(timeValue, 25355.0, 22685.0), 0, 255, 0, combinedHeight - 1)
        
        // One Lissajou function using beatsin8_t
        val x1 = beatsin8_t((23 * speedFactor).toInt(), 0, combinedWidth - 1, timeMs)
        val y1 = beatsin8_t((28 * speedFactor).toInt(), 0, combinedHeight - 1, timeMs)
        
        for (y in 0 until combinedHeight) {
            for (x in 0 until combinedWidth) {
                // Calculate distances of the 3 points from actual pixel
                // and add them together with weighting
                
                // Point 1 (Lissajou) - 2x weight
                var dx = abs(x - x1)
                var dy = abs(y - y1)
                var dist = 2 * sqrt32_bw((dx * dx) + (dy * dy))
                
                // Point 2 (Perlin)
                dx = abs(x - x2)
                dy = abs(y - y2)
                dist += sqrt32_bw((dx * dx) + (dy * dy))
                
                // Point 3 (Perlin)
                dx = abs(x - x3)
                dy = abs(y - y3)
                dist += sqrt32_bw((dx * dx) + (dy * dy))
                
                // Inverse result
                val color = if (dist > 0) 1000 / dist else 255
                
                // Map color between thresholds
                if (color > 0 && color < 60) {
                    val paletteIndex = map(color * 9, 9, 531, 0, 255)
                    val pixelColor = colorFromPalette(paletteIndex, true, 0)
                    setPixelColor(x, y, pixelColor)
                } else {
                    val pixelColor = colorFromPalette(0, true, 0)
                    setPixelColor(x, y, pixelColor)
                }
            }
        }
        
        // Show the 3 points in white
        setPixelColor(x1, y1, RgbColor.WHITE)
        setPixelColor(x2, y2, RgbColor.WHITE)
        setPixelColor(x3, y3, RgbColor.WHITE)
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "MetaBalls"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    /**
     * perlin8 - 3D Perlin noise function returning 0-255
     */
    private fun perlin8(x: Float, y: Double, z: Double): Int {
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
        val xi = x.toInt()
        val yi = y.toInt()
        val zi = z.toInt()
        
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
     * beatsin8_t - Sine wave that beats at a given frequency
     * Returns value oscillating between min and max
     */
    private fun beatsin8_t(frequency: Int, min: Int, max: Int, timeMs: Long): Int {
        val periodMs = (60000.0 / frequency).toLong()
        val phase = ((timeMs % periodMs) * 2.0 * PI / periodMs).toDouble()
        val sine = sin(phase)
        val normalized = (sine + 1.0) / 2.0
        return (min + (normalized * (max - min))).roundToInt()
    }

    /**
     * sqrt32_bw - Fast square root approximation for 32-bit values
     * Returns integer square root
     */
    private fun sqrt32_bw(value: Int): Int {
        if (value <= 0) return 0
        return sqrt(value.toDouble()).toInt()
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).roundToInt()
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Double, toHigh: Double): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow)
        if (fromRange == 0.0) return toLow.roundToInt()
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

