package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.*
import kotlin.random.Random

/**
 * Thunderstorm animation - Dramatic storm with lightning and rain
 * Bass frequencies trigger lightning bolts, volume controls rain intensity
 */
class ThunderstormAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private val random = Random.Default
    
    // Lightning state
    private data class Lightning(
        var active: Boolean = false,
        var x: Int = 0,
        var segments: List<Pair<Int, Int>> = emptyList(), // List of (x, y) points
        var brightness: Float = 1.0f,
        var fadeSpeed: Float = 0.1f
    )
    
    private val lightningBolts = mutableListOf<Lightning>()
    private var flashBrightness = 0f
    
    // Rain state
    private data class RainDrop(
        var x: Double,
        var y: Double,
        var speed: Double
    )
    
    private val rainDrops = mutableListOf<RainDrop>()
    
    // Cloud noise for background
    private var cloudOffset = 0.0
    
    // Colors (much darker for dramatic effect)
    private val darkSkyColor = RgbColor(0, 0, 10)
    private val cloudColor = RgbColor(15, 15, 25)
    private val lightningColor = RgbColor(200, 220, 255)
    private val rainColor = RgbColor(100, 120, 180)

    override fun supportsPalette(): Boolean = false // Fixed storm colors

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
        
        // Initialize audio meters
        fftMeter = FftMeter(bands = 32)
        loudnessMeter = LoudnessMeter()
        
        // Initialize rain drops
        initializeRain()
    }
    
    private fun initializeRain() {
        rainDrops.clear()
        val numDrops = combinedWidth * 2 // Base number of rain drops
        for (i in 0 until numDrops) {
            rainDrops.add(
                RainDrop(
                    x = random.nextDouble() * combinedWidth,
                    y = random.nextDouble() * combinedHeight,
                    speed = 0.25 + random.nextDouble() * 0.5  // Slowed by half
                )
            )
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeSec = timeMs / 1000.0
        
        // Get audio data
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        
        // Calculate bass level (average of first 4 bands)
        val bassLevel = if (bands.size >= 4) {
            (bands[0] + bands[1] + bands[2] + bands[3]) / 4
        } else {
            0
        }
        
        // Draw cloud background
        drawClouds(timeSec)
        
        // Update and draw rain
        updateRain(loudness)
        drawRain()
        
        // Check for lightning trigger (bass threshold) - reduced frequency
        val lightningThreshold = 100
        if (bassLevel > lightningThreshold && random.nextFloat() < 0.1f) {  // Reduced from 0.3f to 0.1f
            createLightning()
            // Major bass hit = full screen flash
            if (bassLevel > 180) {
                flashBrightness = 1.0f
            }
        }
        
        // Update and draw lightning
        updateLightning()
        drawLightning()
        
        // Apply flash effect
        if (flashBrightness > 0) {
            applyFlash()
            flashBrightness = (flashBrightness - 0.15f).coerceAtLeast(0f)
        }
        
        return true
    }
    
    private fun drawClouds(timeSec: Double) {
        cloudOffset += 0.01
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                // Simple noise-like pattern for clouds
                val noiseValue = sin(x * 0.3 + cloudOffset) * cos(y * 0.2 + cloudOffset * 0.7)
                val cloudIntensity = ((noiseValue + 1.0) / 2.0).coerceIn(0.0, 1.0)
                
                // Darker at bottom, lighter at top
                val heightFactor = 1.0 - (y.toDouble() / combinedHeight * 0.5)
                
                val finalIntensity = cloudIntensity * heightFactor
                
                val r = (darkSkyColor.r + (cloudColor.r - darkSkyColor.r) * finalIntensity).toInt().coerceIn(0, 255)
                val g = (darkSkyColor.g + (cloudColor.g - darkSkyColor.g) * finalIntensity).toInt().coerceIn(0, 255)
                val b = (darkSkyColor.b + (cloudColor.b - darkSkyColor.b) * finalIntensity).toInt().coerceIn(0, 255)
                
                pixelColors[x][y] = RgbColor(r, g, b)
            }
        }
    }
    
    private fun updateRain(loudness: Int) {
        // Rain intensity based on loudness (0-1024 -> 0.5-2.0 speed multiplier)
        val intensityMultiplier = (loudness / 1024.0 * 1.5 + 0.5).coerceIn(0.5, 2.0)
        
        for (drop in rainDrops) {
            // Move rain down
            drop.y += drop.speed * intensityMultiplier
            
            // Reset at top when reaching bottom
            if (drop.y >= combinedHeight) {
                drop.y = 0.0
                drop.x = random.nextDouble() * combinedWidth
                drop.speed = 0.25 + random.nextDouble() * 0.5  // Slowed by half
            }
        }
    }
    
    private fun drawRain() {
        for (drop in rainDrops) {
            val x = drop.x.toInt().coerceIn(0, combinedWidth - 1)
            val y = drop.y.toInt().coerceIn(0, combinedHeight - 1)
            
            // Blend rain color with existing pixel
            val existing = pixelColors[x][y]
            pixelColors[x][y] = ColorUtils.blend(existing, rainColor, 128)
        }
    }
    
    private fun createLightning() {
        // Create a jagged lightning bolt from top to random point
        val startX = random.nextInt(combinedWidth)
        val endX = random.nextInt(combinedWidth)
        val endY = random.nextInt(combinedHeight / 2, combinedHeight)
        
        val segments = mutableListOf<Pair<Int, Int>>()
        
        // Generate jagged path
        var currentX = startX
        var currentY = 0
        
        segments.add(Pair(currentX, currentY))
        
        while (currentY < endY) {
            // Move down and randomly left/right
            currentY += random.nextInt(2, 5)
            currentX += random.nextInt(-2, 3)
            currentX = currentX.coerceIn(0, combinedWidth - 1)
            
            segments.add(Pair(currentX, currentY.coerceAtMost(combinedHeight - 1)))
        }
        
        lightningBolts.add(
            Lightning(
                active = true,
                x = startX,
                segments = segments,
                brightness = 1.0f,
                fadeSpeed = 0.2f + random.nextFloat() * 0.1f
            )
        )
    }
    
    private fun updateLightning() {
        val iterator = lightningBolts.iterator()
        while (iterator.hasNext()) {
            val bolt = iterator.next()
            bolt.brightness -= bolt.fadeSpeed
            if (bolt.brightness <= 0) {
                iterator.remove()
            }
        }
    }
    
    private fun drawLightning() {
        for (bolt in lightningBolts) {
            if (!bolt.active) continue
            
            val color = ColorUtils.scaleBrightness(lightningColor, bolt.brightness.toDouble())
            
            // Draw each segment
            for (i in 0 until bolt.segments.size - 1) {
                val (x1, y1) = bolt.segments[i]
                val (x2, y2) = bolt.segments[i + 1]
                
                // Draw line between points
                drawLine(x1, y1, x2, y2, color)
            }
        }
    }
    
    private fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: RgbColor) {
        // Simple line drawing
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val steps = max(dx, dy)
        
        if (steps == 0) {
            setPixel(x1, y1, color)
            return
        }
        
        val xInc = (x2 - x1).toDouble() / steps
        val yInc = (y2 - y1).toDouble() / steps
        
        var x = x1.toDouble()
        var y = y1.toDouble()
        
        for (i in 0..steps) {
            setPixel(x.toInt(), y.toInt(), color)
            x += xInc
            y += yInc
        }
    }
    
    private fun setPixel(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            // Additive blending for lightning
            val existing = pixelColors[x][y]
            val r = (existing.r + color.r).coerceAtMost(255)
            val g = (existing.g + color.g).coerceAtMost(255)
            val b = (existing.b + color.b).coerceAtMost(255)
            pixelColors[x][y] = RgbColor(r, g, b)
        }
    }
    
    private fun applyFlash() {
        val flashColor = RgbColor(255, 255, 255)
        val blendAmount = (flashBrightness * 128).toInt()
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.blend(pixelColors[x][y], flashColor, blendAmount)
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

    override fun getName(): String = "Thunderstorm"

    override fun isAudioReactive(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
        lightningBolts.clear()
        rainDrops.clear()
    }
}
