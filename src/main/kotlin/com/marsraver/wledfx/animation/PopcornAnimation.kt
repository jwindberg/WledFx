package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.physics.ParticleSystem
import kotlin.random.Random

/**
 * Popcorn Animation
 * Kernels "pop" upwards and fall back down with gravity.
 */
class PopcornAnimation : BaseAnimation() {

    private val particleSystem = ParticleSystem(200)
    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var accumulatedTime = 0.0

    override fun getName(): String = "Popcorn"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        particleSystem.clear()
        accumulatedTime = 0.0
    }

    override fun update(now: Long): Boolean {
        fadeToBlack(40) // Fade trails

        // Spawn logic
        // Use paramSpeed/Intensity to control pop rate?
        // Let's use paramSpeed for gravity/speed and paramIntensity for spawn rate.
        
        val spawnRate = (paramIntensity / 255.0) * 0.5 // pops per frame-ish
        accumulatedTime += spawnRate
        
        if (accumulatedTime >= 1.0) {
            val pops = accumulatedTime.toInt()
            accumulatedTime -= pops
            repeat(pops) { spawnPop() }
        } else if (Random.nextDouble() < spawnRate) {
             // Statistical chance for slower rates
             spawnPop()
        }

        // Gravity
        val gravity = 0.05f + (paramSpeed / 255.0f) * 0.05f

        // Update physics
        val particles = particleSystem.particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            // Apply gravity manually or via particle system if it had global forces (it keeps local acc)
            // We can set 'ay' on spawn, but if we want variable gravity we update it here.
            // Let's set 'ay' on spawn for consistency with the system.
            
            p.update()
            
            // Floor bounce? Or just die?
            // "Popcorn" usually stays in the bucket?
            // Let's make them bounce once then die?
            // Or just fall through. WLED popcorn usually pop up and fall.
            // Let's simple fall through for now, or bounce at bottom.
            if (p.y >= height) {
                // Bounce
                p.y = height - 0.1f
                p.vy *= -0.6f // Dampening
                p.life -= 0.3f // Lose "health" on bounce
            }
            
            // Check death
            p.life -= 0.01f
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }

            // Draw
            if (p.x >= 0 && p.x < width && p.y >= 0 && p.y < height) {
                 // Color depends on velocity?
                 // High upward velocity = White/Yellow (Hot)
                 // Falling = Red/Darker (Cooling)
                 
                 val velocityColor = if (p.vy < 0) {
                     // Moving UP (negative Y) -> Hot
                     ColorUtils.blend(p.color, RgbColor.WHITE, 128)
                 } else {
                     // Moving DOWN -> Normal/Cool
                      p.color
                 }
                 
                 // Apply brightness based on life
                 val color = ColorUtils.scaleBrightness(velocityColor, p.life.toDouble())
                 setPixelColor(p.x.toInt(), p.y.toInt(), color)
            }
        }
        
        return true
    }
    
    private fun spawnPop() {
        val x = Random.nextFloat() * width
        val y = height - 1.0f // Spawn at bottom
        
        // Initial velocity UP (negative Y)
        val speed = Random.nextFloat() * 1.5f + 1.0f
        val vx = (Random.nextFloat() - 0.5f) * 0.5f // Slight horizontal drift
        
        // Random color
        val color = getColorFromPalette(Random.nextInt(256))
        
        particleSystem.spawn(
            x = x,
            y = y,
            vx = vx,
            vy = -speed,
            ax = 0f,
            ay = 0.05f + (paramSpeed / 255.0f) * 0.05f, // Gravity
            color = color,
            life = 1.0f
        )
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
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
    
    override fun cleanup() {
        particleSystem.clear()
    }
}
