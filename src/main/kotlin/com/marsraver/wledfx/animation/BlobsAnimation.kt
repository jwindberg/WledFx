package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

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
    private var pixelColors: Array<Array<RgbColor>> = emptyArray()
    private var currentPalette: Palette? = null

    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 32
    private var custom2: Int = 32

    private lateinit var blobs: Array<Blob>
    private var amount: Int = 0
    private val random = Random()
    private var startTime: Long = 0
    private var lastColorChange: Long = 0

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

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Blobs"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    private fun fadeToBlack(amount: Int) {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val current = pixelColors[x][y]
                pixelColors[x][y] = RgbColor(
                    max(0, current.r - amount),
                    max(0, current.g - amount),
                    max(0, current.b - amount)
                )
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }

        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0

                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                            val color = pixelColors[nx][ny]
                            sumR += color.r
                            sumG += color.g
                            sumB += color.b
                            count++
                        }
                    }
                }

                val current = pixelColors[x][y]
                val blurredR = if (count > 0) sumR / count else current.r
                val blurredG = if (count > 0) sumG / count else current.g
                val blurredB = if (count > 0) sumB / count else current.b
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - amount) + blurredR * amount) / 255,
                    (current.g * (255 - amount) + blurredG * amount) / 255,
                    (current.b * (255 - amount) + blurredB * amount) / 255
                )
            }
        }

        pixelColors = temp
    }

    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = rgb
        }
    }

    private fun fillCircle(centerX: Int, centerY: Int, radius: Int, rgb: RgbColor) {
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

    private fun colorFromPalette(hue: Int, wrap: Boolean, @Suppress("UNUSED_PARAMETER") blend: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val adjustedHue = if (wrap) hue else hue % 256
            val paletteIndex = ((adjustedHue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessValue = if (brightness == 0) 255 else brightness
            val brightnessFactor = brightnessValue / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            val adjustedHue = if (wrap) hue else hue % 256
            val brightnessValue = if (brightness == 0) 255 else brightness
            return ColorUtils.hsvToRgb(adjustedHue, 255, brightnessValue)
        }
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

