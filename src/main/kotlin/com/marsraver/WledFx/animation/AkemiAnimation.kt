package com.marsraver.WledFx.animation

import com.marsraver.WledFx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Akemi animation - renders a stylised character with audio-reactive elements and side GEQ bars.
 */
class AkemiAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<IntArray>> = emptyArray()

    private val fftResult = IntArray(16)
    private val spectrumLock = Any()
    private var audioScope: CoroutineScope? = null

    private var colorSpeed: Int = 128
    private var intensity: Int = 128

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.spectrumFlow(bands = 16).collectLatest { spectrum ->
                    synchronized(spectrumLock) {
                        val bands = spectrum.bands
                        for (i in fftResult.indices) {
                            val incoming = bands.getOrNull(i) ?: 0
                            fftResult[i] = ((fftResult[i] * 3) + incoming) / 4
                        }
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val spectrumSnapshot = synchronized(spectrumLock) { fftResult.clone() }
        val timeMs = now / 1_000_000L
        val speedFactor = (colorSpeed shr 2) + 2
        var counter = ((timeMs * speedFactor) and 0xFFFF).toInt()
        counter = counter shr 8

        val lightFactor = 0.15f
        val normalFactor = 0.4f

        val soundColor = intArrayOf(255, 165, 0)
        val armsAndLegsDefault = intArrayOf(0xFF, 0xE0, 0xA0)
        val eyeColor = intArrayOf(255, 255, 255)

        val faceColor = colorWheel(counter and 0xFF)
        val armsAndLegsColor = armsAndLegsDefault.clone()

        val base = spectrumSnapshot.getOrElse(0) { 0 } / 255.0f
        val isDancing = intensity > 128 && spectrumSnapshot.getOrElse(0) { 0 } > 128

        if (isDancing) {
            for (x in 0 until combinedWidth) {
                setPixelColor(x, 0, intArrayOf(0, 0, 0))
            }
        }

        for (y in 0 until combinedHeight) {
            val akY = min(BASE_HEIGHT - 1, y * BASE_HEIGHT / combinedHeight)
            for (x in 0 until combinedWidth) {
                val akX = min(BASE_WIDTH - 1, x * BASE_WIDTH / combinedWidth)
                val ak = AKEMI_MAP[akY * BASE_WIDTH + akX]

                val color = when (ak) {
                    3 -> multiplyColor(armsAndLegsColor, lightFactor)
                    2 -> multiplyColor(armsAndLegsColor, normalFactor)
                    1 -> armsAndLegsColor.clone()
                    6 -> multiplyColor(faceColor, lightFactor)
                    5 -> multiplyColor(faceColor, normalFactor)
                    4 -> faceColor.clone()
                    7 -> eyeColor.clone()
                    8 -> if (base > 0.4f) {
                        val boost = clamp01(base)
                        intArrayOf(
                            min(255, (soundColor[0] * boost).roundToInt()),
                            min(255, (soundColor[1] * boost).roundToInt()),
                            min(255, (soundColor[2] * boost).roundToInt())
                        )
                    } else {
                        armsAndLegsColor.clone()
                    }
                    else -> intArrayOf(0, 0, 0)
                }

                if (isDancing) {
                    val targetY = min(combinedHeight - 1, y + 1)
                    setPixelColor(x, targetY, color)
                } else {
                    setPixelColor(x, y, color)
                }
            }
        }

        val xMax = max(1, combinedWidth / 8)
        val midY = combinedHeight / 2
        val maxBarHeight = max(1, 17 * combinedHeight / 32)

        for (x in 0 until xMax) {
            var band = mapValue(x, 0, max(xMax, 4), 0, 15)
            band = constrain(band, 0, 15)
            var barHeight = mapValue(spectrumSnapshot.getOrElse(band) { 0 }, 0, 255, 0, maxBarHeight)
            barHeight = barHeight.coerceIn(0, maxBarHeight)

            val colorIndex = band * 35
            val barColor = hsvToRgb((colorIndex % 256) * (360f / 255f), 1.0f, 1.0f)

            for (y in 0 until barHeight) {
                val topY = midY - y
                if (topY in 0 until combinedHeight) {
                    setPixelColor(x, topY, barColor)
                    val mirrorX = combinedWidth - 1 - x
                    setPixelColor(mirrorX, topY, barColor)
                }
            }
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y].clone()
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Akemi"

    fun cleanup() {
        audioScope?.cancel()
        audioScope = null
        synchronized(spectrumLock) {
            fftResult.fill(0)
        }
    }

    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun constrain(value: Int, minVal: Int, maxVal: Int): Int = value.coerceIn(minVal, maxVal)

    private fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        var hue = h % 360f
        if (hue < 0) hue += 360f
        val hi = ((hue / 60f) % 6).toInt()
        val f = hue / 60f - hi
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

    private fun colorWheel(posValue: Int): IntArray {
        var pos = ((posValue % 256) + 256) % 256
        return when {
            pos < 85 -> intArrayOf(pos * 3, 255 - pos * 3, 0)
            pos < 170 -> {
                pos -= 85
                intArrayOf(255 - pos * 3, 0, pos * 3)
            }
            else -> {
                pos -= 170
                intArrayOf(0, pos * 3, 255 - pos * 3)
            }
        }
    }

    private fun multiplyColor(rgb: IntArray, factor: Float): IntArray {
        val clampedFactor = clamp01(factor)
        return intArrayOf(
            min(255, (rgb[0] * clampedFactor).roundToInt()),
            min(255, (rgb[1] * clampedFactor).roundToInt()),
            min(255, (rgb[2] * clampedFactor).roundToInt())
        )
    }

    private fun setPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y][0] = rgb[0].coerceIn(0, 255)
            pixelColors[x][y][1] = rgb[1].coerceIn(0, 255)
            pixelColors[x][y][2] = rgb[2].coerceIn(0, 255)
        }
    }

    companion object {
        private const val BASE_WIDTH = 32
        private const val BASE_HEIGHT = 32
        private val AKEMI_MAP = intArrayOf(
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,2,2,3,3,3,3,3,3,2,2,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,2,3,3,0,0,0,0,0,0,3,3,2,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,2,3,0,0,0,6,5,5,4,0,0,0,3,2,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,0,6,6,5,5,5,5,4,4,0,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,2,3,0,6,5,5,5,5,5,5,5,5,5,5,4,0,3,2,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,3,2,0,6,5,5,5,5,5,5,5,5,5,5,4,0,2,3,0,0,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,7,7,5,5,5,5,7,7,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,1,7,7,7,5,5,1,7,7,7,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,8,3,1,3,6,5,5,1,1,5,5,5,5,1,1,5,5,4,3,1,3,8,0,0,0,0,0,
            0,0,0,0,0,2,3,1,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,1,3,2,0,0,0,0,0,
            0,0,0,0,0,0,3,2,3,6,5,5,5,5,5,5,5,5,5,5,5,5,4,3,2,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,7,7,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,0,
            1,0,0,0,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,0,0,0,2,
            0,2,2,2,0,0,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,0,0,2,2,2,0,
            0,0,0,3,2,0,0,0,6,5,4,4,4,4,4,4,4,4,4,4,4,4,4,4,0,0,0,2,2,0,0,0,
            0,0,0,3,2,0,0,0,6,5,5,5,5,5,5,5,5,5,5,5,5,5,5,4,0,0,0,2,3,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,3,0,3,3,0,0,3,3,0,3,3,0,0,0,0,2,2,0,0,0,0,
            0,0,0,0,3,2,0,0,0,0,3,2,0,3,2,0,0,3,2,0,3,2,0,0,0,0,2,3,0,0,0,0,
            0,0,0,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,3,2,0,0,2,3,0,0,0,0,0,
            0,0,0,0,0,3,2,2,2,2,0,0,0,3,2,0,0,3,2,0,0,0,3,2,2,2,3,0,0,0,0,0,
            0,0,0,0,0,0,3,3,3,0,0,0,0,3,2,0,0,3,2,0,0,0,0,3,3,3,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
        )
    }
}

