package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.floor
import java.util.Random

/**
 * Plasma RotoZoomer animation by ldirko, adapted for WLED by Blaz Kristan
 * Creates a rotating and zooming plasma effect
 */
class RotoZoomerAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var plasma: Array<IntArray>
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var alt: Boolean = false  // check1 - use XOR pattern instead of noise
    
    private var angle: Float = 0.0f
    private var startTimeNs: Long = 0L
    private val random = Random()

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
        plasma = Array(combinedHeight) { IntArray(combinedWidth) }
        startTimeNs = System.nanoTime()
        angle = 0.0f
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val ms = timeMs / 15  // strip.now/15
        
        // Generate plasma pattern
        for (j in 0 until combinedHeight) {
            for (i in 0 until combinedWidth) {
                if (alt) {
                    // XOR pattern: (i * 4 ^ j * 4) + ms / 6
                    plasma[j][i] = (((i * 4) xor (j * 4)) + (ms / 6).toInt()) and 0xFF
                } else {
                    // Noise pattern: inoise8(i * 40, j * 40, ms)
                    plasma[j][i] = inoise8(i * 40.0, j * 40.0, ms.toDouble()).toInt()
                }
            }
        }
        
        // Calculate rotozoom parameters
        // f = (sin_t(*a/2) + ((128-intensity)/128.0) + 1.1) / 1.5
        val sinHalf = sin_t(angle / 2.0f)
        val intensityFactor = (128 - intensity) / 128.0f
        val f = (sinHalf + intensityFactor + 1.1f) / 1.5f
        
        val kosinus = cos_t(angle) * f
        val sinus = sin_t(angle) * f
        
        // Apply rotozoom transformation
        for (i in 0 until combinedWidth) {
            val u1 = i * kosinus
            val v1 = i * sinus
            for (j in 0 until combinedHeight) {
                val u = abs8((u1 - j * sinus).toInt()) % combinedWidth
                val v = abs8((v1 + j * kosinus).toInt()) % combinedHeight
                val plasmaValue = plasma[v][u]
                val color = colorFromPalette(plasmaValue, false, 255)
                setPixelColor(i, j, color)
            }
        }
        
        // Update rotation angle
        // *a -= 0.03f + float(SEGENV.speed-128)*0.0002f
        val speedFactor = (speed - 128) * 0.0002f
        angle -= 0.03f + speedFactor
        
        // Protect from very large values: if(*a < -6283.18530718f) *a += 6283.18530718f
        // 6283.18530718f = 1000 * 2 * PI
        val maxAngle = 1000.0f * 2.0f * PI.toFloat()
        if (angle < -maxAngle) {
            angle += maxAngle
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

    override fun getName(): String = "RotoZoomer"

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
     * sin_t - Sine function (equivalent to FastLED's sin_t)
     */
    private fun sin_t(angle: Float): Float {
        return sin(angle.toDouble()).toFloat()
    }

    /**
     * cos_t - Cosine function (equivalent to FastLED's cos_t)
     */
    private fun cos_t(angle: Float): Float {
        return cos(angle.toDouble()).toFloat()
    }

    /**
     * abs8 - Absolute value clamped to 8-bit (0-255)
     */
    private fun abs8(value: Int): Int {
        return abs(value) and 0xFF
    }

    /**
     * inoise8 - 3D noise function returning 0-255
     */
    private fun inoise8(x: Double, y: Double, z: Double): Double {
        val scaledX = x / 128.0
        val scaledY = y / 128.0
        val scaledZ = z / 128.0
        val noise = perlinNoise(scaledX, scaledY, scaledZ)
        return ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0)
    }

    /**
     * Perlin noise implementation (simplified version)
     */
    private fun perlinNoise(x: Double, y: Double, z: Double): Double {
        // Simple hash-based noise for performance
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val zi = floor(z).toInt()
        
        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)
        
        // Hash function
        fun hash(n: Int): Int {
            var h = n
            h = h xor (h shl 15)
            h = h xor (h ushr 10)
            h = h xor (h shl 3)
            h = h xor (h ushr 6)
            h = h xor (h shl 11)
            h = h xor (h ushr 16)
            return h
        }
        
        val a = hash(xi + hash(yi + hash(zi)))
        val b = hash(xi + 1 + hash(yi + hash(zi)))
        val c = hash(xi + hash(yi + 1 + hash(zi)))
        val d = hash(xi + 1 + hash(yi + 1 + hash(zi)))
        val e = hash(xi + hash(yi + hash(zi + 1)))
        val f = hash(xi + 1 + hash(yi + hash(zi + 1)))
        val g = hash(xi + hash(yi + 1 + hash(zi + 1)))
        val h = hash(xi + 1 + hash(yi + 1 + hash(zi + 1)))
        
        // Smooth interpolation
        fun lerp(a: Double, b: Double, t: Double): Double {
            return a + t * (b - a)
        }
        
        fun fade(t: Double): Double {
            return t * t * t * (t * (t * 6 - 15) + 10)
        }
        
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)
        
        val n1 = lerp(
            lerp(lerp(a / 2147483647.0, b / 2147483647.0, u),
                 lerp(c / 2147483647.0, d / 2147483647.0, u), v),
            lerp(lerp(e / 2147483647.0, f / 2147483647.0, u),
                 lerp(g / 2147483647.0, h / 2147483647.0, u), v), w)
        
        return n1.coerceIn(-1.0, 1.0)
    }

    /**
     * Get color from palette (equivalent to SEGMENT.color_from_palette)
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
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, brightness)
        }
    }
}

