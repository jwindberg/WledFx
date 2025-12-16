package com.marsraver.wledfx.color

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
                RgbColor(255, 0, 0),   // Red
                RgbColor(255, 127, 0), // Orange
                RgbColor(255, 255, 0), // Yellow
                RgbColor(0, 255, 0),   // Green
                RgbColor(0, 255, 255), // Cyan
                RgbColor(0, 0, 255),   // Blue
                RgbColor(127, 0, 255), // Purple
                RgbColor(255, 0, 255)  // Magenta
            )
        ),
        "Rainbow" to Palette(
            name = "Rainbow",
            colors = arrayOf(
                RgbColor(255, 0, 0),   // Red
                RgbColor(255, 127, 0), // Orange
                RgbColor(255, 255, 0), // Yellow
                RgbColor(127, 255, 0), // Yellow-Green
                RgbColor(0, 255, 0),   // Green
                RgbColor(0, 255, 127), // Green-Cyan
                RgbColor(0, 255, 255), // Cyan
                RgbColor(0, 127, 255), // Cyan-Blue
                RgbColor(0, 0, 255),   // Blue
                RgbColor(127, 0, 255), // Blue-Purple
                RgbColor(255, 0, 255), // Magenta
                RgbColor(255, 0, 127)  // Magenta-Red
            )
        ),
        "Party" to Palette(
            name = "Party",
            colors = arrayOf(
                RgbColor(255, 0, 0),   // Red
                RgbColor(255, 0, 255), // Magenta
                RgbColor(0, 0, 255),   // Blue
                RgbColor(0, 255, 255), // Cyan
                RgbColor(0, 255, 0),   // Green
                RgbColor(255, 255, 0), // Yellow
                RgbColor(255, 127, 0), // Orange
                RgbColor(255, 0, 0)    // Red
            )
        ),
        "Ocean" to Palette(
            name = "Ocean",
            colors = arrayOf(
                RgbColor(0, 0, 128),   // Dark Blue
                RgbColor(0, 0, 255),   // Blue
                RgbColor(0, 127, 255), // Light Blue
                RgbColor(0, 255, 255), // Cyan
                RgbColor(64, 224, 208), // Turquoise
                RgbColor(0, 255, 255), // Cyan
                RgbColor(0, 127, 255), // Light Blue
                RgbColor(0, 0, 255)    // Blue
            )
        ),
        "Forest" to Palette(
            name = "Forest",
            colors = arrayOf(
                RgbColor(0, 64, 0),    // Dark Green
                RgbColor(0, 128, 0),  // Green
                RgbColor(0, 255, 0),   // Bright Green
                RgbColor(127, 255, 0), // Yellow-Green
                RgbColor(255, 255, 0), // Yellow
                RgbColor(127, 255, 0), // Yellow-Green
                RgbColor(0, 255, 0),   // Bright Green
                RgbColor(0, 128, 0)   // Green
            )
        ),
        "Lava" to Palette(
            name = "Lava",
            colors = arrayOf(
                RgbColor(0, 0, 0),    // Black
                RgbColor(64, 0, 0),   // Dark Red
                RgbColor(128, 0, 0),  // Red
                RgbColor(255, 0, 0),  // Bright Red
                RgbColor(255, 64, 0), // Orange-Red
                RgbColor(255, 127, 0), // Orange
                RgbColor(255, 64, 0), // Orange-Red
                RgbColor(255, 0, 0)   // Bright Red
            )
        ),
        "Cloud" to Palette(
            name = "Cloud",
            colors = arrayOf(
                RgbColor(64, 64, 64),  // Dark Gray
                RgbColor(128, 128, 128), // Gray
                RgbColor(192, 192, 192), // Light Gray
                RgbColor(255, 255, 255), // White
                RgbColor(192, 192, 192), // Light Gray
                RgbColor(128, 128, 128), // Gray
                RgbColor(64, 64, 64),  // Dark Gray
                RgbColor(32, 32, 32)   // Very Dark Gray
            )
        ),
        "Sunset" to Palette(
            name = "Sunset",
            colors = arrayOf(
                RgbColor(0, 0, 0),    // Black
                RgbColor(64, 0, 64),  // Dark Purple
                RgbColor(128, 0, 128), // Purple
                RgbColor(255, 0, 255), // Magenta
                RgbColor(255, 64, 0), // Orange-Red
                RgbColor(255, 127, 0), // Orange
                RgbColor(255, 191, 0), // Yellow-Orange
                RgbColor(255, 255, 0)  // Yellow
            )
        ),
        "Heat" to Palette(
            name = "Heat",
            colors = arrayOf(
                RgbColor(0, 0, 0),    // Black
                RgbColor(64, 0, 0),   // Dark Red
                RgbColor(128, 0, 0),  // Red
                RgbColor(255, 0, 0),  // Bright Red
                RgbColor(255, 64, 0), // Orange-Red
                RgbColor(255, 127, 0), // Orange
                RgbColor(255, 191, 0), // Yellow-Orange
                RgbColor(255, 255, 255) // White
            )
        ),
        "Ice" to Palette(
            name = "Ice",
            colors = arrayOf(
                RgbColor(0, 0, 0),    // Black
                RgbColor(0, 0, 64),   // Dark Blue
                RgbColor(0, 0, 128),  // Blue
                RgbColor(0, 0, 255),  // Bright Blue
                RgbColor(0, 64, 255), // Light Blue
                RgbColor(0, 127, 255), // Cyan-Blue
                RgbColor(0, 255, 255), // Cyan
                RgbColor(255, 255, 255) // White
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
    fun getColors(name: String): Array<RgbColor>? = all[name]?.colors
}

