package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.random.Random

/**
 * GhostRider - Ghost rider trail effect
 */
class GhostRiderAnimation : BaseAnimation() {

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

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    private var lastUpdateNs: Long = 0L
    private lateinit var lighter: Lighter
    private val vSpeed = 5f
    private val maxLighters = 20

    override fun getName(): String = "GhostRider"
    override fun supportsPalette(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        lastUpdateNs = System.nanoTime()
        paramSpeed = 128
        paramIntensity = 128
        
        lighter = Lighter()
        lighter.angleSpeed = (Random.nextInt(20) - 10).toFloat()
        lighter.gAngle = Random.nextFloat() * 360f
        lighter.gPosX = (width / 2f) * 10f
        lighter.gPosY = (height / 2f) * 10f
        
        for (i in 0 until maxLighters) {
            lighter.lightersPosX[i] = lighter.gPosX
            lighter.lightersPosY[i] = lighter.gPosY + i
            lighter.time[i] = i * 2
            lighter.reg[i] = false
        }
    }

    override fun update(now: Long): Boolean {
        val updateInterval = 1024_000_000L / (width + height)
        if (now - lastUpdateNs < updateInterval) return true
        lastUpdateNs = now
        
        fadeToBlack((paramSpeed shr 2) + 64)
        
        setWuPixel(lighter.gPosX / 10f, lighter.gPosY / 10f, RgbColor(255, 255, 255))
        
        val angleRad = lighter.gAngle * PI.toFloat() / 180f
        lighter.gPosX += vSpeed * sin(angleRad)
        lighter.gPosY += vSpeed * cos(angleRad)
        lighter.gAngle += lighter.angleSpeed
        
        if (lighter.gPosX < 0) lighter.gPosX = (width - 1) * 10f
        if (lighter.gPosX > (width - 1) * 10f) lighter.gPosX = 0f
        if (lighter.gPosY < 0) lighter.gPosY = (height - 1) * 10f
        if (lighter.gPosY > (height - 1) * 10f) lighter.gPosY = 0f
        
        for (i in 0 until maxLighters) {
            lighter.time[i] += Random.nextInt(5, 20)
            
            if (lighter.time[i] >= 255 ||
                lighter.lightersPosX[i] <= 0 ||
                lighter.lightersPosX[i] >= (width - 1) * 10f ||
                lighter.lightersPosY[i] <= 0 ||
                lighter.lightersPosY[i] >= (height - 1) * 10f) {
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
            val color = getColorFromPalette(paletteIndex)
            val scaledColor = ColorUtils.scaleBrightness(color, 1.0) // Redundant but consistent with previous logic flow
            setWuPixel(lighter.lightersPosX[i] / 10f, lighter.lightersPosY[i] / 10f, scaledColor)
        }
        
        blur(paramIntensity shr 3)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    private fun blur(amount: Int) {
        if (amount == 0) return
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                temp[x][y] = pixelColors[x][y]
            }
        }
        
        for (x in 1 until width - 1) {
            for (y in 1 until height - 1) {
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
        if (xi in 0 until width && yi in 0 until height) {
            pixelColors[xi][yi] = color
        }
    }
}
