package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import java.util.Random

/**
 * Space Ships animation - 2D spaceships by stepko, adapted by Blaz Kristan
 * Spaceships move in random directions with blur and fade effects
 */
class SpaceShipsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var smear: Boolean = false  // check1 - smear effect for larger grids
    
    private var direction: Int = 0  // 0-7 representing 8 directions
    private var nextDirectionChange: Long = 0L  // Next time bucket when direction should change
    private val random = Random()
    private var startTimeNs: Long = 0L

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
        startTimeNs = System.nanoTime()
        nextDirectionChange = 0L
        direction = random.nextInt(8)
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Change direction every ~4 seconds (tb = now >> 12, every ~4s)
        // SEGENV.step in original stores the next time bucket when change should occur
        val tb = timeMs shr 12
        if (tb > nextDirectionChange) {
            // Randomly adjust direction: dir += random8(3) - 1
            var newDir = direction + (random.nextInt(3) - 1)  // -1, 0, or +1
            if (newDir > 7) newDir = 0
            else if (newDir < 0) newDir = 7
            direction = newDir
            // Set next change time: SEGENV.step = tb + hw_random8(4)
            nextDirectionChange = tb + random.nextInt(4)
        }

        // Fade to black based on speed: map(0, 255, 248, 16)
        val fadeAmount = map(speed, 0, 255, 248, 16)
        fadeToBlack(fadeAmount)

        // Move the entire segment in the current direction
        move(direction, 1)

        val timeMsForBeatsin = timeMs
        
        // Create 8 spaceships
        for (i in 0 until 8) {
            val x = beatsin8(12 + i, 2, combinedWidth - 3, timeMsForBeatsin)
            val y = beatsin8(15 + i, 2, combinedHeight - 3, timeMsForBeatsin)
            val colorIndex = beatsin8(12 + i, 0, 255, timeMsForBeatsin)
            val color = colorFromPalette(colorIndex, 255)
            
            addPixelColor(x, y, color)
            
            // Smear effect for larger grids (>24)
            if (smear && (combinedWidth > 24 || combinedHeight > 24)) {
                addPixelColor(x + 1, y, color)
                addPixelColor(x - 1, y, color)
                addPixelColor(x, y + 1, color)
                addPixelColor(x, y - 1, color)
            }
        }

        // Apply blur based on intensity
        val blurAmount = intensity shr 3
        applyBlur(blurAmount)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Space Ships"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Move the entire pixel buffer in the specified direction
     * Directions: 0=Right, 1=Down-Right, 2=Down, 3=Down-Left, 4=Left, 5=Up-Left, 6=Up, 7=Up-Right
     */
    private fun move(dir: Int, amount: Int) {
        if (amount <= 0) return
        
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val (dx, dy) = when (dir) {
                    0 -> Pair(amount, 0)      // Right
                    1 -> Pair(amount, amount) // Down-Right
                    2 -> Pair(0, amount)     // Down
                    3 -> Pair(-amount, amount) // Down-Left
                    4 -> Pair(-amount, 0)    // Left
                    5 -> Pair(-amount, -amount) // Up-Left
                    6 -> Pair(0, -amount)    // Up
                    7 -> Pair(amount, -amount) // Up-Right
                    else -> Pair(0, 0)
                }
                
                val srcX = x - dx
                val srcY = y - dy
                
                if (srcX in 0 until combinedWidth && srcY in 0 until combinedHeight) {
                    temp[x][y] = pixelColors[srcX][srcY]
                } else {
                    temp[x][y] = RgbColor.BLACK
                }
            }
        }
        
        pixelColors = temp
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                // 3x3 blur kernel
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                            val color = pixelColors[nx][ny]
                            sumR += color.r
                            sumG += color.g
                            sumB += color.b
                            count++
                        }
                    }
                }
                
                val current = pixelColors[x][y]
                val avgR = if (count > 0) sumR / count else current.r
                val avgG = if (count > 0) sumG / count else current.g
                val avgB = if (count > 0) sumB / count else current.b
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + avgR * factor) / 255,
                    (current.g * (255 - factor) + avgG * factor) / 255,
                    (current.b * (255 - factor) + avgB * factor) / 255
                )
            }
        }
        
        pixelColors = temp
    }

    /**
     * beatsin8 - FastLED's beatsin8 function
     * Creates a sine wave that oscillates at the given BPM (beats per minute)
     */
    private fun beatsin8(bpm: Int, low: Int, high: Int, timeMs: Long): Int {
        val beatsPerSecond = bpm / 60.0
        val radiansPerMs = beatsPerSecond * 2.0 * PI / 1000.0
        val phase = timeMs * radiansPerMs
        val sine = sin(phase)
        
        val mid = (low + high) / 2.0
        val range = (high - low) / 2.0
        return (mid + sine * range).roundToInt().coerceIn(min(low, high), max(low, high))
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + rgb.r).coerceAtMost(255),
                (current.g + rgb.g).coerceAtMost(255),
                (current.b + rgb.b).coerceAtMost(255)
            )
        }
    }

    private fun colorFromPalette(colorIndex: Int, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = (colorIndex % 256) * currentPalette.size / 256
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(colorIndex, 255, brightness)
        }
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).roundToInt()
    }
}

