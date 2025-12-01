package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.abs

/**
 * DNA animation - dual sine waves forming a helix pattern.
 */
class DnaAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var tempBuffer: Array<Array<RgbColor>>
    private var lastUpdateNs: Long = 0L
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
        tempBuffer = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth == 0 || combinedHeight == 0) return true

        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }

        fadeToBlack(FADE_AMOUNT)

        val timeMs = now / 1_000_000
        val rows = combinedHeight
        val center = (combinedWidth - 1) / 2.0
        val amplitude = (combinedWidth - 2) / 2.0

        for (row in 0 until rows) {
            val angle = timeMs / 240.0 + row * ROW_PHASE
            val wobble = sin(angle * 0.5) * amplitude * 0.15

            val x1 = (center + sin(angle) * amplitude * 0.55 + wobble).roundToInt().coerceIn(0, combinedWidth - 1)
            val x2 = (center + sin(angle + Math.PI) * amplitude * 0.55 - wobble).roundToInt().coerceIn(0, combinedWidth - 1)

            val hueBase = (timeMs / 25 + row * 14).toInt() and 0xFF
            val hueOpposite = (hueBase + 128) and 0xFF

            val brightness1 = (160 + sin(angle * 1.3) * 95).roundToInt().coerceIn(60, 255)
            val brightness2 = (160 + sin((angle + Math.PI) * 1.3) * 95).roundToInt().coerceIn(60, 255)

            val colorA = hsvToRgb(hueBase, 200, brightness1)
            val colorB = hsvToRgb(hueOpposite, 200, brightness2)

            drawLine(x1, x2, row, blendColors(colorA, colorB), addDot = false)
            drawPixel(x1, row, colorA)
            drawPixel(x2, row, colorB)
        }

        blur2d(18)
        mirrorVertical()

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "DNA"

    override fun cleanup() {
        // nothing to release
    }

    private fun drawPixel(x: Int, y: Int, rgb: RgbColor) {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) return
        val current = pixelColors[x][y]
        pixelColors[x][y] = RgbColor(
            (current.r + rgb.r).coerceAtMost(255),
            (current.g + rgb.g).coerceAtMost(255),
            (current.b + rgb.b).coerceAtMost(255)
        )
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        val current = pixelColors[x][y]
        pixelColors[x][y] = RgbColor(
            (current.r + rgb.r).coerceAtMost(255),
            (current.g + rgb.g).coerceAtMost(255),
            (current.b + rgb.b).coerceAtMost(255)
        )
    }

    private fun blendColors(a: RgbColor, b: RgbColor): RgbColor {
        return RgbColor(
            (a.r + b.r) / 2,
            (a.g + b.g) / 2,
            (a.b + b.b) / 2
        )
    }

    private fun drawLine(x0: Int, x1: Int, y: Int, rgb: RgbColor, addDot: Boolean) {
        val start = x0.coerceIn(0, combinedWidth - 1)
        val end = x1.coerceIn(0, combinedWidth - 1)
        val steps = abs(end - start) + 1
        for (step in 0 until steps) {
            val t = step / (steps - 1.0).coerceAtLeast(1.0)
            val x = ((1 - t) * start + t * end).roundToInt().coerceIn(0, combinedWidth - 1)
            val scale = (160 + t * 95).roundToInt().coerceIn(0, 255)
            val scaled = ColorUtils.scaleBrightness(rgb, scale / 255.0)
            addPixelColor(x, y, scaled)
        }
        if (addDot) {
            addPixelColor(start, y, RgbColor.WHITE)
            addPixelColor(end, y, RgbColor(30, 144, 255))
        }
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun blur2d(amount: Int) {
        if (amount <= 0) return
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
                val blurredR = if (weight > 0) sumR / weight else current.r
                val blurredG = if (weight > 0) sumG / weight else current.g
                val blurredB = if (weight > 0) sumB / weight else current.b
                tempBuffer[x][y] = RgbColor(
                    (current.r * (255 - amount) + blurredR * amount) / 255,
                    (current.g * (255 - amount) + blurredG * amount) / 255,
                    (current.b * (255 - amount) + blurredB * amount) / 255
                )
            }
        }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = tempBuffer[x][y]
            }
        }
    }

    private fun mirrorVertical() {
        val half = combinedHeight / 2
        for (y in 0 until half) {
            val opposite = combinedHeight - 1 - y
            for (x in 0 until combinedWidth) {
                pixelColors[x][opposite] = pixelColors[x][y]
            }
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

    private fun beatsin(speed: Int, amplitude: Int, phaseOffset: Int): Int {
        val angle = (System.currentTimeMillis() / speed.toDouble() + phaseOffset / 256.0) * 2 * PI / 30.0
        val sine = sin(angle)
        val mid = amplitude / 2.0
        val range = amplitude / 2.0
        return (mid + sine * range).roundToInt().coerceIn(0, amplitude)
    }

    companion object {
        private const val FADE_AMOUNT = 200
        private const val SPEED = 30
        private const val ROW_PHASE = 0.35
        private const val BRIGHTNESS_OFFSET = 256 / 14
    }
}
