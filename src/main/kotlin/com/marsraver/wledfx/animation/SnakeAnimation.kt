package com.marsraver.wledfx.animation

/**
 * Snake animation that travels across the grid.
 */
class SnakeAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var snakePosition: Int = 0
    private var colorR: Int = 0
    private var colorG: Int = 255
    private var colorB: Int = 0

    override fun supportsColor(): Boolean = true

    override fun setColor(r: Int, g: Int, b: Int) {
        colorR = r
        colorG = g
        colorB = b
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
            distance == 0 -> intArrayOf(colorR, colorG, colorB) // Head - use selected color
            distance <= 5 -> {
                val intensity = (255 - distance * 40).coerceIn(0, 255)
                val factor = intensity / 255.0
                intArrayOf(
                    (colorR * factor).toInt().coerceIn(0, 255),
                    (colorG * factor).toInt().coerceIn(0, 255),
                    (colorB * factor).toInt().coerceIn(0, 255)
                )
            }
            else -> intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Snake"
}

