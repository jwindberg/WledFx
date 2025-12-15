package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Polar Lights animation - Aurora-like effect using Perlin noise
 * By: Kostyantyn Matviyevskyy, Modified by: Andrew Tuline & @dedehai
 */
class PolarLightsAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var flipPalette: Boolean = false
    private var step: Long = 0L
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        step = 0L
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        // Calculate adjustHeight based on rows: map(rows, 8, 32, 28, 12)
        val adjustHeight = MathUtils.mapf(height.toFloat(), 8.0f, 32.0f, 28.0f, 12.0f)
        
        // Calculate adjScale based on cols: map(cols, 8, 64, 310, 63)
        val adjScale = MathUtils.map(width, 8, 64, 310, 63)
        
        // Calculate _scale based on intensity: map(intensity, 0, 255, 30, adjScale)
        val _scale = MathUtils.map(paramIntensity, 0, 255, 30, adjScale)
        
        // Calculate _speed based on speed: map(speed, 0, 255, 128, 16)
        val _speed = MathUtils.map(paramSpeed, 0, 255, 128, 16)
        
        // Increment step for animation
        step++
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Calculate perlin noise value
                // perlin8((step%2) + x * _scale, y * 16 + step % 16, step / _speed)
                val noiseX = (step % 2) + x * _scale
                val noiseY = y * 16 + (step % 16).toInt()
                val noiseZ = (step / _speed).toInt()
                
                // Using inoise8 3D. Original used double inputs but MathUtils.inoise8 takes Ints.
                // We need to be careful about scaling. The original perlin8 implementation took doubles
                // and scaled them by /128.0.
                // MathUtils.inoise8 takes ints and doesn't scale inside (standard behavior).
                // So we should pass (val * 256) or similar?
                // Actually Looking at PolarLights original:
                // noiseX = (step%2) + x*_scale.
                // perlin8(noiseX, noiseY, noiseZ)
                // perlin8 implementation: scaledX = x / 128.0.
                // If we use MathUtils.inoise8 (standard perlin), it expects coordinate space.
                // If we just pass inputs as is, it might be too high frequency?
                // Let's rely on standard coordinate scaling.
                
                // Let's try passing directly to inoise8 but handling the scaling that perlin8 did.
                // perlin8 did / 128.0.
                // inoise8 typically works on integers where 256 is a period or similar.
                // Let's try to map the logic.
                
                // Cast to Int for 3D noise
                val perlinVal = MathUtils.inoise8(noiseX.toInt(), noiseY.toInt(), noiseZ)
                
                // Calculate distance from center (rows/2)
                val centerY = height / 2.0f
                val distanceFromCenter = abs(centerY - y.toFloat())
                val heightAdjustment = distanceFromCenter * adjustHeight
                
                // qsub8: quantized subtract (subtract with underflow protection)
                val palindex = MathUtils.qsub8(perlinVal, heightAdjustment.roundToInt())
                
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
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Polar Lights"
    override fun supportsIntensity(): Boolean = true

    fun setFlipPalette(enabled: Boolean) { this.flipPalette = enabled }
    fun getFlipPalette(): Boolean { return flipPalette }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = brightness.coerceIn(0, 255) / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
