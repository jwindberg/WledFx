package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

/**
 * Chase Rainbow
 */
class ChaseRainbowAnimation : BaseAnimation() {
    
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Chase Rainbow"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    
    // Override supportsPalette? Original was false.
    // But BaseAnimation defaults to true. Let's leave it as true, 
    // but the logic here explicitly uses HSV Rainbow currently.
    // If we want to support Palette Chase, we can change the logic.
    // For faithful refactoring, let's keep it Rainbow unless palette logic is easy.
    // Let's stick to HSV logic for now as "Rainbow" implies spectrum.
    
    override fun onInit() {
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Instead of Array<RgbColor> memory, calculate on fly
        val time = (System.nanoTime() - startTimeNs) / 1_000_000_000.0
        val speedFactor = paramSpeed * 2.0 
        val shift = (time * speedFactor).toInt()
        
        val index = getPixelIndex(x, y)
        val hue = ((index * 256 / pixelCount) + shift) % 256
        return ColorUtils.hsvToRgb(hue, 255, 255)
    }
}
