package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

/**
 * PlasmaRotoZoom - Rotating and zooming plasma effect
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 6226
 */
class PlasmaRotoZoomAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var plasma: IntArray
    private var startTimeNs: Long = 0L
    
    private var useAltMode: Boolean = false
    private var angle: Float = 0f

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        plasma = IntArray(width * height)
        startTimeNs = System.nanoTime()
        angle = 0f
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val ms = ((now - startTimeNs) / 1_000_000 / 15).toInt()
        
        // Generate plasma
        for (j in 0 until height) {
            val index = j * width
            for (i in 0 until width) {
                plasma[index + i] = if (useAltMode) {
                    ((i * 4) xor (j * 4)) + ms / 6
                } else {
                    MathUtils.inoise8(i * 40, j * 40, ms)
                }
            }
        }
        
        // Rotozoom
        val f = (sinT(angle / 2) + ((128 - paramIntensity) / 128.0f) + 1.1f) / 1.5f
        val kosinus = cosT(angle) * f
        val sinus = sinT(angle) * f
        
        for (i in 0 until width) {
            val u1 = i * kosinus
            val v1 = i * sinus
            for (j in 0 until height) {
                val u = abs((u1 - j * sinus).toInt()) % width
                val v = abs((v1 + j * kosinus).toInt()) % height
                val paletteIndex = plasma[v * width + u]
                pixelColors[i][j] = colorFromPalette(paletteIndex, true, 255)
            }
        }
        
        // Update rotation
        angle -= 0.03f + (paramSpeed - 128) * 0.0002f
        if (angle < -6283.18530718f) angle += 6283.18530718f  // 1000*2*PI
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "PlasmaRotoZoom"
    override fun supportsIntensity(): Boolean = true

    fun setUseAltMode(enabled: Boolean) { this.useAltMode = enabled }
    fun getUseAltMode(): Boolean { return useAltMode }

    private fun sinT(angle: Float): Float {
        return sin(angle.toDouble()).toFloat()
    }
    
    private fun cosT(angle: Float): Float {
        return cos(angle.toDouble()).toFloat()
    }
    
    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
