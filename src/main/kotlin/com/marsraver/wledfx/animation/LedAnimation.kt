package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.Palette
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
     * @return RGB color
     */
    fun getPixelColor(x: Int, y: Int): RgbColor

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
     * @param color RGB color
     */
    fun setColor(color: RgbColor) {
        // Default implementation does nothing
    }

    /**
     * Get the current color of the animation (if supported).
     *
     * @return RGB color, or null if not supported
     */
    fun getColor(): RgbColor? {
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
     * Check if this animation supports speed control.
     *
     * @return true if the animation has a speed parameter
     */
    fun supportsSpeed(): Boolean = false

    /**
     * Set the speed for the animation (if supported).
     * Speed range is 0-255, where higher values typically mean faster.
     *
     * @param speed speed value (0-255)
     */
    fun setSpeed(speed: Int) {
        // Default implementation does nothing
    }

    fun getSpeed(): Int? {
        // Default implementation returns null
        return null
    }

    /**
     * Check if this animation supports intensity control.
     *
     * @return true if the animation has an intensity parameter
     */
    fun supportsIntensity(): Boolean = false

    /**
     * Set the intensity for the animation (if supported).
     * Intensity range is 0-255.
     *
     * @param intensity intensity value (0-255)
     */
    fun setIntensity(intensity: Int) {
        // Default implementation does nothing
    }

    /**
     * Get the current intensity of the animation (if supported).
     *
     * @return intensity value (0-255), or null if not supported
     */
    fun getIntensity(): Int? {
        // Default implementation returns null
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

                gc.fill = Color.rgb(rgb.r, rgb.g, rgb.b)
                gc.fillOval(pixelX - radius, pixelY - radius, radius * 2, radius * 2)
            }
        }
    }

    /**
     * Clean up resources when the animation is stopped.
     * Default implementation does nothing. Override to cancel coroutines,
     * close resources, or perform other cleanup tasks.
     */
    fun cleanup() {
        // Default implementation does nothing
    }

    /**
     * Check if this animation supports multi-mode (e.g., multiple candles).
     *
     * @return true if the animation supports multi-mode
     */
    fun supportsMultiMode(): Boolean = false

    /**
     * Set multi-mode for the animation (if supported).
     *
     * @param enabled true to enable multi-mode, false to disable
     */
    fun setMultiMode(enabled: Boolean) {
        // Default implementation does nothing
    }

    /**
     * Check if this animation supports cat mode (e.g., TwinkleFox).
     *
     * @return true if the animation supports cat mode
     */
    fun supportsCatMode(): Boolean = false

    /**
     * Set cat mode for the animation (if supported).
     *
     * @param enabled true to enable cat mode, false to disable
     */
    fun setCatMode(enabled: Boolean) {
        // Default implementation does nothing
    }

    /**
     * Check if this animation supports text input (e.g., ScrollingText).
     *
     * @return true if the animation supports text input
     */
    fun supportsTextInput(): Boolean = false

    /**
     * Set the text for the animation (if supported).
     *
     * @param text the text to display
     */
    fun setText(text: String?) {
        // Default implementation does nothing
    }

    /**
     * Check if this animation supports speed factor (e.g., ScrollingText).
     *
     * @return true if the animation supports speed factor
     */
    fun supportsSpeedFactor(): Boolean = false

    /**
     * Set the speed factor for the animation (if supported).
     *
     * @param factor speed factor (typically 0.0 to 1.0)
     */
    fun setSpeedFactor(factor: Double) {
        // Default implementation does nothing
    }

    /**
     * Check if this animation supports peak detection (e.g., Puddles).
     *
     * @return true if the animation supports peak detection
     */
    fun supportsPeakDetect(): Boolean = false

    /**
     * Set peak detection for the animation (if supported).
     *
     * @param enabled true to enable peak detection, false to disable
     */
    fun setPeakDetect(enabled: Boolean) {
        // Default implementation does nothing
    }

    /**
     * Check if this animation is primarily a 1D effect (strip effect).
     * Used for filtering in the UI.
     * 
     * @return true if this is a 1D effect being mapped to 2D
     */
    fun is1D(): Boolean = false

    /**
     * Check if this animation is primarily a 2D effect (matrix effect).
     * Used for filtering in the UI.
     * 
     * @return true if this is a 2D effect
     */
    fun is2D(): Boolean = true

    /**
     * Indicates whether this animation reacts to audio input (loudness or FFT).
     *
     * Default is false; audio-reactive animations should override and return true.
     */
    fun isAudioReactive(): Boolean = false
}

