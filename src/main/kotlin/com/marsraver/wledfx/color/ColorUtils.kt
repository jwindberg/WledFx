package com.marsraver.wledfx.color

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * Utility functions for color operations.
 */
object ColorUtils {

    /**
     * Convert HSV to RGB color.
     *
     * @param h hue (0-255, maps to 0-360 degrees)
     * @param s saturation (0-255)
     * @param v value/brightness (0-255)
     * @return RGB color
     */
    fun hsvToRgb(h: Int, s: Int, v: Int): RgbColor {
        val hue = (h % 256 + 256) % 256
        val saturation = s.coerceIn(0, 255) / 255.0
        val value = v.coerceIn(0, 255) / 255.0

        if (saturation <= 0.0) {
            val gray = (value * 255).toInt()
            return RgbColor(gray, gray, gray)
        }

        val hSection = hue / 42.6666667
        val i = hSection.toInt()
        val f = hSection - i

        val p = value * (1 - saturation)
        val q = value * (1 - saturation * f)
        val t = value * (1 - saturation * (1 - f))

        val (r, g, b) = when (i % 6) {
            0 -> Triple(value, t, p)
            1 -> Triple(q, value, p)
            2 -> Triple(p, value, t)
            3 -> Triple(p, q, value)
            4 -> Triple(t, p, value)
            else -> Triple(value, p, q)
        }

        return RgbColor(
            (r * 255).toInt().coerceIn(0, 255),
            (g * 255).toInt().coerceIn(0, 255),
            (b * 255).toInt().coerceIn(0, 255)
        )
    }

    /**
     * Convert RGB color to HSV color.
     *
     * @param color RGB color to convert
     * @return HSV color representation
     */
    fun rgbToHsv(color: RgbColor): HsvColor {
        // Normalize RGB values to 0.0-1.0
        val rNorm = color.r / 255.0f
        val gNorm = color.g / 255.0f
        val bNorm = color.b / 255.0f

        val max = maxOf(rNorm, gNorm, bNorm)
        val min = minOf(rNorm, gNorm, bNorm)
        val delta = max - min

        // Calculate value (brightness)
        val v = max

        // Calculate saturation
        val s = if (max > 0.0f) delta / max else 0.0f

        // Calculate hue
        val h = when {
            delta == 0.0f -> 0.0f // Undefined, but use 0
            max == rNorm -> {
                val hRaw = 60.0f * (((gNorm - bNorm) / delta) % 6.0f)
                if (hRaw < 0) hRaw + 360.0f else hRaw
            }
            max == gNorm -> 60.0f * (((bNorm - rNorm) / delta) + 2.0f)
            else -> 60.0f * (((rNorm - gNorm) / delta) + 4.0f)
        }

        return HsvColor(
            h.coerceIn(0.0f, 360.0f),
            s.coerceIn(0.0f, 1.0f),
            v.coerceIn(0.0f, 1.0f)
        )
    }

    /**
     * Convert HSV to RGB color (float version).
     *
     * @param h hue in degrees (0-360)
     * @param s saturation (0.0-1.0)
     * @param v value/brightness (0.0-1.0)
     * @return RGB color
     */
    fun hsvToRgb(h: Float, s: Float, v: Float): RgbColor {
        if (s <= 0.0f) {
            val gray = (v * 255).toInt()
            return RgbColor(gray, gray, gray)
        }

        val hi = ((h / 60.0f) % 6).toInt()
        val f = h / 60.0f - hi
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

        return RgbColor(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    /**
     * Blend two colors together.
     *
     * @param color1 first color
     * @param color2 second color
     * @param blendAmount blend amount (0-255, where 0 = all color1, 255 = all color2)
     * @return blended color
     */
    fun blend(color1: RgbColor, color2: RgbColor, blendAmount: Int): RgbColor {
        val blend = blendAmount.coerceIn(0, 255) / 255.0
        val invBlend = 1.0 - blend
        return RgbColor(
            ((color1.r * invBlend + color2.r * blend)).toInt().coerceIn(0, 255),
            ((color1.g * invBlend + color2.g * blend)).toInt().coerceIn(0, 255),
            ((color1.b * invBlend + color2.b * blend)).toInt().coerceIn(0, 255)
        )
    }

    /**
     * Get the average light/brightness of a color.
     *
     * @param color RGB color
     * @return average brightness (0-255)
     */
    fun getAverageLight(color: RgbColor): Int {
        return ((color.r + color.g + color.b) / 3.0).toInt()
    }

    /**
     * Scale color brightness.
     *
     * @param color RGB color
     * @param factor brightness factor (0.0-1.0)
     * @return scaled color
     */
    fun scaleBrightness(color: RgbColor, factor: Double): RgbColor {
        val scale = factor.coerceIn(0.0, 1.0)
        return RgbColor(
            (color.r * scale).toInt().coerceIn(0, 255),
            (color.g * scale).toInt().coerceIn(0, 255),
            (color.b * scale).toInt().coerceIn(0, 255)
        )
    }

    /**
     * sin8 - Sine wave mapped to 0-255 range.
     *
     * @param angle angle value (will be normalized to 0-255)
     * @return sine value in 0-255 range
     */
    fun sin8(angle: Int): Int {
        val normalized = (angle % 256 + 256) % 256
        val radians = (normalized / 255.0) * 2 * PI
        val sine = sin(radians)
        val result = ((sine + 1.0) / 2.0 * 255.0).toInt()
        return result.coerceIn(0, 255)
    }
    /**
     * Approximates 'black body radiation' spectrum for a given heat level.
     * 0 = black, 255 = white
     */
    fun heatColor(temperature: Int): RgbColor {
        val t = temperature.coerceIn(0, 255)
        
        // Scale 'heat' down from 0-255 to 0-191,
        // which can then be easily divided into three equal 'thirds' of 64 units each.
        val t192 = (t * 191) / 255
        
        // Calculate ramp up from 0 to 255 in each 'third'
        var heatramp = t192 and 0x3F // 0..63
        heatramp = heatramp shl 2 // scale up to 0..252
        
        return if (t192 and 0x80 != 0) {
            // Hot third
            RgbColor(255, 255, heatramp) // Red + Green + Blue(ramp) -> White
        } else if (t192 and 0x40 != 0) {
            // Middle third
            RgbColor(255, heatramp, 0) // Red + Green(ramp) -> Yellow
        } else {
            // Cool third
            RgbColor(heatramp, 0, 0) // Red(ramp) -> Red
        }
    }
    /**
     * Fade a color to black by a fixed step amount.
     * SUBTRACTS amount from each channel.
     */
    fun fade(color: RgbColor, amount: Int): RgbColor {
        return RgbColor(
            (color.r - amount).coerceAtLeast(0),
            (color.g - amount).coerceAtLeast(0),
            (color.b - amount).coerceAtLeast(0)
        )
    }
    

}

