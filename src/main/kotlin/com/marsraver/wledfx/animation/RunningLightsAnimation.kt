package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import kotlin.math.sin

/**
 * Running Lights
 * Sine wave based running lights pattern.
 */
class RunningLightsAnimation : BaseAnimation() {
    
    private var startTimeNs: Long = 0L
    
    override fun getName(): String = "Running Lights"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false // Strict 1D effect
    
    override fun onInit() {
        startTimeNs = System.nanoTime()
        // Default color red if not set? Base default is White.
        // Original code had Red.
        if (paramColor == RgbColor.WHITE) paramColor = RgbColor.RED
    }
    
    override fun update(now: Long): Boolean {
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index !in 0 until pixelCount) return RgbColor.BLACK

        val time = (System.nanoTime() - startTimeNs) / 1_000_000_000.0
        val speedFactor = paramSpeed / 10.0 
        val pos = time * speedFactor
        
        val waveFreq = 0.2
        val level = (sin(index * waveFreq + pos) * 127 + 128).toInt() / 255.0
        
        return RgbColor(
            (paramColor.r * level).toInt(),
            (paramColor.g * level).toInt(),
            (paramColor.b * level).toInt()
        )
    }
}
