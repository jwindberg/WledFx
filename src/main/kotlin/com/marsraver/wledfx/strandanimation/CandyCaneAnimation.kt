package com.marsraver.wledfx.strandanimation

import com.marsraver.wledfx.audio.LoudnessMeter
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.wled.model.Strand
import com.marsraver.wledfx.wled.model.StrandAnimation

/**
 * Candy Cane animation: scrolling red and white stripes.
 * Uses Processing-like pattern: init() sets up state, draw() updates each frame.
 */
class CandyCaneAnimation : StrandAnimation {
    private val redPixel = RgbColor.RED
    private val whitePixel = RgbColor.WHITE
    private val stripeWidth = 4 // Number of LEDs per stripe
    private var offset = 0 // Scrolling offset
    private var loudnessMeter = LoudnessMeter()

    override fun init(strand: Strand) {
        offset = 0
    }

    override fun draw(strand: Strand) {
        // Clear the strand first
        strand.clear()
        
        // Get current loudness (0-1024, normalized based on recent history)
        val currentLoudness = loudnessMeter.getCurrentLoudness()
        val maxLoudness = 1024
        
        // Calculate how many LEDs to fill based on loudness
        val ledsToFill = ((currentLoudness.toDouble() / maxLoudness) * strand.length).toInt()
        
        // Fill the strand with candy cane pattern up to the loudness level
        for (i in 0 until ledsToFill.coerceAtMost(strand.length)) {
            val effectivePosition = (i + offset) % (stripeWidth * 2)
            if (effectivePosition < stripeWidth) {
                strand.set(i, redPixel)
            } else {
                strand.set(i, whitePixel)
            }
        }
        
        // Update offset for scrolling effect
        offset = (offset + 1) % (stripeWidth * 2) // Scroll one LED per frame
    }
}


