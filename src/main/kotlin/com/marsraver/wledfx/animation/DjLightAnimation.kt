package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import com.marsraver.wledfx.audio.FftMeter
import kotlin.math.*

/**
 * DJLight - WLED v0.15.3 mode_DJLight with radial/polar coordinate mapping
 * 
 * Original 1D C code from FX.cpp line 6965 applies a "spread from center" effect.
 * On 2D matrices, WLED appears to map this using polar coordinates from the top-left corner,
 * creating a quarter-circle pattern where pixels spread radially outward.
 * 
 * The 1D effect:
 * 1. Sets a color at the center based on FFT data
 * 2. Shifts pixels outward from center (left half moves left, right half moves right)
 * 
 * 2D radial mapping:
 * - Maps each pixel to a radial distance from origin (0,0)
 * - Applies the 1D effect along this radial distance
 * - Creates concentric quarter-circle arcs spreading from corner
 */
class DjLightAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var fftMeter: FftMeter? = null
    private var lastSecondHand: Int = -1
    
    // Virtual 1D strip for the radial effect
    private var virtualStripLength: Int = 0
    private lateinit var virtualStrip: Array<RgbColor>
    
    // Radial mapping: pixel -> virtual strip index
    private lateinit var radialMap: Array<Array<Int>>

    override fun supportsPalette(): Boolean = false

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        startTimeNs = System.nanoTime()
        
        // Calculate virtual strip length (max radial distance from top-left corner)
        val maxDistance = sqrt((combinedWidth * combinedWidth + combinedHeight * combinedHeight).toDouble()).toInt()
        virtualStripLength = maxDistance
        virtualStrip = Array(virtualStripLength) { RgbColor.BLACK }
        
        // Build radial mapping from (0,0)
        radialMap = Array(combinedWidth) { Array(combinedHeight) { 0 } }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val distance = sqrt((x * x + y * y).toDouble()).toInt()
                radialMap[x][y] = distance.coerceIn(0, virtualStripLength - 1)
            }
        }
        
        // Initialize FFT meter with 16 bands
        fftMeter = FftMeter(bands = 16)
        lastSecondHand = -1
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) {
            startTimeNs = now
        }
        
        // Convert nanoseconds to microseconds
        val micros = (now - startTimeNs) / 1_000L
        
        // Timing control (matching original)
        val speed = 128
        val speedFactor = (256 - speed).coerceAtLeast(1)
        val secondHand = ((micros / speedFactor / 500L + 1) % 64).toInt()
        
        if (lastSecondHand != secondHand) {
            lastSecondHand = secondHand
            
            // Get FFT results
            val fftResult = fftMeter?.getNormalizedBands() ?: IntArray(16)
            
            // Generate color from FFT (matching original)
            val r = (fftResult.getOrElse(15) { 0 } / 2).coerceIn(0, 255)
            val g = (fftResult.getOrElse(5) { 0 } / 2).coerceIn(0, 255)
            val b = (fftResult.getOrElse(0) { 0 } / 2).coerceIn(0, 255)
            val baseColor = RgbColor(r, g, b)
            
            // Apply fade based on FFT band 4
            val fadeBand = fftResult.getOrElse(4) { 0 }
            val fadeAmount = mapValue(fadeBand, 0, 255, 255, 4).coerceIn(4, 255)
            val fadeFactor = (255 - fadeAmount) / 255.0
            val newColor = ColorUtils.scaleBrightness(baseColor, fadeFactor)
            
            // Apply 1D DJ Light effect to virtual strip
            val mid = virtualStripLength / 2
            
            // Set center pixel
            virtualStrip[mid] = newColor
            
            // Shift right half to the right (spreading outward)
            for (i in virtualStripLength - 1 downTo mid + 1) {
                virtualStrip[i] = virtualStrip[i - 1]
            }
            
            // Shift left half to the left (spreading inward toward origin)
            for (i in 0 until mid) {
                virtualStrip[i] = virtualStrip[i + 1]
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val stripIndex = radialMap[x][y]
            virtualStrip[stripIndex]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "DJLight"

    override fun isAudioReactive(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
