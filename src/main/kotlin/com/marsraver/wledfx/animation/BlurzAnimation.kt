package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.LoudnessMeter
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Blurz animation - Random pixels with blur effect.
 */
class BlurzAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null

    private var pixelBrightness: Array<ByteArray> = emptyArray()
    private var pixelHue: Array<ByteArray> = emptyArray()
    private var currentHue: Int = 0

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    private var fadeSpeed: Int = 20
    private var blurIntensity: Int = 10
    private var lastPixelTime: Long = 0
    private var pixelInterval: Long = 50_000_000L
    private var pixelsPerFrame: Int = 5

    private var audioIndex: Int = 0
    private var lastFadeTime: Long = 0

    private var loudnessMeter: LoudnessMeter? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelBrightness = Array(combinedWidth) { ByteArray(combinedHeight) }
        pixelHue = Array(combinedWidth) { ByteArray(combinedHeight) }
        lastPixelTime = 0
        lastFadeTime = 0
        
        loudnessMeter = LoudnessMeter()
    }

    override fun update(now: Long): Boolean {
        if (now - lastFadeTime > 100_000_000L) {
            for (x in 0 until combinedWidth) {
                for (y in 0 until combinedHeight) {
                    val brightness = (pixelBrightness[x][y].toInt() and 0xFF)
                    if (brightness > 0) {
                        val updated = max(0, brightness - fadeSpeed)
                        pixelBrightness[x][y] = updated.toByte()
                    }
                }
            }
            lastFadeTime = now
        }

        if (now - lastPixelTime > pixelInterval) {
            // Get loudness (0-1024) and convert to 0-255 range
            val loudness = loudnessMeter?.getCurrentLoudness() ?: 0
            val levelSnapshot = (loudness / 1024.0f * 255.0f).toInt().coerceIn(0, 255)
            
            // Use a simple threshold - LoudnessMeter already provides normalized values
            val threshold = 50  // Simple threshold for triggering pixels
            
            if (levelSnapshot < threshold) {
                return true
            }

            val spikeAmount = levelSnapshot - threshold
            val adjustedBrightness = min(255, max(150, spikeAmount * 5))

            currentHue = (currentHue + 15) % 255

            repeat(pixelsPerFrame) {
                val randomX = RANDOM.nextInt(combinedWidth)
                val randomY = RANDOM.nextInt(combinedHeight)
                val spotRadius = 1 + RANDOM.nextInt(2)

                for (dx in -spotRadius..spotRadius) {
                    for (dy in -spotRadius..spotRadius) {
                        val px = randomX + dx
                        val py = randomY + dy
                        val distance = sqrt((dx * dx + dy * dy).toDouble())
                        if (px in 0 until combinedWidth && py in 0 until combinedHeight && distance <= spotRadius) {
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

            audioIndex = (audioIndex + 1) % 16
            lastPixelTime = now
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val brightness = pixelBrightness[x][y].toInt() and 0xFF
        val hue = pixelHue[x][y].toInt() and 0xFF

        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            // Use palette
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            var rFinal = (baseColor.r * brightnessFactor).toInt()
            var gFinal = (baseColor.g * brightnessFactor).toInt()
            var bFinal = (baseColor.b * brightnessFactor).toInt()
            
            // Apply the same brightness boost as original
            rFinal = min(255, (rFinal * 3) / 2)
            gFinal = min(255, (gFinal * 3) / 2)
            bFinal = min(255, (bFinal * 3) / 2)
            
            return RgbColor(rFinal.coerceIn(0, 255), gFinal.coerceIn(0, 255), bFinal.coerceIn(0, 255))
        } else {
            // Use HSV conversion (original behavior)
            val h = hue / 255.0 * 360.0
            val v = brightness / 255.0
            val c = v
            val xComponent = c * (1 - abs((h / 60.0 % 2) - 1))
            val m = v - c

            val (r, g, b) = when {
                h < 60 -> Triple(c, xComponent, 0.0)
                h < 120 -> Triple(xComponent, c, 0.0)
                h < 180 -> Triple(0.0, c, xComponent)
                h < 240 -> Triple(0.0, xComponent, c)
                h < 300 -> Triple(xComponent, 0.0, c)
                else -> Triple(c, 0.0, xComponent)
            }

            var rFinal = ((r + m) * 255).roundToInt()
            var gFinal = ((g + m) * 255).roundToInt()
            var bFinal = ((b + m) * 255).roundToInt()

            rFinal = min(255, (rFinal * 3) / 2)
            gFinal = min(255, (gFinal * 3) / 2)
            bFinal = min(255, (bFinal * 3) / 2)

            return RgbColor(rFinal, gFinal, bFinal)
        }
    }

    override fun getName(): String = "Blurz"

    override fun isAudioReactive(): Boolean = true

    override fun cleanup() {
        loudnessMeter?.stop()
        loudnessMeter = null
    }

    companion object {
        private val RANDOM = Random()
    }
}

