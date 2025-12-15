package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils

/**
 * TwinkleFox animation - Twinkling 'holiday' lights that fade in and out.
 * By Mark Kriegsman: https://gist.github.com/kriegsman/756ea6dcae8e30845b5a
 */
class TwinkleFoxAnimation : BaseAnimation() {

    private var cool: Boolean = true // check1 - cooling effect (fade toward red)
    private var cat: Boolean = false // cat mode - instant on, fade off (twinklecat)
    
    private var startTimeNs: Long = 0L
    private var aux0: Int = 22  // Speed parameter (calculated from speed)
    
    override fun supportsCatMode(): Boolean = true

    override fun setCatMode(enabled: Boolean) {
        cat = enabled
    }

    override fun supportsColor(): Boolean = true

    override fun onInit() {
        startTimeNs = 0L
        paramSpeed = 128
        paramIntensity = 128
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
        val currentPalette = getPalette()?.colors
        if (currentPalette == null || currentPalette.isEmpty()) {
            return RgbColor.BLACK
        }

        val pixelIndex = y * width + x
        
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
    override fun supportsIntensity(): Boolean = true
    
    fun setCool(enabled: Boolean) { cool = enabled }

    private fun updateSpeed() {
        // Calculate speed parameter
        aux0 = if (paramSpeed > 100) {
            3 + ((255 - paramSpeed) shr 3)
        } else {
            22 + ((100 - paramSpeed) shr 1)
        }
    }

    private fun twinklefoxOneTwinkle(ms: Long, salt: Int, cat: Boolean): RgbColor {
        // Overall twinkle speed
        val ticks = (ms / aux0).toInt()
        val fastcycle8 = (ticks and 0xFF)
        var slowcycle16 = ((ticks shr 8) + salt) and 0xFFFF
        
        slowcycle16 = (slowcycle16 + MathUtils.sin8(slowcycle16)) and 0xFFFF
        slowcycle16 = ((slowcycle16 * 2053) + 1384) and 0xFFFF
        val slowcycle8 = ((slowcycle16 and 0xFF) + (slowcycle16 shr 8)) and 0xFF
        
        // Overall twinkle density (0 = NONE lit, 8 = ALL lit at once, default is 5)
        val twinkleDensity = (paramIntensity shr 5) + 1
        
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
            val color = getColorFromHue(hue, bright)
            
            // Cooling effect: fade toward red as dimming (applied when cool checkbox is NOT checked)
            // Wait, original logic: if (!cool ...) applies cooling?
            // "Cooling effect: fade toward red as dimming" usually means RED shift.
            // If cool=false means Warm/Redshift?
            if (!cool && fastcycle8 >= 128) {
                val cooling = (fastcycle8 - 128) shr 4
                RgbColor(
                    color.r,
                    MathUtils.qsub8(color.g, cooling),
                    MathUtils.qsub8(color.b, cooling * 2)
                )
            } else {
                color
            }
        } else {
            RgbColor.BLACK
        }
    }

    private fun getBackgroundColor(): RgbColor {
        val bg = paramColor
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
                // Dim, scale to 1/3rd // 86/255 -> 0.337 ~ 1/3
                ColorUtils.scaleBrightness(bg, 86.0 / 255.0)
            }
        }
    }

    private fun getColorFromHue(hue: Int, brightness: Int): RgbColor {
        // Use BaseAnimation palette if available
        val base = getColorFromPalette(hue)
        // Apply brightness
        val brightFactor = brightness / 255.0
        return ColorUtils.scaleBrightness(base, brightFactor)
    }

    private fun getAverageLight(color: RgbColor): Int {
        return ColorUtils.getAverageLight(color)
    }

    private fun colorBlend(color1: RgbColor, color2: RgbColor, blendAmount: Int): RgbColor {
        return ColorUtils.blend(color1, color2, blendAmount)
    }
}
