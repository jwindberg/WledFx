package com.marsraver.wledfx.animation

import com.marsraver.wledfx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Swirl animation - Audio-reactive swirling pixels with mirrored patterns.
 */
class SwirlAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<IntArray>> = emptyArray()
    private var palette: Array<IntArray>? = null

    private var speed: Int = 128
    private var intensity: Int = 64
    private var custom1: Int = 16
    private var fadeAmount: Int = 4

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Array<IntArray>) {
        this.palette = palette
    }

    @Volatile
    private var volumeSmth: Float = 0.0f
    @Volatile
    private var volumeRaw: Int = 0
    private var smoothedVolumeRaw: Int = 0
    private val volumeLock = Any()
    private var audioScope: CoroutineScope? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        synchronized(volumeLock) {
            volumeSmth = 0.0f
            volumeRaw = 0
            smoothedVolumeRaw = 0
        }
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.rmsFlow().collectLatest { level ->
                    synchronized(volumeLock) {
                        volumeSmth = level.rms.toFloat()
                        smoothedVolumeRaw = (smoothedVolumeRaw * 19 + level.level) / 20
                        volumeRaw = smoothedVolumeRaw
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val (rawVolume, smoothVolume) = synchronized(volumeLock) { volumeRaw to volumeSmth }

        if (rawVolume < NOISE_FLOOR) {
            fadeToBlack(15)
            if (custom1 > 0) applyBlur(custom1)
            return true
        }

        fadeToBlack(fadeAmount)
        if (custom1 > 0) applyBlur(custom1)

        val timeMs = now / 1_000_000
        var freq1 = (27 * speed) / 255
        var freq2 = (41 * speed) / 255
        if (freq1 < 1) freq1 = 1
        if (freq2 < 1) freq2 = 1

        val i = beatsin8(freq1, BORDER_WIDTH, combinedWidth - BORDER_WIDTH, timeMs)
        val j = beatsin8(freq2, BORDER_WIDTH, combinedHeight - BORDER_WIDTH, timeMs)
        val ni = combinedWidth - 1 - i
        val nj = combinedHeight - 1 - j

        val baseBrightness = 200
        val audioBoost = rawVolume / 2
        var brightness = (baseBrightness + audioBoost).coerceAtMost(255)
        brightness = brightness.coerceAtLeast(150)

        val paletteOffset = smoothVolume * 4.0f

        val color1 = colorFromPalette((timeMs / 11 + paletteOffset).toInt(), brightness)
        addPixelColor(i, j, color1)

        val color2 = colorFromPalette((timeMs / 13 + paletteOffset).toInt(), brightness)
        addPixelColor(j, i, color2)

        val color3 = colorFromPalette((timeMs / 17 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, nj, color3)

        val color4 = colorFromPalette((timeMs / 29 + paletteOffset).toInt(), brightness)
        addPixelColor(nj, ni, color4)

        val color5 = colorFromPalette((timeMs / 37 + paletteOffset).toInt(), brightness)
        addPixelColor(i, nj, color5)

        val color6 = colorFromPalette((timeMs / 41 + paletteOffset).toInt(), brightness)
        addPixelColor(ni, j, color6)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val color = pixelColors[x][y].clone()
            color[0] = color[0].coerceIn(0, 255)
            color[1] = color[1].coerceIn(0, 255)
            color[2] = color[2].coerceIn(0, 255)
            color
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Swirl"

    fun cleanup() {
        audioScope?.cancel()
        audioScope = null
        synchronized(volumeLock) {
            volumeSmth = 0f
            volumeRaw = 0
            smoothedVolumeRaw = 0
        }
    }

    private fun beatsin8(frequency: Int, minVal: Int, maxVal: Int, timebase: Long): Int {
        val phase = timebase * frequency / 255.0
        val sine = sin(phase)
        val range = maxVal - minVal
        return minVal + ((sine + 1.0) * range / 2.0).roundToInt()
    }

    private fun fadeToBlack(amount: Int) {
        when {
            amount <= 0 -> return
            amount >= 255 -> {
                for (x in 0 until combinedWidth) {
                    for (y in 0 until combinedHeight) {
                        pixelColors[x][y][0] = 0
                        pixelColors[x][y][1] = 0
                        pixelColors[x][y][2] = 0
                    }
                }
            }
            else -> {
                val factor = 255 - amount
                for (x in 0 until combinedWidth) {
                    for (y in 0 until combinedHeight) {
                        pixelColors[x][y][0] = pixelColors[x][y][0] * factor / 255
                        pixelColors[x][y][1] = pixelColors[x][y][1] * factor / 255
                        pixelColors[x][y][2] = pixelColors[x][y][2] * factor / 255
                    }
                }
            }
        }
    }

    private fun applyBlur(amount: Int) {
        if (amount <= 0) return
        val temp = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (c in 0 until 3) {
                    var sum = 0
                    var count = 0
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                                sum += pixelColors[nx][ny][c]
                                count++
                            }
                        }
                    }
                    val avg = if (count > 0) sum / count else pixelColors[x][y][c]
                    val original = pixelColors[x][y][c]
                    temp[x][y][c] = (original * (255 - amount) + avg * amount) / 255
                }
            }
        }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = temp[x][y][0]
                pixelColors[x][y][1] = temp[x][y][1]
                pixelColors[x][y][2] = temp[x][y][2]
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
            pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
            pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
        }
    }

    private fun colorFromPalette(colorIndexValue: Int, brightness: Int): IntArray {
        val currentPalette = palette
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            var colorIndex = colorIndexValue % 256
            if (colorIndex < 0) colorIndex += 256
            val paletteIndex = (colorIndex / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return intArrayOf(
                (baseColor[0] * brightnessFactor).toInt().coerceIn(0, 255),
                (baseColor[1] * brightnessFactor).toInt().coerceIn(0, 255),
                (baseColor[2] * brightnessFactor).toInt().coerceIn(0, 255)
            )
        } else {
            var colorIndex = colorIndexValue % 256
            if (colorIndex < 0) colorIndex += 256
            val h = colorIndex / 255.0f * 360.0f
            val s = 1.0f
            val v = brightness / 255.0f
            return hsvToRgb(h, s, v)
        }
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val hi = ((h / 60f) % 6).toInt()
        val f = h / 60f - hi
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)

        val (r, g, b) = when (hi) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }

        return intArrayOf(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    companion object {
        private const val BORDER_WIDTH = 2
        private const val NOISE_FLOOR = 100
    }
}

