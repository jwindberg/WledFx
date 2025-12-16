package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.physics.ParticleSystem
import kotlin.random.Random

/**
 * Drip Animation
 * Water/paint drips from top, accumulates at bottom or ripples.
 */
class DripAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // System for Drips
    private val particleSystem = ParticleSystem(50) 
    
    // State for source "leaks"
    private var nextDripTime = 0L

    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true
    override fun getName(): String = "Drip"
    
    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        particleSystem.clear()
        nextDripTime = System.currentTimeMillis()
    }
    
    override fun update(now: Long): Boolean {
        // Fade trails slightly for "wet" look
        val fadeAmount = 32
        fadeToBlack(fadeAmount)
        
        // Spawn Drips
        val nowMs = System.currentTimeMillis()
        if (nowMs > nextDripTime) {
            spawnDrip()
            // Random delay between drips based on speed
            // Higher speed = fewer delays = more drips
            val speedDelay = (255 - paramSpeed) * 5 
            nextDripTime = nowMs + Random.nextLong(100L, 200L + speedDelay)
        }
        
        particleSystem.update()
        
        // Draw & Logic
        val iterator = particleSystem.particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            // Draw Drip (Head)
            if (p.x >= 0 && p.x < width && p.y >= 0 && p.y < height) {
                 val x = p.x.toInt()
                 val y = p.y.toInt()
                 
                 // Add color (water/paint accum)
                 addPixelColor(x, y, p.color)
            }
            
            // Logic: Splash at bottom
            if (p.y >= height - 1) {
                // Splash effect
                val cx = p.x.toInt()
                val cy = (height - 1).coerceAtLeast(0)
                
                // create splash ripple pixels?
                // For now, just light up the bottom row a bit around it
                if (cx >= 0 && cx < width) {
                    addPixelColor(cx, cy, p.color)
                    if (cx > 0) addPixelColor(cx - 1, cy, ColorUtils.scaleBrightness(p.color, 0.5))
                    if (cx < width - 1) addPixelColor(cx + 1, cy, ColorUtils.scaleBrightness(p.color, 0.5))
                }

                iterator.remove() // Drip absorbed
            }
        }
        
        return true
    }
    
    private fun spawnDrip() {
        // Random x location
        val x = Random.nextFloat() * width
        val y = 0f
        
        // Initial velocity 0, accelerates down
        // Gravity depends on speed? Or fixed?
        // Let's make gravity fixed but 'viscosity' depends on something?
        // WLED Drip is usually gravity driven.
        
        // Color: Palette or Blue/White defaults via palette fallback
        val color = getColorFromPalette(Random.nextInt(256))
        
        particleSystem.spawn(
            x = x,
            y = y,
            vx = 0f,
            vy = 0f,
            ax = 0f,
            ay = 0.05f + (paramSpeed / 255.0f) * 0.1f, // Gravity
            color = color
        )
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
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    override fun cleanup() {
        particleSystem.clear()
    }
}
