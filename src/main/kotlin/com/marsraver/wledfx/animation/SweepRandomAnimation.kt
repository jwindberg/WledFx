package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.max
import kotlin.random.Random

/**
 * Sweep Random
 * Fills the strip with a random color, then sweeps back with another.
 */
class SweepRandomAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    
    private var pos: Double = 0.0
    private var direction: Int = 1
    private var currentColor: RgbColor = RgbColor.RED
    
    private var startTimeNs: Long = 0L
    private var lastTimeNs: Long = 0L
    
    override fun getName(): String = "Sweep Random"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false

    override fun onInit() {
        this.leds = Array(pixelCount) { RgbColor.BLACK }
        startTimeNs = System.nanoTime()
        lastTimeNs = startTimeNs
        paramSpeed = 128
        pickNewColor()
    }
    
    private fun pickNewColor() {
        if (getPalette() != null) {
            val pIdx = Random.nextInt(256)
            currentColor = getColorFromPalette(pIdx)
        } else {
            currentColor = ColorUtils.hsvToRgb(Random.nextInt(255), 255, 255)
        }
    }
    
    override fun update(now: Long): Boolean {
        if (lastTimeNs == 0L) lastTimeNs = now
        val deltaS = (now - lastTimeNs) / 1_000_000_000.0
        lastTimeNs = now
        
        // Scale speed
        val move = (10.0 + (paramSpeed * 0.5)) * deltaS * (max(pixelCount, 50) / 50.0) 
        
        pos += move * direction
        
        val iPos = pos.toInt()
        
        if (iPos in 0 until pixelCount) {
            leds[iPos] = currentColor
        }
        
        // Check bounds
        if (direction == 1 && pos >= pixelCount - 1) {
            pos = (pixelCount - 1).toDouble()
            direction = -1
            pickNewColor()
        } else if (direction == -1 && pos <= 0) {
            pos = 0.0
            direction = 1
            pickNewColor()
        }
        
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
