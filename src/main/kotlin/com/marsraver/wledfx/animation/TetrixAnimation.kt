package com.marsraver.wledfx.animation
import com.marsraver.wledfx.palette.Palette

import kotlin.random.Random

/**
 * Tetrix animation - Tetris-like falling blocks on each column.
 * Based on WLED mode_tetrix.
 */
class TetrixAnimation : LedAnimation {

    private data class Tetris(
        var stack: Float = 0f,      // Height of the brick stack
        var step: Int = 0,          // State: 0=init, 1=forming, 2=falling, >2=fade time
        var speed: Float = 0f,      // Falling speed
        var pos: Float = 0f,        // Current position (from top)
        var col: Int = 0,           // Color index
        var brick: Int = 0,         // Size of current brick
        var stackColors: MutableList<IntArray> = mutableListOf(),  // Colors of stacked bricks
        var initialized: Boolean = false,  // Whether this column has been initialized
        var startDelay: Int = 0  // Random delay before first brick starts (in frames)
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<IntArray>>
    private var currentPalette: Palette? = null
    private var currentColor: IntArray = intArrayOf(0, 0, 0) // SEGCOLOR(1)
    private var oneColor: Boolean = false  // check1 - use only one color from palette
    private var intensity: Int = 128      // Controls brick size
    private var speed: Int = 128          // Speed control (0-255)
    
    private val drops = mutableListOf<Tetris>()
    private var callCount: Long = 0
    private var lastUpdateNs: Long = 0L
    
    // Constants
    private val FRAMETIME_MS = 16.67  // ~60 FPS in milliseconds
    private val FADE_TIME_MS = 2000.0

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun supportsColor(): Boolean = true

    override fun setColor(r: Int, g: Int, b: Int) {
        currentColor = intArrayOf(r, g, b)
    }

    override fun getColor(): IntArray {
        return currentColor.clone()
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        
        // Initialize drops for each column (virtual strip)
        drops.clear()
        for (x in 0 until combinedWidth) {
            // Give each column a large random delay (0-500 frames = 0-8+ seconds at 60fps)
            // This creates much more stagger between columns
            val randomDelay = Random.nextInt(0, 500)
            drops.add(Tetris(
                stackColors = mutableListOf(),
                initialized = false,
                startDelay = randomDelay
            ))
        }
        
        // Initialize all pixels to black
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y] = intArrayOf(0, 0, 0)
            }
        }
        
        callCount = 0
        lastUpdateNs = 0L
    }

    override fun update(now: Long): Boolean {
        if (combinedWidth <= 0 || combinedHeight <= 0) return true

        if (lastUpdateNs == 0L) {
            lastUpdateNs = now
            return true
        }

        val deltaNs = now - lastUpdateNs
        lastUpdateNs = now
        val deltaMs = deltaNs / 1_000_000.0
        callCount++

        // Process each column (virtual strip)
        for (stripNr in 0 until combinedWidth) {
            val drop = drops[stripNr]
            runStrip(stripNr, drop, deltaMs)
        }

        return true
    }

    private fun runStrip(stripNr: Int, drop: Tetris, deltaMs: Double) {
        val stripLength = combinedHeight

        // Initialize each column independently
        if (!drop.initialized) {
            drop.stack = 0f
            drop.stackColors.clear()
            drop.step = 0  // Ready to start
            drop.initialized = true
            if (oneColor) drop.col = 0
        }

        // Apply start delay - don't do anything until delay is exhausted
        if (drop.startDelay > 0) {
            drop.startDelay--
            // Draw black while waiting
            for (y in 0 until stripLength) {
                pixelColors[stripNr][y] = intArrayOf(0, 0, 0)
            }
            return
        }

        if (drop.step == 0) {  // Init brick
            // Speed calculation: a single brick should reach bottom in X seconds
            // Speed 1 = 5s, Speed 255 = 0.25s
            val speedValue = if (speed > 0) speed else Random.nextInt(1, 255)
            val timeForFullDrop = map(speedValue, 1, 255, 5000.0, 250.0)
            drop.speed = ((stripLength * FRAMETIME_MS) / timeForFullDrop).toFloat()
            
            drop.pos = -drop.brick.toFloat()  // Start above the visible area (negative = above top)
            
            if (!oneColor) {
                drop.col = Random.nextInt(0, 16) shl 4  // Limit color choices for hue gap
            }
            
            drop.step = 1  // Drop state: 1 = forming
            drop.brick = if (intensity > 0) {
                ((intensity shr 5) + 1) * (1 + (stripLength shr 6))
            } else {
                Random.nextInt(1, 5) * (1 + (stripLength shr 6))
            }
        }

        if (drop.step == 1) {  // Forming
            // Random delay so columns don't all start at once
            // Each column has different random timing
            val randomValue = Random.nextInt(0, 256)
            if (randomValue >= 64) {  // Random drop (hw_random8()>>6)
                drop.step = 2  // Start falling
            }
        }

        if (drop.step == 2) {  // Falling
            // In original: pos starts at SEGLEN and decreases
            // In 2D: pos starts negative (above) and increases (falls down)
            // Stack is at the bottom (high y values)
            val stackTop = stripLength - drop.stack.toInt()  // Top of the stack (lowest y value)
            
            // Check if brick would touch the stack (bottom of brick touches top of stack)
            val brickBottom = drop.pos + drop.brick
            
            if (brickBottom < stackTop) {  // Fall until brick touches stack
                drop.pos += (drop.speed * (deltaMs / FRAMETIME_MS)).toFloat()
                
                // Stop exactly when brick touches stack (no sliding)
                val newBrickBottom = drop.pos + drop.brick
                if (newBrickBottom >= stackTop) {
                    drop.pos = stackTop - drop.brick.toFloat()  // Position so brick bottom touches stack top
                }
                
                // Draw the column: stack at bottom, falling brick above it
                // y=0 is top, y=stripLength-1 is bottom
                val brickStart = drop.pos.toInt().coerceAtLeast(0)
                val brickEnd = (drop.pos + drop.brick).toInt().coerceAtMost(stackTop)
                val stackHeight = drop.stack.toInt()
                
                // Draw from top to bottom
                for (y in 0 until stripLength) {
                    val color = when {
                        // Stack at bottom (y >= stackTop)
                        y >= stackTop -> {
                            val stackIndex = y - stackTop
                            if (stackIndex < drop.stackColors.size) {
                                drop.stackColors[stackIndex]
                            } else {
                                // Fallback color for stack
                                getColorFromPalette((drop.col + stackIndex * 16) % 256)
                            }
                        }
                        // Falling brick (visible part)
                        y >= brickStart && y < brickEnd -> {
                            getColorFromPalette(drop.col)
                        }
                        // Background (black)
                        else -> intArrayOf(0, 0, 0)
                    }
                    pixelColors[stripNr][y] = color
                }
            } else {  // Hit the stack (brick bottom touches stack top)
                // Position the brick exactly at the stack top (no overlap)
                drop.pos = stackTop - drop.brick.toFloat()
                
                // Draw the column with the brick now part of the stack
                val stackHeight = drop.stack.toInt() + drop.brick
                val newStackTop = stripLength - stackHeight
                
                // Add the brick to the stack (at the bottom)
                val brickColor = getColorFromPalette(drop.col)
                // Add colors to the end of the list (newest at the bottom)
                for (i in 0 until drop.brick) {
                    drop.stackColors.add(brickColor)
                }
                // Keep only the last stackHeight colors (trim from top if needed)
                if (drop.stackColors.size > stackHeight) {
                    drop.stackColors = drop.stackColors.takeLast(stackHeight).toMutableList()
                }
                
                // Draw the column with the new stack
                for (y in 0 until stripLength) {
                    val color = when {
                        // Stack at bottom (y >= newStackTop)
                        y >= newStackTop -> {
                            val stackIndex = y - newStackTop
                            if (stackIndex < drop.stackColors.size) {
                                drop.stackColors[stackIndex]
                            } else {
                                // Fallback color for stack
                                getColorFromPalette((drop.col + stackIndex * 16) % 256)
                            }
                        }
                        // Background (black)
                        else -> intArrayOf(0, 0, 0)
                    }
                    pixelColors[stripNr][y] = color
                }
                
                drop.step = 0  // Proceed with next brick
                drop.stack += drop.brick  // Increase stack size
                if (drop.stack >= stripLength) {
                    drop.step = (System.currentTimeMillis() + FADE_TIME_MS).toInt()  // Fade out
                }
            }
        }

        if (drop.step > 2) {  // Fade strip
            drop.brick = 0  // Reset brick size
            
            val fadeEndTime = drop.step.toLong()
            val currentTime = System.currentTimeMillis()
            
            if (currentTime < fadeEndTime) {
                // Fade the strip (blend with black background)
                val black = intArrayOf(0, 0, 0)
                for (y in 0 until stripLength) {
                    pixelColors[stripNr][y] = blendPixelColor(
                        pixelColors[stripNr][y],
                        black,
                        25  // 10% blend per frame
                    )
                }
            } else {
                // Fade complete, reset
                drop.stack = 0f
                drop.stackColors.clear()
                drop.step = 0  // Proceed with next brick
                if (oneColor) {
                    drop.col = (drop.col + 8) % 256  // Gradually increase palette index
                }
            }
        }
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y].clone()
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Tetrix"

    fun cleanup() {
        drops.clear()
    }

    private fun getColorFromPalette(colorIndex: Int): IntArray {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIdx = (colorIndex / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            return currentPalette[paletteIdx]
        } else {
            // Default rainbow
            return hsvToRgb(colorIndex, 255, 255)
        }
    }

    private fun blendPixelColor(color1: IntArray, color2: IntArray, blendAmount: Int): IntArray {
        val blend = blendAmount.coerceIn(0, 255) / 255.0
        val invBlend = 1.0 - blend
        return intArrayOf(
            ((color1[0] * invBlend + color2[0] * blend)).toInt().coerceIn(0, 255),
            ((color1[1] * invBlend + color2[1] * blend)).toInt().coerceIn(0, 255),
            ((color1[2] * invBlend + color2[2] * blend)).toInt().coerceIn(0, 255)
        )
    }

    private fun map(value: Int, inMin: Int, inMax: Int, outMin: Double, outMax: Double): Double {
        if (inMax == inMin) return outMin
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): IntArray {
        val h = (hue % 256 + 256) % 256
        val s = saturation.coerceIn(0, 255) / 255.0
        val v = value.coerceIn(0, 255) / 255.0

        if (s <= 0.0) {
            val gray = (v * 255).toInt()
            return intArrayOf(gray, gray, gray)
        }

        val hSection = h / 42.6666667
        val i = hSection.toInt()
        val f = hSection - i

        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))

        val (r, g, b) = when (i % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }

        return intArrayOf(
            (r * 255).toInt().coerceIn(0, 255),
            (g * 255).toInt().coerceIn(0, 255),
            (b * 255).toInt().coerceIn(0, 255)
        )
    }
}

