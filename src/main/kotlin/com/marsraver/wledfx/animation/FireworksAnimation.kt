package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Exploding fireworks effect
 * adapted from: http://www.anirama.com/1000leds/1d-fireworks/
 * adapted for 2D WLED by blazoncek (Blaz Kristan (AKA blazoncek))
 */
class FireworksAnimation : LedAnimation {

    private data class Spark(
        var pos: Float = 0f,
        var posX: Float = 0f,
        var vel: Float = 0f,
        var velX: Float = 0f,
        var col: Int = 0,
        var colIndex: Int = 0
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private val sparks = mutableListOf<Spark>()
    private var flare: Spark = Spark()
    private var state: Int = 0 // 0=init, 1=launching, 2=ready to explode, 3=exploding, 4+=waiting
    private var dyingGravity: Float = 0f
    private var numSparks: Int = 0
    
    private var speed: Int = 128 // affects gravity
    private var intensity: Int = 128 // affects firing side on 1D
    private var check3: Boolean = false // blur
    
    private var currentPalette: Palette? = null

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
        sparks.clear()
        state = 0
        dyingGravity = 0f
        
        // Calculate number of sparks based on grid size
        numSparks = (5 + ((combinedHeight * combinedWidth) / 2)).coerceAtMost(100)
    }

    override fun update(now: Long): Boolean {
        // Fade out
        fadeToBlack(252)
        
        val cols = combinedWidth
        val rows = combinedHeight
        
        val gravity = (-0.0004f - (speed / 800000.0f)) * rows
        
        if (state < 2) { // FLARE
            if (state == 0) { // init flare
                flare.pos = 0f
                flare.posX = Random.nextInt(2, cols - 3).toFloat()
                val peakHeight = (75 + Random.nextInt(180)) * (rows - 1) / 256
                flare.vel = sqrt(-2.0f * gravity * peakHeight)
                flare.velX = (Random.nextInt(9) - 4) / 64.0f
                flare.col = 255 // brightness
                flare.colIndex = Random.nextInt(256)
                state = 1
            }
            
            // launch
            if (flare.vel > 12 * gravity) {
                // Draw flare
                val x = flare.posX.toInt().coerceIn(0, cols - 1)
                val y = (rows - flare.pos.toInt() - 1).coerceIn(0, rows - 1)
                val color = getColorFromPalette(flare.colIndex, flare.col)
                setPixelColor(x, y, color)
                
                flare.pos += flare.vel
                flare.pos = flare.pos.coerceIn(0f, (rows - 1).toFloat())
                flare.posX += flare.velX
                flare.posX = flare.posX.coerceIn(0f, (cols - 1).toFloat())
                flare.vel += gravity
                flare.col -= 2
            } else {
                state = 2 // ready to explode
            }
        } else if (state < 4) {
            // Explode!
            if (state == 2) {
                var nSparks = (flare.pos + Random.nextInt(4)).toInt()
                nSparks = nSparks.coerceAtLeast(4).coerceAtMost(numSparks)
                
                // Initialize sparks
                sparks.clear()
                for (i in 0 until nSparks) {
                    val spark = Spark()
                    spark.pos = flare.pos
                    spark.posX = flare.posX
                    spark.vel = (Random.nextFloat() * 2.0f - 0.9f) // from -0.9 to 1.1
                    spark.vel *= if (rows < 32) 0.5f else 1.0f // reduce velocity for smaller strips
                    spark.velX = (Random.nextFloat() * 2.0f - 1.0f) // from -1 to 1
                    spark.col = 345
                    spark.colIndex = Random.nextInt(256)
                    spark.vel *= flare.pos / rows // proportional to height
                    spark.velX *= flare.posX / cols // proportional to width
                    spark.vel *= -gravity * 50
                    sparks.add(spark)
                }
                
                dyingGravity = gravity / 2
                state = 3
            }
            
            if (sparks.isNotEmpty() && sparks[0].col > 4) {
                for (spark in sparks) {
                    spark.pos += spark.vel
                    spark.posX += spark.velX
                    spark.vel += dyingGravity
                    spark.velX += dyingGravity
                    if (spark.col > 3) spark.col -= 4
                    
                    if (spark.pos > 0 && spark.pos < rows) {
                        val x = spark.posX.toInt()
                        if (x >= 0 && x < cols) {
                            val y = (rows - spark.pos.toInt() - 1).coerceIn(0, rows - 1)
                            val prog = spark.col
                            
                            val spColor = getColorFromPalette(spark.colIndex, 255)
                            val c = when {
                                prog > 300 -> {
                                    // Fade from white to spark color
                                    val blend = ((prog - 300) * 5).coerceIn(0, 255)
                                    ColorUtils.blend(RgbColor.WHITE, spColor, blend)
                                }
                                prog > 45 -> {
                                    // Fade from spark color to black
                                    val blend = (prog - 45).coerceIn(0, 255)
                                    val color = ColorUtils.blend(RgbColor.BLACK, spColor, blend)
                                    // Cooling effect - reduce green and blue
                                    val cooling = (300 - prog) / 32
                                    RgbColor(
                                        color.r,
                                        (color.g - cooling).coerceAtLeast(0),
                                        (color.b - cooling * 2).coerceAtLeast(0)
                                    )
                                }
                                else -> RgbColor.BLACK
                            }
                            
                            setPixelColor(x, y, c)
                        }
                    }
                }
                
                if (check3) {
                    blur()
                }
                
                dyingGravity *= 0.8f // as sparks burn out they fall slower
            } else {
                state = 6 + Random.nextInt(10) // wait for this many frames
            }
        } else {
            state--
            if (state < 4) {
                state = 0 // back to flare
            }
        }
        
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Fireworks"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
        sparks.clear()
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun blur() {
        // Simple box blur
        val temp = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        for (x in 1 until combinedWidth - 1) {
            for (y in 1 until combinedHeight - 1) {
                var r = 0
                var g = 0
                var b = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val px = pixelColors[x + dx][y + dy]
                        r += px.r
                        g += px.g
                        b += px.b
                    }
                }
                temp[x][y] = RgbColor(r / 9, g / 9, b / 9)
            }
        }
        for (x in 1 until combinedWidth - 1) {
            for (y in 1 until combinedHeight - 1) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun getColorFromPalette(colorIndex: Int, brightness: Int): RgbColor {
        val palette = currentPalette
        if (palette != null && palette.colors.isNotEmpty()) {
            val paletteIndex = (colorIndex % 256 * palette.colors.size / 256).coerceIn(0, palette.colors.size - 1)
            val baseColor = palette.colors[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            // Use color wheel
            val hue = (colorIndex * 360 / 256).toFloat()
            return ColorUtils.hsvToRgb(hue, 1.0f, brightness / 255.0f)
        }
    }
}

