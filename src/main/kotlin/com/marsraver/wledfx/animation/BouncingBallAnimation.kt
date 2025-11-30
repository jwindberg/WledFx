package com.marsraver.wledfx.animation

import java.util.Random
import kotlin.math.sqrt

/**
 * Bouncing ball animation with random behavior.
 */
class BouncingBallAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var ballX: Double = 0.0
    private var ballY: Double = 0.0
    private var velocityX: Double = 0.0
    private var velocityY: Double = 0.0
    private var ballRadius: Int = 2
    private var currentColor: IntArray = intArrayOf(0, 255, 255)

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

        ballX = combinedWidth / 2.0 + (RANDOM.nextDouble() - 0.5) * 10
        ballY = combinedHeight / 2.0 + (RANDOM.nextDouble() - 0.5) * 10

        velocityX = (RANDOM.nextDouble() - 0.5) * 4
        velocityY = (RANDOM.nextDouble() - 0.5) * 4
        ballRadius = 2
    }

    override fun update(now: Long): Boolean {
        if (RANDOM.nextDouble() < RANDOM_CHANGE_PROBABILITY) {
            velocityX += (RANDOM.nextDouble() - 0.5) * 2
            velocityY += (RANDOM.nextDouble() - 0.5) * 2
        }

        ballX += velocityX
        ballY += velocityY

        if (ballX < ballRadius) {
            ballX = ballRadius.toDouble()
            velocityX = -velocityX * BOUNCE_DAMPING
        } else if (ballX >= combinedWidth - ballRadius) {
            ballX = (combinedWidth - ballRadius - 1).toDouble()
            velocityX = -velocityX * BOUNCE_DAMPING
        }

        if (ballY < ballRadius) {
            ballY = ballRadius.toDouble()
            velocityY = -velocityY * BOUNCE_DAMPING
        } else if (ballY >= combinedHeight - ballRadius) {
            ballY = (combinedHeight - ballRadius - 1).toDouble()
            velocityY = -velocityY * BOUNCE_DAMPING
        }

        velocityX = velocityX.coerceIn(-8.0, 8.0)
        velocityY = velocityY.coerceIn(-8.0, 8.0)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        val dx = x - ballX
        val dy = y - ballY
        val distance = sqrt(dx * dx + dy * dy)

        return when {
            distance <= ballRadius -> currentColor.clone()
            distance <= ballRadius + MAX_TRAIL -> {
                val trailDistance = distance - ballRadius
                val factor = (1.0 - trailDistance / MAX_TRAIL).coerceIn(0.0, 1.0)
                intArrayOf(
                    (currentColor[0] * factor).toInt().coerceIn(0, 255),
                    (currentColor[1] * factor).toInt().coerceIn(0, 255),
                    (currentColor[2] * factor).toInt().coerceIn(0, 255)
                )
            }
            else -> intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Bouncing Ball"

    companion object {
        private val RANDOM = Random()
        private const val BOUNCE_DAMPING = 0.95
        private const val RANDOM_CHANGE_PROBABILITY = 0.02
        private const val MAX_TRAIL = 5
    }
}

