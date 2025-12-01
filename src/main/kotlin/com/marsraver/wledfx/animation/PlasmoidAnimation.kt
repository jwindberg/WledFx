package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import com.marsraver.wledfx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * Plasmoid animation - Plasma-like effect with audio reactivity
 * By: Andrew Tuline
 */
class PlasmoidAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    
    private var thisPhase: Int = 0
    private var thatPhase: Int = 0
    
    @Volatile
    private var volumeSmth: Float = 0.0f
    private val audioLock = Any()
    private var audioScope: CoroutineScope? = null
    private var startTimeNs: Long = 0L

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
        thisPhase = 0
        thatPhase = 0
        startTimeNs = System.nanoTime()
        
        synchronized(audioLock) {
            volumeSmth = 0.0f
        }
        
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                // Get RMS volume for volumeSmth
                AudioPipeline.rmsFlow().collectLatest { level ->
                    synchronized(audioLock) {
                        volumeSmth = level.rms.toFloat()
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        // Fade to black by 32
        fadeToBlackBy(32)
        
        // Update phases using beatsin8_t
        // beatsin8_t(6, -4, 4) and beatsin8_t(7, -4, 4)
        thisPhase += beatsin8_t(6, -4, 4, timeMs)
        thatPhase += beatsin8_t(7, -4, 4, timeMs)
        
        val volume = synchronized(audioLock) { volumeSmth }
        val segmentLength = combinedWidth * combinedHeight
        
        // For each pixel in the strand
        for (i in 0 until segmentLength) {
            val x = i % combinedWidth
            val y = i / combinedWidth
            
            // Calculate brightness using cubicwave8 and cos8_t
            // cubicwave8(((i*(1 + (3*speed/32)))+thisPhase) & 0xFF)/2
            val wave1Arg = ((i * (1 + (3 * speed / 32))) + thisPhase) and 0xFF
            val thisbright = cubicwave8(wave1Arg) / 2
            
            // cos8_t(((i*(97 +(5*speed/32)))+thatPhase) & 0xFF)/2
            val wave2Arg = ((i * (97 + (5 * speed / 32))) + thatPhase) and 0xFF
            val brightness = thisbright + (cos8_t(wave2Arg) / 2)
            
            val colorIndex = brightness.coerceIn(0, 255)
            
            // Audio threshold: if volumeSmth * intensity / 64 < brightness, set brightness to 0
            val threshold = (volume * intensity / 64.0f).toInt()
            val finalBrightness = if (threshold < brightness) 0 else brightness
            
            // Get color from palette
            val color = colorFromPalette(colorIndex, true, 0)
            
            // Blend with background color (SEGCOLOR(1) - default black) and add to pixel
            val blendedColor = ColorUtils.blend(RgbColor.BLACK, color, finalBrightness)
            addPixelColor(x, y, blendedColor)
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

    override fun getName(): String = "Plasmoid"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    override fun cleanup() {
        audioScope?.cancel()
        audioScope = null
    }

    private fun fadeToBlackBy(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + color.r).coerceAtMost(255),
                (current.g + color.g).coerceAtMost(255),
                (current.b + color.b).coerceAtMost(255)
            )
        }
    }

    /**
     * beatsin8_t - Sine wave that beats at a given BPM
     * Returns value oscillating between min and max
     */
    private fun beatsin8_t(bpm: Int, min: Int, max: Int, timeMs: Long): Int {
        val periodMs = (60000.0 / bpm).toLong()
        val phase = ((timeMs % periodMs) * 2.0 * PI / periodMs).toDouble()
        val sine = sin(phase)
        val normalized = (sine + 1.0) / 2.0
        return (min + (normalized * (max - min))).roundToInt()
    }

    /**
     * cubicwave8 - Cubic wave function (0-255 input, 0-255 output)
     * Creates a smooth cubic wave pattern
     */
    private fun cubicwave8(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val phase = normalized * 2.0 * PI
        val sine = sin(phase)
        // Cubic approximation: 3*sin - sin^3
        val cubic = 3.0 * sine - sine * sine * sine
        return ((cubic + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * cos8_t - Cosine function for 8-bit input (0-255 maps to 0-2π)
     */
    private fun cos8_t(input: Int): Int {
        val normalized = (input and 0xFF) / 255.0
        val radians = normalized * 2.0 * PI
        val cosine = cos(radians)
        return ((cosine + 1.0) / 2.0 * 255.0).roundToInt().coerceIn(0, 255)
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

