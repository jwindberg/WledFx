package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import com.marsraver.wledfx.math.MathUtils.beatsin16
import com.marsraver.wledfx.math.MathUtils.beatsin8
import com.marsraver.wledfx.math.MathUtils.sin8
import com.marsraver.wledfx.math.MathUtils.scale8
import com.marsraver.wledfx.math.MathUtils.qadd8
import com.marsraver.wledfx.math.MathUtils.beat8

/**
 * Pacifica - Peaceful ocean waves
 */
class PacificaAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    private var sCIStart1: Int = 0
    private var sCIStart2: Int = 0
    private var sCIStart3: Int = 0
    private var sCIStart4: Int = 0

    // Uses custom ocean palettes internally, but lets BaseAnimation handle palette storage if user overrides?
    // Pacifica logic uses fixed palettes usually. We'll stick to original logic but allow override if supported.
    // Original code: supportsPalette=false.
    override fun supportsPalette(): Boolean = false 
    
    override fun getName(): String = "Pacifica"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val timeMs = (now - startTimeNs) / 1_000_000
        val deltams = 16 + (16 * paramSpeed / 128)
        
        // Update wave counters
        val speedfactor1 = beatsin16(3, 179, 269, timeMs)
        val speedfactor2 = beatsin16(4, 179, 269, timeMs)
        val deltams1 = (deltams * speedfactor1) / 256
        val deltams2 = (deltams * speedfactor2) / 256
        val deltams21 = (deltams1 + deltams2) / 2
        
        // beatsin88 mapped to beatsin16
        sCIStart1 += (deltams1 * beatsin16(1011, 10, 13, timeMs)) / 256
        sCIStart2 -= (deltams21 * beatsin16(777, 8, 11, timeMs)) / 256
        sCIStart3 -= (deltams1 * beatsin16(501, 5, 7, timeMs)) / 256
        sCIStart4 -= (deltams2 * beatsin16(257, 4, 6, timeMs)) / 256
        
        val basethreshold = beatsin8(9, 55, 65, timeMs)
        var wave = beat8(7, timeMs)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // We use getting pixel index or loop x,y naturally
                // Original used i = x + y * width 
                // We'll pass i to layer function
                val i = x + y * width
                
                var r = 2
                var g = 6
                var b = 10
                
                val layer1 = pacificaOneLayer(i, sCIStart1, beatsin16(3, 11 * 256, 14 * 256, timeMs), beatsin8(10, 70, 130, timeMs))
                val layer2 = pacificaOneLayer(i, sCIStart2, beatsin16(4, 6 * 256, 9 * 256, timeMs), beatsin8(17, 40, 80, timeMs))
                val layer3 = pacificaOneLayer(i, sCIStart3, 6 * 256, beatsin8(9, 10, 38, timeMs))
                val layer4 = pacificaOneLayer(i, sCIStart4, 5 * 256, beatsin8(8, 10, 28, timeMs))
                
                r += layer1.r + layer2.r + layer3.r + layer4.r
                g += layer1.g + layer2.g + layer3.g + layer4.g
                b += layer1.b + layer2.b + layer3.b + layer4.b
                
                val threshold = scale8(sin8(wave), 20) + basethreshold
                val nextWave = wave + 7
                // Can't mutate wave in loop if shared, but here it accumulates?
                // Original: wave += 7 inside loop?
                // Yes: wave += 7
                wave = nextWave
                
                val avgLight = (r + g + b) / 3
                if (avgLight > threshold) {
                    val overage = avgLight - threshold
                    val overage2 = qadd8(overage, overage)
                    r += overage
                    g += overage2
                    b += qadd8(overage2, overage2)
                }
                
                b = scale8(b, 145)
                g = scale8(g, 200)
                r += 2
                g += 5
                b += 7
                
                pixelColors[x][y] = RgbColor(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    private fun pacificaOneLayer(i: Int, cistart: Int, wavescale: Int, bri: Int): RgbColor {
        val ci = cistart + (i * wavescale)
        val svalue8 = sin8((ci shr 8) and 0xFF)
        val bvalue8 = scale8(svalue8, bri)
        
        val hue = (ci shr 8) and 0xFF
        return ColorUtils.hsvToRgb(hue / 4 + 128, 200, bvalue8)
    }
}
