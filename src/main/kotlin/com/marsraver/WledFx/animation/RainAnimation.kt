package com.marsraver.WledFx.animation

import kotlin.math.PI
import kotlin.math.max
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
    private lateinit var pixelColors: Array<Array<IntArray>>
    private val drops = mutableListOf<Drop>()

    private var lastUpdateNs: Long = 0L
    private var spawnAccumulator = 0.0
    private var hueSeed = Random.nextInt(0, 256)

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
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

            val rgb = hsvToRgb(drop.hue, 255, 255)
            addPixelColor(intX, intY, rgb)
            if (intY + 1 < combinedHeight) {
                val tail = hsvToRgb(drop.hue, 180, 160)
                addPixelColor(intX, intY + 1, tail)
            }
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (::pixelColors.isInitialized && x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val color = pixelColors[x][y].clone()
            color[0] = color[0].coerceIn(0, 255)
            color[1] = color[1].coerceIn(0, 255)
            color[2] = color[2].coerceIn(0, 255)
            color
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Rain"

    fun cleanup() {
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
}
