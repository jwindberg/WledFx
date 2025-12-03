package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * Gravcenter animation - Audio-reactive bars expanding from center with gravity effect
 * By: Andrew Tuline
 */
class GravCenterAnimation : LedAnimation {

    private data class GravityState(
        var topLED: Int = 0,
        var gravityCounter: Int = 0
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    
    private val gravityStates = mutableListOf<GravityState>()
    
    private var loudnessMeter: LoudnessMeter? = null
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
        
        // Initialize gravity states for each row
        gravityStates.clear()
        for (y in 0 until combinedHeight) {
            gravityStates.add(GravityState())
        }
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade out by 251
        fadeOut(251)
        
        // Get loudness (0-1024) and convert to volume-like value
        // The original RMS values were small, but after calculation they need to produce
        // segmentSampleAvg values that map to meaningful pixel counts (0-16 for a 32-wide grid)
        // We need much larger volume values to produce segmentSampleAvg in the 0-32 range
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        // Scale 0-1024 to a range that produces segmentSampleAvg values around 0-32
        // If loudness=1024, we want segmentSampleAvg*2 to be around 32
        // segmentSampleAvg = volume * intensity / 255.0f * 0.125f
        // segmentSampleAvg * 2 = volume * intensity / 255.0f * 0.25f
        // For segmentSampleAvg*2 = 32: volume = 32 * 255.0f / (intensity * 0.25f)
        // With intensity=128: volume = 32 * 255 / (128 * 0.25) = 8160 / 32 = 255
        // So scale 0-1024 to 0-255 (or similar range)
        val volume = (loudness / 1024.0f) * 255.0f  // Convert 0-1024 to 0-255
        
        // Calculate segment sample average
        val segmentSampleAvg = volume * intensity / 255.0f * 0.125f  // divide by 8
        
        // Map to pixels available: mapf(segmentSampleAvg*2.0, 0.0f, 32.0f, 0.0f, SEGLEN/2.0f)
        val mySampleAvg = mapf(segmentSampleAvg * 2.0f, 0.0f, 32.0f, 0.0f, combinedWidth / 2.0f)
        val tempsamp = mySampleAvg.coerceIn(0.0f, combinedWidth / 2.0f).toInt()
        
        val gravity = (8 - speed / 32).coerceAtLeast(1)
        val offset = 1
        
        // Process each row
        for (y in 0 until combinedHeight) {
            val gravcen = gravityStates[y]
            
            // Update topLED based on audio
            if (tempsamp >= gravcen.topLED) {
                gravcen.topLED = tempsamp - offset
            } else if (gravcen.gravityCounter % gravity == 0) {
                gravcen.topLED--
            }
            
            // Draw bars from center
            for (i in 0 until tempsamp) {
                val index = perlin8(i * segmentSampleAvg + timeMs, 5000.0 + i * segmentSampleAvg)
                val brightness = (segmentSampleAvg * 8).toInt().coerceIn(0, 255)
                
                val color = colorFromPalette(index, true, 0)
                val blendedColor = ColorUtils.blend(RgbColor.BLACK, color, brightness)
                
                // Set pixels on both sides of center
                val centerX = combinedWidth / 2
                setPixelColor(centerX + i, y, blendedColor)
                setPixelColor(centerX - i - 1, y, blendedColor)
            }
            
            // Draw top LED indicator
            if (gravcen.topLED >= 0) {
                val centerX = combinedWidth / 2
                val topColor = colorFromPalette((timeMs % 256).toInt(), true, 0)
                setPixelColor(centerX + gravcen.topLED, y, topColor)
                setPixelColor(centerX - 1 - gravcen.topLED, y, topColor)
            }
            
            gravcen.gravityCounter = (gravcen.gravityCounter + 1) % gravity
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

    override fun getName(): String = "GravCenter"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    /**
     * perlin8 - 3D Perlin noise function returning 0-255
     */
    private fun perlin8(x: Float, y: Double): Int {
        val scaledX = x / 128.0
        val scaledY = y / 128.0
        val scaledZ = 0.0
        val noise = perlinNoise(scaledX, scaledY, scaledZ)
        return ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0).toInt()
    }

    /**
     * Perlin noise implementation (simplified version)
     */
    private fun perlinNoise(x: Double, y: Double, z: Double): Double {
        val xi = x.toInt()
        val yi = y.toInt()
        val zi = z.toInt()
        
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
        
        fun hash(n: Int): Int {
            var h = n
            h = ((h shl 13) xor h)
            h = h * (h * h * 15731 + 789221) + 1376312589
            return h and 0x7fffffff
        }
        
        val g000 = hash(xi + hash(yi + hash(zi)))
        val g001 = hash(xi + hash(yi + hash(zi + 1)))
        val g010 = hash(xi + hash(yi + 1 + hash(zi)))
        val g011 = hash(xi + hash(yi + 1 + hash(zi + 1)))
        val g100 = hash(xi + 1 + hash(yi + hash(zi)))
        val g101 = hash(xi + 1 + hash(yi + hash(zi + 1)))
        val g110 = hash(xi + 1 + hash(yi + 1 + hash(zi)))
        val g111 = hash(xi + 1 + hash(yi + 1 + hash(zi + 1)))
        
        fun fade(t: Double): Double {
            return t * t * t * (t * (t * 6 - 15) + 10)
        }
        
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)
        
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

    private fun mapf(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
        val fromRange = fromHigh - fromLow
        val toRange = toHigh - toLow
        if (fromRange == 0.0f) return toLow
        val scaled = (value - fromLow) / fromRange
        return toLow + scaled * toRange
    }

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
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}

