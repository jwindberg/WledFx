package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Ripple Rainbow animation - expanding colorful ripples across the grid.
 */
class RippleRainbowAnimation : BaseAnimation() {

    private data class Ripple(
        val centerX: Double,
        val centerY: Double,
        var radius: Double,
        val speed: Double,
        val thickness: Double,
        val hue: Int,
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val ripples = mutableListOf<Ripple>()
    private var lastSpawnTimeNs: Long = 0L
    private var lastHue: Int = Random.nextInt(256)

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        ripples.clear()
        lastSpawnTimeNs = 0L
        lastHue = Random.nextInt(256)
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        if (width == 0 || height == 0) return true

        fadeToBlack(FADE_AMOUNT)

        // Spawn interval influenced by speed? 
        // Original was fixed 220ms. Let's make it responsive to speed.
        // Higher speed = smaller interval = more ripples?
        // Or speed = ripple expansion speed (already passed to Ripple)?
        // Let's keep interval mostly fixed but scaled slightly.
        val spawnInterval = SPAWN_INTERVAL_NS // Could adjust with paramSpeed if desired

        val elapsedSinceSpawn = if (lastSpawnTimeNs == 0L) spawnInterval else now - lastSpawnTimeNs
        if (elapsedSinceSpawn >= spawnInterval && ripples.size < MAX_RIPPLES) {
            spawnRipple()
            lastSpawnTimeNs = now
        }

        val maxRadius = sqrt((width * width + height * height).toDouble())
        val iterator = ripples.iterator()
        while (iterator.hasNext()) {
            val ripple = iterator.next()
            ripple.radius += ripple.speed
            if (ripple.radius - ripple.thickness > maxRadius) {
                iterator.remove()
                continue
            }

            val hue = ripple.hue
            for (x in 0 until width) {
                for (y in 0 until height) {
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
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Ripple Rainbow"
    override fun supportsIntensity(): Boolean = true

    override fun cleanup() {
        ripples.clear()
    }

    private fun spawnRipple() {
        val centerX = Random.nextDouble(width.toDouble())
        val centerY = Random.nextDouble(height.toDouble())
        
        // Speed scaling based on paramSpeed
        val speedMult = paramSpeed / 128.0
        val speed = Random.nextDouble(0.35, 0.75) * speedMult
        
        val thickness = Random.nextDouble(0.9, 1.8)
        val hueIncrement = Random.nextInt(32, 96)
        
        // Use paramIntensity to control color variability? or brightness?
        // Currently hue cycle.
        lastHue = (lastHue + hueIncrement) and 0xFF
        ripples += Ripple(centerX, centerY, radius = 0.0, speed = speed, thickness = thickness, hue = lastHue)
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + rgb.r).coerceAtMost(255),
                (current.g + rgb.g).coerceAtMost(255),
                (current.b + rgb.b).coerceAtMost(255)
            )
        }
    }

    private fun getColorFromHue(hue: Int, brightness: Int): RgbColor {
        // Use BaseAnimation palette support
        val base = getColorFromPalette(hue)
        val brightnessFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }

    companion object {
        private const val MAX_RIPPLES = 5
        private const val FADE_AMOUNT = 18
        private const val SPAWN_INTERVAL_NS = 220_000_000L
    }
}
