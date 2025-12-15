package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.physics.ParticleSystem
import com.marsraver.wledfx.physics.Particle
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Exploding fireworks effect
 */
class FireworksAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // Using the new ParticleSystem
    private val particleSystem = ParticleSystem(200)
    
    private var flare: Particle? = null
    private var state: Int = 0
    private var dyingGravity: Float = 0f
    private var numSparks: Int = 0
    
    // Internal variable to track flare readiness for explosion since standard Particle doesn't have custom state
    private var flareReadyToExplode = false 

    private var check3: Boolean = false // blur

    override fun getName(): String = "Fireworks"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        particleSystem.clear()
        flare = null
        state = 0
        dyingGravity = 0f
        numSparks = (5 + ((height * width) / 2)).coerceAtMost(100)
    }

    override fun update(now: Long): Boolean {
        // Fade out
        fadeToBlack(252)
        
        val cols = width
        val rows = height
        
        val gravity = (-0.0004f - (paramSpeed / 800000.0f)) * rows
        
        if (state < 2) { // FLARE
            if (state == 0) { // init flare
                val posX = Random.nextInt(2, cols - 3).toFloat()
                val peakHeight = (75 + Random.nextInt(180)) * (rows - 1) / 256
                val velY = sqrt(-2.0f * gravity * peakHeight)
                val velX = (Random.nextInt(9) - 4) / 64.0f
                val colIndex = Random.nextInt(256)
                
                // Spawn flare particle
                // Using data1 for 'brightness' simulation (starts at 255)
                // Using sourceIndex for color index
                flare = particleSystem.spawn(
                    x = posX,
                    y = 0f,
                    vx = velX,
                    vy = velY,
                    ax = 0f, 
                    ay = gravity,
                    life = 1.0f 
                )?.apply {
                    data1 = 255f // Brightness
                    sourceIndex = colIndex
                }
                
                state = 1
                flareReadyToExplode = false
            }
            
            val f = flare
            if (f != null) {
                // Manually update flare because we want to check its velocity logic specifically
                // or just let system update it?
                // The original logic checked velocity > threshold to explode.
                // Standard update applies gravity.
                
                f.update()
                
                // Draw flare
                val x = f.x.toInt().coerceIn(0, cols - 1)
                val y = (rows - f.y.toInt() - 1).coerceIn(0, rows - 1)
                val color = getColorFromPaletteWithBrightness(f.sourceIndex, f.data1.toInt())
                setPixelColor(x, y, color)
                
                // Logic from original: if (flare.vel > 12 * gravity) -> keep going
                // Gravity is negative. So 12 * gravity is negative.
                // If velocity is positive (moving up) and large, it's > negative number.
                // Eventally velocity becomes negative (falling).
                // Wait... original: if (flare.vel > 12 * gravity). Gravity is negative (-0.00something).
                // So 12 * gravity is a small negative number.
                // As long as it is moving up (vel > 0) or slightly down, it continues.
                // When it falls fast enough (vel < 12*gravity), it explodes?
                // Yes.
                
                if (f.vy > 12 * gravity) {
                    // Update brightness for trail effect
                    f.data1 -= 2f
                } else {
                    state = 2 // ready to explode
                    flareReadyToExplode = true
                }
            }
        } else if (state < 4) {
            // Explode!
            if (state == 2) {
                val f = flare
                if (f != null) {
                    var nSparks = (f.y + Random.nextInt(4)).toInt()
                    nSparks = nSparks.coerceAtLeast(4).coerceAtMost(numSparks)
                    
                    particleSystem.clear() // Clear flare
                    
                    for (i in 0 until nSparks) {
                        val velY = (Random.nextFloat() * 2.0f - 0.9f) * (if (rows < 32) 0.5f else 1.0f) * (f.y / rows) * (-gravity * 50)
                        val velX = (Random.nextFloat() * 2.0f - 1.0f) * (f.x / cols) * (-gravity * 50)
                        
                        // data1 = brightness/prog (starts 345 in original?)
                        // Original: spark.col = 345.
                        particleSystem.spawn(
                            x = f.x,
                            y = f.y,
                            vx = velX,
                            vy = velY,
                            ax = 0f, // Gravity added during explosion phase
                            ay = 0f,
                            life = 1.0f
                        )?.apply {
                            data1 = 345f
                            sourceIndex = Random.nextInt(256)
                        }
                    }
                }
                dyingGravity = gravity / 2
                state = 3
            }
            
            // Update sparks
            // We need to apply 'dyingGravity' to them which changes over time
            val particles = particleSystem.particles
            if (particles.isNotEmpty() && particles[0].data1 > 4) {
                 val iter = particles.iterator()
                 while(iter.hasNext()) {
                    val p = iter.next()
                    
                    // Custom physics for explosion
                    p.vx += dyingGravity
                    p.vy += dyingGravity // Original logic added dyingGravity to BOTH x and y velocity? Weird but ok.
                    
                    p.update()
                    
                    if (p.data1 > 3f) p.data1 -= 4f
                    
                    if (p.y > 0 && p.y < rows) {
                        val x = p.x.toInt()
                        if (x >= 0 && x < cols) {
                            val y = (rows - p.y.toInt() - 1).coerceIn(0, rows - 1)
                            val prog = p.data1.toInt()
                            
                            val spColor = getColorFromPaletteWithBrightness(p.sourceIndex, 255)
                            val c = when {
                                prog > 300 -> {
                                    val blend = ((prog - 300) * 5).coerceIn(0, 255)
                                    ColorUtils.blend(RgbColor.WHITE, spColor, blend)
                                }
                                prog > 45 -> {
                                    val blend = (prog - 45).coerceIn(0, 255)
                                    val color = ColorUtils.blend(RgbColor.BLACK, spColor, blend)
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

                if (check3) blur()
                dyingGravity *= 0.8f
            } else {
                state = 6 + Random.nextInt(10)
            }
        } else {
            state--
            if (state < 4) state = 0
        }
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixelColors.isInitialized && x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun cleanup() {
        particleSystem.clear()
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = color
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

    private fun blur() {
        // Simple box blur
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        for (x in 1 until width - 1) {
            for (y in 1 until height - 1) {
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
        for (x in 1 until width - 1) {
            for (y in 1 until height - 1) {
                pixelColors[x][y] = temp[x][y]
            }
        }
    }

    private fun getColorFromPaletteWithBrightness(colorIndex: Int, brightness: Int): RgbColor {
        val palette = paramPalette?.colors
        if (palette != null && palette.isNotEmpty()) {
            val paletteIndex = (colorIndex % 256 * palette.size / 256).coerceIn(0, palette.size - 1)
            val baseColor = palette[paletteIndex]
            val brightnessFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            val hue = (colorIndex * 360 / 256).toFloat()
            return ColorUtils.hsvToRgb(hue, 1.0f, brightness / 255.0f)
        }
    }
}
