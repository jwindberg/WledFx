package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.audio.LoudnessMeter
import java.util.Random
import kotlin.math.*

/**
 * Blurz animation - Random pixels with blur effect.
 */
class BlurzAnimation : BaseAnimation() {

    private lateinit var pixelBrightness: Array<ByteArray>
    private lateinit var pixelHue: Array<ByteArray>
    private var currentHue: Int = 0
    private var loudnessMeter: LoudnessMeter? = null
    
    private var lastPixelTime: Long = 0
    private var lastFadeTime: Long = 0
    
    private val RANDOM = kotlin.random.Random.Default

    override fun getName(): String = "Blurz"
    override fun isAudioReactive(): Boolean = true
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelBrightness = Array(width) { ByteArray(height) }
        pixelHue = Array(width) { ByteArray(height) }
        lastPixelTime = 0
        lastFadeTime = 0
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        if (now - lastFadeTime > 100_000_000L) {
             val fadeSpeed = 20
             for (x in 0 until width) {
                for (y in 0 until height) {
                    val brightness = (pixelBrightness[x][y].toInt() and 0xFF)
                    if (brightness > 0) {
                        val updated = max(0, brightness - fadeSpeed)
                        pixelBrightness[x][y] = updated.toByte()
                    }
                }
             }
             lastFadeTime = now
        }
        
        val pixelInterval = 50_000_000L
        if (now - lastPixelTime > pixelInterval) {
            val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
            val levelSnapshot = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
            val threshold = 50
            
            if (levelSnapshot < threshold) return true
            
            val spikeAmount = levelSnapshot - threshold
            val adjustedBrightness = min(255, max(150, spikeAmount * 5))
            
            currentHue = (currentHue + 15) % 255
            val pixelsPerFrame = 5
            
            repeat(pixelsPerFrame) {
                val randomX = RANDOM.nextInt(width)
                val randomY = RANDOM.nextInt(height)
                val spotRadius = 1 + RANDOM.nextInt(2)

                for (dx in -spotRadius..spotRadius) {
                    for (dy in -spotRadius..spotRadius) {
                        val px = randomX + dx
                        val py = randomY + dy
                        val distance = sqrt((dx * dx + dy * dy).toDouble())
                        if (px in 0 until width && py in 0 until height && distance <= spotRadius) {
                            val fadeFactor = 1.0 - (distance / spotRadius)
                            val spotBrightness = (adjustedBrightness * fadeFactor).roundToInt()
                            val existing = pixelBrightness[px][py].toInt() and 0xFF
                            if (spotBrightness > existing) {
                                pixelBrightness[px][py] = spotBrightness.coerceIn(0, 255).toByte()
                                pixelHue[px][py] = currentHue.toByte()
                            }
                        }
                    }
                }
            }
            lastPixelTime = now
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val brightness = pixelBrightness[x][y].toInt() and 0xFF
        val hue = pixelHue[x][y].toInt() and 0xFF
        
        // Use BaseAnimation Palette Helper
        // Original logic boosts brightness
        val baseColor = getColorFromPalette(hue)
        val brightnessFactor = brightness / 255.0
        
        var rFinal = (baseColor.r * brightnessFactor).toInt()
        var gFinal = (baseColor.g * brightnessFactor).toInt()
        var bFinal = (baseColor.b * brightnessFactor).toInt()
        
        rFinal = min(255, (rFinal * 3) / 2)
        gFinal = min(255, (gFinal * 3) / 2)
        bFinal = min(255, (bFinal * 3) / 2)
        
        return RgbColor(rFinal, gFinal, bFinal)
    }

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }
}
