package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.*
import kotlin.random.Random

/**
 * Aurora Borealis animation - Flowing curtains of northern lights
 * Features vertical curtains with wave-like motion, realistic aurora colors,
 * and layered depth for a mesmerizing effect
 */
class AuroraAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    // Aurora curtain layers
    private data class AuroraCurtain(
        var xOffset: Double,        // Horizontal offset for wave
        var phase: Double,          // Wave phase
        var speed: Double,          // Wave speed
        var amplitude: Double,      // Wave amplitude
        var frequency: Double,      // Wave frequency
        var color: RgbColor,        // Base color
        var brightness: Double,     // Brightness multiplier
        var width: Double           // Curtain width
    )
    
    private val curtains = mutableListOf<AuroraCurtain>()
    
    // Realistic aurora colors
    private val auroraColors = listOf(
        RgbColor(0, 255, 100),      // Bright green (most common)
        RgbColor(100, 255, 150),    // Light green
        RgbColor(0, 200, 80),       // Deep green
        RgbColor(200, 100, 255),    // Purple
        RgbColor(255, 100, 200),    // Pink
        RgbColor(100, 150, 255),    // Blue
        RgbColor(150, 255, 200)     // Cyan-green
    )
    
    // Very dark night sky
    private val nightSkyColor = RgbColor(0, 0, 8)

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
        updateCurtainColors()
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        // Create multiple aurora curtain layers
        initializeCurtains()
    }
    
    private fun initializeCurtains() {
        curtains.clear()
        
        // Create 5-8 curtain layers for depth
        val numCurtains = Random.nextInt(5, 9)
        
        for (i in 0 until numCurtains) {
            val color = getAuroraColor(i)
            curtains.add(
                AuroraCurtain(
                    xOffset = Random.nextDouble() * combinedWidth,
                    phase = Random.nextDouble() * 2 * PI,
                    speed = 0.02 + Random.nextDouble() * 0.03,
                    amplitude = 2.0 + Random.nextDouble() * 4.0,
                    frequency = 0.1 + Random.nextDouble() * 0.15,
                    color = color,
                    brightness = 0.6 + Random.nextDouble() * 0.4,
                    width = 3.0 + Random.nextDouble() * 5.0
                )
            )
        }
    }
    
    private fun getAuroraColor(index: Int): RgbColor {
        val palette = currentPalette?.colors
        return if (palette != null && palette.isNotEmpty()) {
            palette[index % palette.size]
        } else {
            auroraColors[index % auroraColors.size]
        }
    }
    
    private fun updateCurtainColors() {
        curtains.forEachIndexed { index, curtain ->
            curtain.color = getAuroraColor(index)
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeSec = timeMs / 1000.0
        
        // Clear with dark night sky
        drawNightSky()
        
        // Update curtain phases
        for (curtain in curtains) {
            curtain.phase += curtain.speed
        }
        
        // Draw aurora curtains with layering
        drawAuroraCurtains(timeSec)
        
        // Add subtle stars twinkling
        addStars(timeSec)
        
        return true
    }
    
    private fun drawNightSky() {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = nightSkyColor
            }
        }
    }
    
    private fun drawAuroraCurtains(timeSec: Double) {
        // Draw curtains from back to front for proper layering
        for (curtain in curtains) {
            drawCurtain(curtain, timeSec)
        }
    }
    
    private fun drawCurtain(curtain: AuroraCurtain, timeSec: Double) {
        // For each column (x position)
        for (x in 0 until combinedWidth) {
            // Calculate wave displacement for this x position
            val waveX = sin(x * curtain.frequency + curtain.phase) * curtain.amplitude
            val centerX = (curtain.xOffset + waveX).toInt()
            
            // Draw vertical curtain at this position
            for (y in 0 until combinedHeight) {
                // Calculate distance from curtain center
                val dx = abs(x - centerX)
                
                // Curtain intensity falls off with distance
                if (dx < curtain.width) {
                    val distanceFactor = 1.0 - (dx / curtain.width)
                    
                    // Height-based intensity (brighter at top, fading toward bottom)
                    val heightFactor = 1.0 - (y.toDouble() / combinedHeight * 0.4)
                    
                    // Add vertical wave motion
                    val verticalWave = sin(y * 0.2 + curtain.phase * 2) * 0.3 + 0.7
                    
                    // Combine all factors
                    val intensity = distanceFactor * heightFactor * verticalWave * curtain.brightness
                    
                    if (intensity > 0.05) {
                        // Apply color with intensity
                        val curtainColor = ColorUtils.scaleBrightness(curtain.color, intensity)
                        
                        // Additive blending for aurora glow
                        val existing = pixelColors[x][y]
                        val r = (existing.r + curtainColor.r).coerceAtMost(255)
                        val g = (existing.g + curtainColor.g).coerceAtMost(255)
                        val b = (existing.b + curtainColor.b).coerceAtMost(255)
                        
                        pixelColors[x][y] = RgbColor(r, g, b)
                    }
                }
            }
        }
    }
    
    private fun addStars(timeSec: Double) {
        // Add a few twinkling stars in the dark sky
        val numStars = (combinedWidth * combinedHeight) / 100
        
        for (i in 0 until numStars) {
            // Pseudo-random but consistent star positions
            val starSeed = i * 12345
            val x = (starSeed * 7) % combinedWidth
            val y = (starSeed * 13) % combinedHeight
            
            // Only place stars in darker areas (top half)
            if (y < combinedHeight / 2) {
                // Twinkling effect
                val twinkle = sin(timeSec * 2.0 + i * 0.5) * 0.3 + 0.7
                val starBrightness = (twinkle * 0.3).coerceIn(0.0, 0.5)
                
                // Check if this pixel is dark enough for a star
                val existing = pixelColors[x][y]
                val existingBrightness = (existing.r + existing.g + existing.b) / 3
                
                if (existingBrightness < 30) {
                    val starColor = RgbColor(
                        (200 * starBrightness).toInt(),
                        (200 * starBrightness).toInt(),
                        (255 * starBrightness).toInt()
                    )
                    
                    val r = (existing.r + starColor.r).coerceAtMost(255)
                    val g = (existing.g + starColor.g).coerceAtMost(255)
                    val b = (existing.b + starColor.b).coerceAtMost(255)
                    
                    pixelColors[x][y] = RgbColor(r, g, b)
                }
            }
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Aurora"

    override fun cleanup() {
        curtains.clear()
    }
}
