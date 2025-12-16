package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * ChunChun animation - Dots (birds) waving around.
 */
class ChunChunAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTime: Long = 0L

    override fun getName(): String = "ChunChun"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) startTime = now
        val segmentLength = width * height
        if (segmentLength <= 1) return true
        
        fadeOut(254)
        
        val palette = paramPalette?.colors
        if (palette == null || palette.isEmpty()) return true
        
        val elapsedNs = now - startTime
        val stripNow = elapsedNs / 1_000_000L
        
        val speedMultiplier = 1 + (paramSpeed / 16)
        var counter = (stripNow * speedMultiplier).toLong()
        
        val numBirds = 2 + (segmentLength shr 3)
        val span = ((paramIntensity.toLong() shl 8) / numBirds.coerceAtLeast(1)).toInt()
        
        for (i in 0 until numBirds) {
            counter -= span
            
            val sineValue = MathUtils.sin16(counter.toInt())
            val megumin = (sineValue.toLong() + 0x8000)
            val bird = ((megumin * segmentLength) shr 16).toInt()
            val birdIndex = bird.coerceIn(0, segmentLength - 1)
            
            // Map 1D to 2D
            // Assumes standard mapping or just x/y wrap for "flying"
            val birdX = birdIndex % width
            val birdY = birdIndex / width
            
            val colorIndex = (i * 255) / numBirds.coerceAtLeast(1)
            val paletteIndex = (colorIndex % palette.size).coerceIn(0, palette.size - 1)
            val color = palette[paletteIndex]
            
            if (birdY < height) {
                pixelColors[birdX][birdY] = color
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
    
    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
}
