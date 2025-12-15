package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Twinkle Fade
 * Random pixels fade IN and OUT smoothly.
 */
class TwinkleFadeAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    private lateinit var pixelState: IntArray 
    
    override fun getName(): String = "Twinkle Fade"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    override fun supportsColor(): Boolean = true

    override fun onInit() {
        this.leds = Array(pixelCount) { RgbColor.BLACK }
        this.pixelState = IntArray(pixelCount) { 0 }
        paramSpeed = 128
    }
    
    override fun update(now: Long): Boolean {
        // 1. Pick new pixels if available
        val chance = (paramSpeed / 16) + 1
        repeat(chance) {
            if (Random.nextInt(255) < 32) {
                val idx = Random.nextInt(pixelCount)
                if (pixelState[idx] == 0) {
                     // Start Fading IN (State = 1)
                     pixelState[idx] = 1
                     // Pick Color
                     leds[idx] = if (getPalette() != null) {
                         val pIdx = Random.nextInt(getPalette()!!.colors.size)
                         getPalette()!!.colors[pIdx]
                     } else {
                         paramColor
                     }
                }
            }
        }
        
        // 2. Update Fades
        val fadeSpeed = 4 + (paramSpeed / 32) // Step size
        
        for (i in 0 until pixelCount) {
            val state = pixelState[i]
            if (state == 0) {
                 // leds[i] = RgbColor.BLACK // Not strictly needed if getPixelColor handles it, but safe
                 continue
            }
            
            if (state < 256) {
                // Fading IN
                val newBri = state + fadeSpeed
                if (newBri >= 255) {
                    pixelState[i] = 256
                } else {
                    pixelState[i] = newBri
                }
            } else {
                // Fading OUT (State 256..511)
                val currBri = 511 - state
                var newBri = currBri - fadeSpeed
                if (newBri <= 0) {
                    pixelState[i] = 0
                } else {
                    pixelState[i] = 511 - newBri
                }
            }
        }
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index !in 0 until pixelCount) return RgbColor.BLACK
        
        val state = pixelState[index]
        if (state == 0) return RgbColor.BLACK
        
        // Calculate brightness from state
        val brightness = if (state < 256) state else (511 - state)
        
        // Scale the stored base color
        val base = leds[index]
        return ColorUtils.scaleBrightness(base, brightness / 255.0)
    }
}
