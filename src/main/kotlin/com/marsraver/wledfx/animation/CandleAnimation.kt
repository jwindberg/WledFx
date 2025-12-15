package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random
import kotlin.math.max

/**
 * Candle animation - Flickering candle effect.
 */
class CandleAnimation : BaseAnimation() {

    private var s: Int = 128
    private var sTarget: Int = 130
    private var fadeStep: Int = 1
    
    private var multiMode: Boolean = false
    private var candleData: IntArray? = null 

    override fun getName(): String = "Candle"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true
    
    override fun supportsMultiMode(): Boolean = true
    override fun setMultiMode(multi: Boolean) {
        if (multiMode != multi) {
            multiMode = multi
            if (width > 0 && height > 0) onInit()
        }
    }

    override fun onInit() {
        s = 128
        sTarget = 130 + Random.nextInt(4)
        fadeStep = 1
        
        if (multiMode) {
            val segmentLength = width * height
            if (segmentLength > 1) {
                val dataSize = max(1, segmentLength - 1) * 3
                candleData = IntArray(dataSize)
                for (i in 0 until (segmentLength - 1)) {
                    candleData!![i * 3] = 128 
                    candleData!![i * 3 + 1] = 130 + Random.nextInt(4) 
                    candleData!![i * 3 + 2] = 1 
                }
            }
        } else {
            candleData = null
        }
    }

    override fun update(now: Long): Boolean {
        // paramIntensity controls valrange/flicker range
        // paramSpeed controls speed factor
        
        val valrange = paramIntensity
        val rndval = valrange shr 1 
        
        val speedFactor = when {
            paramSpeed > 252 -> 1 
            paramSpeed > 99 -> 2 
            paramSpeed > 49 -> 3 
            else -> 4 
        }
        
        if (multiMode && candleData != null) {
            val segmentLength = width * height
            for (i in 0 until (segmentLength - 1)) {
                val d = i * 3
                var pixelS = candleData!![d]
                var pixelSTarget = candleData!![d + 1]
                var pixelFadeStep = candleData!![d + 2]
                
                if (pixelFadeStep == 0) {
                    pixelS = 128; pixelSTarget = 130 + Random.nextInt(4); pixelFadeStep = 1
                }
                
                var newTarget = false
                if (pixelSTarget > pixelS) {
                    pixelS = minOf(255, pixelS + pixelFadeStep)
                    if (pixelS >= pixelSTarget) newTarget = true
                } else {
                    pixelS = maxOf(0, pixelS - pixelFadeStep)
                    if (pixelS <= pixelSTarget) newTarget = true
                }
                
                if (newTarget) {
                    var target1 = Random.nextInt(rndval)
                    var target2 = Random.nextInt(rndval)
                    pixelSTarget = target1 + target2
                    if (pixelSTarget < (rndval shr 1)) pixelSTarget = (rndval shr 1) + Random.nextInt(rndval)
                    val offset = 255 - valrange
                    pixelSTarget += offset
                    pixelSTarget = pixelSTarget.coerceIn(0, 255)
                    val dif = if (pixelSTarget > pixelS) pixelSTarget - pixelS else pixelS - pixelSTarget
                    pixelFadeStep = (dif shr speedFactor).coerceAtLeast(1)
                }
                candleData!![d] = pixelS; candleData!![d + 1] = pixelSTarget; candleData!![d + 2] = pixelFadeStep
            }
        } else {
             if (fadeStep == 0) { s = 128; sTarget = 130 + Random.nextInt(4); fadeStep = 1 }
             var newTarget = false
             if (sTarget > s) {
                 s = minOf(255, s + fadeStep)
                 if (s >= sTarget) newTarget = true
             } else {
                 s = maxOf(0, s - fadeStep)
                 if (s <= sTarget) newTarget = true
             }
             if (newTarget) {
                var target1 = Random.nextInt(rndval)
                var target2 = Random.nextInt(rndval)
                sTarget = target1 + target2
                if (sTarget < (rndval shr 1)) sTarget = (rndval shr 1) + Random.nextInt(rndval)
                val offset = 255 - valrange
                sTarget += offset
                sTarget = sTarget.coerceIn(0, 255)
                val dif = if (sTarget > s) sTarget - s else s - sTarget
                fadeStep = (dif shr speedFactor).coerceAtLeast(1)
             }
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val pixelIndex = getPixelIndex(x, y)
        val brightness = if (multiMode && candleData != null) {
            if (pixelIndex == 0) s 
            else {
                val d = (pixelIndex - 1) * 3
                if (d < candleData!!.size) candleData!![d] else s
            }
        } else {
            s
        }
        
        // Use BaseAnimation palette
        val palette = paramPalette?.colors
        if (palette != null && palette.isNotEmpty()) {
            val paletteIndex = (pixelIndex % palette.size).coerceIn(0, palette.size - 1)
            val paletteColor = palette[paletteIndex]
            return ColorUtils.blend(RgbColor.BLACK, paletteColor, brightness)
        } else {
            val candleColor = RgbColor(255, 100, 0)
            return ColorUtils.blend(RgbColor.BLACK, candleColor, brightness)
        }
    }
}
