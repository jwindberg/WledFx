package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Julia animation - Animated Julia set fractal
 * By: Andrew Tuline
 */
class JuliaAnimation : BaseAnimation() {

    private data class JuliaState(
        var xcen: Float = 0.0f,
        var ycen: Float = 0.0f,
        var xymag: Float = 1.0f
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var custom1: Int = 128  // X center
    private var custom2: Int = 128  // Y center
    private var custom3: Int = 16   // Area size
    private var check1: Boolean = false  // Blur
    
    private var juliaState = JuliaState()
    private var startTimeNs: Long = 0L
    private var initialized: Boolean = false

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 24
        
        // Reset the center if we've just re-started this animation
        if (!initialized) {
            juliaState.xcen = 0.0f
            juliaState.ycen = 0.0f
            juliaState.xymag = 1.0f
            custom1 = 128
            custom2 = 128
            custom3 = 16
            initialized = true
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Update Julia state based on custom values
        juliaState.xcen += (custom1 - 128) / 100000.0f
        juliaState.ycen += (custom2 - 128) / 100000.0f
        juliaState.xymag += ((custom3 - 16) shl 3) / 100000.0f
        
        // Constrain xymag
        if (juliaState.xymag < 0.01f) juliaState.xymag = 0.01f
        if (juliaState.xymag > 1.0f) juliaState.xymag = 1.0f
        
        // Calculate bounds
        var xmin = juliaState.xcen - juliaState.xymag
        var xmax = juliaState.xcen + juliaState.xymag
        var ymin = juliaState.ycen - juliaState.xymag
        var ymax = juliaState.ycen + juliaState.xymag
        
        // Constrain bounds
        xmin = xmin.coerceIn(-1.2f, 1.2f)
        xmax = xmax.coerceIn(-1.2f, 1.2f)
        ymin = ymin.coerceIn(-0.8f, 1.0f)
        ymax = ymax.coerceIn(-0.8f, 1.0f)
        
        // Calculate deltas
        val dx = (xmax - xmin) / width
        val dy = (ymax - ymin) / height
        
        // Max iterations
        val maxIterations = (paramIntensity / 2).coerceAtLeast(1)
        val maxCalc = 16.0f
        
        // Animate real and imaginary parts
        var reAl = -0.94299f  // PixelBlaze example
        var imAg = 0.3162f
        
        // MathUtils.sin16 takes long/int but here we use sin16_t which is similar logic.
        // Keeping local sin16_t logic or adapting.
        reAl += sin16_t(timeMs * 34) / 655340.0f
        imAg += sin16_t(timeMs * 26) / 655340.0f
        
        // Calculate Julia set for each pixel
        var y = ymin
        for (j in 0 until height) {
            var x = xmin
            for (i in 0 until width) {
                // Test if z = z^2 + c tends towards infinity
                var a = x
                var b = y
                var iter = 0
                
                while (iter < maxIterations) {
                    val aa = a * a
                    val bb = b * b
                    val len = aa + bb
                    
                    if (len > maxCalc) {
                        break  // Bail
                    }
                    
                    // z -> z^2 + c where z=a+ib, c=(reAl, imAg)
                    b = 2 * a * b + imAg
                    a = aa - bb + reAl
                    iter++
                }
                
                // Color based on iteration count
                if (iter == maxIterations) {
                    setPixelColor(i, j, RgbColor.BLACK)
                } else {
                    val colorIndex = (iter * 255 / maxIterations).coerceIn(0, 255)
                    val color = colorFromPalette(colorIndex, true, 0)
                    setPixelColor(i, j, color)
                }
                
                x += dx
            }
            y += dy
        }
        
        // Apply blur if enabled
        if (check1) {
            applyBlur(100, true)
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

    override fun getName(): String = "Julia"
    override fun supportsSpeed(): Boolean = false // speed not used? timeMs is used.
    override fun supportsIntensity(): Boolean = true

    fun setCustom1(value: Int) { this.custom1 = value.coerceIn(0, 255) }
    fun setCustom2(value: Int) { this.custom2 = value.coerceIn(0, 255) }
    fun setCustom3(value: Int) { this.custom3 = value.coerceIn(0, 255) }
    fun setCheck1(enabled: Boolean) { this.check1 = enabled }
    fun getCheck1(): Boolean { return check1 }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    private fun sin16_t(input: Long): Int {
        val normalized = (input % 65536) / 65536.0
        val radians = normalized * 2.0 * PI
        val sine = sin(radians)
        return (sine * 32767.0).roundToInt()
    }

    private fun applyBlur(amount: Int, smear: Boolean) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        // Creating temp array is costly every frame. But for now it's fine.
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val color = pixelColors[nx][ny]
                            sumR += color.r
                            sumG += color.g
                            sumB += color.b
                            count++
                        }
                    }
                }
                
                val current = pixelColors[x][y]
                val avgR = if (count > 0) sumR / count else current.r
                val avgG = if (count > 0) sumG / count else current.g
                val avgB = if (count > 0) sumB / count else current.b
                
                // Simplified blur logic
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + avgR * factor) / 255,
                    (current.g * (255 - factor) + avgG * factor) / 255,
                    (current.b * (255 - factor) + avgB * factor) / 255
                )
            }
        }
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val base = getColorFromPalette(index)
        val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
        return ColorUtils.scaleBrightness(base, brightnessFactor)
    }
}
