package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.random.Random
import kotlin.math.min

/**
 * Starburst animation - Explosive particle bursts
 * By: DedeHai (Damian Schneider)
 */
class StarburstAnimation : LedAnimation {

    private data class Particle(
        var x: Float = 0.0f,
        var y: Float = 0.0f,
        var vx: Float = 0.0f,
        var vy: Float = 0.0f,
        var life: Int = 0,
        var maxLife: Int = 0,
        var hue: Int = 0,
        var sat: Int = 0,
        var size: Int = 0
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 150
    private var intensity: Int = 150
    private var custom1: Int = 120  // Fragment size
    private var custom2: Int = 0    // Motion blur
    private var custom3: Int = 21   // Cooling
    private var check1: Boolean = false  // Gravity
    private var check2: Boolean = false  // Colorful
    private var check3: Boolean = false  // Push
    
    private val particles = mutableListOf<Particle>()
    private var sourceTtl: Int = 1
    private var sourceHue: Int = 0
    private val random = Random.Default
    private var startTimeNs: Long = 0L
    private var callCount: Int = 0

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        callCount = 0
        sourceTtl = 1
        sourceHue = 0
        particles.clear()
    }

    override fun update(now: Long): Boolean {
        callCount++
        
        // Fade out for motion blur
        fadeOut(custom2)
        
        val gravity = if (check1) 8.0f / 10.0f else 0.0f
        
        // Check if it's time for new explosion
        if (sourceTtl > 0) {
            sourceTtl--
        } else {
            // Create new explosion
            val explosionSize = 4 + random.nextInt(intensity shr 2)
            sourceHue = random.nextInt(256)
            val varSpeed = 10 + (explosionSize shl 1)
            val explosionX = random.nextInt(combinedWidth).toFloat()
            val explosionY = random.nextInt(combinedHeight).toFloat()
            
            sourceTtl = 10 + random.nextInt(255 - speed)
            
            // Emit particles
            for (e in 0 until explosionSize) {
                if (check2) {
                    sourceHue = random.nextInt(256)
                }
                
                val particle = Particle()
                particle.x = explosionX
                particle.y = explosionY
                
                // Random direction
                val angle = random.nextFloat() * 2.0f * kotlin.math.PI.toFloat()
                val speed = (2.0f + random.nextFloat() * 3.0f) * 0.5f
                particle.vx = kotlin.math.cos(angle) * speed
                particle.vy = kotlin.math.sin(angle) * speed
                
                particle.life = 0
                particle.maxLife = 250 + random.nextInt(50)
                particle.hue = sourceHue
                particle.sat = 0
                particle.size = custom1
                
                particles.add(particle)
            }
        }
        
        // Update particles
        val particlesToRemove = mutableListOf<Int>()
        for (i in particles.indices) {
            val particle = particles[i]
            
            // Apply gravity
            if (gravity > 0) {
                particle.vy += gravity
            }
            
            // Move particle
            particle.x += particle.vx
            particle.y += particle.vy
            
            // Update life
            particle.life++
            if (particle.life >= particle.maxLife) {
                particlesToRemove.add(i)
                continue
            }
            
            // Shrink particle
            if (particle.size > 0) {
                particle.size--
            }
            
            // Increase saturation (cooling effect)
            if (particle.sat < 251) {
                particle.sat += 1 + (custom3 shr 2)
                particle.sat = min(255, particle.sat)
            }
            
            // Draw particle
            val x = particle.x.toInt().coerceIn(0, combinedWidth - 1)
            val y = particle.y.toInt().coerceIn(0, combinedHeight - 1)
            val brightness = ((particle.maxLife - particle.life) * 255 / particle.maxLife).coerceIn(0, 255)
            val color = colorFromPalette(particle.hue, true, brightness)
            addPixelColor(x, y, color)
            
            // Draw larger particles
            if (particle.size > 0) {
                for (dx in -particle.size..particle.size) {
                    for (dy in -particle.size..particle.size) {
                        if (dx * dx + dy * dy <= particle.size * particle.size) {
                            val px = (x + dx).coerceIn(0, combinedWidth - 1)
                            val py = (y + dy).coerceIn(0, combinedHeight - 1)
                            addPixelColor(px, py, color)
                        }
                    }
                }
            }
        }
        
        // Remove dead particles (in reverse order)
        for (i in particlesToRemove.reversed()) {
            particles.removeAt(i)
        }
        
        // Apply friction every 5 frames
        if (callCount % 5 == 0) {
            for (particle in particles) {
                particle.vx *= 0.95f
                particle.vy *= 0.95f
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Starburst"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    private fun fadeOut(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val existing = pixelColors[x][y]
            pixelColors[x][y] = ColorUtils.blend(existing, color, 200)
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = brightness.coerceIn(0, 255) / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(index, 255, brightness.coerceIn(0, 255))
        }
    }
}

