package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Square Swirl animation - layered sine-driven points with dynamic blur.
 */
class SquareSwirlAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null

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
    }

    override fun update(now: Long): Boolean {
        val timeMs = now / 1_000_000
        
        // Apply blurring - blurAmount oscillates using beatsin8(3, 64, 192) then dim8_raw
        val blurWave = beatsin8(3, 64, 192, timeMs)
        val blurAmount = dim8Raw(blurWave)
        applyBlur(blurAmount)

        // Calculate border width (equivalent to kBorderWidth = 1 in original, but scaled)
        val border = max(1, min(combinedWidth, combinedHeight) / 16)
        val maxX = (combinedWidth - 1 - border).coerceAtLeast(border)
        val maxY = (combinedHeight - 1 - border).coerceAtLeast(border)

        // Use three out-of-sync sine waves
        // In original: all use same range (border to squareWidth-border) since it's a square grid
        // For non-square grids, we use the minimum dimension to maintain the square-like behavior
        val maxCoord = min(maxX, maxY)
        val i = beatsin8(91, border, maxCoord, timeMs)
        val j = beatsin8(109, border, maxCoord, timeMs)
        val k = beatsin8(73, border, maxCoord, timeMs)

        // The color of each point shifts over time, each at a different speed
        val hue1 = ((timeMs / 29) % 256).toInt()
        val hue2 = ((timeMs / 41) % 256).toInt()
        val hue3 = ((timeMs / 73) % 256).toInt()

        // Add colors using additive blending (equivalent to += in FastLED)
        // Note: i, j, k are used as both x and y coordinates in different combinations
        // Original: XY(i,j), XY(j,k), XY(k,i) - all within square bounds
        addPixelColor(i.coerceIn(0, combinedWidth - 1), j.coerceIn(0, combinedHeight - 1), hsvToRgb(hue1, 200, 255))
        addPixelColor(j.coerceIn(0, combinedWidth - 1), k.coerceIn(0, combinedHeight - 1), hsvToRgb(hue2, 200, 255))
        addPixelColor(k.coerceIn(0, combinedWidth - 1), i.coerceIn(0, combinedHeight - 1), hsvToRgb(hue3, 200, 255))

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Square Swirl"

    override fun cleanup() {
        // No resources to release.
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
                var weight = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                            val w = if (dx == 0 && dy == 0) 4 else 1
                            val color = pixelColors[nx][ny]
                            sumR += color.r * w
                            sumG += color.g * w
                            sumB += color.b * w
                            weight += w
                        }
                    }
                }
                val current = pixelColors[x][y]
                val averageR = if (weight == 0) current.r else sumR / weight
                val averageG = if (weight == 0) current.g else sumG / weight
                val averageB = if (weight == 0) current.b else sumB / weight
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + averageR * factor) / 255,
                    (current.g * (255 - factor) + averageG * factor) / 255,
                    (current.b * (255 - factor) + averageB * factor) / 255
                )
            }
        }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    /**
     * beatsin8 - FastLED's beatsin8 function
     * Creates a sine wave that oscillates at the given BPM (beats per minute)
     * @param bpm beats per minute (frequency of oscillation)
     * @param low minimum value
     * @param high maximum value
     * @param timeMs current time in milliseconds
     * @return value oscillating between low and high at the given BPM
     */
    private fun beatsin8(bpm: Int, low: Int, high: Int, timeMs: Long): Int {
        // Convert BPM to frequency (beats per second), then to radians per millisecond
        val beatsPerSecond = bpm / 60.0
        val radiansPerMs = beatsPerSecond * 2.0 * PI / 1000.0
        val phase = timeMs * radiansPerMs
        val sine = sin(phase)
        
        // Map sine wave (-1 to 1) to range (low to high)
        val mid = (low + high) / 2.0
        val range = (high - low) / 2.0
        return (mid + sine * range).roundToInt().coerceIn(min(low, high), max(low, high))
    }

    private fun dim8Raw(value: Int): Int {
        val v = value.coerceIn(0, 255)
        return ((v * v) / 255.0).roundToInt().coerceIn(0, 255)
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

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = value / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(hue, saturation, value)
        }
    }
}

