package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.abs

/**
 * Snake animation
 */
class SnakeAnimation : BaseAnimation() {

    private var snakePosition: Int = 0

    override fun getName(): String = "Snake"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false

    override fun onInit() {
        snakePosition = 0
    }

    override fun update(now: Long): Boolean {
        snakePosition = (snakePosition + 1) % pixelCount
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val ledIndex = getPixelIndex(x, y)
        val distance = abs(ledIndex - snakePosition)

        return when {
            distance == 0 -> paramColor
            distance <= 5 -> {
                val intensity = (255 - distance * 40).coerceIn(0, 255)
                val factor = intensity / 255.0
                ColorUtils.scaleBrightness(paramColor, factor)
            }
            else -> RgbColor.BLACK
        }
    }
}
