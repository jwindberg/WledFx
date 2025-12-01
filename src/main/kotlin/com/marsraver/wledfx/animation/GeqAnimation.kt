package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils

import com.marsraver.wledfx.audio.AudioPipeline
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
 * GEQ (Graphic Equalizer) animation - 2D frequency band visualization.
 */
class GeqAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<RgbColor>> = emptyArray()

    private var numBands: Int = 16
    private var centerBin: Int = 0
    private var speed: Int = 128
    private var intensity: Int = 128
    private var colorBars: Boolean = false
    private val peakColor = RgbColor.WHITE
    private var noiseFloor: Int = 50
    private var maxFFTValue: Int = 255

    private lateinit var previousBarHeight: IntArray
    private var lastRippleTime: Long = 0
    private var callCount: Long = 0

    private val fftResult = IntArray(16)
    private val spectrumLock = Any()
    private var audioScope: CoroutineScope? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        previousBarHeight = IntArray(combinedWidth)
        lastRippleTime = 0
        callCount = 0
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
        callCount++

        val spectrumSnapshot = synchronized(spectrumLock) { fftResult.clone() }

        var rippleTime = false
        var rippleInterval = 256 - intensity
        if (rippleInterval < 1) rippleInterval = 1
        if (now - lastRippleTime >= rippleInterval * 1_000_000L) {
            lastRippleTime = now
            rippleTime = true
        }

        val fadeoutDelay = (256 - speed) / 64
        if (fadeoutDelay <= 1 || callCount % fadeoutDelay.toLong() == 0L) {
            fadeToBlack(speed)
        }

        for (x in 0 until combinedWidth) {
            var band = mapValue(x, 0, combinedWidth, 0, numBands)
            if (numBands < 16) {
                val startBin = constrain(centerBin - numBands / 2, 0, 15 - numBands + 1)
                band = if (numBands <= 1) {
                    centerBin
                } else {
                    mapValue(band, 0, numBands - 1, startBin, startBin + numBands - 1)
                }
            }
            band = constrain(band, 0, 15)

            var colorIndex = band * 17
            val bandValue = spectrumSnapshot.getOrElse(band) { 0 }
            val adjustedFFT = max(0, bandValue - noiseFloor)
            val boostedFFT = adjustedFFT * 2
            val effectiveMax = maxFFTValue - noiseFloor

            var barHeight = 0
            if (boostedFFT > 0) {
                val cappedBoosted = min(boostedFFT, effectiveMax)
                barHeight = mapValue(cappedBoosted, 0, effectiveMax, 0, combinedHeight)
                barHeight = barHeight.coerceIn(0, combinedHeight)
            }

            if (barHeight > previousBarHeight[x]) {
                previousBarHeight[x] = barHeight
            }

            for (y in 0 until barHeight) {
                if (colorBars) {
                    colorIndex = mapValue(y, 0, combinedHeight - 1, 0, 255)
                }
                val ledColor = colorFromPalette(colorIndex, false)
                setPixelColor(x, combinedHeight - 1 - y, ledColor)
            }

            if (previousBarHeight[x] > 0) {
                setPixelColor(x, combinedHeight - previousBarHeight[x], peakColor)
            }

            if (rippleTime && previousBarHeight[x] > 0) {
                previousBarHeight[x]--
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

    override fun getName(): String = "GEQ"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
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

    private fun fadeToBlack(fadeAmount: Int) {
        val amount = fadeAmount.coerceIn(0, 255)
        val factor = (255 - amount) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun colorFromPalette(colorIndexValue: Int, wrap: Boolean): RgbColor {
        var colorIndex = colorIndexValue
        if (!wrap) colorIndex %= 256
        if (colorIndex < 0) colorIndex += 256
        colorIndex %= 256
        val h = colorIndex / 255.0f * 360.0f
        return ColorUtils.hsvToRgb(h, 1.0f, 1.0f)
    }

    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = rgb
        }
    }
}

