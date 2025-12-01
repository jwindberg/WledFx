package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.PI
import kotlin.math.sin

/**
 * TwinkleFox animation - Twinkling 'holiday' lights that fade in and out.
 * By Mark Kriegsman: https://gist.github.com/kriegsman/756ea6dcae8e30845b5a
 */
class TwinkleFoxAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var currentPalette: Palette? = null
    private var currentColor: RgbColor = RgbColor.BLACK // SEGCOLOR(1) - background
    private var intensity: Int = 128  // Controls twinkle density (0-255, default ~160 = 5)
    private var speed: Int = 128     // Speed control (0-255)
    private var cool: Boolean = true // check1 - cooling effect (fade toward red)
    private var cat: Boolean = false // cat mode - instant on, fade off (twinklecat)
    
    private var startTimeNs: Long = 0L
    private var aux0: Int = 22  // Speed parameter (calculated from speed)
    
    override fun supportsCatMode(): Boolean = true

    override fun setCatMode(enabled: Boolean) {
        cat = enabled
    }

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun supportsColor(): Boolean = true

    override fun setColor(color: RgbColor) {
        currentColor = color
    }

    override fun getColor(): RgbColor? {
        return currentColor
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        startTimeNs = 0L
        updateSpeed()
    }

    override fun update(now: Long): Boolean {
        if (startTimeNs == 0L) {
            startTimeNs = now
        }
        updateSpeed()
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return RgbColor.BLACK
        }

        val segmentLength = combinedWidth * combinedHeight
        val pixelIndex = y * combinedWidth + x
        
        // Set up background color
        val bg = getBackgroundColor()
        val bgLight = getAverageLight(bg)
        
        // PRNG16 - must be reset to same starting value each frame
        // Calculate PRNG state for this specific pixel index
        var prng16 = 11337
        
        // Advance PRNG to this pixel's position (each pixel needs 2 PRNG values)
        for (i in 0 until pixelIndex) {
            prng16 = ((prng16 * 2053) + 1384) and 0xFFFF  // First PRNG for clock offset
            prng16 = ((prng16 * 2053) + 1384) and 0xFFFF  // Second PRNG for speed multiplier
        }
        
        // Get clock offset for this pixel
        prng16 = ((prng16 * 2053) + 1384) and 0xFFFF
        val myClockOffset16 = prng16
        
        // Get speed multiplier (in 8ths, from 8/8ths to 23/8ths)
        prng16 = ((prng16 * 2053) + 1384) and 0xFFFF
        val prngLow = prng16 and 0xFF
        val myspeedmultiplierQ5_3 = (((prngLow shr 4) + (prngLow and 0x0F)) and 0x0F) + 0x08
        val myUnique8 = prng16 shr 8  // 'salt' value
        
        // Calculate adjusted clock for this pixel
        val timeMs = (System.nanoTime() - startTimeNs) / 1_000_000L
        val myClock30 = ((timeMs * myspeedmultiplierQ5_3) shr 3) + myClockOffset16.toLong()
        
        // Calculate twinkle color
        val c = twinklefoxOneTwinkle(myClock30, myUnique8, cat)
        
        // Blend with background based on brightness
        val cLight = getAverageLight(c)
        val deltaBright = cLight - bgLight
        
        return when {
            deltaBright >= 32 || bgLight == 0 -> {
                // Significantly brighter than background, use twinkle color
                c
            }
            deltaBright > 0 -> {
                // Slightly brighter, blend
                colorBlend(bg, c, (deltaBright * 8).coerceIn(0, 255))
            }
            else -> {
                // Not brighter, use background
                bg
            }
        }
    }

    override fun getName(): String = "TwinkleFox"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    private fun updateSpeed() {
        // Calculate speed parameter
        aux0 = if (speed > 100) {
            3 + ((255 - speed) shr 3)
        } else {
            22 + ((100 - speed) shr 1)
        }
    }

    private fun twinklefoxOneTwinkle(ms: Long, salt: Int, cat: Boolean): RgbColor {
        // Overall twinkle speed
        val ticks = (ms / aux0).toInt()
        val fastcycle8 = (ticks and 0xFF)
        var slowcycle16 = ((ticks shr 8) + salt) and 0xFFFF
        
        slowcycle16 = (slowcycle16 + sin8(slowcycle16)) and 0xFFFF
        slowcycle16 = ((slowcycle16 * 2053) + 1384) and 0xFFFF
        val slowcycle8 = ((slowcycle16 and 0xFF) + (slowcycle16 shr 8)) and 0xFF
        
        // Overall twinkle density (0 = NONE lit, 8 = ALL lit at once, default is 5)
        val twinkleDensity = (intensity shr 5) + 1
        
        var bright = 0
        if (((slowcycle8 and 0x0E) shr 1) < twinkleDensity) {
            val ph = fastcycle8
            
            if (cat) {
                // twinklecat: instant on, fade off
                bright = 255 - ph
            } else {
                // vanilla twinklefox: triangle wave with faster attack, slower decay
                if (ph < 86) {
                    bright = ph * 3
                } else {
                    val ph2 = ph - 86
                    bright = 255 - (ph2 + (ph2 shr 1))
                }
            }
        }
        
        val hue = (slowcycle8 - salt + 256) % 256
        return if (bright > 0) {
            val color = getColorFromPalette(hue, bright)
            
            // Cooling effect: fade toward red as dimming (applied when cool checkbox is NOT checked)
            if (!cool && fastcycle8 >= 128) {
                val cooling = (fastcycle8 - 128) shr 4
                RgbColor(
                    color.r,
                    qsub8(color.g, cooling),
                    qsub8(color.b, cooling * 2)
                )
            } else {
                color
            }
        } else {
            RgbColor.BLACK
        }
    }

    private fun getBackgroundColor(): RgbColor {
        val bg = currentColor
        val bgLight = getAverageLight(bg)
        
        // Scale background brightness
        return when {
            bgLight > 64 -> {
                // Very bright, scale to 1/16th
                ColorUtils.scaleBrightness(bg, 1.0 / 16.0)
            }
            bgLight > 16 -> {
                // Not that bright, scale to 1/4th
                ColorUtils.scaleBrightness(bg, 1.0 / 4.0)
            }
            else -> {
                // Dim, scale to 1/3rd
                ColorUtils.scaleBrightness(bg, 86.0 / 255.0)
            }
        }
    }

    private fun getColorFromPalette(hue: Int, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIdx = (hue % currentPalette.size).coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIdx]
            // Apply brightness
            val brightFactor = brightness / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightFactor)
        } else {
            // Default rainbow
            return ColorUtils.hsvToRgb(hue, 255, brightness)
        }
    }

    private fun getAverageLight(color: RgbColor): Int {
        return ColorUtils.getAverageLight(color)
    }

    private fun colorBlend(color1: RgbColor, color2: RgbColor, blendAmount: Int): RgbColor {
        return ColorUtils.blend(color1, color2, blendAmount)
    }

    private fun qsub8(value: Int, subtract: Int): Int {
        return if (value > subtract) value - subtract else 0
    }

    private fun sin8(angle: Int): Int {
        return ColorUtils.sin8(angle)
    }

}

