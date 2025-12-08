package com.marsraver.wledfx.strandanimation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.wled.model.Strand
import com.marsraver.wledfx.wled.model.StrandAnimation

/**
 * Cylon-style animation: a red dot moving back and forth.
 * Uses Processing-like pattern: init() sets up state, draw() updates each frame.
 */
class CylonAnimation : StrandAnimation {
    // State stored as instance properties
    private val redPixel = RgbColor.RED
    private var position = 0
    private var direction = 1
    private var width = 5

    override fun init(strand: Strand) {
        // Initialize: reset position and direction
        position = 0
        direction = 1
    }

    override fun draw(strand: Strand) {
        // Draw: clear and set a group of pixels at the current position
        strand.clear()
        
        // Draw width number of pixels starting at position
        for (i in 0 until width) {
            val pixelIndex = position + i
            if (pixelIndex < strand.length) {
                strand.set(pixelIndex, redPixel)
            }
        }

        // Update position for next frame
        position += direction
        
        // Reverse direction at boundaries
        // Check if the group would go out of bounds
        if (position + width - 1 >= strand.length || position <= 0) {
            direction = -direction
            // Clamp position to keep the group within bounds
            position = position.coerceIn(0, strand.length - width)
        }
    }
}

