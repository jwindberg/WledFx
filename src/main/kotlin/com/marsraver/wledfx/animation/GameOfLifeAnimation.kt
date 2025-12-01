package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.random.Random

/**
 * Game of Life animation - Conway's Game of Life cellular automaton
 * Written by Ewoud Wijma, inspired by natureofcode.com, Modified By: Brandon Butler
 */
class GameOfLifeAnimation : LedAnimation {

    private data class Cell(
        var alive: Boolean = false,
        var faded: Boolean = false,
        var toggleStatus: Boolean = false,
        var edgeCell: Boolean = false,
        var oscillatorCheck: Boolean = false,
        var spaceshipCheck: Boolean = false
    )

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var cells: Array<Array<Cell>>
    private var currentPalette: Palette? = null
    
    private var speed: Int = 128
    private var intensity: Int = 128
    private var custom1: Int = 128  // Blur
    private var check3: Boolean = false  // Mutation
    
    private var generation: Int = 0
    private var gliderLength: Int = 0
    private var step: Long = 0L
    private var startTimeNs: Long = 0L
    private val random = Random.Default

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
        cells = Array(combinedWidth) { Array(combinedHeight) { Cell() } }
        startTimeNs = System.nanoTime()
        
        // Calculate glider length LCM(rows,cols)*4
        var a = combinedHeight
        var b = combinedWidth
        while (b != 0) {
            val t = b
            b = a % b
            a = t
        }
        gliderLength = (combinedWidth * combinedHeight / a) shl 2
        
        generation = 0
        step = 0L
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        
        val maxIndex = combinedWidth * combinedHeight
        val mutate = check3
        val blur = map(custom1, 0, 255, 255, 4)
        
        val bgColor = RgbColor.BLACK
        val birthColor = colorFromPalette(128, false, 255)
        
        val setup = generation == 0 && step == 0L
        
        // Timebase jump fix
        if (kotlin.math.abs(timeMs - step) > 2000) {
            step = 0L
        }
        
        val paused = step > timeMs
        
        // Setup New Game of Life
        if ((!paused && generation == 0) || setup) {
            step = timeMs + 1280  // Show initial state for 1.28 seconds
            generation = 1
            
            // Setup Grid
            for (x in 0 until combinedWidth) {
                for (y in 0 until combinedHeight) {
                    cells[x][y] = Cell()
                    val isAlive = random.nextInt(3) == 0  // ~33%
                    cells[x][y].alive = isAlive
                    cells[x][y].faded = !isAlive
                    cells[x][y].edgeCell = (x == 0 || x == combinedWidth - 1 || y == 0 || y == combinedHeight - 1)
                    
                    val color = if (isAlive) {
                        colorFromPalette(random.nextInt(256), false, 0)
                    } else {
                        bgColor
                    }
                    setPixelColor(x, y, color)
                }
            }
            return true
        }
        
        val updateInterval = 1000 / map(speed, 0, 255, 1, 42)
        
        if (paused || (timeMs - step < updateInterval)) {
            // Redraw if paused or between updates to remove blur
            for (x in 0 until combinedWidth) {
                for (y in 0 until combinedHeight) {
                    if (!cells[x][y].alive) {
                        val cellColor = getPixelColor(x, y)
                        if (cellColor != bgColor) {
                            val newColor = if (cells[x][y].faded) {
                                bgColor
                            } else {
                                val blended = ColorUtils.blend(cellColor, bgColor, 2)
                                if (blended == cellColor) {
                                    cells[x][y].faded = true
                                    bgColor
                                } else {
                                    blended
                                }
                            }
                            setPixelColor(x, y, newColor)
                        }
                    }
                }
            }
            return true
        }
        
        // Repeat detection
        val updateOscillator = generation % 16 == 0
        val updateSpaceship = gliderLength != 0 && generation % gliderLength == 0
        var repeatingOscillator = true
        var repeatingSpaceship = true
        var emptyGrid = true
        
        // Update cells
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val cell = cells[x][y]
                
                if (cell.alive) emptyGrid = false
                if (cell.oscillatorCheck != cell.alive) repeatingOscillator = false
                if (cell.spaceshipCheck != cell.alive) repeatingSpaceship = false
                if (updateOscillator) cell.oscillatorCheck = cell.alive
                if (updateSpaceship) cell.spaceshipCheck = cell.alive
                
