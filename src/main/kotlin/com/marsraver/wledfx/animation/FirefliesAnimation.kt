package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.*
import kotlin.random.Random

/**
 * Fireflies animation - Magical fireflies that pulse and move to music
 * Each firefly responds to a different frequency band
 */
class FirefliesAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private val random = Random.Default
    
    private data class Firefly(
        var x: Double,
        var y: Double,
        var vx: Double,
        var vy: Double,
        var targetX: Double,
        var targetY: Double,
        var brightness: Float,
        var pulsePhase: Double,
        var frequencyBand: Int,
        var color: RgbColor
    )
    
    private val fireflies = mutableListOf<Firefly>()
    
    private val fireflyColors = listOf(
        RgbColor(255, 220, 100),
        RgbColor(200, 255, 100),
        RgbColor(255, 200, 80),
        RgbColor(180, 255, 120),
        RgbColor(255, 230, 120)
    )
    
    // Background color (very dark night)
    private val nightColor = RgbColor(0, 0, 5)

    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        fftMeter = FftMeter(bands = 32)
        loudnessMeter = LoudnessMeter()
        
        initializeFireflies()
    }
    
    private fun initializeFireflies() {
        fireflies.clear()
        val numFireflies = min(16, max(8, (width * height) / 64))
        
        for (i in 0 until numFireflies) {
            val color = getFireflyColor(i)
            fireflies.add(
                Firefly(
                    x = random.nextDouble() * width,
                    y = random.nextDouble() * height,
                    vx = 0.0,
                    vy = 0.0,
                    targetX = random.nextDouble() * width,
                    targetY = random.nextDouble() * height,
                    brightness = 0.5f,
                    pulsePhase = random.nextDouble() * 2 * PI,
                    frequencyBand = i % 32,
                    color = color
                )
            )
        }
    }
    
    private fun getFireflyColor(index: Int): RgbColor {
        val palette = getPalette()?.colors
        return if (palette != null && palette.isNotEmpty()) {
            palette[index % palette.size]
        } else {
            fireflyColors[index % fireflyColors.size]
        }
    }
    
    private fun updateFireflyColors() {
        fireflies.forEachIndexed { index, firefly ->
            firefly.color = getFireflyColor(index)
        }
    }

    override fun update(now: Long): Boolean {
        // Param check or re-init logic if palette changed? 
        // BaseAnimation doesn't auto-notify. We can check if palette changed in update if needed, 
        // or just re-fetch in getFireflyColor? 
        // Efficient way: update colors every frame or on change.
        // Let's update colors every frame for safety, or assume static referencing.
        updateFireflyColors()

        val timeMs = (now - startTimeNs) / 1_000_000
        val timeSec = timeMs / 1000.0
        
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        
        val globalBrightness = (loudness / 1024.0 * 0.7 + 0.3).coerceIn(0.3, 1.0)
        
        drawBackground(timeSec)
        
        for (firefly in fireflies) {
            updateFirefly(firefly, bands, timeSec)
        }
        
        drawFireflies(globalBrightness)
        
        return true
    }
    
    private fun drawBackground(timeSec: Double) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val shimmer = sin(timeSec * 0.5 + x * 0.1 + y * 0.1) * 0.05 + 0.95
                val r = (nightColor.r * shimmer).toInt().coerceIn(0, 255)
                val g = (nightColor.g * shimmer).toInt().coerceIn(0, 255)
                val b = (nightColor.b * shimmer).toInt().coerceIn(0, 255)
                pixelColors[x][y] = RgbColor(r, g, b)
            }
        }
    }
    
    private fun updateFirefly(firefly: Firefly, bands: IntArray, timeSec: Double) {
        val bandValue = if (firefly.frequencyBand < bands.size) {
            bands[firefly.frequencyBand]
        } else {
            0
        }
        
        // Use paramSpeed
        val speedFactor = paramSpeed / 128.0

        val targetBrightness = (bandValue / 255.0 * 0.8 + 0.2).toFloat()
        firefly.brightness += (targetBrightness - firefly.brightness) * 0.1f
        firefly.pulsePhase += (0.05 + (bandValue / 255.0 * 0.1)) * speedFactor
        
        val dx = firefly.targetX - firefly.x
        val dy = firefly.targetY - firefly.y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance < 2.0) {
            firefly.targetX = random.nextDouble() * width
            firefly.targetY = random.nextDouble() * height
        }
        
        val acceleration = 0.02 * speedFactor
        val maxSpeed = (0.3 + (bandValue / 255.0 * 0.3)) * speedFactor
        
        if (distance > 0) {
            val ax = (dx / distance) * acceleration
            val ay = (dy / distance) * acceleration
            firefly.vx += ax
            firefly.vy += ay
        }
        
        val speed = sqrt(firefly.vx * firefly.vx + firefly.vy * firefly.vy)
        if (speed > maxSpeed) {
            firefly.vx = (firefly.vx / speed) * maxSpeed
            firefly.vy = (firefly.vy / speed) * maxSpeed
        }
        
        firefly.x += firefly.vx
        firefly.y += firefly.vy
        
        if (firefly.x < 0) firefly.x += width
        if (firefly.x >= width) firefly.x -= width
        if (firefly.y < 0) firefly.y += height
        if (firefly.y >= height) firefly.y -= height
    }
    
    private fun drawFireflies(globalBrightness: Double) {
        for (firefly in fireflies) {
            val pulse = (sin(firefly.pulsePhase) * 0.3 + 0.7).coerceIn(0.0, 1.0)
            val finalBrightness = firefly.brightness * pulse * globalBrightness
            
            val centerX = firefly.x.toInt().coerceIn(0, width - 1)
            val centerY = firefly.y.toInt().coerceIn(0, height - 1)
            
            drawGlowPixel(centerX, centerY, firefly.color, finalBrightness)
            
            val glowRadius = 2
            for (dx in -glowRadius..glowRadius) {
                for (dy in -glowRadius..glowRadius) {
                    if (dx == 0 && dy == 0) continue
                    val x = centerX + dx
                    val y = centerY + dy
                    if (x in 0 until width && y in 0 until height) {
                        val distance = sqrt((dx * dx + dy * dy).toDouble())
                        val glowStrength = (1.0 - distance / glowRadius).coerceAtLeast(0.0)
                        val glowBrightness = finalBrightness * glowStrength * 0.5
                        drawGlowPixel(x, y, firefly.color, glowBrightness)
                    }
                }
            }
        }
    }
    
    private fun drawGlowPixel(x: Int, y: Int, color: RgbColor, brightness: Double) {
        if (x !in 0 until width || y !in 0 until height) return
        val glowColor = ColorUtils.scaleBrightness(color, brightness)
        val existing = pixelColors[x][y]
        val r = (existing.r + glowColor.r).coerceAtMost(255)
        val g = (existing.g + glowColor.g).coerceAtMost(255)
        val b = (existing.b + glowColor.b).coerceAtMost(255)
        pixelColors[x][y] = RgbColor(r, g, b)
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Fireflies"
    override fun isAudioReactive(): Boolean = true
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
        fireflies.clear()
    }
}
