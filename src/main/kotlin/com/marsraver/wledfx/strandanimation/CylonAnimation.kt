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

    override fun init(strand: Strand) {
        // Initialize: reset position and direction
        position = 0
        direction = 1
    }

    override fun draw(strand: Strand) {
        // Draw: clear and set the current position
        strand.clear()
        strand.set(position, redPixel)

        // Update position for next frame
        position += direction
        
        // Reverse direction at boundaries (Kotlin idiom: coerceIn for clamping)
        if (position >= strand.length - 1 || position <= 0) {
            direction = -direction
            position = position.coerceIn(0, strand.length - 1)
        }
    }
}

