package com.marsraver.wledfx.color

/**
 * Represents a color palette with a name and array of RGB colors.
 */
data class Palette(
    val name: String,
    val colors: Array<RgbColor>
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

    /**
     * Get color at index, wrapping if needed.
     */
    fun getColor(index: Int): RgbColor {
        if (colors.isEmpty()) return RgbColor.BLACK
        val idx = (index % colors.size + colors.size) % colors.size
        return colors[idx]
    }

    /**
     * Get color at normalized position (0.0-1.0).
     */
    fun getColorAt(position: Double): RgbColor {
        if (colors.isEmpty()) return RgbColor.BLACK
        val pos = position.coerceIn(0.0, 1.0)
        val index = (pos * colors.size).toInt().coerceIn(0, colors.size - 1)
        return colors[index]
    }

    companion object {
        /**
         * Returns a randomly selected palette from all available palettes.
         * @return a randomly selected Palette
         */
        fun getRandom(): Palette {
            val allPalettes = Palettes.all.values.toList()
            return if (allPalettes.isNotEmpty()) {
                allPalettes.random()
            } else {
                Palettes.getDefault()
            }
        }
    }
}

