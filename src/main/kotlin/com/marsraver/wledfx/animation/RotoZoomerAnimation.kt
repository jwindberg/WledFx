package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Plasma RotoZoomer animation by ldirko, adapted for WLED by Blaz Kristan
 * Creates a rotating and zooming plasma effect
 */
class RotoZoomerAnimation : BaseAnimation() {

    private lateinit var plasma: Array<IntArray>
    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var alt: Boolean = false  // check1 - use XOR pattern instead of noise
    
    private var angle: Float = 0.0f
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        plasma = Array(height) { IntArray(width) }
        startTimeNs = System.nanoTime()
        angle = 0.0f
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val ms = timeMs / 15  // strip.now/15
        
        // Generate plasma pattern
        for (j in 0 until height) {
            for (i in 0 until width) {
                if (alt) {
                    // XOR pattern: (i * 4 ^ j * 4) + ms / 6
                    plasma[j][i] = (((i * 4) xor (j * 4)) + (ms / 6).toInt()) and 0xFF
                } else {
                    // Noise pattern: inoise8(i * 40, j * 40, ms)
                    // MathUtils.inoise8 takes ints.
                    plasma[j][i] = MathUtils.inoise8(i * 40, j * 40, ms.toInt())
                }
            }
        }
        
        // Calculate rotozoom parameters
        // f = (sin_t(*a/2) + ((128-intensity)/128.0) + 1.1) / 1.5
        val sinHalf = sin_t(angle / 2.0f)
        val intensityFactor = (128 - paramIntensity) / 128.0f
        val f = (sinHalf + intensityFactor + 1.1f) / 1.5f
        
        val kosinus = cos_t(angle) * f
        val sinus = sin_t(angle) * f
        
        // Apply rotozoom transformation
        for (i in 0 until width) {
            val u1 = i * kosinus
            val v1 = i * sinus
            for (j in 0 until height) {
                val u = abs8((u1 - j * sinus).toInt()) % width
                val v = abs8((v1 + j * kosinus).toInt()) % height
                val plasmaValue = plasma[v][u]
                val color = colorFromPalette(plasmaValue, false, 255)
                setPixelColor(i, j, color)
            }
        }
        
        // Update rotation angle
        // *a -= 0.03f + float(SEGENV.speed-128)*0.0002f
        val speedFactor = (paramSpeed - 128) * 0.0002f
        angle -= 0.03f + speedFactor
        
        val maxAngle = 1000.0f * 2.0f * PI.toFloat()
        if (angle < -maxAngle) {
            angle += maxAngle
        }
        
        // If angle very positive? Original only checked negative.
        if (angle > maxAngle) {
            angle -= maxAngle
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

    override fun getName(): String = "RotoZoomer"
    override fun supportsIntensity(): Boolean = true

    fun setAlt(enabled: Boolean) { this.alt = enabled }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun sin_t(angle: Float): Float {
        return sin(angle.toDouble()).toFloat()
    }
    
    private fun cos_t(angle: Float): Float {
        return cos(angle.toDouble()).toFloat()
    }
    
    private fun abs8(value: Int): Int {
        return abs(value) and 0xFF
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        // Use BaseAnimation palette support
        val base = getColorFromPalette(index)
        val brightnessFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
