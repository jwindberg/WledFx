package com.marsraver.wledfx.strandanimation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.wled.model.Strand
import com.marsraver.wledfx.wled.model.StrandAnimation

/**
 * Christmas animation: alternating red and green sections with a twinkling effect.
 * Uses Processing-like pattern: init() sets up state, draw() updates each frame.
 */
class ChristmasStrandAnimation : StrandAnimation {
    private val redColor = RgbColor.RED
    private val greenColor = RgbColor.GREEN
    private val whiteColor = RgbColor.WHITE
    private val sectionWidth = 8 // Number of LEDs per color section
    private var frameCount = 0

    override fun init(strand: Strand) {
        frameCount = 0
    }

    override fun draw(strand: Strand) {
        strand.clear()
        
        for (i in 0 until strand.length) {
            val sectionIndex = i / sectionWidth
            val positionInSection = i % sectionWidth
            
            // Alternate between red and green sections
            val baseColor = if (sectionIndex % 2 == 0) redColor else greenColor
            
            // Add twinkling effect: every 4th LED in each section twinkles white
            // The twinkling pattern shifts based on frame count
            val twinkleOffset = (frameCount / 3) % 4 // Slow down twinkling
            val shouldTwinkle = (positionInSection + twinkleOffset) % 4 == 0
            
            val color = if (shouldTwinkle) whiteColor else baseColor
            strand.set(i, color)
        }
        
        frameCount++
    }
}

