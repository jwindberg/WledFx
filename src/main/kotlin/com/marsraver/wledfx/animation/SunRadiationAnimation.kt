package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Sun Radiation animation - heat/palette driven bump-mapped plasma.
 */
class SunRadiationAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<RgbColor>
    private lateinit var bump: IntArray
    private lateinit var bumpTemp: IntArray
    private var lut = Array(256) { RgbColor.BLACK }
    private var lutGenerated = false
    private var lastUpdateNanos: Long = 0L

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth * combinedHeight) { RgbColor.BLACK }
        bump = IntArray((combinedWidth + 2) * (combinedHeight + 2))
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
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x + y * combinedWidth]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Sun Radiation"

    override fun cleanup() {
        // No external resources to release.
    }

    private fun generateLut() {
        for (i in 0 until 256) {
            lut[i] = heatColor((i / 1.4).roundToInt().coerceIn(0, 255))
        }
    }

    private fun generateBump(timeSeconds: Double, deltaMs: Double) {
        val width = combinedWidth + 2
        val height = combinedHeight + 2
        val z = timeSeconds * 0.3
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val noise = perlinNoise(i * 0.22, j * 0.22, z)
                val normalized = ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0)
                bump[index++] = (normalized / 2.0).roundToInt()
            }
        }
        smoothBump(width, height)
    }

    private fun bumpMap() {
        val extendedWidth = combinedWidth + 2
        var yIndex = extendedWidth + 1
        var vly = -(combinedHeight / 2 + 1)

        for (y in 0 until combinedHeight) {
            vly++
            var vlx = -(combinedWidth / 2 + 1)
            for (x in 0 until combinedWidth) {
                vlx++
                val nx = bump[yIndex + x + 1] - bump[yIndex + x - 1]
                val ny = bump[yIndex + x + extendedWidth] - bump[yIndex + x - extendedWidth]
                val difx = abs(vlx * 7 - nx)
                val dify = abs(vly * 7 - ny)
                val temp = difx * difx + dify * dify
                var col = 255 - temp / 12
                if (col < 0) col = 0
                val color = lut[col.coerceIn(0, 255)]
                val idx = x + y * combinedWidth
                pixelColors[idx] = color
            }
            yIndex += extendedWidth
        }
    }

    private fun smoothBump(width: Int, height: Int) {
        for (j in 1 until height - 1) {
            for (i in 1 until width - 1) {
                val idx = i + j * width
                var sum = 0
                sum += bump[idx]
                sum += bump[idx - 1]
                sum += bump[idx + 1]
                sum += bump[idx - width]
                sum += bump[idx + width]
                sum += bump[idx - width - 1]
                sum += bump[idx - width + 1]
                sum += bump[idx + width - 1]
                sum += bump[idx + width + 1]
                bumpTemp[idx] = sum / 9
            }
        }
        for (j in 1 until height - 1) {
            for (i in 1 until width - 1) {
                val idx = i + j * width
                bump[idx] = bumpTemp[idx]
            }
        }
    }

    private fun heatColor(temperature: Int): RgbColor {
        val t = temperature.coerceIn(0, 255)
        val t192 = (t * 191) / 255

        val heatRamp = (t192 and 0x3F) shl 2
        val r: Int
        val g: Int
        val b: Int

        when {
            t192 and 0x80 != 0 -> {
                r = 255
                g = 255
                b = heatRamp
            }
            t192 and 0x40 != 0 -> {
                r = 255
                g = heatRamp
                b = 0
            }
            else -> {
                r = heatRamp
                g = 0
                b = 0
            }
        }
        return RgbColor(r, g, b)
    }

    private fun perlinNoise(x: Double, y: Double, z: Double): Double {
        val xi = floor(x).toInt() and PERM_MASK
        val yi = floor(y).toInt() and PERM_MASK
        val zi = floor(z).toInt() and PERM_MASK

        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)

        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val aaa = PERM[PERM[PERM[xi] + yi] + zi]
        val aba = PERM[PERM[PERM[xi] + ((yi + 1) and PERM_MASK)] + zi]
        val aab = PERM[PERM[PERM[xi] + yi] + ((zi + 1) and PERM_MASK)]
        val abb = PERM[PERM[PERM[xi] + ((yi + 1) and PERM_MASK)] + ((zi + 1) and PERM_MASK)]
        val baa = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + yi] + zi]
        val bba = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + ((yi + 1) and PERM_MASK)] + zi]
        val bab = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + yi] + ((zi + 1) and PERM_MASK)]
        val bbb = PERM[PERM[PERM[(xi + 1) and PERM_MASK] + ((yi + 1) and PERM_MASK)] + ((zi + 1) and PERM_MASK)]

        val x1 = lerp(grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf), u)
        val x2 = lerp(grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf), u)
        val y1 = lerp(x1, x2, v)

        val x3 = lerp(grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1), u)
        val x4 = lerp(grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1), u)
        val y2 = lerp(x3, x4, v)

        return lerp(y1, y2, w)
    }

    private fun fade(t: Double): Double = t.pow(3.0) * (t * (t * 6 - 15) + 10)
    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        val uu = if (h and 1 == 0) u else -u
        val vv = if (h and 2 == 0) v else -v
        return uu + vv
    }

    companion object {
        private const val PERM_MASK = 255
        private val PERM_BASE = intArrayOf(
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99,
            37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27,
            166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102,
            143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116,
            188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126,
            255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152,
            2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113,
            224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50,
            45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61,
            156, 180
        )
        private val PERM = IntArray(512) { PERM_BASE[it and PERM_MASK] }
    }
}

