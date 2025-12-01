package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Soap animation - smooth flowing perlin noise with cross-axis blending.
 */
class SoapAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixels: Array<Array<RgbColor>>
    private lateinit var noiseBuffer: Array<DoubleArray>
    private lateinit var rowBlend: DoubleArray
    private lateinit var colBlend: DoubleArray

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var scaleX: Double = 1.0
    private var scaleY: Double = 1.0
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
        pixels = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        noiseBuffer = Array(combinedWidth) { DoubleArray(combinedHeight) }
        rowBlend = DoubleArray(combinedHeight)
        colBlend = DoubleArray(combinedWidth)

        scaleX = 0.0
        scaleY = 0.0
        if (combinedWidth > 0) scaleX = 1.6 / combinedWidth
        if (combinedHeight > 0) scaleY = 1.6 / combinedHeight

        offsetX = Random.nextDouble(0.0, 10_000.0)
        offsetY = Random.nextDouble(0.0, 10_000.0)
        offsetZ = Random.nextDouble(0.0, 10_000.0)
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth == 0 || combinedHeight == 0) return true

        val baseScale = max(1, min(combinedWidth, combinedHeight))
        val movementFactor = baseScale * 0.0004
        offsetX += movementFactor
        offsetY += movementFactor * 0.87
        offsetZ += movementFactor * 1.13

        val smoothness = 200

        // Compute noise field with exponential smoothing.
        for (x in 0 until combinedWidth) {
            val iOffset = scaleX * (x - combinedWidth / 2)
            for (y in 0 until combinedHeight) {
                val jOffset = scaleY * (y - combinedHeight / 2)
                val noise = perlinNoise(offsetX + iOffset, offsetY + jOffset, offsetZ)
                val value = ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0)
                val previous = noiseBuffer[x][y]
                val blended = (previous * smoothness + value * (255 - smoothness)) / 255.0
                noiseBuffer[x][y] = blended
            }
        }

        // Pre-compute row and column blends for soap-like diffusion.
        for (y in 0 until combinedHeight) {
            var sum = 0.0
            for (x in 0 until combinedWidth) {
                sum += noiseBuffer[x][y]
            }
            rowBlend[y] = sum / combinedWidth
        }
        for (x in 0 until combinedWidth) {
            var sum = 0.0
            for (y in 0 until combinedHeight) {
                sum += noiseBuffer[x][y]
            }
            colBlend[x] = sum / combinedHeight
        }

        // Update pixels combining local value with row/column blends.
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val value = noiseBuffer[x][y]
                val blended = (value * 0.6) + (rowBlend[y] * 0.2) + (colBlend[x] * 0.2)
                val hue = ((255 - blended) * 3).roundToInt() and 0xFF
                val sat = (200 + (value / 255.0) * 40).roundToInt().coerceIn(0, 255)
                val bri = (180 + (blended / 255.0) * 75).roundToInt().coerceIn(0, 255)
                val rgb = hsvToRgb(hue, sat, bri)
                pixels[x][y] = rgb
            }
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixels[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Soap"

    override fun cleanup() {
        // Nothing to dispose.
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

