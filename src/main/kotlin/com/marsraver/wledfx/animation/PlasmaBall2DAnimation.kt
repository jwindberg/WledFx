package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.min

/**
 * PlasmaBall2D - 2D plasma ball effect
 * By: Stepko, Modified by: Andrew Tuline
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 5526
 */
class PlasmaBall2DAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    private var fadeAmount: Int = 64  // custom1 >> 2
    private var blurAmount: Int = 4   // custom2 >> 5

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        // Fade to black
        fadeToBlack(fadeAmount)
        
        val t = ((now - startTimeNs) / 1_000_000 * 8) / (256 - paramSpeed)
        
        for (i in 0 until width) {
            val thisVal = MathUtils.inoise8(i * 30, t.toInt(), t.toInt())
            val thisMax = MathUtils.map(thisVal, 0, 255, 0, width - 1)
            
            for (j in 0 until height) {
                val thisVal_ = MathUtils.inoise8(t.toInt(), j * 30, t.toInt())
                val thisMax_ = MathUtils.map(thisVal_, 0, 255, 0, height - 1)
                
                val x = (i + thisMax_ - width / 2)
                val y = (j + thisMax - width / 2)
                val cx = (i + thisMax_)
                val cy = (j + thisMax)
                
                val shouldDraw = ((x - y > -2) && (x - y < 2)) ||
                                ((width - 1 - x - y) > -2 && (width - 1 - x - y < 2)) ||
                                (width - cx == 0) ||
                                (width - 1 - cx == 0) ||
                                (height - cy == 0) ||
                                (height - 1 - cy == 0)
                
                if (shouldDraw) {
                    val beat = beat8(5)
                    val color = colorFromPalette(beat, true, thisVal)
                    addPixelColor(i, j, color)
                }
            }
        }
        
        blur(blurAmount)
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "PlasmaBall2D"
    override fun isAudioReactive(): Boolean = false

    fun setFadeAmount(value: Int) { this.fadeAmount = value.coerceIn(0, 255) }
    fun setBlurAmount(value: Int) { this.blurAmount = value.coerceIn(0, 255) }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    private fun blur(amount: Int) {
        // Simple box blur
        if (amount == 0) return
        
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                temp[x][y] = pixelColors[x][y]
            }
        }
        
        for (x in 1 until width - 1) {
            for (y in 1 until height - 1) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val color = temp[x + dx][y + dy]
                        r += color.r
                        g += color.g
                        b += color.b
                        count++
                    }
                }
                
                pixelColors[x][y] = RgbColor(r / count, g / count, b / count)
            }
        }
    }
    
    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                min(current.r + color.r, 255),
                min(current.g + color.g, 255),
                min(current.b + color.b, 255)
            )
        }
    }
    
    private fun beat8(bpm: Int): Int {
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        return ((timeMs * bpm / 60000) % 256).toInt()
    }
    
    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
