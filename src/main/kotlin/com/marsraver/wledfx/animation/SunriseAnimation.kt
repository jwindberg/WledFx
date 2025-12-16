package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.sqrt
import kotlin.math.max

/**
 * Sunrise Animation
 * Simulates a sunrise with a rising sun and changing sky colors.
 */
class SunriseAnimation : BaseAnimation() {
    
    // Sky Colors (Dawn to Day)
    private val skyColors = listOf(
        RgbColor(0, 0, 10),      // Night
        RgbColor(20, 0, 40),     // Early Dawn (Purple-ish)
        RgbColor(60, 20, 60),    // Dawn (Purple/Red)
        RgbColor(100, 50, 20),   // Sunrise (Orange/Red)
        RgbColor(100, 100, 200), // Early Morning (Blue-ish)
        RgbColor(0, 150, 255)    // Day (Blue)
    )
    
    // Sun Colors (Red to White)
    private val sunColors = listOf(
        RgbColor(255, 0, 0),     // Red
        RgbColor(255, 100, 0),   // Orange
        RgbColor(255, 200, 0),   // Yellow
        RgbColor(255, 255, 200)  // White-ish
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var progress = 0.0 // 0.0 to 1.0 (Night to Day)
    
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true
    override fun getName(): String = "Sunrise"

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
    }
    
    override fun update(now: Long): Boolean {
        // Init if needed (sometimes onInit called before width/height set?)
        if (!::pixelColors.isInitialized || pixelColors.size != width || pixelColors[0].size != height) {
             pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        }

        // Speed controls the cycle speed
        // map speed 0-255 to step
        val step = 0.0005 + (paramSpeed / 255.0) * 0.005
        progress += step
        if (progress > 1.2) progress = -0.2 // Cycle with a bit of "day" pause and "night" pause
        
        // Clamp for rendering
        val renderProgress = progress.coerceIn(0.0, 1.0)
        
        // Calculate parameters
        val sunRadius = max(2.0, width / 6.0)
        
        // Sun Start Y = height + radius (below screen)
        // Sun End Y = height / 3 (upper third)
        val sunStartY = height + sunRadius
        val sunEndY = height / 3.0
        
        val sunY = sunStartY - (sunStartY - sunEndY) * renderProgress
        val sunX = width / 2.0
        
        drawFrame(renderProgress, sunX, sunY, sunRadius)
        
        return true
    }
    
    private fun drawFrame(p: Double, sunX: Double, sunY: Double, sunRadius: Double) {
        // 1. Draw Sky (Gradient based on progress)
        val skyColor = getInterpolatedColor(skyColors, p)
        val horizonColor = getInterpolatedColor(skyColors, (p + 0.1).coerceIn(0.0, 1.0)) // Horizon is slightly ahead/brighter?
        
        // Actually, simple solid sky fade is nicer for low res, or simple vertical gradient
        // Let's do a vertical gradient where top is skyColor and bottom is lighter/horizon
        
        for (y in 0 until height) {
            val hFactor = y / height.toDouble() // 0 at top, 1 at bottom
            val blendAmt = (hFactor * 0.5 * 255).toInt()
            val rowColor = ColorUtils.blend(skyColor, horizonColor, blendAmt) // Blend a bit
             
             for (x in 0 until width) {
                 setPixelColor(x, y, rowColor)
             }
        }
        
        // 2. Draw Sun
        val sunColor = getInterpolatedColor(sunColors, p)
        
        // Draw sun with soft edge/glow
        val glowRadius = sunRadius * 2.0
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val dx = x - sunX
                val dy = y - sunY
                val dist = sqrt(dx*dx + dy*dy)
                
                if (dist < glowRadius) {
                    // Core
                    if (dist < sunRadius) {
                        // Solid core with slight fade at edge
                        val edge = (dist / sunRadius).coerceIn(0.0, 1.0)
                        // Maybe blend core with white in center?
                        val blendAmt = ((1.0 - edge) * 0.5 * 255).toInt()
                        val pixelC = ColorUtils.blend(sunColor, RgbColor.WHITE, blendAmt)
                        blendPixel(x, y, pixelC, 1.0) // Opaque-ish
                    } else {
                        // Glow/Corona
                        val glow = (dist - sunRadius) / (glowRadius - sunRadius) // 0.0 at sun edge, 1.0 at glow edge
                        val intensity = (1.0 - glow) * p // Glow stronger as day breaks?
                        // Additive blending for glow
                        blendPixel(x, y, sunColor, intensity * 0.5)
                    }
                }
            }
        }
    }
    
    private fun getInterpolatedColor(colors: List<RgbColor>, progress: Double): RgbColor {
        if (progress <= 0) return colors.first()
        if (progress >= 1) return colors.last()
        
        val scaled = progress * (colors.size - 1)
        val idx = scaled.toInt()
        val frac = scaled - idx
        
        val c1 = colors[idx]
        val c2 = colors[(idx + 1).coerceAtMost(colors.size - 1)]
        
        // blend takes Int 0-255
        return ColorUtils.blend(c1, c2, (frac * 255).toInt())
    }
    
    private fun blendPixel(x: Int, y: Int, color: RgbColor, amount: Double) {
        if (amount <= 0) return
        val existing = getPixelColor(x, y)
        // Additive-ish approach for light:
        val r = (existing.r + color.r * amount).toInt().coerceIn(0, 255)
        val g = (existing.g + color.g * amount).toInt().coerceIn(0, 255)
        val b = (existing.b + color.b * amount).toInt().coerceIn(0, 255)
        
        setPixelColor(x, y, RgbColor(r, g, b))
    }
    
    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
}
