package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.audio.FftMeter
import kotlin.math.*

/**
 * DJLight - WLED v0.15.3 mode_DJLight with radial/polar coordinate mapping
 */
class DjLightAnimation : BaseAnimation() {

    private var startTimeNs: Long = 0L
    private var lastSecondHand: Int = -1
    private var fftMeter: FftMeter? = null
    
    // Virtual 1D strip for the radial effect
    private var virtualStripLength: Int = 0
    private lateinit var virtualStrip: Array<RgbColor>
    
    // Radial mapping: pixel -> virtual strip index
    private lateinit var radialMap: Array<Array<Int>>

    override fun getName(): String = "DJLight"
    override fun isAudioReactive(): Boolean = true
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        startTimeNs = System.nanoTime()
        
        // Calculate virtual strip length
        val maxDistance = sqrt((width * width + height * height).toDouble()).toInt()
        virtualStripLength = maxDistance
        virtualStrip = Array(virtualStripLength) { RgbColor.BLACK }
        
        // Build radial mapping from (0,0)
        radialMap = Array(width) { Array(height) { 0 } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                val distance = sqrt((x * x + y * y).toDouble()).toInt()
                radialMap[x][y] = distance.coerceIn(0, virtualStripLength - 1)
            }
        }
        
        fftMeter = FftMeter(bands = 16)
        lastSecondHand = -1
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val micros = (now - startTimeNs) / 1_000L
        
        // Use paramSpeed
        val speedFactor = (256 - paramSpeed).coerceAtLeast(1)
        val secondHand = ((micros / speedFactor / 500L + 1) % 64).toInt()
        
        if (lastSecondHand != secondHand) {
            lastSecondHand = secondHand
            
            val fftResult = fftMeter?.getNormalizedBands() ?: IntArray(16)
            
            val r = (fftResult.getOrElse(15) { 0 } / 2).coerceIn(0, 255)
            val g = (fftResult.getOrElse(5) { 0 } / 2).coerceIn(0, 255)
            val b = (fftResult.getOrElse(0) { 0 } / 2).coerceIn(0, 255)
            val baseColor = RgbColor(r, g, b)
            
            val fadeBand = fftResult.getOrElse(4) { 0 }
            val fadeAmount = mapValue(fadeBand, 0, 255, 255, 4).coerceIn(4, 255)
            val fadeFactor = (255 - fadeAmount) / 255.0
            val newColor = ColorUtils.scaleBrightness(baseColor, fadeFactor)
            
            val mid = virtualStripLength / 2
            virtualStrip[mid] = newColor
            
            for (i in virtualStripLength - 1 downTo mid + 1) virtualStrip[i] = virtualStrip[i - 1]
            for (i in 0 until mid) virtualStrip[i] = virtualStrip[i + 1]
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            val stripIndex = radialMap[x][y]
            virtualStrip[stripIndex]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }
}
