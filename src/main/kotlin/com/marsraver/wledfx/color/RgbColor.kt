package com.marsraver.wledfx.color

/**
 * Represents an RGB color with red, green, and blue components.
 * Each component is in the range 0-255.
 */
data class RgbColor(
    val r: Int,
    val g: Int,
    val b: Int
) {
    init {
        require(r in 0..255) { "Red component must be 0-255, got $r" }
        require(g in 0..255) { "Green component must be 0-255, got $g" }
        require(b in 0..255) { "Blue component must be 0-255, got $b" }
    }

    /**
     * Convert to IntArray for backward compatibility or JavaFX Color conversion.
     */
    fun toIntArray(): IntArray = intArrayOf(r, g, b)

    /**
     * Convert RGB color to HSV color.
     * @return HSV color representation
     */
    fun toHsv(): HsvColor {
        return ColorUtils.rgbToHsv(this)
    }

    /**
     * Create from IntArray (for backward compatibility).
     */
    companion object {
        fun fromIntArray(array: IntArray): RgbColor {
            require(array.size >= 3) { "IntArray must have at least 3 elements" }
            return RgbColor(
                array[0].coerceIn(0, 255),
                array[1].coerceIn(0, 255),
                array[2].coerceIn(0, 255)
            )
        }

        val BLACK = RgbColor(0, 0, 0)
        val WHITE = RgbColor(255, 255, 255)
        val RED = RgbColor(255, 0, 0)
        val GREEN = RgbColor(0, 255, 0)
        val BLUE = RgbColor(0, 0, 255)
    }
}

