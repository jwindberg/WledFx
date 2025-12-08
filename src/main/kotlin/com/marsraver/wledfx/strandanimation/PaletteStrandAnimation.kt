package com.marsraver.wledfx.strandanimation

import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.color.Palettes
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.wled.model.Strand
import com.marsraver.wledfx.wled.model.StrandAnimation

/**
 * Palette-based animation: displays colors from a palette across the strand.
 * Colors cycle through the palette with a scrolling effect.
 * Uses Processing-like pattern: init() sets up state, draw() updates each frame.
 */
class PaletteStrandAnimation : StrandAnimation {
    private var currentPalette: Palette = Palette.getRandom()
    private var offset = 0.0 // Scrolling offset for smooth color transitions

    override fun init(strand: Strand) {
        offset = 0.0
        // Use a random palette
        currentPalette = Palette.getRandom()
    }

    /**
     * Set the palette to use for this animation.
     */
    fun setPalette(palette: Palette) {
        currentPalette = palette
    }

    /**
     * Get the current palette.
     */
    fun getPalette(): Palette = currentPalette

    override fun draw(strand: Strand) {
        strand.clear()

        if (currentPalette.colors.isEmpty()) {
            return
        }

        // Map each LED position to a color in the palette
        for (i in 0 until strand.length) {
            // Calculate position in palette (0.0 to 1.0) with scrolling offset
            val position = (i.toDouble() / strand.length + offset) % 1.0
            val color = currentPalette.getColorAt(position)
            strand.set(i, color)
        }

        // Increment offset for scrolling effect
        offset += 0.01
        if (offset >= 1.0) {
            offset -= 1.0
        }
    }
}

