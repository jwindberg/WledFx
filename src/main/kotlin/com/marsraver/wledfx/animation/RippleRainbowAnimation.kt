package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Ripple Rainbow animation - expanding colorful ripples across the grid.
 */
class RippleRainbowAnimation : LedAnimation {

    private data class Ripple(
        val centerX: Double,
        val centerY: Double,
        var radius: Double,
        val speed: Double,
        val thickness: Double,
        val hue: Int,
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val ripples = mutableListOf<Ripple>()
    private var lastSpawnTimeNs: Long = 0L
    private var lastHue: Int = Random.nextInt(256)
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
        ripples.clear()
        lastSpawnTimeNs = 0L
        lastHue = Random.nextInt(256)
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth == 0 || combinedHeight == 0) return true

        fadeToBlack(FADE_AMOUNT)

        val elapsedSinceSpawn = if (lastSpawnTimeNs == 0L) SPAWN_INTERVAL_NS else now - lastSpawnTimeNs
        if (elapsedSinceSpawn >= SPAWN_INTERVAL_NS && ripples.size < MAX_RIPPLES) {
            spawnRipple()
            lastSpawnTimeNs = now
        }

        val maxRadius = sqrt((combinedWidth * combinedWidth + combinedHeight * combinedHeight).toDouble())
        val iterator = ripples.iterator()
        while (iterator.hasNext()) {
            val ripple = iterator.next()
            ripple.radius += ripple.speed
            if (ripple.radius - ripple.thickness > maxRadius) {
                iterator.remove()
                continue
            }

            val hue = ripple.hue
            for (x in 0 until combinedWidth) {
                for (y in 0 until combinedHeight) {
                    val distance = hypot(x - ripple.centerX, y - ripple.centerY)
                    val diff = abs(distance - ripple.radius)
                    if (diff <= ripple.thickness) {
                        val falloff = 1.0 - diff / ripple.thickness
                        val brightness = (falloff * 255).roundToInt().coerceIn(0, 255)
                        val rgb = getColorFromHue(hue, brightness)
                        addPixelColor(x, y, rgb)
                    }
                }
            }
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Ripple Rainbow"

    override fun cleanup() {
        ripples.clear()
    }

    private fun spawnRipple() {
        val centerX = Random.nextDouble(combinedWidth.toDouble())
        val centerY = Random.nextDouble(combinedHeight.toDouble())
        val speed = Random.nextDouble(0.35, 0.75)
        val thickness = Random.nextDouble(0.9, 1.8)
        val hueIncrement = Random.nextInt(32, 96)
        lastHue = (lastHue + hueIncrement) and 0xFF
        ripples += Ripple(centerX, centerY, radius = 0.0, speed = speed, thickness = thickness, hue = lastHue)
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

    companion object {
        private const val MAX_RIPPLES = 5
        private const val FADE_AMOUNT = 18
        private const val SPAWN_INTERVAL_NS = 220_000_000L
    }
}
