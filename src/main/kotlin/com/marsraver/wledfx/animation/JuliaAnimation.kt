package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.roundToInt

/**
 * Julia animation - Animated Julia set fractal
 * By: Andrew Tuline
 */
class JuliaAnimation : LedAnimation {

    private data class JuliaState(
        var xcen: Float = 0.0f,
        var ycen: Float = 0.0f,
        var xymag: Float = 1.0f
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 24
    private var custom1: Int = 128  // X center
    private var custom2: Int = 128  // Y center
    private var custom3: Int = 16   // Area size
    private var check1: Boolean = false  // Blur
    
    private var juliaState = JuliaState()
    private var startTimeNs: Long = 0L
    private var initialized: Boolean = false

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
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        // Reset the center if we've just re-started this animation
        if (!initialized) {
            juliaState.xcen = 0.0f
            juliaState.ycen = 0.0f
            juliaState.xymag = 1.0f
            custom1 = 128
            custom2 = 128
            custom3 = 16
            intensity = 24
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
        val dx = (xmax - xmin) / combinedWidth
        val dy = (ymax - ymin) / combinedHeight
        
        // Max iterations
        val maxIterations = (intensity / 2).coerceAtLeast(1)
        val maxCalc = 16.0f
        
        // Animate real and imaginary parts
        var reAl = -0.94299f  // PixelBlaze example
        var imAg = 0.3162f
        
        reAl += sin16_t(timeMs * 34) / 655340.0f
        imAg += sin16_t(timeMs * 26) / 655340.0f
        
        // Calculate Julia set for each pixel
        var y = ymin
        for (j in 0 until combinedHeight) {
            var x = xmin
            for (i in 0 until combinedWidth) {
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
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Julia"

    override fun supportsSpeed(): Boolean = false

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set custom1 (X center)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
    }

    /**
     * Set custom2 (Y center)
     */
    fun setCustom2(value: Int) {
        this.custom2 = value.coerceIn(0, 255)
    }

    /**
     * Set custom3 (Area size)
     */
    fun setCustom3(value: Int) {
        this.custom3 = value.coerceIn(0, 255)
    }

    /**
     * Set check1 (Blur)
     */
    fun setCheck1(enabled: Boolean) {
        this.check1 = enabled
    }

    /**
     * Get check1
     */
    fun getCheck1(): Boolean {
        return check1
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    /**
     * sin16_t - Sine function for 16-bit input (returns -32768 to 32767 range, normalized)
     */
    private fun sin16_t(input: Long): Int {
        val normalized = (input % 65536) / 65536.0
        val radians = normalized * 2.0 * PI
        val sine = sin(radians)
        return (sine * 32767.0).roundToInt()
    }

    /**
     * Apply blur to the pixel grid
     */
    private fun applyBlur(amount: Int, smear: Boolean) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                // 3x3 blur kernel
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
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
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - factor) + avgR * factor) / 255,
                    (current.g * (255 - factor) + avgG * factor) / 255,
                    (current.b * (255 - factor) + avgB * factor) / 255
                )
            }
        }
        
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    /**
     * Get color from palette
     */
    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = if (brightness > 0) brightness / 255.0 else 1.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Fallback to HSV if no palette
            return ColorUtils.hsvToRgb(index, 255, if (brightness > 0) brightness else 255)
        }
    }
}

