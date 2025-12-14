package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

/**
 * PlasmaRotoZoom - Rotating and zooming plasma effect
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 6226
 */
class PlasmaRotoZoomAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var plasma: IntArray
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var speed: Int = 128
    private var intensity: Int = 128  // Scale
    private var useAltMode: Boolean = false
    private var angle: Float = 0f

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? = currentPalette

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        plasma = IntArray(combinedWidth * combinedHeight)
        startTimeNs = System.nanoTime()
        angle = 0f
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val ms = ((now - startTimeNs) / 1_000_000 / 15).toInt()
        
        // Generate plasma
        for (j in 0 until combinedHeight) {
            val index = j * combinedWidth
            for (i in 0 until combinedWidth) {
                plasma[index + i] = if (useAltMode) {
                    ((i * 4) xor (j * 4)) + ms / 6
                } else {
                    inoise8(i * 40, j * 40, ms)
                }
            }
        }
        
        // Rotozoom
        val f = (sinT(angle / 2) + ((128 - intensity) / 128.0f) + 1.1f) / 1.5f
        val kosinus = cosT(angle) * f
        val sinus = sinT(angle) * f
        
        for (i in 0 until combinedWidth) {
            val u1 = i * kosinus
            val v1 = i * sinus
            for (j in 0 until combinedHeight) {
                val u = abs((u1 - j * sinus).toInt()) % combinedWidth
                val v = abs((v1 + j * kosinus).toInt()) % combinedHeight
                val paletteIndex = plasma[v * combinedWidth + u]
                pixelColors[i][j] = colorFromPalette(paletteIndex, true, 255)
            }
        }
        
        // Update rotation
        angle -= 0.03f + (speed - 128) * 0.0002f
        if (angle < -6283.18530718f) angle += 6283.18530718f  // 1000*2*PI
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "PlasmaRotoZoom"

    override fun isAudioReactive(): Boolean = false

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {}
    
    private fun inoise8(x: Int, y: Int, z: Int): Int {
        val hash = ((x * 2654435761L + y * 2246822519L + z * 3266489917L) and 0xFFFFFFFF).toInt()
        return (hash and 0xFF)
    }
    
    private fun sinT(angle: Float): Float {
        return sin(angle.toDouble()).toFloat()
    }
    
    private fun cosT(angle: Float): Float {
        return cos(angle.toDouble()).toFloat()
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
