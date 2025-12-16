package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.sin

/**
 * Juggle
 * Eight colored dots, weaving in and out of sync with each other.
 */
class JuggleAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    private var startTimeNs: Long = 0L
    
    override fun getName(): String = "Juggle"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false

    override fun onInit() {
        leds = Array(pixelCount) { RgbColor.BLACK }
        startTimeNs = System.nanoTime()
    }
    
    override fun update(now: Long): Boolean {
        // Fade everything
        val fadeAmt = (paramSpeed / 10) + 10
        // Use ColorUtils.fade
        for (i in 0 until pixelCount) {
            leds[i] = ColorUtils.fade(leds[i], fadeAmt)
        }
        
        val time = (now - startTimeNs) / 1_000_000_000.0
        
        for (i in 0 until 6) {
            val bpm = (paramSpeed / 10.0) + i
            val freq = bpm / 60.0
            
            val s = sin(time * 2 * Math.PI * freq + i)
            val pos = ((s + 1.0) / 2.0 * (pixelCount - 1)).toInt()
            
            val dotColor = ColorUtils.hsvToRgb((i * 42) % 256, 255, 255)
            
            val existing = leds[pos]
            leds[pos] = RgbColor(
                maxOf(existing.r, dotColor.r),
                maxOf(existing.g, dotColor.g),
                maxOf(existing.b, dotColor.b)
            )
        }
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
