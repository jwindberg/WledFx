package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import com.marsraver.wledfx.audio.FftMeter
import com.marsraver.wledfx.audio.LoudnessMeter
import kotlin.math.*
import kotlin.random.Random

/**
 * Thunderstorm animation - Dramatic storm with lightning and rain
 * Bass frequencies trigger lightning bolts, volume controls rain intensity
 */
class ThunderstormAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var loudnessMeter: LoudnessMeter? = null
    private val random = Random.Default
    
    // Lightning state
    private data class Lightning(
        var active: Boolean = false,
        var x: Int = 0,
        var segments: List<Pair<Int, Int>> = emptyList(),
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
    
    private var cloudOffset = 0.0
    
    // Colors
    private val darkSkyColor = RgbColor(0, 0, 10)
    private val cloudColor = RgbColor(15, 15, 25)
    private val lightningColor = RgbColor(200, 220, 255)
    private val rainColor = RgbColor(100, 120, 180)

    override fun supportsPalette(): Boolean = false // Fixed storm colors

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        fftMeter = FftMeter(bands = 32)
        loudnessMeter = LoudnessMeter()
        
        initializeRain()
        paramSpeed = 128
    }
    
    private fun initializeRain() {
        rainDrops.clear()
        val numDrops = width * 2
        for (i in 0 until numDrops) {
            rainDrops.add(
                RainDrop(
                    x = random.nextDouble() * width,
                    y = random.nextDouble() * height,
                    speed = 0.25 + random.nextDouble() * 0.5
                )
            )
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeSec = timeMs / 1000.0
        
        val bands = fftMeter?.getNormalizedBands() ?: IntArray(32)
        val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
        
        val bassLevel = if (bands.size >= 4) {
            (bands[0] + bands[1] + bands[2] + bands[3]) / 4
        } else {
            0
        }
        
        drawClouds(timeSec)
        
        updateRain(loudness)
        drawRain()
        
        // Intensity param can control sensitivity?
        // Default paramIntensity = 128. Map to threshold.
        // Higher intensity = lower threshold = more lightning.
        val baseThreshold = 100
        val thresholdAdjustment = (paramIntensity - 128)
        val lightningThreshold = (baseThreshold - thresholdAdjustment).coerceIn(20, 200)

        if (bassLevel > lightningThreshold && random.nextFloat() < 0.1f) {
            createLightning()
            if (bassLevel > 180) {
                flashBrightness = 1.0f
            }
        }
        
        updateLightning()
        drawLightning()
        
        if (flashBrightness > 0) {
            applyFlash()
            flashBrightness = (flashBrightness - 0.15f).coerceAtLeast(0f)
        }
        
        return true
    }
    
    private fun drawClouds(timeSec: Double) {
        val speedFactor = paramSpeed / 128.0
        cloudOffset += 0.01 * speedFactor
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val noiseValue = sin(x * 0.3 + cloudOffset) * cos(y * 0.2 + cloudOffset * 0.7)
                val cloudIntensity = ((noiseValue + 1.0) / 2.0).coerceIn(0.0, 1.0)
                val heightFactor = 1.0 - (y.toDouble() / height * 0.5)
                val finalIntensity = cloudIntensity * heightFactor
                
                val r = (darkSkyColor.r + (cloudColor.r - darkSkyColor.r) * finalIntensity).toInt().coerceIn(0, 255)
                val g = (darkSkyColor.g + (cloudColor.g - darkSkyColor.g) * finalIntensity).toInt().coerceIn(0, 255)
                val b = (darkSkyColor.b + (cloudColor.b - darkSkyColor.b) * finalIntensity).toInt().coerceIn(0, 255)
                
                pixelColors[x][y] = RgbColor(r, g, b)
            }
        }
    }
    
    private fun updateRain(loudness: Int) {
        val intensityMultiplier = (loudness / 1024.0 * 1.5 + 0.5).coerceIn(0.5, 2.0)
        
        for (drop in rainDrops) {
            drop.y += drop.speed * intensityMultiplier
            if (drop.y >= height) {
                drop.y = 0.0
                drop.x = random.nextDouble() * width
                drop.speed = 0.25 + random.nextDouble() * 0.5
            }
        }
    }
    
    private fun drawRain() {
        for (drop in rainDrops) {
            val x = drop.x.toInt().coerceIn(0, width - 1)
            val y = drop.y.toInt().coerceIn(0, height - 1)
            val existing = pixelColors[x][y]
            pixelColors[x][y] = ColorUtils.blend(existing, rainColor, 128)
        }
    }
    
    private fun createLightning() {
        val startX = random.nextInt(width)
        val endX = random.nextInt(width)
        val endY = random.nextInt(height / 2, height)
        
        val segments = mutableListOf<Pair<Int, Int>>()
        var currentX = startX
        var currentY = 0
        segments.add(Pair(currentX, currentY))
        
        while (currentY < endY) {
            currentY += random.nextInt(2, 5)
            currentX += random.nextInt(-2, 3)
            currentX = currentX.coerceIn(0, width - 1)
            segments.add(Pair(currentX, currentY.coerceAtMost(height - 1)))
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
            for (i in 0 until bolt.segments.size - 1) {
                val (x1, y1) = bolt.segments[i]
                val (x2, y2) = bolt.segments[i + 1]
                drawLine(x1, y1, x2, y2, color)
            }
        }
    }
    
    private fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: RgbColor) {
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
        if (x in 0 until width && y in 0 until height) {
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
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.blend(pixelColors[x][y], flashColor, blendAmount)
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

    override fun getName(): String = "Thunderstorm"
    override fun isAudioReactive(): Boolean = true
    override fun supportsIntensity(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
        loudnessMeter?.stop()
        loudnessMeter = null
        lightningBolts.clear()
        rainDrops.clear()
    }
}
