package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Octopus animation - Radial pattern with tentacle-like effects
 * By: Stepko and Sutaburosu, adapted for WLED by @blazoncek
 */
class OctopusAnimation : LedAnimation {

    private data class MapEntry(
        var angle: Int,   // 0-255
        var radius: Int   // 0-255
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var rMap: Array<Array<MapEntry>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var custom1: Int = 128  // Offset X
    private var custom2: Int = 128  // Offset Y
    private var custom3: Int = 128  // Legs
    
    private var step: Long = 0L
    private var startTimeNs: Long = 0L
    private var lastCols: Int = 0
    private var lastRows: Int = 0
    private var lastOffsX: Int = -1
    private var lastOffsY: Int = -1

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        rMap = Array(combinedWidth) { Array(combinedHeight) { MapEntry(0, 0) } }
        step = 0L
        startTimeNs = System.nanoTime()
        lastCols = 0
        lastRows = 0
        lastOffsX = -1
        lastOffsY = -1
        
        // Initialize map
        initializeMap()
    }

    override fun update(now: Long): Boolean {
        // Re-init if dimensions or offsets changed
        if (lastCols != combinedWidth || lastRows != combinedHeight || 
            lastOffsX != custom1 || lastOffsY != custom2) {
            initializeMap()
            lastCols = combinedWidth
            lastRows = combinedHeight
            lastOffsX = custom1
            lastOffsY = custom2
        }
        
        // Update step: step += speed / 32 + 1 (1-4 range)
        step += (speed / 32 + 1).toLong()
        
        val mapp = 180.0f / max(combinedWidth, combinedHeight)
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val mapEntry = rMap[x][y]
                val angle = mapEntry.angle
                val radius = mapEntry.radius
                
                // Calculate intensity using nested sin8_t
                // sin8_t(sin8_t((angle * 4 - radius) / 4 + step/2) + radius - step + angle * (custom3/4+1))
                val stepInt = step.toInt()
                val innerArg = ((angle * 4 - radius) / 4 + stepInt / 2) and 0xFF
                val innerSin = sin8_t(innerArg)
                val outerArg = (innerSin + radius - stepInt + angle * (custom3 / 4 + 1)) and 0xFF
                var intensity = sin8_t(outerArg)
                
                // Add non-linearity: map((intensity*intensity) & 0xFFFF, 0, 65535, 0, 255)
                val squared = (intensity * intensity) and 0xFFFF
                intensity = map(squared, 0, 65535, 0, 255)
                
                // Get color from palette: step / 2 - radius
                val colorIndex = ((stepInt / 2 - radius) % 256 + 256) % 256
                val color = colorFromPalette(colorIndex, false, intensity)
                
                setPixelColor(x, y, color)
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Octopus"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set custom1 (Offset X)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
    }

    /**
     * Set custom2 (Offset Y)
     */
    fun setCustom2(value: Int) {
        this.custom2 = value.coerceIn(0, 255)
    }

    /**
     * Set custom3 (Legs)
     */
    fun setCustom3(value: Int) {
        this.custom3 = value.coerceIn(0, 255)
    }

    private fun initializeMap() {
        val mapp = 180.0f / max(combinedWidth, combinedHeight)
        
        // Calculate center with offset
        // C_X = (cols / 2) + ((custom1 - 128)*cols)/255
        // C_Y = (rows / 2) + ((custom2 - 128)*rows)/255
        val centerX = (combinedWidth / 2) + ((custom1 - 128) * combinedWidth) / 255
        val centerY = (combinedHeight / 2) + ((custom2 - 128) * combinedHeight) / 255
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val dx = (x - centerX).toFloat()
                val dy = (y - centerY).toFloat()
                
                // angle = int(40.7436f * atan2_t(dy, dx))
                val angleRad = atan2_t(dy, dx)
                val angle = (40.7436f * angleRad).roundToInt() and 0xFF
                
                // radius = sqrtf(dx * dx + dy * dy) * mapp
                val radius = (sqrt(dx * dx + dy * dy) * mapp).roundToInt().coerceIn(0, 255)
                
                rMap[x][y] = MapEntry(angle, radius)
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    /**
     * atan2_t - atan2 function returning 0-255 range (normalized to 0-2π)
     */
    private fun atan2_t(y: Float, x: Float): Float {
        val angle = atan2(y, x)
        // Normalize from [-π, π] to [0, 1] then scale to 0-255
        return ((angle + PI) / (2.0 * PI)).toFloat()
    }

    /**
     * sin8_t - Sine function for 8-bit input (0-255 maps to 0-2π)
     */
    private fun sin8_t(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val sine = sin(radians)
        return ((sine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).roundToInt()
    }

    /**
     * Get color from palette
     */
    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = brightness.coerceIn(0, 255) / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, brightness.coerceIn(0, 255))
        }
    }
}

