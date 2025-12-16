package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * ICU Animation
 * Eyes scanning from center to edges.
 */
class ICUAnimation : BaseAnimation() {
    
    private var pos: Double = 0.0
    private var direction: Int = 1
    
    private lateinit var leds: Array<RgbColor>
    private var lastTimeNs: Long = 0L

    override fun getName(): String = "ICU"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false

    override fun onInit() {
        super.onInit()
        leds = Array(pixelCount) { RgbColor.BLACK }
        lastTimeNs = System.nanoTime()
        pos = 0.0
        direction = 1
    }

    override fun update(now: Long): Boolean {
        if (lastTimeNs == 0L) lastTimeNs = now
        val deltaS = (now - lastTimeNs) / 1_000_000_000.0
        lastTimeNs = now
        
        // Fade
        for (i in 0 until pixelCount) {
            leds[i] = com.marsraver.wledfx.color.ColorUtils.fade(leds[i], 25)
        }
        
        val half = pixelCount / 2
        val move = (paramSpeed / 255.0) * half * deltaS * 2.0
        
        pos += move * direction
        
        // Ping Pong between 0 and half-length
        if (pos >= half - 1) {
            pos = (half - 1).toDouble()
            direction = -1
        } else if (pos <= 0) {
            pos = 0.0
            direction = 1
        }
        
        // Center Out Logic
        val offset = pos.toInt()
        val left = (half - 1 - offset).coerceIn(0, pixelCount - 1)
        val right = (half + offset).coerceIn(0, pixelCount - 1)
        
        val c = if (paramPalette != null) getColorFromPalette(128) else paramColor
        
        if (left in 0 until pixelCount) leds[left] = c
        if (right in 0 until pixelCount) leds[right] = c

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
