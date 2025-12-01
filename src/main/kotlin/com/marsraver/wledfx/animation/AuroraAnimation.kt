package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.*
import kotlin.random.Random

/**
 * Aurora effect - Multiple waves moving across the grid with smooth color blending.
 */
class AuroraAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    
    private data class AuroraWave(
        var center: Double,           // Center position (in pixels)
        var age: Int,                  // Current age
        var ttl: Int,                  // Time to live
        var width: Double,             // Wave width
        var baseAlpha: Double,         // Base alpha (0.0-1.0)
        var speedFactor: Double,       // Speed multiplier
        var goingLeft: Boolean,        // Direction
        var baseColor: RgbColor,       // Base RGB color
        var alive: Boolean = true
    ) {
        fun updateCachedValues(): Pair<Int, Int> {
            val halfTtl = ttl / 2.0
            val ageFactor = if (age < halfTtl) {
                age / halfTtl
            } else {
                (ttl - age) / halfTtl
            }.coerceIn(0.0, 1.0)
            
            val centerLed = center.toInt()
            val waveStart = (centerLed - width).toInt()
            val waveEnd = (centerLed + width).toInt()
            
            return Pair(waveStart, waveEnd)
        }
        
        fun getColorForLED(ledIndex: Int, ageFactor: Double): RgbColor? {
            val halfTtl = ttl / 2.0
            val ageFactorCached = if (age < halfTtl) {
                age / halfTtl
            } else {
                (ttl - age) / halfTtl
            }.coerceIn(0.0, 1.0)
            
            val centerLed = center
            val waveStart = centerLed - width
            val waveEnd = centerLed + width
            
            if (ledIndex < waveStart || ledIndex > waveEnd) return null
            
            val offset = abs(ledIndex - centerLed)
            val offsetFactor = (offset / width).coerceIn(0.0, 1.0)
            if (offsetFactor > 1.0) return null
            
            val brightnessFactor = (1.0 - offsetFactor) * ageFactorCached * baseAlpha
            
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        }
        
        fun update(segmentLength: Int, speed: Double) {
            val step = speedFactor * speed
            center += if (goingLeft) -step else step
            age++
            
            if (age > ttl) {
                alive = false
            } else {
                if (goingLeft) {
                    if (center < -width) {
                        alive = false
                    }
                } else {
                    if (center > segmentLength + width) {
                        alive = false
                    }
                }
            }
        }
    }
    
    private val waves = mutableListOf<AuroraWave>()
    private val maxWaveCount = 20
    private val maxSpeed = 6.0
    private val widthFactor = 6.0
    private var lastUpdateTime: Long = 0L

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
        waves.clear()
        lastUpdateTime = 0L
        
        // Initialize waves based on intensity (default to 10 waves)
        val waveCount = 10 // Map intensity 0-255 to 2-20 waves
        for (i in 0 until waveCount) {
            createNewWave()
        }
    }

    private fun createNewWave() {
        val currentPalette = this.currentPalette?.colors
        val segmentLength = combinedWidth // Waves move horizontally
        
        val ttl = Random.nextInt(500, 1501)
        val baseColor = if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = Random.nextInt(currentPalette.size)
            currentPalette[paletteIndex]
        } else {
            // Default rainbow color
            val hue = Random.nextInt(256)
            hsvToRgb(hue, 255, 255)
        }
        
        val baseAlpha = Random.nextDouble(0.6, 1.0)
        val width = Random.nextDouble(segmentLength / 20.0, segmentLength / widthFactor) + 1.0
        val center = Random.nextDouble(0.0, segmentLength.toDouble())
        val goingLeft = Random.nextBoolean()
        val speedFactor = Random.nextDouble(10.0, 31.0) * maxSpeed / (100.0 * 255.0)
        
        waves.add(AuroraWave(
            center = center,
            age = 0,
            ttl = ttl,
            width = width,
            baseAlpha = baseAlpha,
            speedFactor = speedFactor,
            goingLeft = goingLeft,
            baseColor = baseColor,
            alive = true
        ))
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateTime == 0L) {
            lastUpdateTime = now
            return true
        }
        
        val elapsed = now - lastUpdateTime
        val delay = 16_000_000L // ~60 FPS
        if (elapsed < delay) {
            return true
        }
        
        lastUpdateTime = now
        
        val speed = 128.0 / 255.0 // Default speed (0-1 range)
        val segmentLength = combinedWidth
        
        // Update existing waves and create new ones if needed
        val waveCount = 10 // Based on intensity
        val iterator = waves.iterator()
        while (iterator.hasNext()) {
            val wave = iterator.next()
            wave.update(segmentLength, speed)
            if (!wave.alive) {
                iterator.remove()
            }
        }
        
        // Create new waves to maintain count
        while (waves.size < waveCount) {
            createNewWave()
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Aurora waves move horizontally, so we use x coordinate
        val ledIndex = x
        
        // Start with backlight (dark background)
        var mixedR = 0
        var mixedG = 0
        var mixedB = 0
        
        // Sum all waves influencing this pixel
        for (wave in waves) {
            val halfTtl = wave.ttl / 2.0
            val ageFactor = if (wave.age < halfTtl) {
                wave.age / halfTtl
            } else {
                (wave.ttl - wave.age) / halfTtl
            }.coerceIn(0.0, 1.0)
            
            val color = wave.getColorForLED(ledIndex, ageFactor)
            if (color != null) {
                mixedR = (mixedR + color.r).coerceAtMost(255)
                mixedG = (mixedG + color.g).coerceAtMost(255)
                mixedB = (mixedB + color.b).coerceAtMost(255)
            }
        }
        
        return RgbColor(mixedR, mixedG, mixedB)
    }

    override fun getName(): String = "Aurora"

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): RgbColor {
        return ColorUtils.hsvToRgb(hue, saturation, value)
    }
}

