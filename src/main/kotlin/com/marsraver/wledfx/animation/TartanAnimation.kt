package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

/**
 * Tartan animation - animated plaid pattern using sine-based oscillations.
 */
class TartanAnimation : BaseAnimation() {

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var horizontalScale: Double = 1.0
    private var hueBase: Int = 0
    private var hueAccumulatorMs: Double = 0.0
    private var lastUpdateNanos: Long = 0L

    override fun onInit() {
        offsetX = 0.0
        offsetY = 0.0
        horizontalScale = 1.0
        hueBase = 0
        hueAccumulatorMs = 0.0
        lastUpdateNanos = 0L
        paramSpeed = 128
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now
        }
        val deltaMs = (now - lastUpdateNanos) / 1_000_000.0
        
        // Use paramSpeed to influence hue change speed?
        // Original: hardcoded 8.0 ms per increment.
        val speedFactor = paramSpeed / 128.0
        val incrementThreshold = 8.0 / speedFactor.coerceAtLeast(0.1)
        
        hueAccumulatorMs += deltaMs
        while (hueAccumulatorMs >= incrementThreshold) {
            hueBase = (hueBase + 1) and 0xFF
            hueAccumulatorMs -= incrementThreshold
        }
        lastUpdateNanos = now

        val timeSeconds = now / 1_000_000_000.0
        
        // Original frequencies hardcoded in beatsin calls.
        // We can scale them by paramSpeed too if desired.
        val freqScale = paramSpeed / 128.0
        
        offsetX = beatsin(timeSeconds, 0.25 * freqScale, -180.0, 180.0)
        offsetY = beatsin(timeSeconds, 0.18 * freqScale, -180.0, 180.0, phaseOffsetSeconds = 6.0)
        horizontalScale = beatsin(timeSeconds, 0.35 * freqScale, 0.5, 4.0)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        if (x !in 0 until width || y !in 0 until height) {
            return RgbColor.BLACK
        }

        val hueX = (x * horizontalScale + offsetY + hueBase).toFloat()
        val brightnessX = sin8(x * 18.0 + offsetX)
        // Use palette for hue
        val colorX = getColorFromHue(hueX, brightnessX)

        val hueY = (y * 2.0 + offsetX + hueBase).toFloat()
        val brightnessY = sin8(y * 18.0 + offsetY)
        val colorY = getColorFromHue(hueY, brightnessY)

        return RgbColor(
            (colorX.r + colorY.r).coerceAtMost(255),
            (colorX.g + colorY.g).coerceAtMost(255),
            (colorX.b + colorY.b).coerceAtMost(255)
        )
    }

    override fun getName(): String = "Tartan"

    private fun beatsin(
        timeSeconds: Double,
        frequencyHz: Double,
        low: Double,
        high: Double,
        phaseOffsetSeconds: Double = 0.0,
    ): Double {
        val angle = 2.0 * PI * (frequencyHz * (timeSeconds + phaseOffsetSeconds))
        val sine = sin(angle)
        return low + (high - low) * (sine + 1.0) / 2.0
    }

    private fun sin8(input: Double): Int {
        var angle = input % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        val sine = sin(radians)
        return ((sine + 1.0) * 127.5).roundToInt().coerceIn(0, 255)
    }

    private fun getColorFromHue(hue: Float, brightness: Int): RgbColor {
        val h = (hue.toInt() % 256 + 256) % 256
        val base = getColorFromPalette(h)
        return ColorUtils.scaleBrightness(base, brightness / 255.0)
    }
}
