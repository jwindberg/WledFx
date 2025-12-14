package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.min

/**
 * PlasmaBall2D - 2D plasma ball effect
 * By: Stepko, Modified by: Andrew Tuline
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 5526
 */
class PlasmaBall2DAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var speed: Int = 128
    private var fadeAmount: Int = 64  // custom1 >> 2
    private var blurAmount: Int = 4   // custom2 >> 5

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? = currentPalette

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        // Fade to black
        fadeToBlack(fadeAmount)
        
        val t = ((now - startTimeNs) / 1_000_000 * 8) / (256 - speed)
        
        for (i in 0 until combinedWidth) {
            val thisVal = inoise8(i * 30, t.toInt(), t.toInt())
            val thisMax = map(thisVal, 0, 255, 0, combinedWidth - 1)
            
            for (j in 0 until combinedHeight) {
                val thisVal_ = inoise8(t.toInt(), j * 30, t.toInt())
                val thisMax_ = map(thisVal_, 0, 255, 0, combinedHeight - 1)
                
                val x = (i + thisMax_ - combinedWidth / 2)
                val y = (j + thisMax - combinedWidth / 2)
                val cx = (i + thisMax_)
                val cy = (j + thisMax)
                
                val shouldDraw = ((x - y > -2) && (x - y < 2)) ||
                                ((combinedWidth - 1 - x - y) > -2 && (combinedWidth - 1 - x - y < 2)) ||
                                (combinedWidth - cx == 0) ||
                                (combinedWidth - 1 - cx == 0) ||
                                (combinedHeight - cy == 0) ||
                                (combinedHeight - 1 - cy == 0)
                
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
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "PlasmaBall2D"

    override fun isAudioReactive(): Boolean = false

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {}
    
    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    private fun blur(amount: Int) {
        // Simple box blur
        if (amount == 0) return
        
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                temp[x][y] = pixelColors[x][y]
            }
        }
        
        for (x in 1 until combinedWidth - 1) {
            for (y in 1 until combinedHeight - 1) {
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
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                min(current.r + color.r, 255),
                min(current.g + color.g, 255),
                min(current.b + color.b, 255)
            )
        }
    }
    
    private fun inoise8(x: Int, y: Int, z: Int): Int {
        val hash = ((x * 2654435761L + y * 2246822519L + z * 3266489917L) and 0xFFFFFFFF).toInt()
        return (hash and 0xFF)
    }
    
    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        if (fromHigh == fromLow) return toLow
        return (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow
    }
    
    private fun beat8(bpm: Int): Int {
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        return ((timeMs * bpm / 60000) % 256).toInt()
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
            return ColorUtils.hsvToRgb(index % 256, 255, if (brightness > 0) brightness else 255)
        }
    }
}
