package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.min

/**
 * FireNoise2D - 2D fire effect with Perlin noise
 * By: Andrew Tuline
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 5045
 */
class FireNoise2DAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var speed: Int = 128  // Y scale
    private var intensity: Int = 128  // X scale
    private var usePalette: Boolean = false
    
    // Fire palette colors
    private val firePalette = listOf(
        RgbColor.BLACK, RgbColor.BLACK, RgbColor.BLACK, RgbColor.BLACK,
        RgbColor(255, 0, 0), RgbColor(255, 0, 0), RgbColor(255, 0, 0), RgbColor(255, 69, 0),
        RgbColor(255, 69, 0), RgbColor(255, 69, 0), RgbColor(255, 165, 0), RgbColor(255, 165, 0),
        RgbColor(255, 255, 0), RgbColor(255, 165, 0), RgbColor(255, 255, 0), RgbColor(255, 255, 0)
    )

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
        usePalette = true
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
        
        val timeMs = (now - startTimeNs) / 1_000_000
        val xscale = intensity * 4
        val yscale = speed * 8
        
        for (j in 0 until combinedWidth) {
            for (i in 0 until combinedHeight) {
                val indexx = inoise8(j * yscale * combinedHeight / 255, i * xscale + timeMs / 4)
                val paletteIndex = min(i * (indexx shr 4), 255)
                val brightness = i * 255 / combinedWidth
                
                val paletteToUse = if (usePalette && currentPalette != null) {
                    currentPalette!!.colors.toList()
                } else {
                    firePalette
                }
                
                pixelColors[j][i] = colorFromPalette(paletteToUse, paletteIndex, brightness)
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

    override fun getName(): String = "FireNoise2D"

    override fun isAudioReactive(): Boolean = false

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {}
    
    private fun inoise8(x: Int, y: Long): Int {
        val hash = ((x * 2654435761L + y * 2246822519L) and 0xFFFFFFFF).toInt()
        return (hash and 0xFF)
    }
    
    private fun colorFromPalette(palette: List<RgbColor>, index: Int, brightness: Int): RgbColor {
        val paletteIndex = ((index % 256) * palette.size / 256).coerceIn(0, palette.size - 1)
        val baseColor = palette[paletteIndex]
        return ColorUtils.scaleBrightness(baseColor, brightness / 255.0)
    }
}
