package com.marsraver.wledfx.strandanimation

import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.wled.model.Strand
import com.marsraver.wledfx.wled.model.StrandAnimation

class FftAnimation : StrandAnimation {

    private var fftMeter: FftMeter? = null
    private var hue = 0.0
    private var hueStep = 360.0 / 16.01

    override fun init(strand: Strand) {
        fftMeter = FftMeter(bands = 16)
    }

    override fun draw(strand: Strand) {
        val bands = fftMeter?.getNormalizedBands() ?: return

        strand.clear()

        // Map each of the 16 FFT bands to a row width (0-16)
        // Band 0 (low freq) at bottom row, band 15 (high freq) at top row
        for (column in 0 until 16) {
            val bandValue = bands.getOrElse(column) { 0 }
            // Map from 0-255 range to 0-16 range
            val height = ((bandValue * 16) / 255).coerceIn(0, 16)
            val rgbColor = ColorUtils.hsvToRgb(hue.toFloat(), 1.0f, 1.0f)
            setColumn(strand, column, height, rgbColor)
            hue = (hue + hueStep) % 360.0
        }
    }

    fun setColumn(strand: Strand, column: Int, height: Int, color: RgbColor) {

        // Fill from left (column 0) to the specified width
        for (x in 0 until height) {
            setPixel(strand, x, column, color)
        }
    }

    fun setPixel(strand: Strand, x: Int, y: Int, color: RgbColor) {
        strand.set((15 - x) * 16 + (15 - y), color)
    }
}