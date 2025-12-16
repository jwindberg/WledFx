package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * Plasmoid animation - Plasma-like effect with audio reactivity
 */
class PlasmoidAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var thisPhase: Int = 0
    private var thatPhase: Int = 0
    
    private var loudnessMeter: LoudnessMeter? = null
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Plasmoid"
    override fun isAudioReactive(): Boolean = true
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        thisPhase = 0
        thatPhase = 0
        startTimeNs = System.nanoTime()
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade to black by 32
        fadeToBlackBy(32)
        
        // Update phases
        thisPhase += MathUtils.beatsin8(6, -4, 4, timeMs)
        thatPhase += MathUtils.beatsin8(7, -4, 4, timeMs)
        
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val volumeScaled = (loudness / 1024.0f * 255.0f).coerceIn(0.0f, 255.0f)
        val audioModulation = (volumeScaled * paramIntensity / 255.0f).toInt().coerceIn(0, 255)
        
        val segmentLength = width * height
        
        for (i in 0 until segmentLength) {
            val x = i % width
            val y = i / width
            
            // cubicwave8(((i*(1 + (3*speed/32)))+thisPhase) & 0xFF)/2
            val wave1Arg = ((i * (1 + (3 * paramSpeed / 32))) + thisPhase) and 0xFF
            val thisbright = MathUtils.cubicwave8(wave1Arg) / 2
            
            // cos8(((i*(97 +(5*speed/32)))+thatPhase) & 0xFF)/2
            val wave2Arg = ((i * (97 + (5 * paramSpeed / 32))) + thatPhase) and 0xFF
            val brightness = thisbright + (MathUtils.cos8(wave2Arg) / 2)
            
            val colorIndex = brightness.coerceIn(0, 255)
            
            // Audio reactivity
            val finalBrightness = if (audioModulation < 10) {
                (brightness * 0.1f).toInt().coerceIn(0, 255)
            } else {
                val baseBrightness = (brightness * (0.3f + audioModulation / 255.0f * 0.7f)).toInt()
                val audioBoost = audioModulation / 2 
                (baseBrightness + audioBoost).coerceIn(0, 255)
            }
            
            val color = getColorFromPaletteWithBrightness(colorIndex, true, 0)
            val blendedColor = ColorUtils.blend(RgbColor.BLACK, color, finalBrightness)
            addPixelColor(x, y, blendedColor)
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

    private fun fadeToBlackBy(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + color.r).coerceAtMost(255),
                (current.g + color.g).coerceAtMost(255),
                (current.b + color.b).coerceAtMost(255)
            )
        }
    }
    
    private fun getColorFromPaletteWithBrightness(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        // Base class helper doesn't support wrap/brightness args perfectly yet, so customizing slightly or using base and scaling
        // Base class: getColorFromPalette(index) -> returns RgbColor
        // If we want wrapping, index % 256 is handled by us before calling, or by palette logic?
        // Base logic maps index 0-255 to Palette 0-SIZE.
        // So just calling getColorFromPalette(index) works for basic wrap if index is 0-255.
        // But here we might pass large index?
        // The original code handled wrapping. Base code handles wrapping too.
        
        val base = getColorFromPalette(index)
        if (brightness > 0 && brightness < 255) {
            return ColorUtils.scaleBrightness(base, brightness / 255.0)
        }
        return base
    }
}
