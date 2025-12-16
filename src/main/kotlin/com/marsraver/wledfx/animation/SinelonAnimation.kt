package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.sin

/**
 * Sinelon
 * A beat-synced dot moving back and forth with a fading trail.
 * Like Larson Scanner but using sine waves and auto-color.
 */
class SinelonAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    
    private var hue: Int = 0
    private var startTimeNs: Long = 0L
    
    override fun getName(): String = "Sinelon"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false

    override fun onInit() {
        this.leds = Array(pixelCount) { RgbColor.BLACK }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
    }
    
    override fun update(now: Long): Boolean {
        // Fade everything a lot
        val fadeAmt = (paramSpeed / 10) + 5
        for (i in 0 until pixelCount) {
            val c = leds[i]
            leds[i] = RgbColor(
                (c.r - fadeAmt).coerceAtLeast(0),
                (c.g - fadeAmt).coerceAtLeast(0),
                (c.b - fadeAmt).coerceAtLeast(0)
            )
        }
        
        // Calculate position
        // beatsin16 equivalent
        // BPM = speed?
        val time = (now - startTimeNs) / 1_000_000_000.0
        val bpm = paramSpeed / 2.0 // 0-128 bpm approx
        val freq = bpm / 60.0
        
        val s = sin(time * 2 * Math.PI * freq)
        val pos = ((s + 1.0) / 2.0 * (pixelCount - 1)).toInt()
        
        // Cycle hue
        hue = (hue + 1) % 256
        leds[pos] = ColorUtils.hsvToRgb(hue, 255, 255)
        
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) {
            return leds[index]
        }
        return RgbColor.BLACK
    }
}
