package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import kotlin.random.Random

/**
 * Flash Sparkle
 * Background strobe (Flash) + Sparkles.
 */
class FlashSparkleAnimation : BaseAnimation() {
    
    private lateinit var leds: Array<RgbColor>
    private var startTimeNs: Long = 0L
    
    override fun getName(): String = "Flash Sparkle"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false
    override fun supportsColor(): Boolean = true
    
    override fun onInit() {
        leds = Array(pixelCount) { RgbColor.BLACK }
        startTimeNs = System.nanoTime()
    }
    
    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        val timeMs = (now - startTimeNs) / 1_000_000
        
        val frequency = 1 + (paramSpeed / 10)
        val periodMs = 10000 / frequency
        val phase = timeMs % periodMs
        val onDuration = 50L
        
        if (phase < onDuration) {
            // Flash ON (White)
            for (i in 0 until pixelCount) {
                leds[i] = RgbColor.WHITE
            }
        } else {
            // Fade
            for (i in 0 until pixelCount) {
                val c = leds[i]
                leds[i] = RgbColor(
                    (c.r * 200) / 255, 
                    (c.g * 200) / 255, 
                    (c.b * 200) / 255
                )
            }
            
            // Sparkles
            val chance = paramSpeed / 3 + 1
            if (Random.nextInt(255) < chance) {
                val idx = Random.nextInt(pixelCount)
                leds[idx] = getAllowedColor()
            }
        }
        return true
    }
    
    private fun getAllowedColor(): RgbColor {
        // Use paramColor or palette
        return if (getPalette() != null) {
            getPalette()!!.colors[Random.nextInt(getPalette()!!.colors.size)]
        } else {
            if (paramColor != RgbColor.BLACK && paramColor != RgbColor.WHITE) paramColor 
            else RgbColor(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
        }
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
