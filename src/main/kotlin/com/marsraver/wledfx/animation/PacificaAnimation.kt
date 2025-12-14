package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.sin
import kotlin.math.PI

/**
 * Pacifica - Peaceful ocean waves
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 3992
 * Simplified version focusing on core wave behavior
 */
class PacificaAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    private var speed: Int = 128
    private var sCIStart1: Int = 0
    private var sCIStart2: Int = 0
    private var sCIStart3: Int = 0
    private var sCIStart4: Int = 0

    override fun supportsPalette(): Boolean = false  // Uses custom ocean palettes

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? = currentPalette

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val timeMs = (now - startTimeNs) / 1_000_000
        val deltams = 16 + (16 * speed / 128)
        
        // Update wave counters
        val speedfactor1 = beatsin16(3, 179, 269, timeMs)
        val speedfactor2 = beatsin16(4, 179, 269, timeMs)
        val deltams1 = (deltams * speedfactor1) / 256
        val deltams2 = (deltams * speedfactor2) / 256
        val deltams21 = (deltams1 + deltams2) / 2
        
        sCIStart1 += (deltams1 * beatsin88(1011, 10, 13, timeMs)) / 256
        sCIStart2 -= (deltams21 * beatsin88(777, 8, 11, timeMs)) / 256
        sCIStart3 -= (deltams1 * beatsin88(501, 5, 7, timeMs)) / 256
        sCIStart4 -= (deltams2 * beatsin88(257, 4, 6, timeMs)) / 256
        
        val basethreshold = beatsin8(9, 55, 65, timeMs)
        var wave = beat8(7, timeMs)
        
        // Render ocean waves for each pixel
        for (y in 0 until combinedHeight) {
            for (x in 0 until combinedWidth) {
                val i = x + y * combinedWidth
                
                // Base ocean color
                var r = 2
                var g = 6
                var b = 10
                
                // Add four wave layers
                val layer1 = pacificaOneLayer(i, sCIStart1, beatsin16(3, 11 * 256, 14 * 256, timeMs), beatsin8(10, 70, 130, timeMs), timeMs)
                val layer2 = pacificaOneLayer(i, sCIStart2, beatsin16(4, 6 * 256, 9 * 256, timeMs), beatsin8(17, 40, 80, timeMs), timeMs)
                val layer3 = pacificaOneLayer(i, sCIStart3, 6 * 256, beatsin8(9, 10, 38, timeMs), timeMs)
                val layer4 = pacificaOneLayer(i, sCIStart4, 5 * 256, beatsin8(8, 10, 28, timeMs), timeMs)
                
                r += layer1.r + layer2.r + layer3.r + layer4.r
                g += layer1.g + layer2.g + layer3.g + layer4.g
                b += layer1.b + layer2.b + layer3.b + layer4.b
                
                // Add whitecaps
                val threshold = scale8(sin8(wave), 20) + basethreshold
                wave += 7
                val avgLight = (r + g + b) / 3
                if (avgLight > threshold) {
                    val overage = avgLight - threshold
                    val overage2 = qadd8(overage, overage)
                    r += overage
                    g += overage2
                    b += qadd8(overage2, overage2)
                }
                
                // Deepen blues and greens
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
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Pacifica"

    override fun isAudioReactive(): Boolean = false

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {}
    
    private fun pacificaOneLayer(i: Int, cistart: Int, wavescale: Int, bri: Int, timeMs: Long): RgbColor {
        val ci = cistart + (i * wavescale)
        val svalue8 = sin8((ci shr 8) and 0xFF)
        val bvalue8 = scale8(svalue8, bri)
        
        // Ocean palette (simplified)
        val hue = (ci shr 8) and 0xFF
        return ColorUtils.hsvToRgb(hue / 4 + 128, 200, bvalue8)  // Blue-green tones
    }
    
    private fun beatsin16(bpm: Int, low: Int, high: Int, timeMs: Long): Int {
        val beat = ((timeMs * bpm / 60000.0) % 1.0) * 2 * PI
        val sinVal = sin(beat)
        return (((sinVal + 1.0) / 2.0) * (high - low) + low).toInt()
    }
    
    private fun beatsin8(bpm: Int, low: Int, high: Int, timeMs: Long): Int {
        return beatsin16(bpm, low, high, timeMs).coerceIn(0, 255)
    }
    
    private fun beatsin88(bpm: Int, low: Int, high: Int, timeMs: Long): Int {
        return beatsin16(bpm, low, high, timeMs)
    }
    
    private fun beat8(bpm: Int, timeMs: Long): Int {
        return ((timeMs * bpm / 60000) % 256).toInt()
    }
    
    private fun sin8(angle: Int): Int {
        val radians = (angle % 256) * 2 * PI / 256
        return ((sin(radians) + 1.0) * 127.5).toInt()
    }
    
    private fun scale8(value: Int, scale: Int): Int {
        return (value * scale / 256).coerceIn(0, 255)
    }
    
    private fun qadd8(a: Int, b: Int): Int {
        return (a + b).coerceIn(0, 255)
    }
}
