package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.*
import kotlin.random.Random

/**
 * Aurora Borealis animation - Flowing curtains of northern lights
 */
class AuroraAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
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

    override fun is1D() = false
    override fun is2D() = true
    override fun getName(): String = "Aurora"

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        initializeCurtains()
    }
    
    private fun initializeCurtains() {
        curtains.clear()
        val numCurtains = Random.nextInt(5, 9)
        
        for (i in 0 until numCurtains) {
            val color = getAuroraColor(i)
            curtains.add(
                AuroraCurtain(
                    xOffset = Random.nextDouble() * width,
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
        val palette = paramPalette?.colors
        return if (palette != null && palette.isNotEmpty()) {
            palette[index % palette.size]
        } else {
            auroraColors[index % auroraColors.size]
        }
    }
    
    override fun setPalette(palette: com.marsraver.wledfx.color.Palette) {
        super.setPalette(palette)
        // Update curtain colors when palette changes
        curtains.forEachIndexed { index, curtain ->
            curtain.color = getAuroraColor(index)
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeSec = timeMs / 1000.0
        
        drawNightSky()
        
        for (curtain in curtains) {
            curtain.phase += curtain.speed
        }
        
        drawAuroraCurtains(timeSec)
        addStars(timeSec)
        
        return true
    }
    
    private fun drawNightSky() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = nightSkyColor
            }
        }
    }
    
    private fun drawAuroraCurtains(timeSec: Double) {
        for (curtain in curtains) {
            drawCurtain(curtain, timeSec)
        }
    }
    
    private fun drawCurtain(curtain: AuroraCurtain, timeSec: Double) {
        for (x in 0 until width) {
            val waveX = sin(x * curtain.frequency + curtain.phase) * curtain.amplitude
            val centerX = (curtain.xOffset + waveX).toInt()
            
            for (y in 0 until height) {
                val dx = abs(x - centerX)
                if (dx < curtain.width) {
                    val distanceFactor = 1.0 - (dx / curtain.width)
                    val heightFactor = 1.0 - (y.toDouble() / height * 0.4)
                    val verticalWave = sin(y * 0.2 + curtain.phase * 2) * 0.3 + 0.7
                    val intensity = distanceFactor * heightFactor * verticalWave * curtain.brightness
                    
                    if (intensity > 0.05) {
                        val curtainColor = ColorUtils.scaleBrightness(curtain.color, intensity)
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
        // Use height/width from base class
        val numStars = (width * height) / 100
        
        for (i in 0 until numStars) {
            val starSeed = i * 12345
            val x = (starSeed * 7) % width
            val y = (starSeed * 13) % height
            
            if (y < height / 2) {
                val twinkle = sin(timeSec * 2.0 + i * 0.5) * 0.3 + 0.7
                val starBrightness = (twinkle * 0.3).coerceIn(0.0, 0.5)
                
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
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun cleanup() {
        curtains.clear()
    }
}
