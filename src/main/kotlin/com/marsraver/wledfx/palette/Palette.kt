package com.marsraver.wledfx.palette

/**
 * Represents a color palette with a name and array of RGB colors.
 * Each color is represented as [R, G, B] with values 0-255.
 */
data class Palette(
    val name: String,
    val colors: Array<IntArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Palette

        if (name != other.name) return false
        if (!colors.contentDeepEquals(other.colors)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + colors.contentDeepHashCode()
        return result
    }
}

