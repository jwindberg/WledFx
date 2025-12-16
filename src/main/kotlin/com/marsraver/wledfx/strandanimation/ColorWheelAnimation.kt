package com.marsraver.wledfx.strandanimation

import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.wled.model.Strand
import com.marsraver.wledfx.wled.model.StrandAnimation

/**
 * Color Wheel animation: rolls through a rainbow color wheel across the strand.
 * Each pixel gets a different hue, and the pattern advances each frame,
 * creating a rolling rainbow effect.
 * Uses Processing-like pattern: init() sets up state, draw() updates each frame.
 */
class ColorWheelAnimation : StrandAnimation {
    private var hue = 0.0
    private var hueStep = 0.0

    override fun init(strand: Strand) {
        hueStep = 360.0 / strand.length + 0.01
    }

    override fun draw(strand: Strand) {
        for (position in 0 until strand.length) {
            // Use ColorUtils.hsvToRgb (Float version) - returns RgbColor directly
            val rgbColor = ColorUtils.hsvToRgb(hue.toFloat(), 1.0f, 1.0f)
            strand.set(position, rgbColor)
            hue = (hue + hueStep) % 360.0
        }
    }
}

