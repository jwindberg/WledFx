package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.physics.ParticleSystem
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * Rain animation - drifting rainbow droplets falling across the matrix.
 * Refactored to use ParticleSystem.
 */
class RainAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    // ParticleSystem: x, y, vx, vy, etc.
    private val particleSystem = ParticleSystem(200)

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        particleSystem.clear()
        // Default values
        paramSpeed = 128
        paramIntensity = 128
    }

    override fun update(now: Long): Boolean {
        // Fade trails
        // Intensity controls trail length? Higher intensity = less fade = longer trail?
        // Original used fixed 24.
        val fadeAmt = (255 - paramIntensity).coerceIn(10, 100)
        fadeToBlack(fadeAmt)

        // Spawn drops
        // Rate depends on width and speed
        val spawnChance = (width / 32.0) * (paramSpeed / 255.0) + 0.05
        if (Random.nextDouble() < spawnChance) {
            spawnDrop()
        }

        // Update Physics
        // We can iterate manually to draw trails or heads
        val iterator = particleSystem.particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            // Apply speed scaling?
            // ParticleSystem moves by vx/vy each frame (assuming 60fps or delta).
            // BaseAnimation update is called ~60fps often.
            // Let's assume vy is pixels/frame.
            
            // Update position
            p.update()
            
            // Draw
            val x = p.x.roundToInt()
            val y = p.y.roundToInt()
            
            if (y >= height) {
                iterator.remove()
                continue
            }
            
            if (x in 0 until width && y in 0 until height) {
                // Hue is stored in data1
                val hue = p.data1.toInt()
                
                // Head color
                val color = getColorFromHue(hue, 255)
                addPixelColor(x, y, color)
                
                // Tail (previous position?)
                // Since we faded, the previous pixel is already there but faded.
                // We can draw a pixel above it to smoothe it?
                if (y > 0) {
                     // Maybe draw a dimmer pixel above to connect?
                     // Not strictly necessary if we fade.
                }
            }
        }
        
        return true
    }

    private fun spawnDrop() {
        // x random
        val x = Random.nextDouble() * width
        val y = -Random.nextDouble(0.0, 3.0) // Start above
        
        // velocity (vy)
        // Original: 6-11 units/sec? No, units/frame?
        // Let's use 0.3 - 0.8 pixels/frame
        val speed = 0.2f + (paramSpeed / 255.0f) * 0.8f
        
        // Hue
        // We can cycle hue or random
        val hue = Random.nextInt(256).toFloat()
        
        particleSystem.spawn(
            x = x.toFloat(),
            y = y.toFloat(),
            vx = 0f,
            vy = speed,
            life = 100f
        )?.apply {
            data1 = hue // Store hue
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

    private fun getColorFromHue(hue: Int, brightness: Int): RgbColor {
        val base = getColorFromPalette(hue)
        val brightnessFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    override fun getName(): String = "Rain"
    override fun cleanup() {
        particleSystem.clear()
    }
    override fun is1D() = false
    override fun is2D() = true
}
