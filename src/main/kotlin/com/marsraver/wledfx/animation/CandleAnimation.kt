package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.max
import kotlin.random.Random

/**
 * Candle animation - Flickering candle effect with random brightness variations.
 * Based on original C++ code from WLED.
 */
class CandleAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private var startTime: Long = 0L

    // Single candle mode: all pixels use same flicker
    private var s: Int = 128 // current brightness
    private var sTarget: Int = 130 // target brightness
    private var fadeStep: Int = 1 // step size for fading
    
    // Multi candle mode: each pixel has independent flicker
    private var multiMode: Boolean = false
    private var candleData: IntArray? = null // Flat array: [s0, sTarget0, fadeStep0, s1, sTarget1, fadeStep1, ...]

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
        startTime = 0L
        
        // Initialize values (matching original: s=128, s_target=130+random(4), fadeStep=1)
        s = 128
        sTarget = 130 + Random.nextInt(4)
        fadeStep = 1
        
        // Initialize multi-candle data if needed
        if (multiMode) {
            val segmentLength = combinedWidth * combinedHeight
            if (segmentLength > 1) {
                val dataSize = max(1, segmentLength - 1) * 3
                candleData = IntArray(dataSize)
                // Initialize all candles
                for (i in 0 until (segmentLength - 1)) {
                    candleData!![i * 3] = 128 // s
                    candleData!![i * 3 + 1] = 130 + Random.nextInt(4) // sTarget
                    candleData!![i * 3 + 2] = 1 // fadeStep
                }
            }
        } else {
            candleData = null
        }
    }
    
    override fun supportsMultiMode(): Boolean = true

    override fun setMultiMode(multi: Boolean) {
        if (multiMode != multi) {
            multiMode = multi
            // Reinitialize if already initialized
            if (combinedWidth > 0 && combinedHeight > 0) {
                init(combinedWidth, combinedHeight)
            }
        }
    }
    
    fun isMultiMode(): Boolean = multiMode

    override fun update(now: Long): Boolean {
        if (startTime == 0L) {
            startTime = now
        }
        
        // Update candle flicker logic (once per frame)
        val intensity = 224 // Default intensity (0-255) - controls flicker range
        val speed = 96 // Default speed (0-255)
        
        // Calculate valrange and rndval (matching original)
        val valrange = intensity
        val rndval = valrange shr 1 // max 127
        
        // Calculate speedFactor based on speed (matching original logic)
        val speedFactor = when {
            speed > 252 -> 1 // epilepsy
            speed > 99 -> 2 // regular candle
            speed > 49 -> 3 // slower fade
            else -> 4 // slowest
        }
        
        if (multiMode && candleData != null) {
            // Multi-candle mode: update each candle independently
            val segmentLength = combinedWidth * combinedHeight
            for (i in 0 until (segmentLength - 1)) {
                val d = i * 3
                var pixelS = candleData!![d]
                var pixelSTarget = candleData!![d + 1]
                var pixelFadeStep = candleData!![d + 2]
                
                // Initialize if needed
                if (pixelFadeStep == 0) {
                    pixelS = 128
                    pixelSTarget = 130 + Random.nextInt(4)
                    pixelFadeStep = 1
                }
                
                // Update flicker state
                var newTarget = false
                if (pixelSTarget > pixelS) {
                    pixelS = minOf(255, pixelS + pixelFadeStep)
                    if (pixelS >= pixelSTarget) newTarget = true
                } else {
                    pixelS = maxOf(0, pixelS - pixelFadeStep)
                    if (pixelS <= pixelSTarget) newTarget = true
                }
                
                if (newTarget) {
                    val target1 = Random.nextInt(rndval)
                    val target2 = Random.nextInt(rndval)
                    pixelSTarget = target1 + target2
                    
                    if (pixelSTarget < (rndval shr 1)) {
                        pixelSTarget = (rndval shr 1) + Random.nextInt(rndval)
                    }
                    
                    val offset = 255 - valrange
                    pixelSTarget += offset
                    pixelSTarget = pixelSTarget.coerceIn(0, 255)
                    
                    val dif = if (pixelSTarget > pixelS) pixelSTarget - pixelS else pixelS - pixelSTarget
                    pixelFadeStep = (dif shr speedFactor).coerceAtLeast(1)
                }
                
                candleData!![d] = pixelS
                candleData!![d + 1] = pixelSTarget
                candleData!![d + 2] = pixelFadeStep
            }
        } else {
            // Single candle mode: all pixels use same flicker
            // Initialize if needed
            if (fadeStep == 0) {
                s = 128
                sTarget = 130 + Random.nextInt(4)
                fadeStep = 1
            }
            
            // Update flicker state
            var newTarget = false
            if (sTarget > s) {
                // Fade up: qadd8(s, fadeStep)
                s = minOf(255, s + fadeStep)
                if (s >= sTarget) newTarget = true
            } else {
                // Fade down: qsub8(s, fadeStep)
                s = maxOf(0, s - fadeStep)
                if (s <= sTarget) newTarget = true
            }
            
            if (newTarget) {
                // Pick new random target
                // s_target = hw_random8(rndval) + hw_random8(rndval)
                val target1 = Random.nextInt(rndval)
                val target2 = Random.nextInt(rndval)
                sTarget = target1 + target2
                
                // Ensure minimum brightness
                if (sTarget < (rndval shr 1)) {
                    sTarget = (rndval shr 1) + Random.nextInt(rndval)
                }
                
                // Add offset
                val offset = 255 - valrange
                sTarget += offset
                sTarget = sTarget.coerceIn(0, 255)
                
                // Calculate fade step based on distance
                val dif = if (sTarget > s) sTarget - s else s - sTarget
                fadeStep = (dif shr speedFactor).coerceAtLeast(1)
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val pixelIndex = y * combinedWidth + x
        
        // Get brightness for this pixel
        val brightness = if (multiMode && candleData != null) {
            // Multi-candle mode: pixel 0 uses single candle values, others use data array
            if (pixelIndex == 0) {
                s // Pixel 0 uses single candle values
            } else {
                val d = (pixelIndex - 1) * 3
                if (d < candleData!!.size) {
                    candleData!![d] // s value for this pixel
                } else {
                    s // Fallback to single candle
                }
            }
        } else {
            // Single candle mode: all pixels use same brightness
            s
        }
        
        // Get color from palette
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = (pixelIndex % currentPalette.size).coerceIn(0, currentPalette.size - 1)
            val paletteColor = currentPalette[paletteIndex]
            
            // Blend with secondary color (black/off) using brightness
            // color_blend(SEGCOLOR(1), paletteColor, brightness)
            val secondaryColor = RgbColor.BLACK // SEGCOLOR(1) - secondary color (black)
            return colorBlend(secondaryColor, paletteColor, brightness)
        } else {
            // Default: warm orange/yellow candle color
            val candleColor = RgbColor(255, 100, 0) // Warm orange
            val secondaryColor = RgbColor.BLACK
            return colorBlend(secondaryColor, candleColor, brightness)
        }
    }

    override fun getName(): String = "Candle"

    /**
     * Color blend function matching FastLED's color_blend
     * Blends color1 and color2 based on blend amount (0-255)
     */
    private fun colorBlend(color1: RgbColor, color2: RgbColor, blend: Int): RgbColor {
        return ColorUtils.blend(color1, color2, blend)
    }
}

