package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.abs

/**
 * Rocktaves animation - Same note from each octave is same colour
 * By: Andrew Tuline, adapted for WLED
 * Audio-reactive animation that maps frequency to musical notes
 */
class RocktavesAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var fftMeter: FftMeter? = null
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
        
        fftMeter = FftMeter(bands = 32)
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade to black
        fadeToBlack(16)
        
        val frTemp = fftMeter?.getMajorPeakFrequency() ?: 0.0f
        val bands = fftMeter?.getBands() ?: IntArray(32)
        val totalMagnitude = bands.sum()
        val magnitude = (totalMagnitude / 8.0 * 1.5).toFloat().coerceIn(0.0f, 255.0f)
        
        if (magnitude < 15.0f) {
            return true
        }
        
        var octCount = 0
        var freq = frTemp
        while (freq > 249.0f && octCount < 5) {
            octCount++
            freq = freq / 2.0f
        }
        
        var volTemp = 32.0f + magnitude * 1.5f
        if (magnitude < 15.0f) volTemp = 0.0f
        if (magnitude > 144.0f) volTemp = 255.0f
        val brightness = volTemp.coerceIn(0.0f, 255.0f).toInt()
        
        var noteFreq = freq - 132.0f
        noteFreq = abs(noteFreq * 2.1f)
        val noteIndex = noteFreq.coerceIn(0.0f, 255.0f).toInt()
        
        val bpm = 8 + octCount * 4
        val phaseOffset = octCount * 8
        val beatsinValue = MathUtils.beatsin8(bpm, 0, 255, timeMs, phaseOffset)
        
        val segmentLength = width * height
        var i = MathUtils.map(beatsinValue, 0, 255, 0, segmentLength - 1)
        i = i.coerceIn(0, segmentLength - 1)
        
        val x = i % width
        val y = i / width
        
        val baseColor = colorFromPalette(noteIndex, false, 0)
        val brightnessFactor = brightness / 255.0
        val blendedColor = ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = blendedColor
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

    override fun getName(): String = "Rocktaves"
    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
