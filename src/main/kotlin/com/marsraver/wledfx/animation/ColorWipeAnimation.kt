package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor

/**
 * Color Wipe.
 * Fills the strip one by one.
 */
class ColorWipeAnimation : BaseAnimation() {
    
    private var pos: Double = 0.0
    private var lastTimeNs: Long = 0L

    override fun getName(): String = "Color Wipe"
    override fun is1D(): Boolean = true
    // Wipe is a classic 1D effect, looks like snake on matrix if not remapped.
    // BaseAnimation provides default Serpentine map, so it will fill row by row (zigzag).
    override fun is2D(): Boolean = false 
    
    override fun onInit() {
        super.onInit()
        pos = 0.0
        lastTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (lastTimeNs == 0L) lastTimeNs = now
        val deltaS = (now - lastTimeNs) / 1_000_000_000.0
        lastTimeNs = now
        
        // Speed: 128 = ~1 led per frame? 
        // WLED wipe speed roughly: full strip in (256-speed) * something
        // Let's move pixels per second = (paramSpeed / 255) * pixelCount
        
        val move = maxOf(1.0, (paramSpeed / 255.0) * pixelCount * 0.5) * deltaS * 10.0
        pos += move
        
        if (pos >= pixelCount) {
             pos = 0.0
             // In WLED wipe usually toggles color or palette index. 
             // We'll reset for now.
        }
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index < pos.toInt()) {
            return paramColor
        }
        return RgbColor.BLACK
    }
}
