package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.LoudnessMeter
import com.marsraver.wledfx.math.MathUtils

/**
 * GravCenter animation - Audio-reactive bars expanding from center with gravity effect
 * By: Andrew Tuline
 */
class GravCenterAnimation : BaseAnimation() {

    private data class GravityState(
        var topLED: Int = 0,
        var gravityCounter: Int = 0
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val gravityStates = mutableListOf<GravityState>()
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
        
        // Initialize gravity states for each row
        gravityStates.clear()
        for (y in 0 until height) {
            gravityStates.add(GravityState())
        }
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade out by 251
        fadeOut(251)
        
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volume = (loudness / 1024.0f) * 255.0f 
        
        val segmentSampleAvg = volume * paramIntensity / 255.0f * 0.125f 
        
        val mySampleAvg = MathUtils.mapf(segmentSampleAvg * 2.0f, 0.0f, 32.0f, 0.0f, width / 2.0f)
        val tempsamp = mySampleAvg.coerceIn(0.0f, width / 2.0f).toInt()
        
        val gravity = (8 - paramSpeed / 32).coerceAtLeast(1)
        val offset = 1
        
        // Process each row
        for (y in 0 until height) {
            val gravcen = gravityStates[y]
            
            // Update topLED based on audio
            if (tempsamp >= gravcen.topLED) {
                gravcen.topLED = tempsamp - offset
            } else if (gravcen.gravityCounter % gravity == 0) {
                gravcen.topLED--
            }
            
            // Draw bars from center
            for (i in 0 until tempsamp) {
                // Using MathUtils here? MathUtils may not have Perlin8.
                // Keeping local Perlin implementation or simplifying.
                // The original had a local Perlin implementation. We should duplicate it or add it to Utils.
                // For now, I'll stick to a simpler noise or re-implement perlin8 locally to be safe.
                val index = perlin8(i * segmentSampleAvg + timeMs, 5000.0 + i * segmentSampleAvg)
                val brightness = (segmentSampleAvg * 8).toInt().coerceIn(0, 255)
                
                val color = colorFromPalette(index, true, 0)
                val blendedColor = ColorUtils.blend(RgbColor.BLACK, color, brightness)
                
                val centerX = width / 2
                setPixelColor(centerX + i, y, blendedColor)
                setPixelColor(centerX - i - 1, y, blendedColor)
            }
            
            if (gravcen.topLED >= 0) {
                val centerX = width / 2
                val topColor = colorFromPalette((timeMs % 256).toInt(), true, 0)
                setPixelColor(centerX + gravcen.topLED, y, topColor)
                setPixelColor(centerX - 1 - gravcen.topLED, y, topColor)
            }
            
            gravcen.gravityCounter = (gravcen.gravityCounter + 1) % gravity
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

    override fun getName(): String = "GravCenter"
    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    // Local Perlin implementation (copied from original)
    private fun perlin8(x: Float, y: Double): Int {
        val scaledX = x / 128.0
        val scaledY = y / 128.0
        val scaledZ = 0.0
        val noise = perlinNoise(scaledX, scaledY, scaledZ)
        return ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0).toInt()
    }

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

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
