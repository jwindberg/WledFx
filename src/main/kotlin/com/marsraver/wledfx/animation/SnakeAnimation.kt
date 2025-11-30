package com.marsraver.wledfx.animation

/**
 * Snake animation that travels across the grid.
 */
class SnakeAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var snakePosition: Int = 0
    private var currentColor: IntArray = intArrayOf(0, 255, 0)

    override fun supportsColor(): Boolean = true

    override fun setColor(r: Int, g: Int, b: Int) {
        currentColor = intArrayOf(r, g, b)
    }

    override fun getColor(): IntArray {
        return currentColor.clone()
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

    override fun getPixelColor(x: Int, y: Int): IntArray {
        val ledIndex = y * combinedWidth + x
        val distance = kotlin.math.abs(ledIndex - snakePosition)

        return when {
            distance == 0 -> currentColor.clone() // Head - use selected color
            distance <= 5 -> {
                val intensity = (255 - distance * 40).coerceIn(0, 255)
                val factor = intensity / 255.0
                intArrayOf(
                    (currentColor[0] * factor).toInt().coerceIn(0, 255),
                    (currentColor[1] * factor).toInt().coerceIn(0, 255),
                    (currentColor[2] * factor).toInt().coerceIn(0, 255)
                )
            }
            else -> intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Snake"
}

