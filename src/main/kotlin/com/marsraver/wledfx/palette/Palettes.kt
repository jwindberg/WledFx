package com.marsraver.wledfx.palette

/**
 * Container for all available color palettes.
 * Defines the default palette for animations that don't specify their own.
 */
object Palettes {
    
    /**
     * The default palette name used for animations that don't provide their own startup palette.
     */
    const val DEFAULT_PALETTE_NAME = "Default"
    
    /**
     * All available palettes mapped by name.
     */
    val all: Map<String, Palette> = mapOf(
        DEFAULT_PALETTE_NAME to Palette(
            name = DEFAULT_PALETTE_NAME,
            colors = arrayOf(
                intArrayOf(255, 0, 0),   // Red
                intArrayOf(255, 127, 0), // Orange
                intArrayOf(255, 255, 0), // Yellow
                intArrayOf(0, 255, 0),   // Green
                intArrayOf(0, 255, 255), // Cyan
                intArrayOf(0, 0, 255),   // Blue
                intArrayOf(127, 0, 255), // Purple
                intArrayOf(255, 0, 255)  // Magenta
            )
        ),
        "Rainbow" to Palette(
            name = "Rainbow",
            colors = arrayOf(
                intArrayOf(255, 0, 0),   // Red
                intArrayOf(255, 127, 0), // Orange
                intArrayOf(255, 255, 0), // Yellow
                intArrayOf(127, 255, 0), // Yellow-Green
                intArrayOf(0, 255, 0),   // Green
                intArrayOf(0, 255, 127), // Green-Cyan
                intArrayOf(0, 255, 255), // Cyan
                intArrayOf(0, 127, 255), // Cyan-Blue
                intArrayOf(0, 0, 255),   // Blue
                intArrayOf(127, 0, 255), // Blue-Purple
                intArrayOf(255, 0, 255), // Magenta
                intArrayOf(255, 0, 127)  // Magenta-Red
            )
        ),
        "Party" to Palette(
            name = "Party",
            colors = arrayOf(
                intArrayOf(255, 0, 0),   // Red
                intArrayOf(255, 0, 255), // Magenta
                intArrayOf(0, 0, 255),   // Blue
                intArrayOf(0, 255, 255), // Cyan
                intArrayOf(0, 255, 0),   // Green
                intArrayOf(255, 255, 0), // Yellow
                intArrayOf(255, 127, 0), // Orange
                intArrayOf(255, 0, 0)    // Red
            )
        ),
        "Ocean" to Palette(
            name = "Ocean",
            colors = arrayOf(
                intArrayOf(0, 0, 128),   // Dark Blue
                intArrayOf(0, 0, 255),   // Blue
                intArrayOf(0, 127, 255), // Light Blue
                intArrayOf(0, 255, 255), // Cyan
                intArrayOf(64, 224, 208), // Turquoise
                intArrayOf(0, 255, 255), // Cyan
                intArrayOf(0, 127, 255), // Light Blue
                intArrayOf(0, 0, 255)    // Blue
            )
        ),
        "Forest" to Palette(
            name = "Forest",
            colors = arrayOf(
                intArrayOf(0, 64, 0),    // Dark Green
                intArrayOf(0, 128, 0),  // Green
                intArrayOf(0, 255, 0),   // Bright Green
                intArrayOf(127, 255, 0), // Yellow-Green
                intArrayOf(255, 255, 0), // Yellow
                intArrayOf(127, 255, 0), // Yellow-Green
                intArrayOf(0, 255, 0),   // Bright Green
                intArrayOf(0, 128, 0)   // Green
            )
        ),
        "Lava" to Palette(
            name = "Lava",
            colors = arrayOf(
                intArrayOf(0, 0, 0),    // Black
                intArrayOf(64, 0, 0),   // Dark Red
                intArrayOf(128, 0, 0),  // Red
                intArrayOf(255, 0, 0),  // Bright Red
                intArrayOf(255, 64, 0), // Orange-Red
                intArrayOf(255, 127, 0), // Orange
                intArrayOf(255, 64, 0), // Orange-Red
                intArrayOf(255, 0, 0)   // Bright Red
            )
        ),
        "Cloud" to Palette(
            name = "Cloud",
            colors = arrayOf(
                intArrayOf(64, 64, 64),  // Dark Gray
                intArrayOf(128, 128, 128), // Gray
                intArrayOf(192, 192, 192), // Light Gray
                intArrayOf(255, 255, 255), // White
                intArrayOf(192, 192, 192), // Light Gray
                intArrayOf(128, 128, 128), // Gray
                intArrayOf(64, 64, 64),  // Dark Gray
                intArrayOf(32, 32, 32)   // Very Dark Gray
            )
        ),
        "Sunset" to Palette(
            name = "Sunset",
            colors = arrayOf(
                intArrayOf(0, 0, 0),    // Black
                intArrayOf(64, 0, 64),  // Dark Purple
                intArrayOf(128, 0, 128), // Purple
                intArrayOf(255, 0, 255), // Magenta
                intArrayOf(255, 64, 0), // Orange-Red
                intArrayOf(255, 127, 0), // Orange
                intArrayOf(255, 191, 0), // Yellow-Orange
                intArrayOf(255, 255, 0)  // Yellow
            )
        ),
        "Heat" to Palette(
            name = "Heat",
            colors = arrayOf(
                intArrayOf(0, 0, 0),    // Black
                intArrayOf(64, 0, 0),   // Dark Red
                intArrayOf(128, 0, 0),  // Red
                intArrayOf(255, 0, 0),  // Bright Red
                intArrayOf(255, 64, 0), // Orange-Red
                intArrayOf(255, 127, 0), // Orange
                intArrayOf(255, 191, 0), // Yellow-Orange
                intArrayOf(255, 255, 255) // White
            )
        ),
        "Ice" to Palette(
            name = "Ice",
            colors = arrayOf(
                intArrayOf(0, 0, 0),    // Black
                intArrayOf(0, 0, 64),   // Dark Blue
                intArrayOf(0, 0, 128),  // Blue
                intArrayOf(0, 0, 255),  // Bright Blue
                intArrayOf(0, 64, 255), // Light Blue
                intArrayOf(0, 127, 255), // Cyan-Blue
                intArrayOf(0, 255, 255), // Cyan
                intArrayOf(255, 255, 255) // White
            )
        )
    )
    
    /**
     * Get a palette by name.
     * @param name the palette name
     * @return the Palette if found, null otherwise
     */
    fun get(name: String): Palette? = all[name]
    
    /**
     * Get the default palette.
     * @return the default Palette
     */
    fun getDefault(): Palette = all[DEFAULT_PALETTE_NAME]!!
    
    /**
     * Get all palette names in sorted order.
     * @return sorted list of palette names
     */
    fun getNames(): List<String> = all.keys.sorted()
    
    /**
     * Get the colors array for a palette by name.
     * This is a convenience method for backward compatibility.
     * @param name the palette name
     * @return the colors array if found, null otherwise
     */
    fun getColors(name: String): Array<IntArray>? = all[name]?.colors
}

