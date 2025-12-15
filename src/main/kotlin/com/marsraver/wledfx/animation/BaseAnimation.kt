package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.Palette

/**
 * Strategy for mapping 2D (x,y) coordinates to 1D linear indices.
 */
interface PixelMapper {
    fun getPixelIndex(x: Int, y: Int, width: Int): Int
}

/**
 * Standard Serpentine Mapping (Zig-Zag).
 * Rows alternate direction:
 * Even rows: Left -> Right
 * Odd rows: Right -> Left
 */
object SerpentineMapper : PixelMapper {
    override fun getPixelIndex(x: Int, y: Int, width: Int): Int {
        return if (y % 2 == 0) {
            y * width + x
        } else {
            y * width + (width - 1 - x)
        }
    }
}

/**
 * Linear / Row-Major Mapping.
 * All rows go Left -> Right.
 */
object RowMajorMapper : PixelMapper {
    override fun getPixelIndex(x: Int, y: Int, width: Int): Int {
        return y * width + x
    }
}

/**
 * Base class for all animations.
 */
abstract class BaseAnimation : LedAnimation {

    protected var width: Int = 0
    protected var height: Int = 0
    protected var pixelCount: Int = 0
    
    // Pixel Mapper Strategy - Default to Serpentine
    protected var mapper: PixelMapper = SerpentineMapper
    
    // Properties renamed to avoid clash
    protected var paramSpeed: Int = 128
    protected var paramIntensity: Int = 128 
    protected var paramColor: RgbColor = RgbColor.WHITE
    protected var paramPalette: Palette? = null

    override fun init(width: Int, height: Int) {
        this.width = width
        this.height = height
        this.pixelCount = width * height
        onInit()
    }
    
    protected open fun onInit() {}

    // --- Standard Hooks ---
    
    // Allow external configuration of mapper
    fun setPixelMapper(newMapper: PixelMapper) {
        this.mapper = newMapper
    }
    
    override fun supportsSpeed(): Boolean = true
    override fun setSpeed(speed: Int) { this.paramSpeed = speed }
    override fun getSpeed(): Int = paramSpeed
    
    override fun supportsIntensity(): Boolean = true
    override fun setIntensity(intensity: Int) { this.paramIntensity = intensity }
    override fun getIntensity(): Int = paramIntensity
    
    override fun supportsPalette(): Boolean = true
    override fun setPalette(palette: Palette) { this.paramPalette = palette }
    override fun getPalette(): Palette? = paramPalette
    
    override fun supportsColor(): Boolean = true
    override fun setColor(color: RgbColor) { this.paramColor = color }
    override fun getColor(): RgbColor? = paramColor

    // --- Helper Methods ---

    /**
     * Convert (x,y) to a linear index using the current Mapper.
     */
    protected fun getPixelIndex(x: Int, y: Int): Int {
        return mapper.getPixelIndex(x, y, width)
    }

    /**
     * Helper to get a color from the current palette (safe).
     */
    protected fun getColorFromPalette(index: Int): RgbColor {
        val pal = paramPalette?.colors
        if (pal != null && pal.isNotEmpty()) {
            val pIdx = (index * pal.size) / 256
            return pal[pIdx.coerceIn(0, pal.size - 1)]
        }
        return com.marsraver.wledfx.color.ColorUtils.hsvToRgb(index, 255, 255)
    }
}
