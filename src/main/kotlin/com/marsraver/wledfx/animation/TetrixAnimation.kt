package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.math.MathUtils
import kotlin.random.Random

/**
 * Tetrix animation - Tetris-like falling blocks
 */
class TetrixAnimation : BaseAnimation() {

    private data class Tetris(
        var stack: Float = 0f,      
        var step: Int = 0,          
        var speed: Float = 0f,      
        var pos: Float = 0f,        
        var col: Int = 0,           
        var brick: Int = 0,         
        var stackColors: MutableList<RgbColor> = mutableListOf(),  
        var initialized: Boolean = false,  
        var startDelay: Int = 0  
    )

    private lateinit var pixelColors: Array<Array<RgbColor>>
    
    // speed/intensity/palette/color managed by BaseAnimation
    // but currentColor -> paramColor
    
    private var oneColor: Boolean = false  // check1
    private val drops = mutableListOf<Tetris>()
    
    private var lastUpdateNs: Long = 0L
    private val FRAMETIME_MS = 16.67
    private val FADE_TIME_MS = 2000.0

    override fun getName(): String = "Tetrix"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        drops.clear()
        for (x in 0 until width) {
            val randomDelay = Random.nextInt(0, 500)
            drops.add(Tetris(stackColors = mutableListOf(), initialized = false, startDelay = randomDelay))
        }
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (width <= 0 || height <= 0) return true
        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }

        val deltaNs = now - lastUpdateNs
        lastUpdateNs = now
        val deltaMs = deltaNs / 1_000_000.0

        for (stripNr in 0 until width) {
            runStrip(stripNr, drops[stripNr], deltaMs)
        }
        return true
    }
    
    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    // ... logic for runStrip, keeping implementation details ...
    private fun runStrip(stripNr: Int, drop: Tetris, deltaMs: Double) {
        val stripLength = height
        
        if (!drop.initialized) {
            drop.stack = 0f
            drop.stackColors.clear()
            drop.step = 0
            drop.initialized = true
            if (oneColor) drop.col = 0
        }
        
        if (drop.startDelay > 0) {
            drop.startDelay--
            for (y in 0 until stripLength) pixelColors[stripNr][y] = RgbColor.BLACK
            return
        }

        if (drop.step == 0) {
            val speedValue = if (paramSpeed > 0) paramSpeed else Random.nextInt(1, 255)
            val timeForFullDrop = MathUtils.map(speedValue, 1, 255, 5000.0, 250.0)
            drop.speed = ((stripLength * FRAMETIME_MS) / timeForFullDrop).toFloat()
            drop.pos = -drop.brick.toFloat()
            
            if (!oneColor) drop.col = Random.nextInt(0, 16) shl 4
            
            drop.step = 1
            // Use paramIntensity
            drop.brick = if (paramIntensity > 0) {
                ((paramIntensity shr 5) + 1) * (1 + (stripLength shr 6))
            } else {
                Random.nextInt(1, 5) * (1 + (stripLength shr 6))
            }
        }
        
        if (drop.step == 1) {
             val randomValue = Random.nextInt(0, 256)
             if (randomValue >= 64) drop.step = 2
        }
        
        if (drop.step == 2) {
             val stackTop = stripLength - drop.stack.toInt()
             val brickBottom = drop.pos + drop.brick
             
             if (brickBottom < stackTop) {
                 drop.pos += (drop.speed * (deltaMs / FRAMETIME_MS)).toFloat()
                 val newBrickBottom = drop.pos + drop.brick
                 if (newBrickBottom >= stackTop) {
                     drop.pos = stackTop - drop.brick.toFloat()
                 }
                 
                 val brickStart = drop.pos.toInt().coerceAtLeast(0)
                 val brickEnd = (drop.pos + drop.brick).toInt().coerceAtMost(stackTop)
                 
                 for (y in 0 until stripLength) {
                     val color = when {
                         y >= stackTop -> {
                             val stackIndex = y - stackTop
                             if (stackIndex < drop.stackColors.size) drop.stackColors[stackIndex]
                             else getColorFromPalette((drop.col + stackIndex * 16) % 256)
                         }
                         y >= brickStart && y < brickEnd -> getColorFromPalette(drop.col)
                         else -> RgbColor.BLACK
                     }
                     pixelColors[stripNr][y] = color
                 }
             } else {
                 drop.pos = stackTop - drop.brick.toFloat()
                 val stackHeight = drop.stack.toInt() + drop.brick
                 val newStackTop = stripLength - stackHeight
                 
                 val brickColor = getColorFromPalette(drop.col)
                 for (i in 0 until drop.brick) drop.stackColors.add(brickColor)
                 
                 if (drop.stackColors.size > stackHeight) {
                     drop.stackColors = drop.stackColors.takeLast(stackHeight).toMutableList()
                 }
                 
                 for (y in 0 until stripLength) {
                     val color = when {
                         y >= newStackTop -> {
                             val stackIndex = y - newStackTop
                             if (stackIndex < drop.stackColors.size) drop.stackColors[stackIndex]
                             else getColorFromPalette((drop.col + stackIndex * 16) % 256)
                         }
                         else -> RgbColor.BLACK
                     }
                     pixelColors[stripNr][y] = color
                 }
                 
                 drop.step = 0
                 drop.stack += drop.brick
                 if (drop.stack >= stripLength) {
                     drop.step = (System.currentTimeMillis() + FADE_TIME_MS).toInt()
                 }
             }
        }
        
        if (drop.step > 2) {
            drop.brick = 0
            val fadeEndTime = drop.step.toLong()
            val currentTime = System.currentTimeMillis()
            
            if (currentTime < fadeEndTime) {
                for (y in 0 until stripLength) {
                    pixelColors[stripNr][y] = ColorUtils.blend(pixelColors[stripNr][y], RgbColor.BLACK, 25)
                }
            } else {
                drop.stack = 0f
                drop.stackColors.clear()
                drop.step = 0
                if (oneColor) drop.col = (drop.col + 8) % 256
            }
        }
    }
    
    override fun cleanup() {
        drops.clear()
    }
}
