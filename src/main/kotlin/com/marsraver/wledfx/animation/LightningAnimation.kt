package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Lightning animation - Random lightning flashes
 */
class LightningAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var overlay: Boolean = false  // check2
    
    private var aux0: Long = 0L  // Delay between flashes (ms)
    private var aux1: Int = 0     // Number of flashes remaining
    private var step: Long = 0L   // Last flash time
    private var startTimeNs: Long = 0L
    private val random = Random.Default

    override fun getName(): String = "Lightning"
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        aux0 = 0L
        aux1 = 0
        step = 0L
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val segmentLength = width * height
        val ledstart = random.nextInt(segmentLength)
        val ledlen = 1 + random.nextInt(segmentLength - ledstart)
        
        var bri = 255 / random.nextInt(1, 3)
        
        if (aux1 == 0) {
            aux1 = (4 + random.nextInt(4 + paramIntensity / 20 - 4)) * 2
            bri = 52
            aux0 = 200
        }
        
        if (!overlay) {
            fillBackground()
        }
        
        if (aux1 > 3 && (aux1 and 0x01) == 0) {
            for (i in ledstart until ledstart + ledlen) {
                val x = i % width
                val y = i / width
                if (x in 0 until width && y in 0 until height) {
                    val color = getColorFromPaletteWithBrightness(i, true, bri)
                    setPixelColor(x, y, color)
                }
            }
            aux1--
            step = timeMs
        } else {
            if (timeMs - step > aux0) {
                aux1--
                if (aux1 < 2) aux1 = 0
                
                aux0 = (50 + random.nextInt(100)).toLong()
                
                if (aux1 == 2) {
                    aux0 = (random.nextInt(255 - paramSpeed) * 100).toLong()
                }
                
                step = timeMs
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

    fun setOverlay(enabled: Boolean) {
        this.overlay = enabled
    }

    fun getOverlay(): Boolean {
        return overlay
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun fillBackground() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = RgbColor.BLACK
            }
        }
    }

    private fun getColorFromPaletteWithBrightness(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        // BaseAnimation.getColorFromPalette(index)
        val baseColor = getColorFromPalette(index)
        return ColorUtils.scaleBrightness(baseColor, brightness / 255.0)
    }
}
