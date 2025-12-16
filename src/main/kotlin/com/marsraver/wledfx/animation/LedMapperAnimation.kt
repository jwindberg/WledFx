package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor

/**
 * Maps LED indices by lighting one LED at a time in sequence.
 * Use this to see which physical LED corresponds to which index.
 */
class LedMapperAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentLedIndex: Int = 0
    private var lastUpdateTime: Long = 0
    private val LED_CHANGE_INTERVAL_MS = 2000L // 2 seconds per LED

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        this.currentLedIndex = 0
        this.lastUpdateTime = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastUpdateTime >= LED_CHANGE_INTERVAL_MS) {
            currentLedIndex = (currentLedIndex + 1) % 256
            lastUpdateTime = nowMs
            println("LedMapper: Now showing LED index $currentLedIndex")
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Returns black for all - we'll set the LED directly in render loop
        return RgbColor.BLACK
    }

    override fun getName(): String = "LED Mapper"
    
    fun getCurrentLedIndex(): Int = currentLedIndex
}

