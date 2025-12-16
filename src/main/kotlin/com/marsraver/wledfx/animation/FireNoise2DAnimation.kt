package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.min

/**
 * FireNoise2D - 2D fire effect with Perlin noise
 * By: Andrew Tuline
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 5045
 */
class FireNoise2DAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    // Fire palette colors
    private val firePalette = listOf(
        RgbColor.BLACK, RgbColor.BLACK, RgbColor.BLACK, RgbColor.BLACK,
        RgbColor(255, 0, 0), RgbColor(255, 0, 0), RgbColor(255, 0, 0), RgbColor(255, 69, 0),
        RgbColor(255, 69, 0), RgbColor(255, 69, 0), RgbColor(255, 165, 0), RgbColor(255, 165, 0),
        RgbColor(255, 255, 0), RgbColor(255, 165, 0), RgbColor(255, 255, 0), RgbColor(255, 255, 0)
    )

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val timeMs = (now - startTimeNs) / 1_000_000
        val xscale = paramIntensity * 4
        val yscale = paramSpeed * 8
        
        for (j in 0 until width) {
            for (i in 0 until height) {
                // Use MathUtils.inoise8
                val indexx = MathUtils.inoise8(j * yscale * height / 255, (i * xscale + timeMs / 4).toInt())
                val paletteIndex = min(i * (indexx shr 4), 255)
                val brightness = i * 255 / width
                
                // If paramPalette is set, use it. Otherwise use firePalette.
                // Note: getColorFromPalette logic in BaseAnimation handles palette vs HSV, but we want custom fallback here?
                // Actually BaseAnimation.getColorFromPalette falls back to HSV rainbow.
                // We want to fall back to FirePalette if no Palette is set.
                
                // However, paramPalette updates when setPalette is called.
                // If paramPalette is null, use firePalette.
                
                pixelColors[j][i] = if (paramPalette != null) {
                    getColorFromPalette((paletteIndex * 256 / 255)) // scaling?
                    // Re-implement colorFromPalette logic slightly modified
                     val p = paramPalette!!.colors
                     val pIdx = ((paletteIndex.coerceIn(0, 255)) * p.size / 256).coerceIn(0, p.size - 1)
                     ColorUtils.scaleBrightness(p[pIdx], brightness / 255.0)

                } else {
                     val pIdx = ((paletteIndex % 256) * firePalette.size / 256).coerceIn(0, firePalette.size - 1)
                     ColorUtils.scaleBrightness(firePalette[pIdx], brightness / 255.0)
                }
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

    override fun getName(): String = "FireNoise2D"
    override fun isAudioReactive(): Boolean = false
    override fun supportsIntensity(): Boolean = true
}
