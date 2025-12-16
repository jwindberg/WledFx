package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Twinkle Random
 * Random pixels fade in and out with RANDOM colors.
 */
class TwinkleRandomAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    
    override fun getName(): String = "Twinkle Random"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    override fun supportsColor(): Boolean = false // Always random

    override fun onInit() {
        leds = Array(pixelCount) { RgbColor.BLACK }
        paramSpeed = 128
    }
    
    override fun update(now: Long): Boolean {
        // Randomly light up new pixels
        val chance = paramSpeed / 4 + 1
        if (Random.nextInt(255) < chance) {
            val idx = Random.nextInt(pixelCount)
            // Pick random color
            leds[idx] = if (getPalette() != null) {
                // Random from palette
                val pIdx = Random.nextInt(getPalette()!!.colors.size)
                getPalette()!!.colors[pIdx]
            } else {
                // Random HSV
                ColorUtils.hsvToRgb(Random.nextInt(255), 255, 255)
            }
        }
        
        // Fade all pixels
        for (i in 0 until pixelCount) {
            val c = leds[i]
            if (c.r > 0 || c.g > 0 || c.b > 0) {
                // Fade speed
                val fade = 10 + (paramSpeed / 10)
                leds[i] = RgbColor(
                    (c.r - fade).coerceAtLeast(0),
                    (c.g - fade).coerceAtLeast(0),
                    (c.b - fade).coerceAtLeast(0)
                )
            }
        }
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
