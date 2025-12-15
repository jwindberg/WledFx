package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Hiphotic animation - Nested sine/cosine pattern
 */
class HiphoticAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var custom3: Int = 128 
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Hiphotic"
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        val a = (timeMs / ((custom3 shr 1) + 1).coerceAtLeast(1)).toInt()
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cosArg = (x * paramSpeed / 16 + a / 3) and 0xFF
                val cosVal = cos8_t(cosArg)
                
                val sinArg = (y * paramIntensity / 16 + a / 4) and 0xFF
                val sinVal = sin8_t(sinArg)
                
                val finalArg = (cosVal + sinVal + a) and 0xFF
                val colorIndex = sin8_t(finalArg)
                
                val color = getColorFromPalette(colorIndex)
                setPixelColor(x, y, color)
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

    fun setCustom3(value: Int) {
        this.custom3 = value.coerceIn(0, 255)
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun sin8_t(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val sine = sin(radians)
        return ((sine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    private fun cos8_t(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val cosine = cos(radians)
        return ((cosine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }
}
