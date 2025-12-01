package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Rain animation - drifting rainbow droplets falling across the matrix.
 */
class RainAnimation : LedAnimation {

    private data class Drop(
        var x: Double,
        var y: Double,
        var velocity: Double,
        var hue: Int,
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val drops = mutableListOf<Drop>()
    private var currentPalette: Palette? = null

    private var lastUpdateNs: Long = 0L
    private var spawnAccumulator = 0.0
    private var hueSeed = Random.nextInt(0, 256)

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
        drops.clear()
        lastUpdateNs = 0L
        spawnAccumulator = 0.0
        hueSeed = Random.nextInt(0, 256)
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth <= 0 || combinedHeight <= 0) return true

        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }

        val deltaNs = now - lastUpdateNs
        lastUpdateNs = now
        val deltaSeconds = deltaNs / 1_000_000_000.0

        fadeToBlack(24)

        val spawnRatePerSecond = min(6.0, 1.5 + combinedWidth / 12.0) // vary with matrix size
        spawnAccumulator += spawnRatePerSecond * deltaSeconds
        while (spawnAccumulator >= 1.0) {
            spawnDrop()
            spawnAccumulator -= 1.0
        }

        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val drop = iterator.next()
            drop.y += drop.velocity * deltaSeconds
            val intX = drop.x.roundToInt().coerceIn(0, combinedWidth - 1)
            val intY = drop.y.roundToInt()

            if (intY >= combinedHeight) {
                iterator.remove()
                continue
            }

            val rgb = getColorFromHue(drop.hue, 255)
            addPixelColor(intX, intY, rgb)
            if (intY + 1 < combinedHeight) {
                val tail = getColorFromHue(drop.hue, 160)
                addPixelColor(intX, intY + 1, tail)
            }
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Rain"

    override fun cleanup() {
        drops.clear()
    }

    private fun spawnDrop() {
        val x = Random.nextDouble(0.0, combinedWidth.toDouble())
        val initialY = -Random.nextDouble(0.0, 3.0)
        val velocity = Random.nextDouble(6.0, 11.0)
        hueSeed = (hueSeed + Random.nextInt(12, 48)) and 0xFF
        val hue = hueSeed
        drops += Drop(x, initialY, velocity, hue)
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
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

    private fun getColorFromHue(hue: Int, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(hue, 255, brightness)
        }
    }
}
