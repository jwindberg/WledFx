package com.marsraver.wledfx.animation

/**
 * Snake animation that travels across the grid.
 */
class SnakeAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var snakePosition: Int = 0

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
            distance == 0 -> intArrayOf(0, 255, 0) // Head - bright green
            distance <= 5 -> {
                val intensity = (255 - distance * 40).coerceIn(0, 255)
                intArrayOf(0, intensity, 0)
            }
            else -> intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Snake"
}

