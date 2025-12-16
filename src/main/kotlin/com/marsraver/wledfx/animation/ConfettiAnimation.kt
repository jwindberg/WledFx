package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Confetti
 * Random colored specks that blink in and fade smoothly.
 */
class ConfettiAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    private var hue: Int = 0
    
    override fun getName(): String = "Confetti"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    
    override fun onInit() {
        leds = Array(pixelCount) { RgbColor.BLACK }
    }
    
    override fun update(now: Long): Boolean {
        // Fade all
        val fadeAmt = 1 + (paramSpeed / 32)
        for (i in 0 until pixelCount) {
             leds[i] = ColorUtils.fade(leds[i], fadeAmt)
        }
        
        if (Random.nextInt(50) < 25) {
             val pos = Random.nextInt(pixelCount)
             hue = (hue + 1) % 256
             val h = (hue + Random.nextInt(64)) % 256
             leds[pos] = ColorUtils.hsvToRgb(h, 255, 255)
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
