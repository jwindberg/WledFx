package com.marsraver.WledFx.animation

import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Crazy Bees animation - Bees flying to random targets with flowers.
 */
class CrazyBeesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<IntArray>> = emptyArray()
    private lateinit var bees: Array<Bee>
    private var numBees: Int = 0
    private var lastUpdateTime: Long = 0
    private var updateInterval: Long = 0

    private var fadeAmount: Int = 32
    private var blurAmount: Int = 10
    private var speed: Int = 128

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        lastUpdateTime = 0

        numBees = min(MAX_BEES, (combinedWidth * combinedHeight) / 256 + 1)
        bees = Array(numBees) { Bee() }

        RANDOM.setSeed(System.currentTimeMillis())
        for (i in 0 until numBees) {
            val bee = bees[i]
            bee.posX = RANDOM.nextInt(combinedWidth)
            bee.posY = RANDOM.nextInt(combinedHeight)
            bee.setAim(combinedWidth, combinedHeight)
        }

        val speedFactor = (speed shr 4) + 1
        updateInterval = 16_000_000L * 16L / speedFactor
    }

    override fun update(now: Long): Boolean {
        if (now - lastUpdateTime < updateInterval) {
            return true
        }
        lastUpdateTime = now

        fadeToBlack(fadeAmount)
        applyBlur(blurAmount)

        for (i in 0 until numBees) {
            val bee = bees[i]

            val flowerColor = hsvToRgb(bee.hue, 255, 255)
            addPixelColor(bee.aimX + 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY + 1, flowerColor)
            addPixelColor(bee.aimX - 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY - 1, flowerColor)

            if (bee.posX != bee.aimX || bee.posY != bee.aimY) {
                val beeColor = hsvToRgb(bee.hue, 153, 255)
                setPixelColor(bee.posX, bee.posY, beeColor)

                val error2 = bee.error * 2
                if (error2 > -bee.deltaY) {
                    bee.error -= bee.deltaY
                    bee.posX += bee.signX
                }
                if (error2 < bee.deltaX) {
                    bee.error += bee.deltaX
                    bee.posY += bee.signY
                }
            } else {
                bee.setAim(combinedWidth, combinedHeight)
            }
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y].clone()
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Crazy Bees"

    private fun fadeToBlack(amount: Int) {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (c in 0 until 3) {
                    pixelColors[x][y][c] = max(0, pixelColors[x][y][c] - amount)
                }
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return

        val temp = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }

        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (c in 0 until 3) {
                    var sum = 0
                    var count = 0

                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                                sum += pixelColors[nx][ny][c]
                                count++
                            }
                        }
                    }

                    val blurred = if (count > 0) sum / count else pixelColors[x][y][c]
                    temp[x][y][c] = (pixelColors[x][y][c] * (255 - amount) + blurred * amount) / 255
                }
            }
        }

        pixelColors = temp
    }

    private fun setPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = rgb[0].coerceIn(0, 255)
            pixelColors[x][y][1] = rgb[1].coerceIn(0, 255)
            pixelColors[x][y][2] = rgb[2].coerceIn(0, 255)
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
            pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
            pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
        }
    }

    private fun hsvToRgb(h: Int, s: Int, v: Int): IntArray {
        val hue = (h % 256) / 255.0f * 360.0f
        val saturation = s / 255.0f
        val value = v / 255.0f

        val hi = ((hue / 60.0f) % 6).toInt()
        val f = hue / 60.0f - hi
        val p = value * (1 - saturation)
        val q = value * (1 - f * saturation)
        val t = value * (1 - (1 - f) * saturation)

        val (r, g, b) = when (hi) {
            0 -> Triple(value, t, p)
            1 -> Triple(q, value, p)
            2 -> Triple(p, value, t)
            3 -> Triple(p, q, value)
            4 -> Triple(t, p, value)
            else -> Triple(value, p, q)
        }

        return intArrayOf(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    private inner class Bee {
        var posX: Int = 0
        var posY: Int = 0
        var aimX: Int = 0
        var aimY: Int = 0
        var hue: Int = 0
        var deltaX: Int = 0
        var deltaY: Int = 0
        var signX: Int = 1
        var signY: Int = 1
        var error: Int = 0

        fun setAim(width: Int, height: Int) {
            aimX = RANDOM.nextInt(width)
            aimY = RANDOM.nextInt(height)
            hue = RANDOM.nextInt(256)
            deltaX = abs(aimX - posX)
            deltaY = abs(aimY - posY)
            signX = if (posX < aimX) 1 else -1
            signY = if (posY < aimY) 1 else -1
            error = deltaX - deltaY
        }
    }

    companion object {
        private val RANDOM = Random()
        private const val MAX_BEES = 5
    }
}

