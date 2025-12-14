package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette
import kotlin.math.*
import kotlin.random.Random

/**
 * Aquarium animation - Swimming fish with bubbles
 * Creates a peaceful underwater scene with colorful fish swimming across the LED matrix
 * and bubbles rising from the bottom
 */
class AquariumAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var currentPalette: Palette? = null
    private var startTimeNs: Long = 0L
    
    // Fish data class
    private data class Fish(
        var x: Double,
        var y: Double,
        var speed: Double,
        var direction: Int, // 1 for right, -1 for left
        var size: Int,
        var color: RgbColor,
        var tailPhase: Double,
        var verticalOffset: Double = 0.0,
        var verticalSpeed: Double = 0.0
    )
    
    // Bubble data class
    private data class Bubble(
        var x: Double,
        var y: Double,
        var speed: Double,
        var size: Int,
        var wobblePhase: Double
    )
    
    private val fish = mutableListOf<Fish>()
    private val bubbles = mutableListOf<Bubble>()
    private val random = Random(System.currentTimeMillis())
    
    // Water colors for background effect
    private val waterColors = listOf(
        RgbColor(0, 50, 100),   // Deep blue
        RgbColor(0, 80, 140),   // Medium blue
        RgbColor(0, 119, 190)   // Light blue
    )

    override fun supportsPalette(): Boolean = true

    override fun setPalette(palette: Palette) {
        this.currentPalette = palette
        // Update fish colors when palette changes
        updateFishColors()
    }

    override fun getPalette(): Palette? {
        return currentPalette
    }

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        
        // Initialize fish
        initializeFish()
        
        // Initialize bubbles
        initializeBubbles()
    }
    
    private fun initializeFish() {
        fish.clear()
        val numFish = min(5, max(2, combinedWidth / 10)) // Scale with width
        
        for (i in 0 until numFish) {
            val fishColor = getRandomFishColor(i)
            fish.add(
                Fish(
                    x = random.nextDouble() * combinedWidth,
                    y = random.nextDouble() * (combinedHeight - 2) + 1.0, // Keep away from edges
                    speed = 0.3 + random.nextDouble() * 0.5,
                    direction = if (random.nextBoolean()) 1 else -1,
                    size = 2 + random.nextInt(2), // Size 2-3
                    color = fishColor,
                    tailPhase = random.nextDouble() * 2 * PI,
                    verticalOffset = 0.0,
                    verticalSpeed = 0.02 + random.nextDouble() * 0.03
                )
            )
        }
    }
    
    private fun initializeBubbles() {
        bubbles.clear()
        val numBubbles = min(8, max(3, combinedWidth / 5))
        
        for (i in 0 until numBubbles) {
            bubbles.add(
                Bubble(
                    x = random.nextDouble() * combinedWidth,
                    y = random.nextDouble() * combinedHeight,
                    speed = 0.1 + random.nextDouble() * 0.2,
                    size = 1,
                    wobblePhase = random.nextDouble() * 2 * PI
                )
            )
        }
    }
    
    private fun getRandomFishColor(index: Int): RgbColor {
        val palette = currentPalette?.colors
        return if (palette != null && palette.isNotEmpty()) {
            palette[index % palette.size]
        } else {
            // Default fish colors
            when (index % 5) {
                0 -> RgbColor(255, 107, 53)   // Orange
                1 -> RgbColor(78, 205, 196)   // Cyan
                2 -> RgbColor(240, 147, 251)  // Pink
                3 -> RgbColor(255, 216, 155)  // Yellow
                else -> RgbColor(168, 237, 234) // Light cyan
            }
        }
    }
    
    private fun updateFishColors() {
        fish.forEachIndexed { index, f ->
            f.color = getRandomFishColor(index)
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        val timeSec = timeMs / 1000.0
        
        // Clear with water effect
        drawWaterBackground(timeSec)
        
        // Draw seaweed at bottom
        drawSeaweed(timeSec)
        
        // Update and draw bubbles
        updateBubbles(timeSec)
        drawBubbles()
        
        // Update and draw fish
        updateFish(timeSec)
        drawFish()
        
        return true
    }
    
    private fun drawWaterBackground(timeSec: Double) {
        // Create subtle water shimmer effect
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                val shimmer = sin(timeSec * 0.5 + x * 0.1 + y * 0.1) * 0.1 + 0.9
                val depth = y.toDouble() / combinedHeight
                val baseColor = waterColors[0]
                val r = (baseColor.r * shimmer * (1.0 - depth * 0.3)).toInt().coerceIn(0, 255)
                val g = (baseColor.g * shimmer * (1.0 - depth * 0.3)).toInt().coerceIn(0, 255)
                val b = (baseColor.b * shimmer * (1.0 - depth * 0.3)).toInt().coerceIn(0, 255)
                pixelColors[x][y] = RgbColor(r, g, b)
            }
        }
    }
    
    private fun drawSeaweed(timeSec: Double) {
        if (combinedHeight < 4) return
        
        val seaweedPositions = listOf(
            combinedWidth / 4,
            combinedWidth / 2,
            combinedWidth * 3 / 4
        )
        
        val seaweedColor = RgbColor(45, 80, 22) // Dark green
        
        for (baseX in seaweedPositions) {
            if (baseX >= combinedWidth) continue
            
            val height = min(4, combinedHeight / 2)
            for (i in 0 until height) {
                val y = combinedHeight - 1 - i
                val sway = sin(timeSec * 2.0 + i * 0.5) * 1.5
                val x = (baseX + sway).toInt().coerceIn(0, combinedWidth - 1)
                
                if (y >= 0 && y < combinedHeight) {
                    pixelColors[x][y] = seaweedColor
                }
            }
        }
    }
    
    private fun updateBubbles(timeSec: Double) {
        for (bubble in bubbles) {
            // Move bubble up
            bubble.y -= bubble.speed
            
            // Add horizontal wobble
            bubble.wobblePhase += 0.1
            val wobble = sin(bubble.wobblePhase) * 0.3
            bubble.x += wobble
            
            // Wrap around horizontally
            if (bubble.x < 0) bubble.x += combinedWidth
            if (bubble.x >= combinedWidth) bubble.x -= combinedWidth
            
            // Reset bubble when it reaches the top
            if (bubble.y < 0) {
                bubble.y = combinedHeight.toDouble()
                bubble.x = random.nextDouble() * combinedWidth
                bubble.wobblePhase = random.nextDouble() * 2 * PI
            }
        }
    }
    
    private fun drawBubbles() {
        val bubbleColor = RgbColor(200, 220, 255) // Light blue-white
        
        for (bubble in bubbles) {
            val x = bubble.x.toInt().coerceIn(0, combinedWidth - 1)
            val y = bubble.y.toInt().coerceIn(0, combinedHeight - 1)
            
            // Draw bubble with slight transparency effect
            val existingColor = pixelColors[x][y]
            pixelColors[x][y] = ColorUtils.blend(existingColor, bubbleColor, 153)
        }
    }
    
    private fun updateFish(timeSec: Double) {
        for (f in fish) {
            // Move fish horizontally
            f.x += f.speed * f.direction
            
            // Update tail animation
            f.tailPhase += 0.2
            
            // Add subtle vertical movement (bobbing)
            f.verticalOffset = sin(timeSec * f.verticalSpeed * 2 * PI) * 0.5
            
            // Turn around at edges
            if (f.x < -f.size) {
                f.direction = 1
                f.x = -f.size.toDouble()
            } else if (f.x > combinedWidth + f.size) {
                f.direction = -1
                f.x = (combinedWidth + f.size).toDouble()
            }
        }
    }
    
    private fun drawFish() {
        for (f in fish) {
            val centerX = f.x.toInt()
            val centerY = (f.y + f.verticalOffset).toInt()
            
            // Draw fish body (3 pixels for larger fish, 2 for smaller)
            for (i in 0 until f.size) {
                val x = centerX + (i * f.direction)
                val y = centerY
                
                if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
                    // Main body color
                    pixelColors[x][y] = f.color
                }
            }
            
            // Draw tail (behind the fish)
            val tailX = centerX - (f.size * f.direction)
            val tailWag = (sin(f.tailPhase) * 0.5).toInt()
            val tailY = centerY + tailWag
            
            if (tailX in 0 until combinedWidth && tailY in 0 until combinedHeight) {
                // Tail is slightly dimmer
                val tailColor = ColorUtils.scaleBrightness(f.color, 0.7)
                pixelColors[tailX][tailY] = tailColor
            }
            
            // Draw eye (front of fish)
            val eyeX = centerX + ((f.size - 1) * f.direction)
            val eyeY = centerY
            
            if (eyeX in 0 until combinedWidth && eyeY in 0 until combinedHeight) {
                // Add a bright spot for the eye
                val eyeColor = ColorUtils.blend(f.color, RgbColor.WHITE, 128)
                pixelColors[eyeX][eyeY] = eyeColor
            }
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Aquarium"

    override fun isAudioReactive(): Boolean = false

    override fun cleanup() {
        fish.clear()
        bubbles.clear()
    }
}
