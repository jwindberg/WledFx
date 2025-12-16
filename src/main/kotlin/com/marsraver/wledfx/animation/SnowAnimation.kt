package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.physics.ParticleSystem
import kotlin.random.Random
import kotlin.math.sin

/**
 * Snow Animation
 * Gentle drifting snowflakes.
 */
class SnowAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // System for Snowflakes
    private val particleSystem = ParticleSystem(100) 
    
    // Time tracker for drift
    private var time = 0.0

    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true
    override fun getName(): String = "Snow"
    

    override fun getDefaultPaletteName(): String = "Ice"

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        particleSystem.clear()
        time = 0.0
    }
    
    override fun update(now: Long): Boolean {
        // Fade background slowly (trails? Snow usually doesn't have trails, but maybe slight blur)
        // Or clear/fade hard for crisp snow.
        
        // Let's do a hard fade (or clear) to assume fresh flakes each frame if we draw them as points
        // But if they move slowly, we want to clear previous positions.
        // fadeToBlack(255) // Clear completely? 
        // Actually, let's just fade heavily so we don't have trails.
        fadeToBlack(128)
        
        time += 0.01 + (paramSpeed / 2048.0)

        // Spawn Snow
        // Intensity controls density/spawn rate
        val spawnChance = (paramIntensity / 255.0) * 0.5
        if (Random.nextDouble() < spawnChance) {
            spawnSnowflake()
        }
        
        // Manual Update for drift logic
        val iterator = particleSystem.particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            // Custom Drift Logic
            // Use p.data1 as a random phase offset
            val drift = (sin(time + p.data1) * 0.2).toFloat()
            p.x += drift
            
            // Apply Gravity (vx/vy/ax/ay)
            p.update() 
            
            // Wrap X?
            if (p.x < 0) p.x += width
            if (p.x >= width) p.x -= width
            
            // Remove if off bottom
            if (p.y >= height) {
                iterator.remove()
                continue
            }
            
            // Draw
            val x = p.x.toInt()
            val y = p.y.toInt()
            if (x in 0 until width && y in 0 until height) {
                 // Pixel art snow: vary brightness/color slightly?
                 // p.color is usually White
                 // randomize brightness slightly based on depth (size)
                 pixelColors[x][y] = p.color
            }
        }
        
        return true
    }
    
    private fun spawnSnowflake() {
        val x = Random.nextFloat() * width
        val y = -1.0f // Start just above
        
        // Fall speed
        val speed = 0.05f + (paramSpeed / 255.0f) * 0.2f
        
        // Color (White or bluish?)
        // Use palette if we want colored snow, otherwise white
        val color = if (getPalette() != null) getColorFromPalette(Random.nextInt(256)) else RgbColor.WHITE
        
        particleSystem.spawn(
            x = x,
            y = y,
            vx = 0f,
            vy = speed,
            color = color,
            life = 1000f
        )?.apply {
            data1 = Random.nextFloat() * 100f // Random phase for drift
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
