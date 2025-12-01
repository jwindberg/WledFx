package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * Metadata about an animation, including its capabilities and defaults.
 */
data class AnimationMetadata(
    val name: String,
    val supportsColor: Boolean = false,
    val supportsPalette: Boolean = false,
    val defaultColor: RgbColor? = null,
    val defaultPaletteName: String? = null,
    val hasFlags: List<String> = emptyList() // e.g., ["cool", "multiCandle"]
)

