package com.marsraver.WledFx.animation

import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Floating Blobs animation - Blobs that move around, bounce off edges, and grow/shrink.
 */
class BlobsAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<IntArray>> = emptyArray()

    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 32
    private var custom2: Int = 32

    private lateinit var blobs: Array<Blob>
    private var amount: Int = 0
    private val random = Random()
    private var startTime: Long = 0
    private var lastColorChange: Long = 0

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        startTime = System.currentTimeMillis()
        lastColorChange = startTime

        amount = min(MAX_BLOBS, (intensity shr 5) + 1)
        blobs = Array(MAX_BLOBS) { Blob() }

        for (i in 0 until MAX_BLOBS) {
            val blob = blobs[i]

            val maxRadius = max(2, combinedWidth / 4)
            blob.radius = random.nextFloat() * (maxRadius - 1) + 1

            val speedDiv = max(1, 256 - speed).toFloat()
            blob.speedX = (random.nextInt(combinedWidth.coerceAtLeast(4) - 3) + 3) / speedDiv
            blob.speedY = (random.nextInt(combinedHeight.coerceAtLeast(4) - 3) + 3) / speedDiv

            blob.x = random.nextInt(combinedWidth).toFloat()
            blob.y = random.nextInt(combinedHeight).toFloat()

            blob.color = random.nextInt(256)
            blob.grow = blob.radius < 1.0f

            if (blob.speedX == 0f) blob.speedX = 1f
            if (blob.speedY == 0f) blob.speedY = 1f
        }
    }

    override fun update(now: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val fadeAmount = (custom2 shr 3) + 1
        fadeToBlack(fadeAmount)

        if (currentTime - lastColorChange >= 2000) {
            for (i in 0 until amount) {
                blobs[i].color = (blobs[i].color + 4) % 256
            }
            lastColorChange = currentTime
        }

        for (i in 0 until amount) {
            val blob = blobs[i]
            val speedFactor = max(abs(blob.speedX), abs(blob.speedY))

            if (blob.grow) {
                blob.radius += speedFactor * 0.05f
                val maxRadius = min(combinedWidth / 4.0f, 2.0f)
                if (blob.radius >= maxRadius) {
                    blob.grow = false
                }
            } else {
                blob.radius -= speedFactor * 0.05f
                if (blob.radius < 1.0f) {
                    blob.grow = true
                }
            }

            val color = colorFromPalette(blob.color, false, false, 0)
            val xPos = blob.x.roundToInt()
            val yPos = blob.y.roundToInt()
            if (blob.radius > 1.0f) {
                fillCircle(xPos, yPos, blob.radius.roundToInt(), color)
            } else {
                setPixelColor(xPos, yPos, color)
            }

            blob.x = when {
                blob.x + blob.radius >= combinedWidth - 1 -> {
                    blob.x + blob.speedX * ((combinedWidth - 1 - blob.x) / blob.radius + 0.005f)
                }
                blob.x - blob.radius <= 0 -> {
                    blob.x + blob.speedX * (blob.x / blob.radius + 0.005f)
                }
                else -> blob.x + blob.speedX
            }

            blob.y = when {
                blob.y + blob.radius >= combinedHeight - 1 -> {
                    blob.y + blob.speedY * ((combinedHeight - 1 - blob.y) / blob.radius + 0.005f)
                }
                blob.y - blob.radius <= 0 -> {
                    blob.y + blob.speedY * (blob.y / blob.radius + 0.005f)
                }
                else -> blob.y + blob.speedY
            }

            if (blob.x < 0.01f) {
                val speedDiv = max(1, 256 - speed).toFloat()
                blob.speedX = (random.nextInt(combinedWidth.coerceAtLeast(4) - 3) + 3) / speedDiv
                blob.x = 0.01f
            } else if (blob.x > combinedWidth - 1.01f) {
                val speedDiv = max(1, 256 - speed).toFloat()
                blob.speedX = -((random.nextInt(combinedWidth.coerceAtLeast(4) - 3) + 3) / speedDiv)
                blob.x = combinedWidth - 1.01f
            }

            if (blob.y < 0.01f) {
                val speedDiv = max(1, 256 - speed).toFloat()
                blob.speedY = (random.nextInt(combinedHeight.coerceAtLeast(4) - 3) + 3) / speedDiv
                blob.y = 0.01f
            } else if (blob.y > combinedHeight - 1.01f) {
                val speedDiv = max(1, 256 - speed).toFloat()
                blob.speedY = -((random.nextInt(combinedHeight.coerceAtLeast(4) - 3) + 3) / speedDiv)
                blob.y = combinedHeight - 1.01f
            }
        }

        val blurAmount = custom1 shr 2
        if (blurAmount > 0) {
            applyBlur(blurAmount)
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

    override fun getName(): String = "Blobs"

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

    private fun fillCircle(centerX: Int, centerY: Int, radius: Int, rgb: IntArray) {
        if (radius <= 0) {
            setPixelColor(centerX, centerY, rgb)
            return
        }

        val minX = max(0, centerX - radius)
        val maxX = min(combinedWidth - 1, centerX + radius)
        val minY = max(0, centerY - radius)
        val maxY = min(combinedHeight - 1, centerY + radius)
        val radiusSq = radius * radius

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val dx = x - centerX
                val dy = y - centerY
                val distSq = dx * dx + dy * dy
                if (distSq <= radiusSq) {
                    addPixelColor(x, y, rgb)
                }
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
            pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
            pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
        }
    }

    private fun colorFromPalette(hue: Int, wrap: Boolean, @Suppress("UNUSED_PARAMETER") blend: Boolean, brightness: Int): IntArray {
        val adjustedHue = if (wrap) hue else hue % 256
        val h = (adjustedHue % 256) / 255.0f * 360.0f
        val s = 1.0f
        val v = if (brightness == 0) 1.0f else brightness / 255.0f
        return hsvToRgb(h, s, v)
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val hi = ((h / 60.0f) % 6).toInt()
        val f = h / 60.0f - hi
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)

        val (r, g, b) = when (hi) {
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
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    private class Blob {
        var x: Float = 0f
        var y: Float = 0f
        var speedX: Float = 0f
        var speedY: Float = 0f
        var radius: Float = 1f
        var grow: Boolean = false
        var color: Int = 0
    }

    companion object {
        private const val MAX_BLOBS = 8
    }
}

