package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

import com.marsraver.wledfx.audio.FftMeter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Akemi animation - renders a stylised character with audio-reactive elements and side GEQ bars.
 */
class AkemiAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<RgbColor>> = emptyArray()
    
    private var fftMeter: FftMeter? = null

    private var colorSpeed: Int = 128
    private var intensity: Int = 128

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        // Use 16 bands for side GEQ bars
        fftMeter = FftMeter(bands = 16)
    }

    override fun update(now: Long): Boolean {
        // Use normalized bands for better sensitivity across different volumes
        val spectrumSnapshot = fftMeter?.getNormalizedBands() ?: IntArray(16)
        val timeMs = now / 1_000_000L
        val speedFactor = (colorSpeed shr 2) + 2
        var counter = ((timeMs * speedFactor) and 0xFFFF).toInt()
        counter = counter shr 8

        val lightFactor = 0.15f
        val normalFactor = 0.4f

        val soundColor = RgbColor(255, 165, 0)
        val armsAndLegsDefault = RgbColor(0xFF, 0xE0, 0xA0)
        val eyeColor = RgbColor.WHITE

        val faceColor = colorWheel(counter and 0xFF)
        val armsAndLegsColor = armsAndLegsDefault

        val base = spectrumSnapshot.getOrElse(0) { 0 } / 255.0f
        // More sensitive dancing trigger: responds at lower intensities and lower audio levels
        val isDancing = intensity > 64 && spectrumSnapshot.getOrElse(0) { 0 } > 64

        if (isDancing) {
            for (x in 0 until combinedWidth) {
                setPixelColor(x, 0, RgbColor.BLACK)
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
                    1 -> armsAndLegsColor
                    6 -> multiplyColor(faceColor, lightFactor)
                    5 -> multiplyColor(faceColor, normalFactor)
                    4 -> faceColor
                    7 -> eyeColor
                    8 -> if (base > 0.2f) {
                        // Boost sound color more aggressively but still clamp to valid range
                        val boost = clamp01(base * 1.8f)
                        RgbColor(
                            min(255, (soundColor.r * boost).roundToInt()),
                            min(255, (soundColor.g * boost).roundToInt()),
                            min(255, (soundColor.b * boost).roundToInt())
                        )
                    } else {
                        armsAndLegsColor
                    }
                    else -> RgbColor.BLACK
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

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Akemi"

    override fun isAudioReactive(): Boolean = true

    override fun cleanup() {
        fftMeter?.stop()
        fftMeter = null
    }

    private fun mapValue(value: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Int {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun constrain(value: Int, minVal: Int, maxVal: Int): Int = value.coerceIn(minVal, maxVal)

    private fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)

    private fun hsvToRgb(h: Float, s: Float, v: Float): RgbColor {
        return ColorUtils.hsvToRgb(h, s, v)
    }

    private fun colorWheel(posValue: Int): RgbColor {
        var pos = ((posValue % 256) + 256) % 256
        return when {
            pos < 85 -> RgbColor(pos * 3, 255 - pos * 3, 0)
            pos < 170 -> {
                pos -= 85
                RgbColor(255 - pos * 3, 0, pos * 3)
            }
            else -> {
                pos -= 170
                RgbColor(0, pos * 3, 255 - pos * 3)
            }
        }
    }

    private fun multiplyColor(rgb: RgbColor, factor: Float): RgbColor {
        val clampedFactor = clamp01(factor)
        return RgbColor(
            min(255, (rgb.r * clampedFactor).roundToInt()),
            min(255, (rgb.g * clampedFactor).roundToInt()),
            min(255, (rgb.b * clampedFactor).roundToInt())
        )
    }

    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = rgb
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

