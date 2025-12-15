package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.math.MathUtils
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.min
import kotlin.math.max

/**
 * Distortion Waves animation
 */
class DistortionWavesAnimation : BaseAnimation() {
    
    private var scale: Int = 64
    private var fill: Boolean = false
    private var zoom: Boolean = false
    private var alt: Boolean = false
    private var paletteMode: Int = 0
    private var startTime: Long = 0

    override fun getName(): String = "Distortion Waves"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        startTime = System.currentTimeMillis()
    }

    override fun update(now: Long): Boolean = true

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        if (x !in 0 until width || y !in 0 until height) return RgbColor.BLACK

        val timeMs = System.currentTimeMillis() - startTime
        val speedVal = paramSpeed / 32
        var scaleVal = scale / 32

        if (zoom) scaleVal += 192 / (width + height)

        val a = timeMs / 32
        val a2 = a / 2
        val a3 = a / 3

        val colsScaled = width * scaleVal
        val rowsScaled = height * scaleVal

        val cx = MathUtils.beatsin8(10 - speedVal, 0, colsScaled, timeMs)
        val cy = MathUtils.beatsin8(12 - speedVal, 0, rowsScaled, timeMs)
        val cx1 = MathUtils.beatsin8(13 - speedVal, 0, colsScaled, timeMs)
        val cy1 = MathUtils.beatsin8(15 - speedVal, 0, rowsScaled, timeMs)
        val cx2 = MathUtils.beatsin8(17 - speedVal, 0, colsScaled, timeMs)
        val cy2 = MathUtils.beatsin8(14 - speedVal, 0, rowsScaled, timeMs)

        val xoffs = x * scaleVal
        val yoffs = y * scaleVal

        val rdistort: Int
        val gdistort: Int
        val bdistort: Int

        // Using MathUtils.cos8
        if (alt) {
            rdistort = MathUtils.cos8(((x + y) * 8 + a2.toInt()) and 255) shr 1
            gdistort = MathUtils.cos8(((x + y) * 8 + a3.toInt() + 32) and 255) shr 1
            bdistort = MathUtils.cos8(((x + y) * 8 + a.toInt() + 64) and 255) shr 1
        } else {
            val termR = (MathUtils.cos8(((x shl 3) + a.toInt()) and 255)
                    + MathUtils.cos8(((y shl 3) - a2.toInt()) and 255)
                    + a3.toInt()) and 255
            val termG = (MathUtils.cos8(((x shl 3) - a2.toInt()) and 255)
                    + MathUtils.cos8(((y shl 3) + a3.toInt()) and 255)
                    + a.toInt() + 32) and 255
            val termB = (MathUtils.cos8(((x shl 3) + a3.toInt()) and 255)
                    + MathUtils.cos8(((y shl 3) - a.toInt()) and 255)
                    + a2.toInt() + 64) and 255

            rdistort = MathUtils.cos8(termR) shr 1
            gdistort = MathUtils.cos8(termG) shr 1
            bdistort = MathUtils.cos8(termB) shr 1
        }

        val distR = ((xoffs - cx) * (xoffs - cx) + (yoffs - cy) * (yoffs - cy)) shr 7
        val distG = ((xoffs - cx1) * (xoffs - cx1) + (yoffs - cy1) * (yoffs - cy1)) shr 7
        val distB = ((xoffs - cx2) * (xoffs - cx2) + (yoffs - cy2) * (yoffs - cy2)) shr 7

        var valueR = rdistort + (((a - distR).toInt()) shl 1)
        var valueG = gdistort + (((a2 - distG).toInt()) shl 1)
        var valueB = bdistort + (((a3 - distB).toInt()) shl 1)

        valueR = MathUtils.cos8(valueR and 255)
        valueG = MathUtils.cos8(valueG and 255)
        valueB = MathUtils.cos8(valueB and 255)

        return if (paletteMode == 0) {
            RgbColor(valueR, valueG, valueB)
        } else {
            val brightness = (valueR + valueG + valueB) / 3
            if (fill) {
                // Brightness = hue? Or index?
                getColorFromPalette(brightness)
            } else {
                val hsv = ColorUtils.rgbToHsv(RgbColor(valueR shr 2, valueG shr 2, valueB shr 2))
                // Use H as index
                getColorFromPalette((hsv.h * 255 / 360).toInt())
            }
        }
    }
}
