package com.marsraver.wledfx.animation

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
    private lateinit var pixelColors: Array<Array<IntArray>>
    private val ripples = mutableListOf<Ripple>()
    private var lastSpawnTimeNs: Long = 0L
    private var lastHue: Int = Random.nextInt(256)

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
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
                        val rgb = hsvToRgb(hue, 255, brightness)
                        addPixelColor(x, y, rgb)
                    }
                }
            }
        }

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

    override fun getName(): String = "Ripple Rainbow"

    fun cleanup() {
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
        val factor = (255 - amount).coerceIn(0, 255)
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = pixelColors[x][y][0] * factor / 255
                pixelColors[x][y][1] = pixelColors[x][y][1] * factor / 255
                pixelColors[x][y][2] = pixelColors[x][y][2] * factor / 255
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
        pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
        pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
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
        private const val MAX_RIPPLES = 5
        private const val FADE_AMOUNT = 18
        private const val SPAWN_INTERVAL_NS = 220_000_000L
    }
}
