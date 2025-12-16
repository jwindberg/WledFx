package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.physics.ParticleSystem
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Starburst Animation
 * Particles explode radially from a center point and fade out.
 * No gravity, just pure radial moment using the ParticleSystem.
 */
class StarburstAnimation : BaseAnimation() {

    private val particleSystem = ParticleSystem(250)
    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // Properties
    // starbursts occur randomly
    private var nextBurstTime = 0L

    override fun getName(): String = "Starburst"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        particleSystem.clear()
        nextBurstTime = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        // 1. Fade existing trails
        // Intensity controls trail length (less fade = longer trail)
        // Map intensity 0-255 to fade 255-0?
        // Standard fade: ~32 is good.
        // Let's make it configurable via intensity.
        // High intensity = fast fade (short trails) or low fade (long trails)?
        // Usually, intensity = brightness/power.
        // Let's use fixed fade for now or slight modulation.
        val fade = 40
        fadeToBlack(fade)

        // 2. Spawn new bursts
        val nowMs = System.currentTimeMillis()
        if (nowMs > nextBurstTime) {
            spawnBurst()
            // Speed controls frequency of bursts
            // paramSpeed: 0 (slow) to 255 (fast)
            // delay: high speed -> low delay
            val minDelay = 100
            val maxDelay = 2000
            val speedFactor = paramSpeed / 255.0
            val delay = (maxDelay - (maxDelay - minDelay) * speedFactor).toLong()
            nextBurstTime = nowMs + delay // + Random deviation?
        }

        // 3. Update physics
        particleSystem.update()

        // 4. Draw particles
        val particles = particleSystem.particles
        if (particles.isEmpty()) return true

        for (p in particles) {
            // Check bounds
            if (p.x >= 0 && p.x < width && p.y >= 0 && p.y < height) {
                // Color calculation:
                // Fade color over life?
                // p.life goes 1.0 -> 0.0?
                // Or just use the stored color.
                
                // Let's use life to control brightness
                val lifeBrightness = p.life // 1.0 to 0.0
                val color = ColorUtils.scaleBrightness(p.color, lifeBrightness.toDouble())
                
                // Additive blending for "glowing" core effect
                addPixelColor(p.x.toInt(), p.y.toInt(), color)
            }
            
            // Logic update for specific particle needs (drag?)
            // Apply drag to slow them down gracefully
            p.vx *= 0.95f
            p.vy *= 0.95f
            p.life -= 0.02f // Die over time
        }

        return true
    }
    
    private fun spawnBurst() {
        // Random center
        val cx = Random.nextFloat() * width
        val cy = Random.nextFloat() * height
        
        // Random color from palette or random hue
        val burstColorIndex = Random.nextInt(256)
        val burstColor = getColorFromPalette(burstColorIndex)
        
        // Number of particles per burst
        val particleCount = Random.nextInt(10, 30)
        
        for (i in 0 until particleCount) {
            val angle = Random.nextFloat() * 6.28318f // 0 to 2PI
            val speed = Random.nextFloat() * 1.5f + 0.5f // random speed
            
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            
            particleSystem.spawn(
                x = cx,
                y = cy,
                vx = vx,
                vy = vy,
                life = 1.0f + Random.nextFloat() * 0.5f, // Randomize life slightly
                color = burstColor
            )
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
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
