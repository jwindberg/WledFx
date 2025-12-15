package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.random.Random

/**
 * Multi Comet
 * Multiple comets moving along the strip.
 */
class MultiCometAnimation : BaseAnimation() {
    
    // Internal Comet class
    private class Comet(
        var pos: Double,
        var speed: Double, // pixels per second
        var hue: Int,
        var length: Int
    )

    private lateinit var leds: Array<RgbColor>
    private val comets = ArrayList<Comet>()
    private var startTimeNs: Long = 0L
    private var lastTimeNs: Long = 0L
    
    override fun getName(): String = "Multi Comet"
    override fun is1D(): Boolean = true
    override fun is2D(): Boolean = false

    override fun onInit() {
        leds = Array(pixelCount) { RgbColor.BLACK }
        paramSpeed = 128
        spawnComets()
        startTimeNs = System.nanoTime()
        lastTimeNs = startTimeNs
    }
    
    private fun spawnComets() {
        comets.clear()
        // Spawn initial comets
        val count = 3 + (paramSpeed / 64) // 3 to 6 comets
        repeat(count) {
            spawnComet()
        }
    }
    
    private fun spawnComet() {
        val dir = if (Random.nextBoolean()) 1.0 else -1.0
        val s = (10.0 + Random.nextInt(50)) * (paramSpeed / 128.0 + 0.1) * dir
        comets.add(Comet(
            pos = Random.nextDouble() * pixelCount,
            speed = s,
            hue = Random.nextInt(255),
            length = 5 + Random.nextInt(10)
        ))
    }
    
    override fun update(now: Long): Boolean {
        if (lastTimeNs == 0L) lastTimeNs = now
        val deltaS = (now - lastTimeNs) / 1_000_000_000.0
        lastTimeNs = now
        
        // 1. Fade existing LEDs (Trails)
        val fade = (50 * deltaS * 60).toInt().coerceIn(10, 255)
        for (i in 0 until pixelCount) {
             val c = leds[i]
             if (c.r > 0 || c.g > 0 || c.b > 0) {
                 val fadeAmt = 20 // Fixed fade for now
                 leds[i] = RgbColor(
                     (c.r - fadeAmt).coerceAtLeast(0),
                     (c.g - fadeAmt).coerceAtLeast(0),
                     (c.b - fadeAmt).coerceAtLeast(0)
                 )
             }
        }
        
        // 2. Move and Draw Comets
        val spdMult = paramSpeed / 64.0
        
        comets.forEach { c ->
            c.pos += c.speed * spdMult * deltaS * 20.0
            
            // Wrap
            if (c.pos >= pixelCount) c.pos -= pixelCount
            if (c.pos < 0) c.pos += pixelCount
            
            // Draw Head
            val idx = c.pos.toInt() % pixelCount
            // Use getColorFromPalette from BaseAnimation
            // c.hue is 0-255
            val color = getColorFromPalette(c.hue)
            
            if (idx in 0 until pixelCount) {
                leds[idx] = color
            }
            
            c.hue = (c.hue + 1) % 256
        }
        
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        val index = getPixelIndex(x, y)
        if (index in 0 until pixelCount) return leds[index]
        return RgbColor.BLACK
    }
}
