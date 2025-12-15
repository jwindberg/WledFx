package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import java.util.Random
import kotlin.math.sqrt

/**
 * Bouncing ball animation with random behavior.
 */
class BouncingBallAnimation : BaseAnimation() {

    private var ballX: Double = 0.0
    private var ballY: Double = 0.0
    private var velocityX: Double = 0.0
    private var velocityY: Double = 0.0
    private var ballRadius: Int = 2
    
    private val RANDOM = Random()
    private val BOUNCE_DAMPING = 0.95
    private val RANDOM_CHANGE_PROBABILITY = 0.02
    private val MAX_TRAIL = 5

    override fun getName(): String = "Bouncing Ball"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        ballX = width / 2.0 + (RANDOM.nextDouble() - 0.5) * 10
        ballY = height / 2.0 + (RANDOM.nextDouble() - 0.5) * 10
        velocityX = (RANDOM.nextDouble() - 0.5) * 4
        velocityY = (RANDOM.nextDouble() - 0.5) * 4
        ballRadius = 1
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
        } else if (ballX >= width - ballRadius) {
            ballX = (width - ballRadius - 1).toDouble()
            velocityX = -velocityX * BOUNCE_DAMPING
        }

        if (ballY < ballRadius) {
            ballY = ballRadius.toDouble()
            velocityY = -velocityY * BOUNCE_DAMPING
        } else if (ballY >= height - ballRadius) {
            ballY = (height - ballRadius - 1).toDouble()
            velocityY = -velocityY * BOUNCE_DAMPING
        }
        
        // Speed check?
        // Original logic didn't use speed param.
        // We can use paramSpeed to scale velocity if we want, but let's stick to original behavior for now
        // Or constrain logic.

        velocityX = velocityX.coerceIn(-8.0, 8.0)
        velocityY = velocityY.coerceIn(-8.0, 8.0)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val dx = x - ballX
        val dy = y - ballY
        val distance = sqrt(dx * dx + dy * dy)

        return when {
            distance <= ballRadius -> paramColor
            distance <= ballRadius + MAX_TRAIL -> {
                val trailDistance = distance - ballRadius
                val factor = (1.0 - trailDistance / MAX_TRAIL).coerceIn(0.0, 1.0)
                ColorUtils.scaleBrightness(paramColor, factor)
            }
            else -> RgbColor.BLACK
        }
    }
}