                // Count alive neighbors
                var neighbors = 0
                var aliveParents = 0
                val parentIdx = mutableListOf<Pair<Int, Int>>()
                
                for (i in -1..1) {
                    for (j in -1..1) {
                        if (i == 0 && j == 0) continue
                        
                        var nX = x + j
                        var nY = y + i
                        
                        if (cell.edgeCell) {
                            nX = (nX + combinedWidth) % combinedWidth
                            nY = (nY + combinedHeight) % combinedHeight
                        } else {
                            if (nX < 0 || nX >= combinedWidth || nY < 0 || nY >= combinedHeight) continue
                        }
                        
                        val neighbor = cells[nX][nY]
                        if (neighbor.alive) {
                            neighbors++
                            if (!neighbor.toggleStatus && aliveParents < 3) {
                                parentIdx.add(Pair(nX, nY))
                                aliveParents++
                            }
                        }
                    }
                }
                
                var newColor: RgbColor? = null
                var needsColor = false
                
                if (cell.alive && (neighbors < 2 || neighbors > 3)) {
                    // Loneliness or Overpopulation
                    cell.toggleStatus = true
                    if (blur == 255) cell.faded = true
                    val currentColor = getPixelColor(x, y)
                    newColor = if (cell.faded) {
                        bgColor
                    } else {
                        ColorUtils.blend(currentColor, bgColor, blur)
                    }
                    needsColor = true
                } else if (!cell.alive) {
                    val mutationRoll = if (mutate) random.nextInt(128) else 1
                    if ((neighbors == 3 && mutationRoll != 0) || (mutate && neighbors == 2 && mutationRoll == 0)) {
                        // Reproduction or Mutation
                        cell.toggleStatus = true
                        cell.faded = false
                        
                        val finalBirthColor = if (aliveParents > 0) {
                            val parent = parentIdx[random.nextInt(aliveParents)]
                            getPixelColor(parent.first, parent.second)
                        } else {
                            birthColor
                        }
                        newColor = finalBirthColor
                        needsColor = true
                    } else if (!cell.faded) {
                        // No change, fade dead cells
                        val cellColor = getPixelColor(x, y)
                        val blended = ColorUtils.blend(cellColor, bgColor, blur)
                        newColor = if (blended == cellColor) {
                            cell.faded = true
                            bgColor
                        } else {
                            blended
                        }
                        needsColor = true
                    }
                }
                
                if (needsColor && newColor != null) {
                    setPixelColor(x, y, newColor)
                }
            }
        }
        
        // Apply toggles
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val cell = cells[x][y]
                if (cell.toggleStatus) {
                    cell.alive = !cell.alive
                    cell.toggleStatus = false
                }
            }
        }
        
        if (repeatingOscillator || repeatingSpaceship || emptyGrid) {
            generation = 0
            step += 1024  // Pause final generation for ~1 second
        } else {
            generation++
            step = timeMs
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

    override fun getName(): String = "Game Of Life"

    override fun supportsSpeed(): Boolean = true

    override fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 255)
    }

    override fun getSpeed(): Int? {
        return speed
    }

    /**
     * Set custom1 (Blur)
     */
    fun setCustom1(value: Int) {
        this.custom1 = value.coerceIn(0, 255)
    }

    /**
     * Set check3 (Mutation)
     */
    fun setCheck3(enabled: Boolean) {
        this.check3 = enabled
    }

    private fun setPixelColor(x: Int, y: Int, color: RgbColor) {
        if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y] = color
        }
    }

    private fun map(value: Int, fromLow: Int, fromHigh: Int, toLow: Int, toHigh: Int): Int {
        val fromRange = (fromHigh - fromLow).toDouble()
        val toRange = (toHigh - toLow).toDouble()
        if (fromRange == 0.0) return toLow
        val scaled = (value - fromLow) / fromRange
        return (toLow + scaled * toRange).toInt()
    }

    private fun colorFromPalette(index: Int, wrap: Boolean, brightness: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = if (wrap) {
                (index % 256) * currentPalette.size / 256
            } else {
                ((index % 256) * currentPalette.size / 256).coerceIn(0, currentPalette.size - 1)
            }
            val baseColor = currentPalette[paletteIndex.coerceIn(0, currentPalette.size - 1)]
            val brightnessFactor = brightness.coerceIn(0, 255) / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(index, 255, brightness.coerceIn(0, 255))
        }
    }
}

