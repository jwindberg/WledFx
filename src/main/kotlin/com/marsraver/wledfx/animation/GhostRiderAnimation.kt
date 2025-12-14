package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.random.Random

/**
 * GhostRider - Ghost rider trail effect
 * 
 * Original C code from WLED v0.15.3 FX.cpp line 5910
 * Simplified version focusing on core trail behavior
 */
class GhostRiderAnimation : LedAnimation {

    private data class Lighter(
        var gPosX: Float = 0f,
        var gPosY: Float = 0f,
        var gAngle: Float = 0f,
        var angleSpeed: Float = 0f,
        val lightersPosX: FloatArray = FloatArray(20),
        val lightersPosY: FloatArray = FloatArray(20),
        val angles: FloatArray = FloatArray(20),
        val time: IntArray = IntArray(20),
        val reg: BooleanArray = BooleanArray(20)
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    private var lastUpdateNs: Long = 0L
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private lateinit var lighter: Lighter
    private val vSpeed = 5f
    private val maxLighters = 20

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? = currentPalette

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        lastUpdateNs = startTimeNs
        
        // Initialize lighter
        lighter = Lighter()
        lighter.angleSpeed = (Random.nextInt(20) - 10).toFloat()
        lighter.gAngle = Random.nextFloat() * 360f
        lighter.gPosX = (combinedWidth / 2f) * 10f
        lighter.gPosY = (combinedHeight / 2f) * 10f
        
        for (i in 0 until maxLighters) {
            lighter.lightersPosX[i] = lighter.gPosX
            lighter.lightersPosY[i] = lighter.gPosY + i
            lighter.time[i] = i * 2
            lighter.reg[i] = false
        }
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) startTimeNs = now
        
        val updateInterval = 1024_000_000L / (combinedWidth + combinedHeight)
        if (now - lastUpdateNs < updateInterval) return true
        lastUpdateNs = now
        
        // Fade to black
        fadeToBlack((speed shr 2) + 64)
        
        // Draw main lighter
        setWuPixel(lighter.gPosX / 10f, lighter.gPosY / 10f, RgbColor(255, 255, 255))
        
        // Update main lighter position
        val angleRad = lighter.gAngle * PI.toFloat() / 180f
        lighter.gPosX += vSpeed * sin(angleRad)
        lighter.gPosY += vSpeed * cos(angleRad)
        lighter.gAngle += lighter.angleSpeed
        
        // Wrap around
        if (lighter.gPosX < 0) lighter.gPosX = (combinedWidth - 1) * 10f
        if (lighter.gPosX > (combinedWidth - 1) * 10f) lighter.gPosX = 0f
        if (lighter.gPosY < 0) lighter.gPosY = (combinedHeight - 1) * 10f
        if (lighter.gPosY > (combinedHeight - 1) * 10f) lighter.gPosY = 0f
        
        // Update trail lighters
        for (i in 0 until maxLighters) {
            lighter.time[i] += Random.nextInt(5, 20)
            
            if (lighter.time[i] >= 255 ||
                lighter.lightersPosX[i] <= 0 ||
                lighter.lightersPosX[i] >= (combinedWidth - 1) * 10f ||
                lighter.lightersPosY[i] <= 0 ||
                lighter.lightersPosY[i] >= (combinedHeight - 1) * 10f) {
                lighter.reg[i] = true
            }
            
            if (lighter.reg[i]) {
                lighter.lightersPosY[i] = lighter.gPosY
                lighter.lightersPosX[i] = lighter.gPosX
                lighter.angles[i] = lighter.gAngle + (Random.nextInt(20) - 10)
                lighter.time[i] = 0
                lighter.reg[i] = false
            } else {
                val trailAngleRad = lighter.angles[i] * PI.toFloat() / 180f
                lighter.lightersPosX[i] += -7 * sin(trailAngleRad)
                lighter.lightersPosY[i] += -7 * cos(trailAngleRad)
            }
            
            val paletteIndex = 256 - lighter.time[i]
            val color = colorFromPalette(paletteIndex, true, 255)
            setWuPixel(lighter.lightersPosX[i] / 10f, lighter.lightersPosY[i] / 10f, color)
        }
        
        blur(intensity shr 3)
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "GhostRider"

    override fun isAudioReactive(): Boolean = false

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int = speed

    override fun cleanup() {}
    
    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    private fun blur(amount: Int) {
        if (amount == 0) return
        // Simple box blur (same as PlasmaBall2D)
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                temp[x][y] = pixelColors[x][y]
            }
        }
        
        for (x in 1 until combinedWidth - 1) {
            for (y in 1 until combinedHeight - 1) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val color = temp[x + dx][y + dy]
                        r += color.r
                        g += color.g
                        b += color.b
                        count++
                    }
                }
                
                pixelColors[x][y] = RgbColor(r / count, g / count, b / count)
            }
        }
    }
    
    private fun setWuPixel(x: Float, y: Float, color: RgbColor) {
        val xi = x.toInt()
        val yi = y.toInt()
        if (xi in 0 until combinedWidth && yi in 0 until combinedHeight) {
            pixelColors[xi][yi] = color
        }
    }
    
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
            return ColorUtils.hsvToRgb(index % 256, 255, if (brightness > 0) brightness else 255)
        }
    }
}
