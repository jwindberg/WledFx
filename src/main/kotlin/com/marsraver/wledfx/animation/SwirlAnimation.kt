package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.roundToInt

/**
 * Swirl animation - Audio-reactive swirling pixels with mirrored patterns.
 */
class SwirlAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var custom1: Int = 16
    private var fadeAmount: Int = 4
    
    private var loudnessMeter: LoudnessMeter? = null

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        loudnessMeter = LoudnessMeter()
        paramSpeed = 128
        paramIntensity = 64
        custom1 = 16
        fadeAmount = 4
    }

    override fun update(now: Long): Boolean {
        // Get loudness (0-1024) and convert to appropriate ranges
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        val rawVolume = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
        val smoothVolume = loudness / 1024.0f * 255.0f

        if (rawVolume < NOISE_FLOOR) {
            fadeToBlack(15)
            if (custom1 > 0) applyBlur(custom1)
            return true
        }

        fadeToBlack(fadeAmount)
        if (custom1 > 0) applyBlur(custom1)

        val timeMs = now / 1_000_000
        var freq1 = (27 * paramSpeed) / 255
        var freq2 = (41 * paramSpeed) / 255
        if (freq1 < 1) freq1 = 1
        if (freq2 < 1) freq2 = 1

        val i = MathUtils.beatsin8(freq1, BORDER_WIDTH, width - BORDER_WIDTH, timeMs)
        val j = MathUtils.beatsin8(freq2, BORDER_WIDTH, height - BORDER_WIDTH, timeMs)
        val ni = width - 1 - i
        val nj = height - 1 - j

        val baseBrightness = 200
        val audioBoost = rawVolume / 2
        var brightness = (baseBrightness + audioBoost).coerceAtMost(255)
        brightness = brightness.coerceAtLeast(150)

        val paletteOffset = smoothVolume * 4.0f

        val color1 = colorFromPalette((timeMs / 11 + paletteOffset).toInt(), brightness)
        addPixelColor(i, j, color1)

        val color2 = colorFromPalette((timeMs / 13 + paletteOffset).toInt(), brightness)
        addPixelColor(j, i, color2)

        val color3 = colorFromPalette((timeMs / 17 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, nj, color3)

        val color4 = colorFromPalette((timeMs / 29 + paletteOffset).toInt(), brightness)
        addPixelColor(nj, ni, color4)

        val color5 = colorFromPalette((timeMs / 37 + paletteOffset).toInt(), brightness)
        addPixelColor(i, nj, color5)

        val color6 = colorFromPalette((timeMs / 41 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, j, color6)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Swirl"

    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true // Although used as 'paramIntensity' is mapped to something
    // Original mapped intensity to field 'intensity', default 64. 
    // Wait, original file had `private var intensity: Int = 64`.
    // It used `intensity` in logic? No, only defined.
    // Ah, wait. The original code defined 'intensity' but didn't seem to Use it in update().
    // It used 'custom1' (16) and 'fadeAmount' (4).
    // Let's stick to using params if possible or just use custom settings.

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
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
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
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
                    (current.r * (255 - amount) + avgR * amount) / 255,
                    (current.g * (255 - amount) + avgG * amount) / 255,
                    (current.b * (255 - amount) + avgB * amount) / 255
                )
            }
        }
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = temp[x][y]
            }
        }
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

    private fun colorFromPalette(colorIndexValue: Int, brightness: Int): RgbColor {
        val colorIndex = (colorIndexValue % 256 + 256) % 256
        // BaseAnimation helper
        val base = getColorFromPalette(colorIndex)
        val brightnessFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }

    companion object {
        private const val BORDER_WIDTH = 2
        private const val NOISE_FLOOR = 100
    }
}
