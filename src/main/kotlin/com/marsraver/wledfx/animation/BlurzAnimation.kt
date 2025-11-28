package com.marsraver.wledfx.animation

import com.marsraver.wledfx.audio.AudioPipeline
import java.util.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    private var palette: Array<IntArray>? = null

    private var pixelBrightness: Array<ByteArray> = emptyArray()
    private var pixelHue: Array<ByteArray> = emptyArray()
    private var currentHue: Int = 0

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Array<IntArray>) {
        this.palette = palette
    }

    private var fadeSpeed: Int = 20
    private var blurIntensity: Int = 10
    private var lastPixelTime: Long = 0
    private var pixelInterval: Long = 50_000_000L
    private var pixelsPerFrame: Int = 5

    private var audioIndex: Int = 0
    private var lastFadeTime: Long = 0

    @Volatile
    private var currentSoundLevel: Int = 0
    private var soundLevelWindow: IntArray = IntArray(WINDOW_SIZE)
    private var soundLevelIndex: Int = 0
    private val soundLevelLock = Any()
    private var audioScope: CoroutineScope? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelBrightness = Array(combinedWidth) { ByteArray(combinedHeight) }
        pixelHue = Array(combinedWidth) { ByteArray(combinedHeight) }
        lastPixelTime = 0
        lastFadeTime = 0
        soundLevelWindow = IntArray(WINDOW_SIZE)
        soundLevelIndex = 0

        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.rmsFlow().collectLatest { level ->
                    synchronized(soundLevelLock) {
                        currentSoundLevel = level.level
                        soundLevelWindow[soundLevelIndex] = currentSoundLevel
                        soundLevelIndex = (soundLevelIndex + 1) % WINDOW_SIZE
                    }
                }
            }
        }
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
            val (levelSnapshot, windowSnapshot) = synchronized(soundLevelLock) {
                currentSoundLevel to soundLevelWindow.clone()
            }
            val sortedWindow = windowSnapshot.sortedArray()
            val percentileIndex = (WINDOW_SIZE * 0.75).roundToInt().coerceAtMost(WINDOW_SIZE - 1)
            val threshold = sortedWindow[percentileIndex]

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

    override fun getPixelColor(x: Int, y: Int): IntArray {
        val brightness = pixelBrightness[x][y].toInt() and 0xFF
        val hue = pixelHue[x][y].toInt() and 0xFF

        val currentPalette = palette
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            // Use palette
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            var rFinal = (baseColor[0] * brightnessFactor).toInt()
            var gFinal = (baseColor[1] * brightnessFactor).toInt()
            var bFinal = (baseColor[2] * brightnessFactor).toInt()
            
            // Apply the same brightness boost as original
            rFinal = min(255, (rFinal * 3) / 2)
            gFinal = min(255, (gFinal * 3) / 2)
            bFinal = min(255, (bFinal * 3) / 2)
            
            return intArrayOf(rFinal.coerceIn(0, 255), gFinal.coerceIn(0, 255), bFinal.coerceIn(0, 255))
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

            return intArrayOf(rFinal, gFinal, bFinal)
        }
    }

    override fun getName(): String = "Blurz"

    fun cleanup() {
        audioScope?.cancel()
        audioScope = null
        synchronized(soundLevelLock) {
            currentSoundLevel = 0
            soundLevelWindow.fill(0)
            soundLevelIndex = 0
        }
    }

    fun getSoundLevel(): Int = currentSoundLevel

    fun isMicrophoneActive(): Boolean = audioScope != null

    companion object {
        private val RANDOM = Random()
        private const val WINDOW_SIZE = 40
    }
}

