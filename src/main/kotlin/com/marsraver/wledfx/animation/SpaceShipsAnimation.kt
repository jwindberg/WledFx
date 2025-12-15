package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Space Ships animation - 2D spaceships by stepko, adapted by Blaz Kristan
 * Spaceships move in random directions with blur and fade effects
 */
class SpaceShipsAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var smear: Boolean = false  // check1 - smear effect for larger grids
    
    private var direction: Int = 0  // 0-7 representing 8 directions
    private var nextDirectionChange: Long = 0L  // Next time bucket when direction should change
    private val random = Random.Default
    private var startTimeNs: Long = 0L

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        nextDirectionChange = 0L
        direction = random.nextInt(8)
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        val tb = timeMs shr 12
        if (tb > nextDirectionChange) {
            var newDir = direction + (random.nextInt(3) - 1)
            if (newDir > 7) newDir = 0
            else if (newDir < 0) newDir = 7
            direction = newDir
            nextDirectionChange = tb + random.nextInt(4)
        }

        // Fade to black based on speed
        val fadeAmount = MathUtils.map(paramSpeed, 0, 255, 248, 16)
        fadeToBlack(fadeAmount)

        // Move buffer
        move(direction, 1)
        
        // Speed scaling for movement frequency?
        // Original uses timeMs directly in beatsin8.
        // We can leave it or scale it.
        val timeMsForBeatsin = timeMs
        
        for (i in 0 until 8) {
            val x = MathUtils.beatsin8(12 + i, 2, width - 3, timeMsForBeatsin)
            val y = MathUtils.beatsin8(15 + i, 2, height - 3, timeMsForBeatsin)
            val colorIndex = MathUtils.beatsin8(12 + i, 0, 255, timeMsForBeatsin)
            // Use BaseAnimation palette
            val color = getColorFromPalette(colorIndex)
            
            addPixelColor(x, y, color)
            
            if (smear && (width > 24 || height > 24)) {
                addPixelColor(x + 1, y, color)
                addPixelColor(x - 1, y, color)
                addPixelColor(x, y + 1, color)
                addPixelColor(x, y - 1, color)
            }
        }

        val blurAmount = paramIntensity shr 3
        applyBlur(blurAmount)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Space Ships"
    override fun supportsIntensity(): Boolean = true
    
    fun setSmear(enabled: Boolean) { smear = enabled }

    private fun move(dir: Int, amount: Int) {
        if (amount <= 0) return
        
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        
        // Optimize: double buffering or just simpler copy logic
        // Original creates temp buffer.
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val (dx, dy) = when (dir) {
                    0 -> Pair(amount, 0)
                    1 -> Pair(amount, amount)
                    2 -> Pair(0, amount)
                    3 -> Pair(-amount, amount)
                    4 -> Pair(-amount, 0)
                    5 -> Pair(-amount, -amount)
                    6 -> Pair(0, -amount)
                    7 -> Pair(amount, -amount)
                    else -> Pair(0, 0)
                }
                
                val srcX = x - dx
                val srcY = y - dy
                
                if (srcX in 0 until width && srcY in 0 until height) {
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
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        
        // This is a Box Blur (3x3).
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
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

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + rgb.r).coerceAtMost(255),
                (current.g + rgb.g).coerceAtMost(255),
                (current.b + rgb.b).coerceAtMost(255)
            )
        }
    }
}
