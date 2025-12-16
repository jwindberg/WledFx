package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.cos
import kotlin.math.PI

/**
 * Lake - Water ripple effect
 */
class LakeAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L

    override fun getName(): String = "Lake"
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val sp = paramSpeed / 10
        val wave1 = beatsin8(sp + 2, -64, 64)
        val wave2 = beatsin8(sp + 1, -64, 64)
        val wave3 = beatsin8(sp + 2, 0, 80)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = x + y * width
                val index = cos8((i * 15) + wave1) / 2 + cubicwave8((i * 23) + wave2) / 2
                val lum = if (index > wave3) index - wave3 else 0
                
                val baseColor = getColorFromPalette(index)
                val pixelColor = if (lum > 0) {
                     // If lum is used as brightness/alpha
                     ColorUtils.scaleBrightness(baseColor, lum / 255.0)
                } else {
                     RgbColor.BLACK
                }
                
                pixelColors[x][y] = pixelColor
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

    // Using local helpers to match visual output exactly if desired, or could substitute with MathUtils
    // beatsin8 is in MathUtils usually.
    // cos8 is in MathUtils as cos8.
    // cubicwave8 might be unique.
    
    // Checks on MathUtils:
    // I've been using MathUtils.beatsin8 in other files.
    // I'll define these locally to ensure exact "Lake" behavior as ported from WLED C++ source which often has specific lookup tables or integer math.
    
    private fun beatsin8(bpm: Int, low: Int, high: Int): Int {
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000
        val beat = ((timeMs * bpm / 60000.0) % 1.0) * 2 * PI
        val sinVal = kotlin.math.sin(beat)
        return (((sinVal + 1.0) / 2.0) * (high - low) + low).toInt()
    }
    
    private fun cos8(angle: Int): Int {
        val radians = (angle % 256) * 2 * PI / 256
        return ((cos(radians) + 1.0) * 127.5).toInt()
    }
    
    private fun cubicwave8(angle: Int): Int {
        val x = (angle % 256) / 256.0
        val cubic = 4 * x * x * x - 6 * x * x + 3 * x
        return (cubic * 255).toInt().coerceIn(0, 255)
    }
}
