package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import java.util.Random
import kotlin.math.*

/**
 * Crazy Bees animation - Bees flying to random targets.
 */
class CrazyBeesAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var bees: Array<Bee>
    private var numBees: Int = 0
    private var lastUpdateTime: Long = 0
    private var updateInterval: Long = 0
    
    private val RANDOM = kotlin.random.Random.Default
    private val MAX_BEES = 5

    override fun getName(): String = "Crazy Bees"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        lastUpdateTime = 0

        numBees = min(MAX_BEES, (width * height) / 256 + 1)
        bees = Array(numBees) { Bee() }

        // RANDOM.setSeed(System.currentTimeMillis()) // Kotlin Random default is seeded
        for (i in 0 until numBees) {
            val bee = bees[i]
            bee.posX = RANDOM.nextInt(width)
            bee.posY = RANDOM.nextInt(height)
            bee.setAim(width, height)
        }

        val speedFactor = (paramSpeed shr 4) + 1
        updateInterval = 16_000_000L * 16L / speedFactor
    }

    override fun update(now: Long): Boolean {
        if (now - lastUpdateTime < updateInterval) return true
        lastUpdateTime = now

        val fadeAmount = 32
        val blurAmount = 10
        fadeToBlack(fadeAmount)
        applyBlur(blurAmount)

        for (i in 0 until numBees) {
            val bee = bees[i]

            val flowerColor = getColorFromHue(bee.hue, 255)
            // Draw flower (cross)
            addPixelColor(bee.aimX + 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY + 1, flowerColor)
            addPixelColor(bee.aimX - 1, bee.aimY, flowerColor)
            addPixelColor(bee.aimX, bee.aimY - 1, flowerColor)

            if (bee.posX != bee.aimX || bee.posY != bee.aimY) {
                // Draw bee
                val beeColor = getColorFromHue(bee.hue, 200) 
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
                bee.setAim(width, height)
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
    
    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }

        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0; var sumG = 0; var sumB = 0; var count = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val c = pixelColors[nx][ny]
                            sumR += c.r; sumG += c.g; sumB += c.b
                            count++
                        }
                    }
                }
                val current = pixelColors[x][y]
                val br = if (count > 0) sumR/count else current.r
                val bg = if (count > 0) sumG/count else current.g
                val bb = if (count > 0) sumB/count else current.b
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - amount) + br * amount) / 255,
                    (current.g * (255 - amount) + bg * amount) / 255,
                    (current.b * (255 - amount) + bb * amount) / 255
                )
            }
        }
        for (x in 0 until width) for (y in 0 until height) pixelColors[x][y] = temp[x][y]
    }

    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) pixelColors[x][y] = rgb
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
        val base = getColorFromPalette(hue)
        if (brightness < 255) return ColorUtils.scaleBrightness(base, brightness / 255.0)
        return base
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

        fun setAim(w: Int, h: Int) {
            aimX = RANDOM.nextInt(w)
            aimY = RANDOM.nextInt(h)
            hue = RANDOM.nextInt(256)
            deltaX = abs(aimX - posX)
            deltaY = abs(aimY - posY)
            signX = if (posX < aimX) 1 else -1
            signY = if (posY < aimY) 1 else -1
            error = deltaX - deltaY
        }
    }
}
