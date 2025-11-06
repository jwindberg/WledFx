package com.marsraver.WledFx.animation

import kotlin.math.sqrt

/**
 * Color test animation with red, green, and blue circles on different grids.
 */
class ColorTestAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
    }

    override fun update(now: Long): Boolean = true

    override fun getPixelColor(x: Int, y: Int): IntArray {
        if (x < 16 && y < 16) {
            val distance = distance(x, y, 8, 8)
            if (distance < 6) return intArrayOf(255, 0, 0)
        }

        if (x in 16 until 32 && y < 16) {
            val distance = distance(x - 16, y, 8, 8)
            if (distance < 6) return intArrayOf(0, 255, 0)
        }

        if (x < 16 && y in 16 until 32) {
            val distance = distance(x, y - 16, 8, 8)
            if (distance < 6) return intArrayOf(0, 0, 255)
        }

        return intArrayOf(0, 0, 0)
    }

    override fun getName(): String = "Color Test"

    private fun distance(x: Int, y: Int, centerX: Int, centerY: Int): Double {
        val dx = x - centerX
        val dy = y - centerY
        return sqrt((dx * dx + dy * dy).toDouble())
    }
}

