package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.random.Random

/**
 * Lightning animation - Random lightning flashes
 * By: Original WLED implementation
 */
class LightningAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var overlay: Boolean = false  // check2
    
    private var aux0: Long = 0L  // Delay between flashes (ms)
    private var aux1: Int = 0     // Number of flashes remaining
    private var step: Long = 0L   // Last flash time
    private var startTimeNs: Long = 0L
    private val random = Random.Default

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
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        aux0 = 0L
        aux1 = 0
        step = 0L
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Determine starting location of flash (random position)
        val segmentLength = combinedWidth * combinedHeight
        val ledstart = random.nextInt(segmentLength)
        
        // Determine length of flash (not to go beyond end)
        val ledlen = 1 + random.nextInt(segmentLength - ledstart)
        
        var bri = 255 / random.nextInt(1, 3)  // Random brightness
        
        // Init: leader flash
        if (aux1 == 0) {
            // Number of flashes: random(4, 4 + intensity/20) * 2
            aux1 = (4 + random.nextInt(4 + intensity / 20 - 4)) * 2
            
            bri = 52  // Leader has lower brightness
            aux0 = 200  // 200ms delay after leader
        }
        
        // Fill background if not overlay mode
        if (!overlay) {
            fillBackground()
        }
        
        // Flash on even number > 2
        if (aux1 > 3 && (aux1 and 0x01) == 0) {
            // Draw lightning flash
            for (i in ledstart until ledstart + ledlen) {
                val x = i % combinedWidth
                val y = i / combinedWidth
                if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
                    val color = colorFromPalette(i, true, 0, bri)
                    setPixelColor(x, y, color)
                }
            }
            aux1--
            step = timeMs
        } else {
            // Wait for delay
            if (timeMs - step > aux0) {
                aux1--
                if (aux1 < 2) aux1 = 0
                
                aux0 = (50 + random.nextInt(100)).toLong()  // Delay between flashes
                
                if (aux1 == 2) {
                    // Delay between strikes: random(255 - speed) * 100
                    aux0 = (random.nextInt(255 - speed) * 100).toLong()
                }
                
                step = timeMs
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Lightning"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set overlay mode (check2)
     */
    fun setOverlay(enabled: Boolean) {
        this.overlay = enabled
    }

    /**
     * Get overlay mode
     */
    fun getOverlay(): Boolean {
        return overlay
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun fillBackground() {
        // Fill with background color (SEGCOLOR(1) - default black)
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = RgbColor.BLACK
            }
        }
    }

    /**
     * Get color from palette
     */
    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int, brightnessOverride: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        val finalBrightness = if (brightnessOverride > 0) brightnessOverride else brightness
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = finalBrightness.coerceIn(0, 255) / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, finalBrightness.coerceIn(0, 255))
        }
    }
}

