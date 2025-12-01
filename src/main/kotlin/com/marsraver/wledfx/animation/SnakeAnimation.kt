package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

/**
 * Snake animation that travels across the grid.
 */
class SnakeAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var snakePosition: Int = 0
    private var currentColor: RgbColor = RgbColor(0, 255, 0)

    override fun supportsColor(): Boolean = true

    override fun setColor(color: RgbColor) {
        currentColor = color
    }

    override fun getColor(): RgbColor? {
        return currentColor
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        snakePosition = 0
    }

    override fun update(now: Long): Boolean {
        snakePosition = (snakePosition + 1) % (combinedWidth * combinedHeight)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val ledIndex = y * combinedWidth + x
        val distance = kotlin.math.abs(ledIndex - snakePosition)

        return when {
            distance == 0 -> currentColor // Head - use selected color
            distance <= 5 -> {
                val intensity = (255 - distance * 40).coerceIn(0, 255)
                val factor = intensity / 255.0
                ColorUtils.scaleBrightness(currentColor, factor)
            }
            else -> RgbColor.BLACK
        }
    }

    override fun getName(): String = "Snake"
}

