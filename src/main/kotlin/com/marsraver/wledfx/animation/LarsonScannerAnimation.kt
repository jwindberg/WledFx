package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * Larson Scanner
 * "Knight Rider" effect.
 * Single eye moving back and forth.
 */
class LarsonScannerAnimation : BaseAnimation() {
    
    private var pos: Double = 0.0
    private var direction: Int = 1
    
    override fun getName(): String = "Larson Scanner"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false // Strict 1D effect acting on 2D grid via map
    

    
    // Scanners usually render in update() directly to a buffer
    // BUT this framework seems to pull via getPixelColor.
    // However, looking at previous implementations, they often modify 'leds' array in update().
    // BaseAnimation doesn't enforce storage.
    // We should implement storage in Larson to be safe, OR compute mathematically.
    // Mathematical computation is stateless but harder for fading trails.
    // Let's stick to the stateless computed version if possible, OR keep the 'leds' array.
    
    // Actually, Larson Scanner typically needs a buffer for trails.
    // Let's add a simple buffer to this class.
    
    private lateinit var leds: Array<RgbColor>
    private var lastTimeNs: Long = 0L

    override fun onInit() {
        super.onInit()
        leds = Array(pixelCount) { RgbColor.BLACK }
        lastTimeNs = System.nanoTime()
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
        val move = (paramSpeed / 255.0) * pixelCount * deltaS * 2.0 // Tuning
        pos += move * direction
        
        if (pos >= pixelCount - 1) {
            pos = (pixelCount - 1).toDouble()
            direction = -1
        } else if (pos <= 0) {
            pos = 0.0
            direction = 1
        }
        
        // Draw Eyes
        val idx = pos.toInt()
        val c = if (paramPalette != null) getColorFromPalette(idx % 255) else paramColor
        
        if (idx in 0 until pixelCount) leds[idx] = c
        // Anti-aliasing / Neighbors?
        // Keep simple for refactor.
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
