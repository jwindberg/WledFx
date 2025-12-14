package com.marsraver.wledfx.strandanimation

import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.wled.model.Strand
import com.marsraver.wledfx.wled.model.StrandAnimation
import kotlin.random.Random

/**
 * Fireworks animation: particle system with bursts that explode outward,
 * flash white on ignition, and fade over time.
 * 
 * Based on FireworksEffect by Davepl.
 */
class FireworksAnimation : StrandAnimation {
    
    /**
     * Particle in the particle system.
     * Tracks position, velocity, color, and lifecycle timing.
     */
    private class Particle(
        val starColor: RgbColor,
        var position: Double,
        var velocity: Double
    ) {
        val birthTime: Long = System.currentTimeMillis()
        var lastUpdate: Long = System.currentTimeMillis()
        
        /**
         * Age of the particle in seconds.
         */
        val age: Double
            get() = (System.currentTimeMillis() - birthTime) / 1000.0
        
        /**
         * Update particle physics: position and velocity.
         */
        fun update() {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdate) / 1000.0
            
            // Update position based on velocity
            position += velocity * deltaTime
            
            // Apply damping to velocity (friction/gravity effect)
            velocity -= (2.0 * velocity * deltaTime)
            
            lastUpdate = currentTime
        }
    }
    
    // Animation parameters
    private val maxSpeed = 375.0                    // Max particle velocity
    private val newParticleProbability = 0.01       // Odds of new burst per frame
    private val particleIgnition = 0.2              // White flash duration (seconds)
    private val particleFadeTime = 2.0              // Fade out duration (seconds)
    
    // Particle storage
    private val particles = mutableListOf<Particle>()
    private val random = Random.Default
    
    override fun init(strand: Strand) {
        particles.clear()
    }
    
    override fun draw(strand: Strand) {
        // Randomly create new firework bursts
        // Number of attempts scales with strand size to maintain effect density
        for (pass in 0 until strand.length / 50) {
            if (random.nextDouble() < newParticleProbability) {
                spawnFireworkBurst(strand.length)
            }
        }
        
        // Limit total particles to prevent unbounded growth
        while (particles.size > strand.length) {
            particles.removeAt(0)
        }
        
        // Start with blank canvas
        strand.clear()
        
        // Update and render each particle
        for (particle in particles) {
            particle.update()
            
            // Determine particle color based on lifecycle stage
            val color = when {
                // Ignition phase: flash white
                particle.age < particleIgnition -> RgbColor.WHITE
                
                // Fade phase: gradually fade to black
                else -> {
                    val fadeAge = particle.age - particleIgnition
                    val fade = if (fadeAge > particleFadeTime) {
                        1.0  // Fully faded
                    } else {
                        fadeAge / particleFadeTime  // Fading
                    }
                    
                    // Scale brightness based on fade amount
                    ColorUtils.scaleBrightness(particle.starColor, 1.0 - fade)
                }
            }
            
            // Draw the particle at its position
            val pixelIndex = particle.position.toInt()
            if (pixelIndex in 0 until strand.length) {
                // Blend with existing pixel for overlapping particles
                val existing = strand.get(pixelIndex)
                val blended = RgbColor(
                    (existing.r + color.r).coerceAtMost(255),
                    (existing.g + color.g).coerceAtMost(255),
                    (existing.b + color.b).coerceAtMost(255)
                )
                strand.set(pixelIndex, blended)
            }
        }
        
        // Remove particles that have completed their lifecycle
        particles.removeAll { it.age > particleIgnition + particleFadeTime }
    }
    
    /**
     * Spawn a burst of particles at a random position.
     */
    private fun spawnFireworkBurst(strandLength: Int) {
        // Pick random position and color
        val startPos = random.nextDouble() * strandLength
        val hue = random.nextInt(256)
        val color = ColorUtils.hsvToRgb(hue, 255, 255)
        
        // Create 10-50 particles with random velocities
        val particleCount = random.nextInt(10, 51)
        val speedMultiplier = random.nextDouble() * 3.0
        
        for (i in 0 until particleCount) {
            // Random velocity in both directions (positive and negative)
            val velocity = (random.nextDouble() * 2.0 - 1.0) * maxSpeed * speedMultiplier
            particles.add(Particle(color, startPos, velocity))
        }
    }
}
