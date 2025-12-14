package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.cos
import kotlin.math.PI

/**
 * Lake - Water ripple effect
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 2315
 */
class LakeAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var speed: Int = 128

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
        
        val sp = speed / 10
        val wave1 = beatsin8(sp + 2, -64, 64)
        val wave2 = beatsin8(sp + 1, -64, 64)
        val wave3 = beatsin8(sp + 2, 0, 80)
        
        // Apply to each row
        for (y in 0 until combinedHeight) {
            for (x in 0 until combinedWidth) {
                val i = x + y * combinedWidth  // Linear index
                val index = cos8((i * 15) + wave1) / 2 + cubicwave8((i * 23) + wave2) / 2
                val lum = if (index > wave3) index - wave3 else 0
                
                pixelColors[x][y] = colorFromPalette(index, true, lum)
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

    override fun getName(): String = "Lake"

    override fun isAudioReactive(): Boolean = false

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {}
    
    private fun beatsin8(bpm: Int, low: Int, high: Int): Int {
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        val beat = ((timeMs * bpm / 60000.0) % 1.0) * 2 * PI
        val sinVal = kotlin.math.sin(beat)
        return (((sinVal + 1.0) / 2.0) * (high - low) + low).toInt()
    }
    
    private fun cos8(angle: Int): Int {
        val radians = (angle % 256) * 2 * PI / 256
        return ((cos(radians) + 1.0) * 127.5).toInt()
    }
    
    private fun cubicwave8(angle: Int): Int {
        val x = (angle % 256) / 256.0
        val cubic = 4 * x * x * x - 6 * x * x + 3 * x
        return (cubic * 255).toInt().coerceIn(0, 255)
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
