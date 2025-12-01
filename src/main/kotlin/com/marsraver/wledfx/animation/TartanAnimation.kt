package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tartan animation - animated plaid pattern using sine-based oscillations.
 */
class TartanAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0

    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0
    private var horizontalScale: Double = 1.0
    private var hueBase: Int = 0
    private var hueAccumulatorMs: Double = 0.0
    private var lastUpdateNanos: Long = 0L
    private var currentPalette: Palette? = null

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        offsetX = 0.0
        offsetY = 0.0
        horizontalScale = 1.0
        hueBase = 0
        hueAccumulatorMs = 0.0
        lastUpdateNanos = 0L
    }

    override fun update(now: Long): Boolean {
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now
        }
        val deltaMs = (now - lastUpdateNanos) / 1_000_000.0
        hueAccumulatorMs += deltaMs
        while (hueAccumulatorMs >= 8.0) {
            hueBase = (hueBase + 1) and 0xFF
            hueAccumulatorMs -= 8.0
        }
        lastUpdateNanos = now

        val timeSeconds = now / 1_000_000_000.0
        offsetX = beatsin(timeSeconds, 0.25, -180.0, 180.0)
        offsetY = beatsin(timeSeconds, 0.18, -180.0, 180.0, phaseOffsetSeconds = 6.0)
        horizontalScale = beatsin(timeSeconds, 0.35, 0.5, 4.0)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) {
            return RgbColor.BLACK
        }

        val hueX = (x * horizontalScale + offsetY + hueBase).toFloat()
        val brightnessX = sin8(x * 18.0 + offsetX)
        val colorX = hsvToRgb(hueX, 200, brightnessX)

        val hueY = (y * 2.0 + offsetX + hueBase).toFloat()
        val brightnessY = sin8(y * 18.0 + offsetY)
        val colorY = hsvToRgb(hueY, 200, brightnessY)

        return RgbColor(
            (colorX.r + colorY.r).coerceAtMost(255),
            (colorX.g + colorY.g).coerceAtMost(255),
            (colorX.b + colorY.b).coerceAtMost(255)
        )
    }

    override fun getName(): String = "Tartan"

    override fun cleanup() {
        // No resources to release; provided for interface parity.
    }

    private fun beatsin(
        timeSeconds: Double,
        frequencyHz: Double,
        low: Double,
        high: Double,
        phaseOffsetSeconds: Double = 0.0,
    ): Double {
        val angle = 2.0 * Math.PI * (frequencyHz * (timeSeconds + phaseOffsetSeconds))
        val sine = sin(angle)
        return low + (high - low) * (sine + 1.0) / 2.0
    }

    private fun sin8(input: Double): Int {
        var angle = input % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * Math.PI
        val sine = sin(radians)
        return ((sine + 1.0) * 127.5).roundToInt().coerceIn(0, 255)
    }

    private fun hsvToRgb(hue: Float, saturation: Int, value: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val h = (hue % 256 + 256) % 256
            val paletteIndex = (h / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = value / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            val h = (hue % 256 + 256) % 256
            val s = saturation.coerceIn(0, 255) / 255.0f
            val v = value.coerceIn(0, 255) / 255.0f
            return ColorUtils.hsvToRgb(h * 360.0f / 255.0f, s, v)
        }
    }
}

