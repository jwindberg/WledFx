package com.marsraver.wledfx.animation

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Distortion Waves animation - Wave distortion effects with oscillating centers.
 */
class DistortionWavesAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0

    private var speed: Int = 128
    private var scale: Int = 64
    private var fill: Boolean = false
    private var zoom: Boolean = false
    private var alt: Boolean = false
    private var paletteMode: Int = 0

    private var startTime: Long = 0

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        startTime = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean = true

    override fun getPixelColor(x: Int, y: Int): IntArray {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) {
            return intArrayOf(0, 0, 0)
        }

        val timeMs = System.currentTimeMillis() - startTime
        val speedVal = speed / 32
        var scaleVal = scale / 32

        if (zoom) {
            scaleVal += 192 / (combinedWidth + combinedHeight)
        }

        val a = timeMs / 32
        val a2 = a / 2
        val a3 = a / 3

        val colsScaled = combinedWidth * scaleVal
        val rowsScaled = combinedHeight * scaleVal

        val cx = beatsin(10 - speedVal, 0, colsScaled, timeMs)
        val cy = beatsin(12 - speedVal, 0, rowsScaled, timeMs)
        val cx1 = beatsin(13 - speedVal, 0, colsScaled, timeMs)
        val cy1 = beatsin(15 - speedVal, 0, rowsScaled, timeMs)
        val cx2 = beatsin(17 - speedVal, 0, colsScaled, timeMs)
        val cy2 = beatsin(14 - speedVal, 0, rowsScaled, timeMs)

        val xoffs = x * scaleVal
        val yoffs = y * scaleVal

        val rdistort: Int
        val gdistort: Int
        val bdistort: Int

        if (alt) {
            rdistort = cos8(((x + y) * 8 + a2.toInt()) and 255) shr 1
            gdistort = cos8(((x + y) * 8 + a3.toInt() + 32) and 255) shr 1
            bdistort = cos8(((x + y) * 8 + a.toInt() + 64) and 255) shr 1
        } else {
            val termR = (cos8(((x shl 3) + a.toInt()) and 255)
                    + cos8(((y shl 3) - a2.toInt()) and 255)
                    + a3.toInt()) and 255
            val termG = (cos8(((x shl 3) - a2.toInt()) and 255)
                    + cos8(((y shl 3) + a3.toInt()) and 255)
                    + a.toInt() + 32) and 255
            val termB = (cos8(((x shl 3) + a3.toInt()) and 255)
                    + cos8(((y shl 3) - a.toInt()) and 255)
                    + a2.toInt() + 64) and 255

            rdistort = cos8(termR) shr 1
            gdistort = cos8(termG) shr 1
            bdistort = cos8(termB) shr 1
        }

        val distR = ((xoffs - cx) * (xoffs - cx) + (yoffs - cy) * (yoffs - cy)) shr 7
        val distG = ((xoffs - cx1) * (xoffs - cx1) + (yoffs - cy1) * (yoffs - cy1)) shr 7
        val distB = ((xoffs - cx2) * (xoffs - cx2) + (yoffs - cy2) * (yoffs - cy2)) shr 7

        var valueR = rdistort + (((a - distR).toInt()) shl 1)
        var valueG = gdistort + (((a2 - distG).toInt()) shl 1)
        var valueB = bdistort + (((a3 - distB).toInt()) shl 1)

        valueR = cos8(valueR and 255)
        valueG = cos8(valueG and 255)
        valueB = cos8(valueB and 255)

        return if (paletteMode == 0) {
            intArrayOf(valueR, valueG, valueB)
        } else {
            val brightness = (valueR + valueG + valueB) / 3
            if (fill) {
                colorFromPalette(brightness, 255)
            } else {
                val hsv = rgb2hsv(valueR shr 2, valueG shr 2, valueB shr 2)
                colorFromPalette(hsv[0], brightness)
            }
        }
    }

    override fun getName(): String = "Distortion Waves"

    private fun cos8(theta: Int): Int {
        val radians = (theta and 255) / 255.0 * 2 * Math.PI
        val cosValue = cos(radians)
        return ((cosValue + 1.0) * 127.5).roundToInt().coerceIn(0, 255)
    }

    private fun beatsin(frequency: Int, min: Int, max: Int, timeMs: Long): Int {
        val phase = timeMs * frequency / 1000.0
        val sine = sin(phase)
        val range = max - min
        return min + ((sine + 1.0) * range / 2.0).roundToInt()
    }

    private fun rgb2hsv(r: Int, g: Int, b: Int): IntArray {
        val rf = r / 255.0f
        val gf = g / 255.0f
        val bf = b / 255.0f

        val max = max(rf, max(gf, bf))
        val min = min(rf, min(gf, bf))
        val delta = max - min

        var h = 0f
        when {
            delta == 0f -> h = 0f
            max == rf -> h = 60 * (((gf - bf) / delta) % 6)
            max == gf -> h = 60 * (((bf - rf) / delta) + 2)
            else -> h = 60 * (((rf - gf) / delta) + 4)
        }
        if (h < 0) h += 360f
        val s = if (max == 0f) 0f else delta / max
        val v = max

        return intArrayOf(
            (h * 255 / 360).roundToInt().coerceIn(0, 255),
            (s * 255).roundToInt().coerceIn(0, 255),
            (v * 255).roundToInt().coerceIn(0, 255)
        )
    }

    private fun colorFromPalette(hue: Int, brightness: Int): IntArray {
        val h = (hue % 256) / 255.0f * 360.0f
        val s = 1.0f
        val v = brightness / 255.0f
        return hsvToRgb(h, s, v)
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
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

        return intArrayOf(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }
}

