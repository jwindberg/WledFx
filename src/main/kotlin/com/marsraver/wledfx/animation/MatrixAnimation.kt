package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.random.Random
import kotlin.math.min

/**
 * Matrix animation - Falling code rain effect
 * By: Jeremy Williams, adapted by Andrew Tuline & improved by merkisoft, ewowi, and softhack007
 */
class MatrixAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var fallingCodes: Array<Array<Boolean>>  // Track which pixels are falling codes
    
    private var custom1: Int = 128  // Trail size (fade)
    private var useCustomColors: Boolean = false  // check1
    
    private var step: Long = 0L
    private var startTimeNs: Long = 0L
    private val random = Random.Default

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        fallingCodes = Array(width) { Array(height) { false } }
        step = 0L
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Calculate fade: map(custom1, 0, 255, 50, 250)
        val fade = MathUtils.map(custom1, 0, 255, 50, 250)
        
        // Calculate speed: (256-speed) >> map(min(rows, 150), 0, 150, 0, 3)
        val minRows = min(height, 150)
        val shiftAmount = MathUtils.map(minRows, 0, 150, 0, 3)
        val speedValue = (256 - paramSpeed) shr shiftAmount
        
        // Define colors
        val spawnColor: RgbColor
        val trailColor: RgbColor
        
        if (useCustomColors) {
            // Use custom colors (would need to get from SEGCOLOR(0) and SEGCOLOR(1))
            // For now, use default green colors
            spawnColor = RgbColor(175, 255, 175)
            trailColor = RgbColor(27, 130, 39)
        } else {
            spawnColor = RgbColor(175, 255, 175)  // Light green
            trailColor = RgbColor(27, 130, 39)    // Dark green
        }
        
        var emptyScreen = true
        
        // Check if enough time has passed
        if (timeMs - step >= speedValue) {
            step = timeMs
            
            // Fade all pixels
            fadeToBlackBy(fade)
            
            // Move pixels one row down
            // Process from bottom to top
            for (row in height - 1 downTo 0) {
                for (col in 0 until width) {
                    if (fallingCodes[col][row]) {
                        // This is a falling code - create trail
                        setPixelColor(col, row, trailColor)
                        
                        // Clear current position
                        fallingCodes[col][row] = false
                        
                        // Move down if not at bottom
                        if (row < height - 1) {
                            setPixelColor(col, row + 1, spawnColor)
                            fallingCodes[col][row + 1] = true
                            emptyScreen = false
                        }
                    }
                }
            }
            
            // Spawn new falling code at top
            // if (random8() <= intensity || emptyScreen)
            val shouldSpawn = random.nextInt(256) <= paramIntensity || emptyScreen
            if (shouldSpawn) {
                val spawnX = random.nextInt(width)
                setPixelColor(spawnX, 0, spawnColor)
                fallingCodes[spawnX][0] = true
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

    override fun getName(): String = "Matrix"
    override fun supportsIntensity(): Boolean = true

    fun setCustom1(value: Int) { this.custom1 = value.coerceIn(0, 255) }
    fun setUseCustomColors(enabled: Boolean) { this.useCustomColors = enabled }
    fun getUseCustomColors(): Boolean { return useCustomColors }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeToBlackBy(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
}
