package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Soap animation - smooth flowing perlin noise with cross-axis blending.
 */
class SoapAnimation : BaseAnimation() {

    private lateinit var pixels: Array<Array<RgbColor>>
    private lateinit var noiseBuffer: Array<DoubleArray>
    private lateinit var rowBlend: DoubleArray
    private lateinit var colBlend: DoubleArray

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var offsetZ: Double = 0.0
    private var scaleX: Double = 1.0
    private var scaleY: Double = 1.0

    override fun onInit() {
        pixels = Array(width) { Array(height) { RgbColor.BLACK } }
        noiseBuffer = Array(width) { DoubleArray(height) }
        rowBlend = DoubleArray(height)
        colBlend = DoubleArray(width)

        paramSpeed = 128
        paramIntensity = 128

        scaleX = 0.0
        scaleY = 0.0
        if (width > 0) scaleX = 1.6 / width
        if (height > 0) scaleY = 1.6 / height

        offsetX = Random.nextDouble(0.0, 10_000.0)
        offsetY = Random.nextDouble(0.0, 10_000.0)
        offsetZ = Random.nextDouble(0.0, 10_000.0)
    }

    override fun update(now: Long): Boolean {
        if (width == 0 || height == 0) return true

        val baseScale = max(1, min(width, height))
        val movementFactor = baseScale * 0.0004
        val speedMult = paramSpeed / 128.0
        
        offsetX += movementFactor * speedMult
        offsetY += movementFactor * 0.87 * speedMult
        offsetZ += movementFactor * 1.13 * speedMult

        val smoothness = 200

        // Compute noise field with exponential smoothing.
        for (x in 0 until width) {
            val iOffset = scaleX * (x - width / 2)
            for (y in 0 until height) {
                val jOffset = scaleY * (y - height / 2)
                val noise = MathUtils.perlinNoise(offsetX + iOffset, offsetY + jOffset, offsetZ)
                val value = ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0)
                val previous = noiseBuffer[x][y]
                val blended = (previous * smoothness + value * (255 - smoothness)) / 255.0
                noiseBuffer[x][y] = blended
            }
        }

        // Pre-compute row and column blends for soap-like diffusion.
        for (y in 0 until height) {
            var sum = 0.0
            for (x in 0 until width) {
                sum += noiseBuffer[x][y]
            }
            rowBlend[y] = sum / width
        }
        for (x in 0 until width) {
            var sum = 0.0
            for (y in 0 until height) {
                sum += noiseBuffer[x][y]
            }
            colBlend[x] = sum / height
        }

        // Update pixels combining local value with row/column blends.
        for (x in 0 until width) {
            for (y in 0 until height) {
                val value = noiseBuffer[x][y]
                val blended = (value * 0.6) + (rowBlend[y] * 0.2) + (colBlend[x] * 0.2)
                val hue = ((255 - blended) * 3).roundToInt() and 0xFF
                val sat = (200 + (value / 255.0) * 40).roundToInt().coerceIn(0, 255)
                val bri = (180 + (blended / 255.0) * 75).roundToInt().coerceIn(0, 255)
                
                // Use BaseAnimation palette if available
                val rgb = hsvToRgb(hue, sat, bri)
                pixels[x][y] = rgb
            }
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixels[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Soap"

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): RgbColor {
        // Here we override the default color generation because Soap relies on specific H/S/V calculations
        // However, if a palette is set, we might want to map the Hue to the palette?
        // Let's stick to the original behavior mostly, but call base getPalette()
        val currentPalette = getPalette()?.colors
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
