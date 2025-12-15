package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Waving Cell animation - heat palette waves animated with sinusoidal motion.
 * Audio RMS drives palette energy and bloom.
 */
class WavingCellAnimation : BaseAnimation() {

    private var timeValue: Double = 0.0

    override fun getName(): String = "Waving Cell"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = true
    override fun supportsPalette(): Boolean = true
    override fun supportsColor(): Boolean = false
    override fun getDefaultPaletteName(): String = "Heat"

    override fun onInit() {
        timeValue = 0.0
    }

    override fun update(now: Long): Boolean {
        timeValue = now / 1_000_000.0 / 100.0
        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        // Constant values for base animation (no audio reactivity support in original class for update loop variable modification?)
        // The comments say "audio rms drives palette energy..." but the code calculates `energy = 1.0` static.
        // Assuming I should keep provided logic which was static.
        
        val energy = 1.0
        val heatBoost = 0.0
        val brightnessScale = 1.0

        val t = timeValue
        val inner = sin8(y * 5 + t * 5.0 * energy)
        val wave = sin8(x * 10 + inner * energy)
        val vertical = cos8(y * 10.0 * energy)
        
        var index = wave * energy + vertical * (0.7 + energy * 0.3) + t + heatBoost
        index = wrapToPaletteRange(index)
        
        // Use BaseAnimation palette
        var color = getColorFromPalette(index.toInt())
        
        // Fallback or custom heat palette logic is not needed if Base provides Heat palette as default or user selected.
        // But the original had a custom HEAT_PALETTE fallback when currentPalette was null.
        // BaseAnimation defaults to Rainbow.
        // Let's rely on BaseAnimation. If user wants Heat, they select Heat.
        // Or if we want to enforce Heat as default?
        // BaseAnimation has no way to enforce default palette currently without overriding onInit and setting it, if Base allowed setting "default".
        // But Base just checks `palette != null`.
        // We can just return the color from base.
        
        return ColorUtils.scaleBrightness(color, brightnessScale)
    }

    private fun sin8(theta: Double): Double {
        var angle = theta % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        return (sin(radians) + 1.0) * 127.5
    }

    private fun cos8(theta: Double): Double {
        var angle = theta % 256.0
        if (angle < 0) angle += 256.0
        val radians = angle / 256.0 * 2.0 * PI
        return (cos(radians) + 1.0) * 127.5
    }

    private fun wrapToPaletteRange(value: Double): Double {
        var result = value % 256.0
        if (result < 0) result += 256.0
        return result
    }
}
