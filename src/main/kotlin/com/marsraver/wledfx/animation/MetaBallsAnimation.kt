package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * MetaBalls animation - Blob-like effect with 3 moving points
 * By: Stefan Petrick, adapted by Andrew Tuline
 */
class MetaBallsAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Speed: 0.25f * (1+(speed>>6))
        val speedFactor = 0.25f * (1 + (paramSpeed shr 6))
        val timeValue = timeMs * speedFactor
        
        // Get 2 random moving points using perlin8
        // Note: Using a simplified timeValue cast to Int for inoise8 might lose smoothness if speed is low? 
        // inoise8 in MathUtils takes Float or Int. Let's use Float signature if we can pass floats.
        // Actually MathUtils.inoise8 takes (x:Float, y:Float). 
        // The original used perlin8(timeValue, const, const). 
        // We can map timeValue to what perlin8 expects.
        // The original perlin8 implementation generated noise from 3D inputs (time, constant, constant).
        
        val x2 = MathUtils.map(MathUtils.inoise8(timeValue.toInt(), 25355, 685), 0, 255, 0, width - 1)
        val y2 = MathUtils.map(MathUtils.inoise8(timeValue.toInt(), 355, 11685), 0, 255, 0, height - 1)
        
        val x3 = MathUtils.map(MathUtils.inoise8(timeValue.toInt(), 55355, 6685), 0, 255, 0, width - 1)
        val y3 = MathUtils.map(MathUtils.inoise8(timeValue.toInt(), 25355, 22685), 0, 255, 0, height - 1)
        
        // One Lissajou function using beatsin8_t
        val x1 = MathUtils.beatsin8((23 * speedFactor).toInt(), 0, width - 1, timeMs)
        val y1 = MathUtils.beatsin8((28 * speedFactor).toInt(), 0, height - 1, timeMs)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Calculate distances of the 3 points from actual pixel
                // and add them together with weighting
                
                // Point 1 (Lissajou) - 2x weight
                var dx = abs(x - x1)
                var dy = abs(y - y1)
                var dist = 2 * Math.sqrt(((dx * dx) + (dy * dy)).toDouble()).toInt()
                
                // Point 2 (Perlin)
                dx = abs(x - x2)
                dy = abs(y - y2)
                dist += Math.sqrt(((dx * dx) + (dy * dy)).toDouble()).toInt()
                
                // Point 3 (Perlin)
                dx = abs(x - x3)
                dy = abs(y - y3)
                dist += Math.sqrt(((dx * dx) + (dy * dy)).toDouble()).toInt()
                
                // Inverse result
                val color = if (dist > 0) 1000 / dist else 255
                
                // Map color between thresholds
                if (color > 0 && color < 60) {
                    val paletteIndex = MathUtils.map(color * 9, 9, 531, 0, 255)
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
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "MetaBalls"

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
