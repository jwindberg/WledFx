package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

import com.marsraver.wledfx.audio.FftMeter
import kotlin.math.max

/**
 * GEQ (Graphic Equalizer) animation - 2D frequency band visualization.
 */
class GeqAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var numBands: Int = 16
    private var centerBin: Int = 0
    private var colorBars: Boolean = false
    private val peakColor = RgbColor.WHITE
    private var noiseFloor: Int = 20
    
    private lateinit var previousBarHeight: IntArray
    private var lastRippleTime: Long = 0
    private var callCount: Long = 0
    
    private var fftMeter: FftMeter? = null

    override fun getName(): String = "GEQ"
    override fun isAudioReactive(): Boolean = true
    override fun supportsPalette(): Boolean = true // Implicitly via colorFromPalette logic

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        previousBarHeight = IntArray(width)
        lastRippleTime = 0
        callCount = 0
        fftMeter = FftMeter(bands = 16)
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        callCount++

        val spectrumSnapshot = fftMeter?.getNormalizedBands() ?: IntArray(16)

        var rippleTime = false
        var rippleInterval = 256 - paramIntensity
        if (rippleInterval < 1) rippleInterval = 1
        if (now - lastRippleTime >= rippleInterval * 1_000_000L) {
            lastRippleTime = now
            rippleTime = true
        }

        val fadeoutDelay = (256 - paramSpeed) / 64
        if (fadeoutDelay <= 1 || callCount % fadeoutDelay.toLong() == 0L) {
            fadeToBlack(paramSpeed)
        }

        for (x in 0 until width) {
            var band = mapValue(x, 0, width, 0, numBands)
            if (numBands < 16) {
                val startBin = (centerBin - numBands / 2).coerceIn(0, 15 - numBands + 1)
                band = if (numBands <= 1) {
                    centerBin
                } else {
                    mapValue(band, 0, numBands - 1, startBin, startBin + numBands - 1)
                }
            }
            band = band.coerceIn(0, 15)

            var colorIndex = band * 17
            val bandValue = spectrumSnapshot.getOrElse(band) { 0 }

            val effectiveValue = max(0, bandValue - noiseFloor)
            var barHeight = mapValue(effectiveValue, 0, 255 - noiseFloor, 0, height)
            barHeight = barHeight.coerceIn(0, height)

            if (barHeight > previousBarHeight[x]) {
                previousBarHeight[x] = barHeight
            }

            for (y in 0 until barHeight) {
                if (colorBars) {
                    colorIndex = mapValue(y, 0, height - 1, 0, 255)
                }
                // Use built-in palette support
                val ledColor = getColorFromPalette(colorIndex) // Replaces local colorFromPalette hsv logic with proper palette support
                setPixelColor(x, height - 1 - y, ledColor)
            }

            if (previousBarHeight[x] > 0) {
                setPixelColor(x, height - previousBarHeight[x], peakColor)
            }

            if (rippleTime && previousBarHeight[x] > 0) {
                previousBarHeight[x]--
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

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun fadeToBlack(fadeAmount: Int) {
        val amount = fadeAmount.coerceIn(0, 255)
        val factor = (255 - amount) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = rgb
        }
    }
}
