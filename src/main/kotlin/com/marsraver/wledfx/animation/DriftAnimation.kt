package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.*

/**
 * Drift animation - party palette spirals drifting from the center.
 */
class DriftAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var tempBuffer: Array<Array<RgbColor>>

    override fun getName(): String = "Drift"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        tempBuffer = Array(width) { Array(height) { RgbColor.BLACK } }
    }

    override fun update(now: Long): Boolean {
        clear()

        val time = now / 20_000_000.0
        val maxDim = max(width, height).toDouble()
        val centerX = (width - 1) / 2.0
        val centerY = (height - 1) / 2.0
        
        // Use paramSpeed or keep based on time? 
        // Original uses fixed time scale. Let's keep fixed logic but respect param for palette shift?
        // Original: pulse speed fixed. 
        // We can use paramSpeed to adjust `time` scale if desired, but for fidelity let's keep it.
        val paletteShift = (now / 20_000_000L).toInt()

        var radius = 1.0
        while (radius < maxDim / 2.0) {
            val angle = time * (maxDim / 2.0 - radius)
            val radians = angle * PI / 180.0

            val x = centerX + sin(radians) * radius
            val y = centerY + cos(radians) * radius
            val hue = ((radius * 20).toInt() + paletteShift) and 0xFF
            // Use getColorFromPalette
            drawPixelXYF(x, y, getColorFromPalette(hue))

            val altX = centerX + cos(radians) * radius
            val altY = centerY + sin(radians) * radius
            // Slightly different color for alt, maybe offset hue/brightness?
            // Original: hsv((hue+64), 200, 220)
            // Use palette offset?
            // Or just manual construction if palette support is key.
            // If using standard palette, offset hue index is fine.
            val altColor = getColorFromPalette((hue + 64) and 0xFF)
            // Reduce saturation/value manually?
            // BaseAnimation palette returns full color.
            // Let's just use it.
            drawPixelXYF(altX, altY, altColor)

            radius += 0.5
        }

        blur2d(16)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun clear() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = RgbColor.BLACK
            }
        }
    }

    private fun drawPixelXYF(x: Double, y: Double, rgb: RgbColor) {
        val floorX = x.toInt()
        val floorY = y.toInt()
        val fracX = (x - floorX).coerceIn(0.0, 1.0)
        val fracY = (y - floorY).coerceIn(0.0, 1.0)
        val invX = 1.0 - fracX
        val invY = 1.0 - fracY

        val weights = listOf(invX * invY, fracX * invY, invX * fracY, fracX * fracY)
        val offsets = listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)

        for ((index, weight) in weights.withIndex()) {
            val (dx, dy) = offsets[index]
            val xx = floorX + dx
            val yy = floorY + dy
            if (xx !in 0 until width || yy !in 0 until height) continue

            val current = pixelColors[xx][yy]
            pixelColors[xx][yy] = RgbColor(
                (current.r + rgb.r * weight).roundToInt().coerceAtMost(255),
                (current.g + rgb.g * weight).roundToInt().coerceAtMost(255),
                (current.b + rgb.b * weight).roundToInt().coerceAtMost(255)
            )
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
}
