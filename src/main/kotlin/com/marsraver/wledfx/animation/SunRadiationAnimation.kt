package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Sun Radiation animation - heat/palette driven bump-mapped plasma.
 */
class SunRadiationAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<RgbColor>
    private lateinit var bump: IntArray
    private lateinit var bumpTemp: IntArray
    private var lut = Array(256) { RgbColor.BLACK }
    private var lutGenerated = false
    private var lastUpdateNanos: Long = 0L

    override fun onInit() {
        pixelColors = Array(width * height) { RgbColor.BLACK }
        bump = IntArray((width + 2) * (height + 2))
        bumpTemp = IntArray(bump.size)
        lutGenerated = false
        lastUpdateNanos = 0L
    }

    override fun update(now: Long): Boolean {
        if (!lutGenerated) {
            generateLut()
            lutGenerated = true
        }

        val deltaMs = if (lastUpdateNanos == 0L) 0.0 else (now - lastUpdateNanos) / 1_000_000.0
        lastUpdateNanos = now
        val timeSeconds = now / 1_000_000_000.0

        generateBump(timeSeconds, deltaMs)
        bumpMap()
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x + y * width]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Sun Radiation"

    private fun generateLut() {
        // Use current palette if available?
        // Original creates a heat color LUT.
        // If palette is supported, we should generate LUT from palette.
        if (getPalette() != null) {
            for (i in 0 until 256) {
                lut[i] = getColorFromPalette(i)
            }
        } else {
            // Default heat ramp
            for (i in 0 until 256) {
                lut[i] = ColorUtils.heatColor((i / 1.4).roundToInt().coerceIn(0, 255))
            }
        }
    }

    private fun generateBump(timeSeconds: Double, deltaMs: Double) {
        val w = width + 2
        val h = height + 2
        val z = timeSeconds * 0.3
        var index = 0
        for (j in 0 until h) {
            for (i in 0 until w) {
                // Use MathUtils.perlinNoise
                val noise = MathUtils.perlinNoise(i * 0.22, j * 0.22, z)
                val normalized = ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0)
                bump[index++] = (normalized / 2.0).roundToInt()
            }
        }
        smoothBump(w, h)
    }

    private fun bumpMap() {
        val extendedWidth = width + 2
        var yIndex = extendedWidth + 1
        var vly = -(height / 2 + 1)

        for (y in 0 until height) {
            vly++
            var vlx = -(width / 2 + 1)
            for (x in 0 until width) {
                vlx++
                val nx = bump[yIndex + x + 1] - bump[yIndex + x - 1]
                val ny = bump[yIndex + x + extendedWidth] - bump[yIndex + x - extendedWidth]
                val difx = abs(vlx * 7 - nx)
                val dify = abs(vly * 7 - ny)
                val temp = difx * difx + dify * dify
                var col = 255 - temp / 12
                if (col < 0) col = 0
                val color = lut[col.coerceIn(0, 255)]
                val idx = x + y * width
                pixelColors[idx] = color
            }
            yIndex += extendedWidth
        }
    }

    private fun smoothBump(w: Int, h: Int) {
        for (j in 1 until h - 1) {
            for (i in 1 until w - 1) {
                val idx = i + j * w
                var sum = 0
                sum += bump[idx]
                sum += bump[idx - 1]
                sum += bump[idx + 1]
                sum += bump[idx - w]
                sum += bump[idx + w]
                sum += bump[idx - w - 1]
                sum += bump[idx - w + 1]
                sum += bump[idx + w - 1]
                sum += bump[idx + w + 1]
                bumpTemp[idx] = sum / 9
            }
        }
        for (j in 1 until h - 1) {
            for (i in 1 until w - 1) {
                val idx = i + j * w
                bump[idx] = bumpTemp[idx]
            }
        }
    }
}
