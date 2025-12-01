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
 * Crazy Bees animation - Bees flying to random targets with flowers.
 */
class CrazyBeesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<RgbColor>> = emptyArray()
    private var currentPalette: Palette? = null
    private lateinit var bees: Array<Bee>
    private var numBees: Int = 0
    private var lastUpdateTime: Long = 0
    private var updateInterval: Long = 0

    private var fadeAmount: Int = 32
    private var blurAmount: Int = 10
    private var speed: Int = 128

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

            val flowerColor = getColorFromHue(bee.hue, 255)
            addPixelColor(bee.aimX + 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY + 1, flowerColor)
            addPixelColor(bee.aimX - 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY - 1, flowerColor)

            if (bee.posX != bee.aimX || bee.posY != bee.aimY) {
                val beeColor = getColorFromHue(bee.hue, 200) // Slightly dimmer for bees
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

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Crazy Bees"

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

