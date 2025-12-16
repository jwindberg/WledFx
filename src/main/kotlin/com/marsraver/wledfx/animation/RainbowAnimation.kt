package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

/**
 * Rainbow animation - Cycles all LEDs through a rainbow
 */
class RainbowAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Calculate counter
        val speedFactor = (paramSpeed shr 2) + 2
        val counter = ((timeMs * speedFactor).toLong() and 0xFFFF).toInt()
        val hue = counter shr 8
        
        // Get color: BaseAnimation uses palette if set, or hsv rainbow default
        // The original code forced "Color Wheel" rainbow even if palette was ignored? 
        // Original: getPalette() returned null.
        // BaseAnimation defaults to Rainbow palette if none set, but user can set palettes.
        // Let's respect BaseAnimation's colorFromPalette which handles the default rainbow behavior if no palette is custom set,
        // or uses the custom palette if set.
        
        val color = getColorFromPalette(hue)
        
        // Fill entire grid with color
        val fillColor = if (paramIntensity < 128) {
            // Blend with white based on intensity (desaturate)
            val blendAmount = 128 - paramIntensity
            ColorUtils.blend(color, RgbColor.WHITE, blendAmount)
        } else {
            color
        }
        
        // Fill all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = fillColor
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

    override fun getName(): String = "Rainbow"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    override fun supportsIntensity(): Boolean = true
}
