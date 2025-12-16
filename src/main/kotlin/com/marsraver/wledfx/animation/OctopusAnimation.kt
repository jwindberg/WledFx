package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.max

/**
 * Octopus animation - Radial pattern with tentacle-like effects
 * By: Stepko and Sutaburosu, adapted for WLED by @blazoncek
 */
class OctopusAnimation : BaseAnimation() {

    private data class MapEntry(
        var angle: Int,   // 0-255
        var radius: Int   // 0-255
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var rMap: Array<Array<MapEntry>>
    
    private var custom1: Int = 128  // Offset X
    private var custom2: Int = 128  // Offset Y
    private var custom3: Int = 128  // Legs
    
    private var step: Long = 0L
    private var startTimeNs: Long = 0L
    private var lastCols: Int = 0
    private var lastRows: Int = 0
    private var lastOffsX: Int = -1
    private var lastOffsY: Int = -1

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        rMap = Array(width) { Array(height) { MapEntry(0, 0) } }
        step = 0L
        startTimeNs = System.nanoTime()
        lastCols = 0
        lastRows = 0
        lastOffsX = -1
        lastOffsY = -1
        paramSpeed = 128
        
        initializeMap()
    }

    override fun update(now: Long): Boolean {
        // Re-init if dimensions or offsets changed
        if (lastCols != width || lastRows != height || 
            lastOffsX != custom1 || lastOffsY != custom2) {
            
            if (width != lastCols || height != lastRows) {
                 rMap = Array(width) { Array(height) { MapEntry(0, 0) } }
                 pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
            }
            initializeMap()
            lastCols = width
            lastRows = height
            lastOffsX = custom1
            lastOffsY = custom2
        }
        
        // Update step: step += speed / 32 + 1 (1-4 range)
        step += (paramSpeed / 32 + 1).toLong()
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val mapEntry = rMap[x][y]
                val angle = mapEntry.angle
                val radius = mapEntry.radius
                
                // Calculate intensity using nested sin8
                val stepInt = step.toInt()
                val innerArg = ((angle * 4 - radius) / 4 + stepInt / 2) and 0xFF
                val innerSin = MathUtils.sin8(innerArg)
                val outerArg = (innerSin + radius - stepInt + angle * (custom3 / 4 + 1)) and 0xFF
                var intensity = MathUtils.sin8(outerArg)
                
                // Add non-linearity: map((intensity*intensity) & 0xFFFF, 0, 65535, 0, 255)
                val squared = (intensity * intensity) and 0xFFFF
                intensity = MathUtils.map(squared, 0, 65535, 0, 255)
                
                // Get color from palette
                val colorIndex = ((stepInt / 2 - radius) % 256 + 256) % 256
                val color = colorFromPalette(colorIndex, false, intensity)
                
                setPixelColor(x, y, color)
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Octopus"

    fun setCustom1(value: Int) { this.custom1 = value.coerceIn(0, 255) }
    fun setCustom2(value: Int) { this.custom2 = value.coerceIn(0, 255) }
    fun setCustom3(value: Int) { this.custom3 = value.coerceIn(0, 255) }

    private fun initializeMap() {
        val mapp = 180.0f / max(width, height)
        
        val centerX = (width / 2) + ((custom1 - 128) * width) / 255
        val centerY = (height / 2) + ((custom2 - 128) * height) / 255
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val dx = (x - centerX).toFloat()
                val dy = (y - centerY).toFloat()
                
                val angleRad = atan2_t(dy, dx)
                val angle = (40.7436f * angleRad).roundToInt() and 0xFF
                
                val radius = (sqrt(dx * dx + dy * dy) * mapp).roundToInt().coerceIn(0, 255)
                
                rMap[x][y] = MapEntry(angle, radius)
            }
        }
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun atan2_t(y: Float, x: Float): Float {
        val angle = atan2(y, x)
        return ((angle + PI) / (2.0 * PI)).toFloat()
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = brightness.coerceIn(0, 255) / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
