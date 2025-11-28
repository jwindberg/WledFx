package com.marsraver.wledfx.animation

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Frizzles animation - Color frizzles bouncing across the matrix with blur trails.
 */
class FrizzlesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<IntArray>>
    private var palette: Array<IntArray>? = null
    private var lastUpdateNs: Long = 0L

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Array<IntArray>) {
        this.palette = palette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
        }
        lastUpdateNs = now

        fadeToBlack(16)

        val loops = 8
        val timeMs = now / 1_000_000
        for (i in loops downTo 1) {
            val freqBase = 12
            val x = beatsin8(freqBase + i, BORDER, combinedWidth - 1 - BORDER, timeMs)
            val y = beatsin8(15 - i, BORDER, combinedHeight - 1 - BORDER, timeMs)
            val hue = beatsin8(freqBase, 0, 255, timeMs)
            val color = getColorFromHue(hue, 255)
            addPixelColor(x, y, color)

            if (combinedWidth > 24 || combinedHeight > 24) {
                addPixelColor(x + 1, y, color)
                addPixelColor(x - 1, y, color)
                addPixelColor(x, y + 1, color)
                addPixelColor(x, y - 1, color)
            }
        }

        applyBlur(16)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val color = pixelColors[x][y].clone()
            color[0] = color[0].coerceIn(0, 255)
            color[1] = color[1].coerceIn(0, 255)
            color[2] = color[2].coerceIn(0, 255)
            color
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Frizzles"

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255)
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = pixelColors[x][y][0] * factor / 255
                pixelColors[x][y][1] = pixelColors[x][y][1] * factor / 255
                pixelColors[x][y][2] = pixelColors[x][y][2] * factor / 255
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val temp = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        val factor = amount.coerceIn(0, 255)
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (channel in 0 until 3) {
                    var sum = 0
                    var weight = 0
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                                val w = if (dx == 0 && dy == 0) 4 else 1
                                sum += pixelColors[nx][ny][channel] * w
                                weight += w
                            }
                        }
                    }
                    val average = if (weight == 0) pixelColors[x][y][channel] else sum / weight
                    temp[x][y][channel] = (pixelColors[x][y][channel] * (255 - factor) + average * factor) / 255
                }
            }
        }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = temp[x][y][0]
                pixelColors[x][y][1] = temp[x][y][1]
                pixelColors[x][y][2] = temp[x][y][2]
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) return
        pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
        pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
        pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
    }

    private fun beatsin8(frequency: Int, low: Int, high: Int, timeMs: Long): Int {
        val freq = frequency.coerceAtLeast(1)
        val period = 1_000.0 / freq
        val angle = (timeMs % period) / period * 2 * PI
        val sine = sin(angle)
        val mid = (low + high) / 2.0
        val amplitude = (high - low) / 2.0
        return (mid + sine * amplitude).roundToInt().coerceIn(low, high)
    }

    private fun getColorFromHue(hue: Int, brightness: Int): IntArray {
        val currentPalette = palette
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return intArrayOf(
                (baseColor[0] * brightnessFactor).toInt().coerceIn(0, 255),
                (baseColor[1] * brightnessFactor).toInt().coerceIn(0, 255),
                (baseColor[2] * brightnessFactor).toInt().coerceIn(0, 255)
            )
        } else {
            return hsvToRgb(hue, 255, brightness)
        }
    }

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): IntArray {
        val h = (hue % 256 + 256) % 256
        val s = saturation.coerceIn(0, 255) / 255.0
        val v = value.coerceIn(0, 255) / 255.0

        if (s <= 0.0) {
            val gray = (v * 255).roundToInt()
            return intArrayOf(gray, gray, gray)
        }

        val hSection = h / 42.6666667
        val i = hSection.toInt()
        val f = hSection - i

        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))

        val (r, g, b) = when (i % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }

        return intArrayOf(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255),
        )
    }

    companion object {
        private const val BORDER = 0
    }
}
