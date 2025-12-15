package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter

/**
 * Funky Plank animation - 2D scrolling FFT visualization.
 */
class FunkyPlankAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var custom1: Int = 255 
    private var noiseThreshold: Int = 10 
    
    private var fftMeter: FftMeter? = null
    private var lastSecondHand: Int = -1
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Funky Plank"
    override fun isAudioReactive(): Boolean = true
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        lastSecondHand = -1
        startTimeNs = System.nanoTime()
        fftMeter = FftMeter(bands = 16)
        paramSpeed = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val numBands = map(custom1, 0, 255, 1, 16).coerceIn(1, 16)

        val micros = (now - startTimeNs) / 1_000L
        val speedDivisor = (256 - paramSpeed).coerceAtLeast(1)
        val secondHand = ((micros / speedDivisor / 500 + 1) % 64).toInt()

        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            val fftBands = fftMeter?.getNormalizedBands() ?: IntArray(16)

            for (x in 0 until width) {
                val bandIndex = x % 16
                val fftValue = fftBands.getOrElse(bandIndex) { 0 }
                
                val rgb = if (fftValue < noiseThreshold) {
                    RgbColor.BLACK
                } else {
                    val colorIndex = x % numBands
                    // Map to 0-255 for colorFromPalette
                    val paletteIdx = (colorIndex * 255 / numBands.coerceAtLeast(1))
                    val baseColor = getColorFromPalette(paletteIdx)
                    
                    val brightness = map(fftValue, noiseThreshold, 255, 10, 255).coerceIn(10, 255)
                    ColorUtils.scaleBrightness(baseColor, brightness / 255.0)
                }
                pixelColors[x][0] = rgb
            }
        }

        for (i in height - 1 downTo 1) {
            for (j in 0 until width) {
                pixelColors[j][i] = pixelColors[j][i - 1]
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

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).toInt().coerceIn(toLow, toHigh)
    }
}
