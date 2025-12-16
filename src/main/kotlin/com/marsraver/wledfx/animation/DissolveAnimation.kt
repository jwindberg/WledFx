package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import kotlin.random.Random

/**
 * Dissolve Animation
 */
class DissolveAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    private var targetState: Boolean = true // true = filling to color, false = filling to black
    private var filledCount: Int = 0

    override fun getName(): String = "Dissolve"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    override fun supportsColor(): Boolean = true
    override fun supportsPalette(): Boolean = false

    override fun onInit() {
        leds = Array(pixelCount) { RgbColor.BLACK }
        filledCount = 0
    }

    override fun update(now: Long): Boolean {
        // changeCount based on paramSpeed
        val changeCount = 1 + (paramSpeed / 16)
        
        val targetColor = if (targetState) paramColor else RgbColor.BLACK
        
        var changesMade = 0
        for (i in 0 until changeCount * 2) {
            val idx = Random.nextInt(pixelCount)
            if (leds[idx] != targetColor) {
                leds[idx] = targetColor
                changesMade++
                filledCount = if (targetState) filledCount + 1 else filledCount - 1
            }
            if (changesMade >= changeCount) break
        }
        
        if (changesMade == 0) {
            val isFull = leds.all { it == targetColor }
            if (isFull) targetState = !targetState
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
