package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Juggles animation - Bouncing balls with audio reactivity
 */
class JugglesAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Juggles"
    override fun supportsPalette(): Boolean = true
    override fun isAudioReactive(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        fadeOut(224)
        
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val mySampleAgc = (loudness / 4).coerceIn(0, 255)
        
        val segmentLength = width * height
        val numBalls = (paramIntensity / 32 + 1).coerceAtLeast(1)
        
        for (i in 0 until numBalls) {
            val frequency = paramSpeed / 4 + i * 2
            val position = beatsin16_t(frequency, 0, segmentLength - 1, timeMs)
            
            val x = position % width
            val y = position / width
            
            val colorIndex = ((timeMs / 4 + i * 2) % 256).toInt()
            val color = getColorFromPalette(colorIndex)
            
            val blendedColor = ColorUtils.blend(RgbColor.BLACK, color, mySampleAgc)
            setPixelColor(x, y, blendedColor)
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

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeOut(amount: Int) {
        val factor = amount.coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun beatsin16_t(frequency: Int, min: Int, max: Int, timeMs: Long): Int {
        if (frequency <= 0) return min
        val periodMs = (60000.0 / frequency).toLong()
        val phase = ((timeMs % periodMs) * 2.0 * PI / periodMs).toDouble()
        val sine = sin(phase)
        val normalized = (sine + 1.0) / 2.0
        return (min + (normalized * (max - min))).roundToInt()
    }
}
