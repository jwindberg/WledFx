package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Twinkle Animation
 * Random pixels fade in and out.
 */
class TwinkleAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    
    override fun getName(): String = "Twinkle"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    override fun supportsColor(): Boolean = true
    
    override fun onInit() {
        leds = Array(pixelCount) { RgbColor.BLACK }
        paramSpeed = 128
    }
    
    override fun update(now: Long): Boolean {
        // Randomly light up new pixels
        val chance = paramSpeed / 4 + 1
        if (Random.nextInt(255) < chance) {
            val idx = Random.nextInt(pixelCount)
            leds[idx] = paramColor
        }
        
        // Fade all pixels
        val fade = 10 + (paramSpeed / 10)
        for (i in 0 until pixelCount) {
             leds[i] = ColorUtils.fade(leds[i], fade)
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
