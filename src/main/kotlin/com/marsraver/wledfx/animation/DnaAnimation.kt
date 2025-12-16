package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.*

/**
 * DNA animation - dual sine waves forming a helix pattern.
 */
class DnaAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var tempBuffer: Array<Array<RgbColor>>
    private var lastUpdateNs: Long = 0L

    override fun getName(): String = "DNA"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        tempBuffer = Array(width) { Array(height) { RgbColor.BLACK } }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }

        val fadeAmount = 200 // Could use paramIntensity, but let's stick to constant for now or map?
        // Let's use constant as in original for specific effect tuning
        fadeToBlack(fadeAmount)

        val timeMs = now / 1_000_000
        val rows = height
        val center = (width - 1) / 2.0
        val amplitude = (width - 2) / 2.0

        for (row in 0 until rows) {
            val angle = timeMs / 240.0 + row * 0.35
            val wobble = sin(angle * 0.5) * amplitude * 0.15

            val x1 = (center + sin(angle) * amplitude * 0.55 + wobble).roundToInt().coerceIn(0, width - 1)
            val x2 = (center + sin(angle + PI) * amplitude * 0.55 - wobble).roundToInt().coerceIn(0, width - 1)

            val hueBase = (timeMs / 25 + row * 14).toInt() and 0xFF
            val hueOpposite = (hueBase + 128) and 0xFF

            val brightness1 = (160 + sin(angle * 1.3) * 95).roundToInt().coerceIn(60, 255)
            val brightness2 = (160 + sin((angle + PI) * 1.3) * 95).roundToInt().coerceIn(60, 255)

            // Use getColorFromPalette helper logic?
            // Actually original uses explicit hsv logic with palette support. 
            // We can delegate to BaseAnimation palette support if needed.
            // But original `hsvToRgb` calls `ColorUtils.hsvToRgb` if no palette.
            // We should use BaseAnimation's getColorFromPalette if we want palette support.
            
            val colorA = getColorFromPalette(hueBase, brightness1)
            val colorB = getColorFromPalette(hueOpposite, brightness2)

            drawLine(x1, x2, row, blendColors(colorA, colorB), addDot = false)
            drawPixel(x1, row, colorA)
            drawPixel(x2, row, colorB)
        }

        blur2d(18)
        mirrorVertical()
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun getColorFromPalette(hue: Int, brightness: Int): RgbColor {
        val base = getColorFromPalette(hue) 
        if (brightness < 255) return ColorUtils.scaleBrightness(base, brightness / 255.0)
        return base
    }

    private fun drawPixel(x: Int, y: Int, rgb: RgbColor) {
        if (x !in 0 until width || y !in 0 until height) return
        val current = pixelColors[x][y]
        pixelColors[x][y] = RgbColor(
            (current.r + rgb.r).coerceAtMost(255),
            (current.g + rgb.g).coerceAtMost(255),
            (current.b + rgb.b).coerceAtMost(255)
        )
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        drawPixel(x, y, rgb)
    }

    private fun blendColors(a: RgbColor, b: RgbColor): RgbColor {
        return RgbColor((a.r + b.r) / 2, (a.g + b.g) / 2, (a.b + b.b) / 2)
    }

    private fun drawLine(x0: Int, x1: Int, y: Int, rgb: RgbColor, addDot: Boolean) {
        val start = x0.coerceIn(0, width - 1)
        val end = x1.coerceIn(0, width - 1)
        val steps = abs(end - start) + 1
        for (step in 0 until steps) {
            val t = step / (steps - 1.0).coerceAtLeast(1.0)
            val x = ((1 - t) * start + t * end).roundToInt().coerceIn(0, width - 1)
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
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun blur2d(amount: Int) {
        if (amount <= 0) return
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0; var sumG = 0; var sumB = 0; var weight = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val w = if (dx == 0 && dy == 0) 4 else 1
                            val color = pixelColors[nx][ny]
                            sumR += color.r * w; sumG += color.g * w; sumB += color.b * w
                            weight += w
                        }
                    }
                }
                val current = pixelColors[x][y]
                val br = if (weight > 0) sumR/weight else current.r
                val bg = if (weight > 0) sumG/weight else current.g
                val bb = if (weight > 0) sumB/weight else current.b
                tempBuffer[x][y] = RgbColor(
                    (current.r * (255 - amount) + br * amount) / 255,
                    (current.g * (255 - amount) + bg * amount) / 255,
                    (current.b * (255 - amount) + bb * amount) / 255
                )
            }
        }
        for (x in 0 until width) for (y in 0 until height) pixelColors[x][y] = tempBuffer[x][y]
    }

    private fun mirrorVertical() {
        val half = height / 2
        for (y in 0 until half) {
            val opposite = height - 1 - y
            for (x in 0 until width) pixelColors[x][opposite] = pixelColors[x][y]
        }
    }
}
