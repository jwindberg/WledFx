package com.marsraver.wledfx.animation

import com.marsraver.wledfx.palette.Palette
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color

/**
 * Interface for LED animations across multiple devices.
 */
interface LedAnimation {

    /**
     * Initialize the animation state.
     *
     * @param combinedWidth total width of the combined grid
     * @param combinedHeight total height of the combined grid
     */
    fun init(combinedWidth: Int, combinedHeight: Int)

    /**
     * Update the animation for the current frame.
     *
     * @param now current time in nanoseconds
     * @return true if the animation should continue, false to stop
     */
    fun update(now: Long): Boolean

    /**
     * Get the color for a specific pixel in the combined grid.
     *
     * @param x X coordinate in the combined grid (0 to combinedWidth-1)
     * @param y Y coordinate in the combined grid (0 to combinedHeight-1)
     * @return RGB color values as [R, G, B] each in range 0-255
     */
    fun getPixelColor(x: Int, y: Int): IntArray

    /**
     * Get the name of the animation for display in UI.
     *
     * @return animation name
     */
    fun getName(): String

    /**
     * Check if this animation supports color selection.
     *
     * @return true if the animation uses a primary color
     */
    fun supportsColor(): Boolean = false

    /**
     * Check if this animation supports palette selection.
     *
     * @return true if the animation uses a color palette
     */
    fun supportsPalette(): Boolean = false

    /**
     * Set the primary color for the animation (if supported).
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     */
    fun setColor(r: Int, g: Int, b: Int) {
        // Default implementation does nothing
    }

    /**
     * Get the current color of the animation (if supported).
     *
     * @return RGB color values as [R, G, B] each in range 0-255, or null if not supported
     */
    fun getColor(): IntArray? {
        // Default implementation returns null
        return null
    }

    /**
     * Get the current palette of the animation (if supported).
     *
     * @return Palette object, or null if not supported
     */
    fun getPalette(): Palette? {
        // Default implementation returns null
        return null
    }

    /**
     * Set the color palette for the animation (if supported).
     *
     * @param palette Palette object containing the color array
     */
    fun setPalette(palette: Palette) {
        // Default implementation does nothing
    }

    /**
     * Get the default palette name for this animation.
     * If null, the global default palette will be used.
     *
     * @return the default palette name, or null to use the global default
     */
    fun getDefaultPaletteName(): String? {
        // Default implementation returns null, meaning use global default
        return null
    }

    /**
     * Draw the animation on the canvas (optional, for custom rendering).
     *
     * @param gc graphics context for drawing
     * @param combinedWidth total width of the combined grid
     * @param combinedHeight total height of the combined grid
     * @param cellSize size of each cell
     */
    fun draw(gc: GraphicsContext, combinedWidth: Int, combinedHeight: Int, cellSize: Double) {
        val spacing = cellSize * 1.2
        val radius = cellSize / 2.0
        val startX = spacing / 2
        val startY = spacing / 2

        for (y in 0 until combinedHeight) {
            for (x in 0 until combinedWidth) {
                val rgb = getPixelColor(x, y)
                val pixelX = x * spacing + startX
                val pixelY = y * spacing + startY

                gc.fill = Color.rgb(rgb[0], rgb[1], rgb[2])
                gc.fillOval(pixelX - radius, pixelY - radius, radius * 2, radius * 2)
            }
        }
    }
}

