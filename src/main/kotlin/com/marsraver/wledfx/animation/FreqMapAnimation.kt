package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import kotlin.math.log10

/**
 * FreqMap - Maps FFT Major Peak frequency to LED position
 */
class FreqMapAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    private var fftMeter: FftMeter? = null
    
    private val MAX_FREQ_LOG10 = 4.04238f

    override fun getName(): String = "FreqMap"
    override fun isAudioReactive(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        fftMeter = FftMeter(bands = 16)
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        var fftMajorPeak = fftMeter?.getMajorPeakFrequency() ?: 1.0f
        val myMagnitude = if (fftMajorPeak > 100) 128.0f else 64.0f
        
        if (fftMajorPeak < 1) fftMajorPeak = 1.0f
        
        val fadeoutDelay = (256 - paramSpeed) / 32
        if (fadeoutDelay <= 1 || ((now / 1_000_000) % fadeoutDelay == 0L)) {
            fadeOut(paramSpeed)
        }
        
        var locn = ((log10(fftMajorPeak) - 1.78f) * width / (MAX_FREQ_LOG10 - 1.78f)).toInt()
        locn = locn.coerceIn(0, width - 1)
        
        var pixCol = ((log10(fftMajorPeak) - 1.78f) * 255.0f / (MAX_FREQ_LOG10 - 1.78f)).toInt()
        if (fftMajorPeak < 61.0f) pixCol = 0
        
        val bright = myMagnitude.toInt().coerceIn(0, 255)
        
        val paletteColor = getColorFromPalette(paramIntensity + pixCol)
        val blendColor = ColorUtils.scaleBrightness(paletteColor, bright / 255.0)
        
        for (y in 0 until height) {
            pixelColors[locn][y] = blendColor
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
        fftMeter?.stop()
        fftMeter = null
    }
    
    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
}
