package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.*
import kotlin.random.Random

/**
 * Fireflies animation - Magical fireflies that pulse and move to music
 * Each firefly responds to a different frequency band
 */
class FirefliesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private val random = Random.Default
    
    // Firefly entity
    private data class Firefly(
        var x: Double,
        var y: Double,
        var vx: Double, // Velocity X
        var vy: Double, // Velocity Y
        var targetX: Double,
        var targetY: Double,
        var brightness: Float,
        var pulsePhase: Double,
        var frequencyBand: Int, // Which FFT band controls this firefly
        var color: RgbColor
    )
    
    private val fireflies = mutableListOf<Firefly>()
    
    // Firefly colors (warm bioluminescent)
    private val fireflyColors = listOf(
        RgbColor(255, 220, 100), // Warm yellow
        RgbColor(200, 255, 100), // Yellow-green
        RgbColor(255, 200, 80),  // Orange-yellow
        RgbColor(180, 255, 120), // Light green
        RgbColor(255, 230, 120)  // Pale yellow
    )
    
    // Background color (very dark night)
    private val nightColor = RgbColor(0, 0, 5)

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
        updateFireflyColors()
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        // Initialize audio meters
        fftMeter = FftMeter(bands = 32)
        loudnessMeter = LoudnessMeter()
        
        // Create fireflies
        initializeFireflies()
    }
    
    private fun initializeFireflies() {
        fireflies.clear()
        
        // Scale number of fireflies with grid size
        val numFireflies = min(16, max(8, (combinedWidth * combinedHeight) / 64))
        
        for (i in 0 until numFireflies) {
            val color = getFireflyColor(i)
            fireflies.add(
                Firefly(
                    x = random.nextDouble() * combinedWidth,
                    y = random.nextDouble() * combinedHeight,
                    vx = 0.0,
                    vy = 0.0,
                    targetX = random.nextDouble() * combinedWidth,
                    targetY = random.nextDouble() * combinedHeight,
                    brightness = 0.5f,
                    pulsePhase = random.nextDouble() * 2 * PI,
                    frequencyBand = i % 32, // Distribute across frequency bands
                    color = color
                )
            )
        }
    }
    
    private fun getFireflyColor(index: Int): RgbColor {
        val palette = currentPalette?.colors
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
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeSec = timeMs / 1000.0
        
        // Get audio data
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        
        // Calculate global brightness multiplier (0-1024 -> 0.3-1.0)
        val globalBrightness = (loudness / 1024.0 * 0.7 + 0.3).coerceIn(0.3, 1.0)
        
        // Draw dark background with slight shimmer
        drawBackground(timeSec)
        
        // Update fireflies
        for (firefly in fireflies) {
            updateFirefly(firefly, bands, timeSec)
        }
        
        // Draw fireflies with glow
        drawFireflies(globalBrightness)
        
        return true
    }
    
    private fun drawBackground(timeSec: Double) {
        // Very dark background with subtle shimmer
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val shimmer = sin(timeSec * 0.5 + x * 0.1 + y * 0.1) * 0.05 + 0.95
                val r = (nightColor.r * shimmer).toInt().coerceIn(0, 255)
                val g = (nightColor.g * shimmer).toInt().coerceIn(0, 255)
                val b = (nightColor.b * shimmer).toInt().coerceIn(0, 255)
                pixelColors[x][y] = RgbColor(r, g, b)
            }
        }
    }
    
    private fun updateFirefly(firefly: Firefly, bands: IntArray, timeSec: Double) {
        // Get frequency band value for this firefly
        val bandValue = if (firefly.frequencyBand < bands.size) {
            bands[firefly.frequencyBand]
        } else {
            0
        }
        
        // Update brightness based on frequency band (0-255 -> 0.2-1.0)
        val targetBrightness = (bandValue / 255.0 * 0.8 + 0.2).toFloat()
        
        // Smooth brightness transition
        firefly.brightness += (targetBrightness - firefly.brightness) * 0.1f
        
        // Update pulse phase
        firefly.pulsePhase += 0.05 + (bandValue / 255.0 * 0.1)
        
        // Movement: fly toward target with smooth acceleration
        val dx = firefly.targetX - firefly.x
        val dy = firefly.targetY - firefly.y
        val distance = sqrt(dx * dx + dy * dy)
        
        // Reached target? Pick new one
        if (distance < 2.0) {
            firefly.targetX = random.nextDouble() * combinedWidth
            firefly.targetY = random.nextDouble() * combinedHeight
        }
        
        // Acceleration toward target
        val acceleration = 0.02
        val maxSpeed = 0.3 + (bandValue / 255.0 * 0.3) // Faster with higher frequency
        
        if (distance > 0) {
            val ax = (dx / distance) * acceleration
            val ay = (dy / distance) * acceleration
            
            firefly.vx += ax
            firefly.vy += ay
        }
        
        // Limit speed
        val speed = sqrt(firefly.vx * firefly.vx + firefly.vy * firefly.vy)
        if (speed > maxSpeed) {
            firefly.vx = (firefly.vx / speed) * maxSpeed
            firefly.vy = (firefly.vy / speed) * maxSpeed
        }
        
        // Apply velocity
        firefly.x += firefly.vx
        firefly.y += firefly.vy
        
        // Wrap around edges
        if (firefly.x < 0) firefly.x += combinedWidth
        if (firefly.x >= combinedWidth) firefly.x -= combinedWidth
        if (firefly.y < 0) firefly.y += combinedHeight
        if (firefly.y >= combinedHeight) firefly.y -= combinedHeight
    }
    
    private fun drawFireflies(globalBrightness: Double) {
        for (firefly in fireflies) {
            // Calculate pulse effect
            val pulse = (sin(firefly.pulsePhase) * 0.3 + 0.7).coerceIn(0.0, 1.0)
            
            // Final brightness
            val finalBrightness = firefly.brightness * pulse * globalBrightness
            
            // Draw firefly with glow
            val centerX = firefly.x.toInt().coerceIn(0, combinedWidth - 1)
            val centerY = firefly.y.toInt().coerceIn(0, combinedHeight - 1)
            
            // Draw center (brightest)
            drawGlowPixel(centerX, centerY, firefly.color, finalBrightness)
            
            // Draw glow around center
            val glowRadius = 2
            for (dx in -glowRadius..glowRadius) {
                for (dy in -glowRadius..glowRadius) {
                    if (dx == 0 && dy == 0) continue
                    
                    val x = centerX + dx
                    val y = centerY + dy
                    
                    if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
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
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) return
        
        val glowColor = ColorUtils.scaleBrightness(color, brightness)
        
        // Additive blending
        val existing = pixelColors[x][y]
        val r = (existing.r + glowColor.r).coerceAtMost(255)
        val g = (existing.g + glowColor.g).coerceAtMost(255)
        val b = (existing.b + glowColor.b).coerceAtMost(255)
        
        pixelColors[x][y] = RgbColor(r, g, b)
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Fireflies"

    override fun isAudioReactive(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
        fireflies.clear()
    }
}
