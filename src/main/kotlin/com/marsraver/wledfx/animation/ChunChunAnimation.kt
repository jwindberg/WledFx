package com.marsraver.wledfx.animation
import com.marsraver.wledfx.palette.Palette

import kotlin.math.*

/**
 * ChunChun animation - Dots (birds) waving around in a sine/pendulum motion.
 * Based on original C++ code from WLED.
 * Little pixel birds flying in a wave pattern.
 */
class ChunChunAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private lateinit var pixelColors: Array<Array<IntArray>>
    private var startTime: Long = 0L

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
        val segmentLength = combinedWidth * combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        startTime = 0L
    }

    override fun update(now: Long): Boolean {
        if (startTime == 0L) {
            startTime = now
        }
        
        val segmentLength = combinedWidth * combinedHeight
        if (segmentLength <= 1) {
            return true
        }
        
        // Fade out for trail effect (matching original: fade_out(254))
        fadeOut(254)
        
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return true
        }
        
        // Convert nanoseconds to milliseconds (strip.now in original is milliseconds)
        val elapsedNs = now - startTime
        val stripNow = elapsedNs / 1_000_000L // Convert to milliseconds
        
        // Counter calculation matching original: strip.now * (6 + (speed >> 4))
        val speed = 20 // Default speed (0-255)
        var counter = (stripNow * (6 + (speed shr 4))).toLong()
        
        // Number of birds: 2 + 1/8 of segment length
        val numBirds = 2 + (segmentLength shr 3)
        
        // Span calculation: (intensity << 8) / numBirds
        val intensity = 128 // Default intensity (0-255)
        val span = ((intensity.toLong() shl 8) / numBirds.coerceAtLeast(1)).toInt()
        
        // Draw each bird
        for (i in 0 until numBirds) {
            // counter -= span (matching original - decreasing counter for phase offset)
            counter -= span
            
            // Calculate bird position: sin16_t(counter) + 0x8000, then map to segment length
            val sineValue = sin16(counter)
            val megumin = (sineValue.toLong() + 0x8000) // Normalize to 0-65535
            val bird = ((megumin * segmentLength) shr 16).toInt()
            val birdIndex = bird.coerceIn(0, segmentLength - 1)
            
            // Convert 1D index to 2D coordinates
            val birdX = birdIndex % combinedWidth
            val birdY = birdIndex / combinedWidth
            
            // Get color from palette: (i * 255) / numBirds, no wrapping
            val colorIndex = (i * 255) / numBirds.coerceAtLeast(1)
            val paletteIndex = (colorIndex % currentPalette.size).coerceIn(0, currentPalette.size - 1)
            val color = currentPalette[paletteIndex]
            
            // Set pixel color
            if (birdY < combinedHeight) {
                pixelColors[birdX][birdY] = color.clone()
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y].clone()
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "ChunChun"

    /**
     * Fade out pixels for trail effect
     * fadeAmount: 0-255, higher = faster fade
     */
    private fun fadeOut(fadeAmount: Int) {
        val factor = (255 - fadeAmount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = (pixelColors[x][y][0] * factor).toInt().coerceIn(0, 255)
                pixelColors[x][y][1] = (pixelColors[x][y][1] * factor).toInt().coerceIn(0, 255)
                pixelColors[x][y][2] = (pixelColors[x][y][2] * factor).toInt().coerceIn(0, 255)
            }
        }
    }

    /**
     * sin16 - 16-bit sine function matching FastLED's sin16_t
     * Returns value from -32768 to 32767
     */
    private fun sin16(angle: Long): Int {
        // Convert angle to 0-65535 range
        val normalizedAngle = (angle % 65536).toInt()
        // Convert to radians (0-65535 maps to 0-2π)
        val radians = normalizedAngle * 2.0 * PI / 65536.0
        val sine = sin(radians)
        // Map from -1..1 to -32768..32767
        return (sine * 32767.0).toInt().coerceIn(-32768, 32767)
    }
}

