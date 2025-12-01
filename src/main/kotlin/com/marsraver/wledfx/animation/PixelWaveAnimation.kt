package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * PixelWave animation - Wave expanding from center based on audio
 * By: Andrew Tuline
 */
class PixelWaveAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 64
    
    private var secondHand: Int = 0
    private var lastSecondHand: Int = -1
    
    @Volatile
    private var volumeRaw: Int = 0
    private val audioLock = Any()
    private var audioScope: CoroutineScope? = null
    private var startTimeNs: Long = 0L

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
        secondHand = 0
        lastSecondHand = -1
        startTimeNs = System.nanoTime()
        
        synchronized(audioLock) {
            volumeRaw = 0
        }
        
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                // Get RMS volume for volumeRaw
                AudioPipeline.rmsFlow().collectLatest { level ->
                    synchronized(audioLock) {
                        volumeRaw = level.level
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeMicros = (now - startTimeNs) / 1_000
        
        // Calculate secondHand: micros()/(256-speed)/500+1 % 16
        // Higher speed = faster ticks
        val speedFactor = (256 - speed).coerceAtLeast(1)
        secondHand = ((timeMicros / speedFactor / 500 + 1) % 16).toInt()
        
        val volume = synchronized(audioLock) { volumeRaw }
        
        // Only update when secondHand changes
        if (secondHand != lastSecondHand) {
            lastSecondHand = secondHand
            
            // Calculate pixel brightness: volumeRaw * intensity / 64
            val pixBri = (volume * intensity / 64).coerceIn(0, 255)
            
            // Get color from palette based on time
            val colorIndex = (timeMs % 256).toInt()
            val color = colorFromPalette(colorIndex, true, 0)
            
            // Set center pixel
            val centerX = combinedWidth / 2
            val centerY = combinedHeight / 2
            val centerColor = ColorUtils.blend(RgbColor.BLACK, color, pixBri)
            setPixelColor(centerX, centerY, centerColor)
            
            // Expand wave outward from center
            // Shift pixels outward in all directions
            expandWaveFromCenter()
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

    override fun getName(): String = "PixelWave"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
        audioScope?.cancel()
        audioScope = null
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }


    /**
     * Expand wave from center - shifts pixels outward in all directions
     */
    private fun expandWaveFromCenter() {
        val centerX = combinedWidth / 2
        val centerY = combinedHeight / 2
        
        // Create temporary buffer
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        
        // Copy current state
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                temp[x][y] = pixelColors[x][y]
            }
        }
        
        // Shift pixels outward from center
        // For each pixel, move it one step away from center
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                if (x == centerX && y == centerY) {
                    // Center pixel stays (already set)
                    continue
                }
                
                // Calculate direction from center
                val dx = x - centerX
                val dy = y - centerY
                
                // Get source pixel (one step closer to center)
                val sourceX = centerX + (dx * 0.9).roundToInt()
                val sourceY = centerY + (dy * 0.9).roundToInt()
                
                if (sourceX in 0 until combinedWidth && sourceY in 0 until combinedHeight) {
                    pixelColors[x][y] = temp[sourceX][sourceY]
                } else {
                    pixelColors[x][y] = RgbColor.BLACK
                }
            }
        }
    }

    /**
     * Get color from palette
     */
    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}

