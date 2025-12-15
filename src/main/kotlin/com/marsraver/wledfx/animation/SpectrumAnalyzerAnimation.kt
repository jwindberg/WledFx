package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter

/**
 * Spectrum Analyzer Bars animation
 */
class SpectrumAnalyzerAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    
    private lateinit var peakHeights: FloatArray
    private lateinit var peakFallSpeeds: FloatArray
    
    private val gravity = 0.3f 
    private val minBarHeight = 1

    override fun getName(): String = "Spectrum Analyzer"
    override fun isAudioReactive(): Boolean = true
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        peakHeights = FloatArray(width) { 0f }
        peakFallSpeeds = FloatArray(width) { 0f }
        
        fftMeter = FftMeter(bands = width.coerceIn(8, 64))
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        clearDisplay()
        
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(width)
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        
        val brightnessMultiplier = (loudness / 1024.0 * 0.7 + 0.3).coerceIn(0.3, 1.0)
        
        val numBands = bands.size
        
        for (x in 0 until width) {
            val bandIndex = (x * numBands / width).coerceIn(0, numBands - 1)
            val bandValue = bands[bandIndex]
            
            val targetHeight = (bandValue / 255.0 * height).toFloat()
            
            if (targetHeight > peakHeights[x]) {
                peakHeights[x] = targetHeight
                peakFallSpeeds[x] = 0f
            } else {
                peakFallSpeeds[x] += gravity
                peakHeights[x] = (peakHeights[x] - peakFallSpeeds[x]).coerceAtLeast(targetHeight)
            }
            
            val barHeight = peakHeights[x].toInt().coerceIn(minBarHeight, height)
            drawBar(x, barHeight, brightnessMultiplier, timeMs)
        }
        return true
    }
    
    private fun clearDisplay() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = RgbColor.BLACK
            }
        }
    }
    
    private fun drawBar(x: Int, barHeight: Int, brightnessMultiplier: Double, timeMs: Long) {
        if (barHeight <= 0) return
        
        for (y in 0 until barHeight.coerceAtMost(height)) {
            val heightRatio = y.toDouble() / height
            
            // Map height to color index (0-255)
            val colorIndex = (heightRatio * 255).toInt()
            
            // Use BaseAnimation methods. 
            // If palette is set -> getColorFromPalette handles it.
            // If not -> Base uses default (HSV rainbow-ish).
            // But we want a specific gradient if no palette.
            // Let's rely on BaseAnimation's fallback or custom logic.
            // Actually BaseAnimation's fallback is just HSV.
            // That matches typical rainbow behavior for analyzers.
            
            val baseColor = getColorFromPalette(colorIndex)
            val finalColor = ColorUtils.scaleBrightness(baseColor, brightnessMultiplier)
            
            val displayY = height - 1 - y
            pixelColors[x][displayY] = finalColor
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
    }
}
