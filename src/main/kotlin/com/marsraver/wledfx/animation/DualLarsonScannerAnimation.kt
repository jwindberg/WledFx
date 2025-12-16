package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * Dual Larson Scanner
 * Two eyes moving in opposite directions.
 */
class DualLarsonScannerAnimation : BaseAnimation() {
    
    // Similar to Larson but two positions
    private var pos: Double = 0.0
    private var direction: Int = 1
    
    private lateinit var leds: Array<RgbColor>
    private var lastTimeNs: Long = 0L

    override fun getName(): String = "Dual Larson Scanner"
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
            leds[i] = com.marsraver.wledfx.color.ColorUtils.fade(leds[i], 20)
        }
        
        // Move
        val move = (paramSpeed / 255.0) * pixelCount * deltaS * 2.0
        pos += move * direction
        
        if (pos >= pixelCount - 1) {
            pos = (pixelCount - 1).toDouble()
            direction = -1
        } else if (pos <= 0) {
            pos = 0.0
            direction = 1
        }
        
        // Draw Eye 1
        val idx1 = pos.toInt()
        val c = if (paramPalette != null) getColorFromPalette(0) else paramColor // Use palette index 0?
        if (idx1 in 0 until pixelCount) leds[idx1] = c
        
        // Draw Eye 2 (Opposite)
        val idx2 = (pixelCount - 1 - idx1).coerceIn(0, pixelCount - 1)
        if (idx2 in 0 until pixelCount) leds[idx2] = c
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
