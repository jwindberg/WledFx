package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import kotlin.math.*
import kotlin.random.Random

/**
 * Aquarium animation - Swimming fish with bubbles
 */
class AquariumAnimation : BaseAnimation() {

    private lateinit var pixelColors: Array<Array<RgbColor>>
    private var startTimeNs: Long = 0L
    
    private data class Fish(
        var x: Double,
        var y: Double,
        var speed: Double,
        var direction: Int, 
        var size: Int,
        var color: RgbColor,
        var tailPhase: Double,
        var verticalOffset: Double = 0.0,
        var verticalSpeed: Double = 0.0
    )
    
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
    
    private val waterColors = listOf(
        RgbColor(0, 50, 100),   // Deep blue
        RgbColor(0, 80, 140),   // Medium blue
        RgbColor(0, 119, 190)   // Light blue
    )

    override fun is1D() = false
    override fun is2D() = true
    override fun getName(): String = "Aquarium"

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        startTimeNs = System.nanoTime()
        initializeFish()
        initializeBubbles()
    }
    
    private fun initializeFish() {
        fish.clear()
        val numFish = min(5, max(2, width / 10))
        
        for (i in 0 until numFish) {
            val fishColor = getRandomFishColor(i)
            fish.add(
                Fish(
                    x = random.nextDouble() * width,
                    y = random.nextDouble() * (height - 2) + 1.0,
                    speed = 0.3 + random.nextDouble() * 0.5,
                    direction = if (random.nextBoolean()) 1 else -1,
                    size = 2 + random.nextInt(2),
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
        val numBubbles = min(8, max(3, width / 5))
        for (i in 0 until numBubbles) {
            bubbles.add(
                Bubble(
                    x = random.nextDouble() * width,
                    y = random.nextDouble() * height,
                    speed = 0.1 + random.nextDouble() * 0.2,
                    size = 1,
                    wobblePhase = random.nextDouble() * 2 * PI
                )
            )
        }
    }
    
    private fun getRandomFishColor(index: Int): RgbColor {
        val palette = paramPalette?.colors
        return if (palette != null && palette.isNotEmpty()) {
            palette[index % palette.size]
        } else {
            when (index % 5) {
                0 -> RgbColor(255, 107, 53)
                1 -> RgbColor(78, 205, 196)
                2 -> RgbColor(240, 147, 251)
                3 -> RgbColor(255, 216, 155)
                else -> RgbColor(168, 237, 234)
            }
        }
    }
    
    override fun setPalette(palette: com.marsraver.wledfx.color.Palette) {
        super.setPalette(palette)
        fish.forEachIndexed { index, f ->
            f.color = getRandomFishColor(index)
        }
    }

    override fun update(now: Long): Boolean {
        val timeMs = (now - startTimeNs) / 1_000_000
        // Scale speed: 128 = 1.0x, range 0.0 to 2.0x approx
        val speedFactor = paramSpeed / 128.0
        val timeSec = (timeMs / 1000.0) * speedFactor
        
        drawWaterBackground(timeSec)
        drawSeaweed(timeSec)
        updateBubbles(timeSec)
        drawBubbles()
        updateFish(timeSec)
        drawFish()
        
        return true
    }
    
    private fun drawWaterBackground(timeSec: Double) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val shimmer = sin(timeSec * 0.5 + x * 0.1 + y * 0.1) * 0.1 + 0.9
                val depth = y.toDouble() / height
                val baseColor = waterColors[0]
                val r = (baseColor.r * shimmer * (1.0 - depth * 0.3)).toInt().coerceIn(0, 255)
                val g = (baseColor.g * shimmer * (1.0 - depth * 0.3)).toInt().coerceIn(0, 255)
                val b = (baseColor.b * shimmer * (1.0 - depth * 0.3)).toInt().coerceIn(0, 255)
                pixelColors[x][y] = RgbColor(r, g, b)
            }
        }
    }
    
    private fun drawSeaweed(timeSec: Double) {
        if (height < 4) return
        val seaweedPositions = listOf(width / 4, width / 2, width * 3 / 4)
        val seaweedColor = RgbColor(45, 80, 22)
        
        for (baseX in seaweedPositions) {
            if (baseX >= width) continue
            val h = min(4, height / 2)
            for (i in 0 until h) {
                val y = height - 1 - i
                val sway = sin(timeSec * 2.0 + i * 0.5) * 1.5
                val x = (baseX + sway).toInt().coerceIn(0, width - 1)
                if (y >= 0 && y < height) {
                    pixelColors[x][y] = seaweedColor
                }
            }
        }
    }
    
    private fun updateBubbles(timeSec: Double) {
        for (bubble in bubbles) {
            bubble.y -= bubble.speed
            bubble.wobblePhase += 0.1
            val wobble = sin(bubble.wobblePhase) * 0.3
            bubble.x += wobble
            if (bubble.x < 0) bubble.x += width
            if (bubble.x >= width) bubble.x -= width
            if (bubble.y < 0) {
                bubble.y = height.toDouble()
                bubble.x = random.nextDouble() * width
                bubble.wobblePhase = random.nextDouble() * 2 * PI
            }
        }
    }
    
    private fun drawBubbles() {
        val bubbleColor = RgbColor(200, 220, 255)
        for (bubble in bubbles) {
            val x = bubble.x.toInt().coerceIn(0, width - 1)
            val y = bubble.y.toInt().coerceIn(0, height - 1)
            val existingColor = pixelColors[x][y]
            pixelColors[x][y] = ColorUtils.blend(existingColor, bubbleColor, 153)
        }
    }
    
    private fun updateFish(timeSec: Double) {
        for (f in fish) {
            f.x += f.speed * f.direction
            f.tailPhase += 0.2
            f.verticalOffset = sin(timeSec * f.verticalSpeed * 2 * PI) * 0.5
            if (f.x < -f.size) {
                f.direction = 1
                f.x = -f.size.toDouble()
            } else if (f.x > width + f.size) {
                f.direction = -1
                f.x = (width + f.size).toDouble()
            }
        }
    }
    
    private fun drawFish() {
        for (f in fish) {
            val centerX = f.x.toInt()
            val centerY = (f.y + f.verticalOffset).toInt()
            for (i in 0 until f.size) {
                val x = centerX + (i * f.direction)
                val y = centerY
                if (x in 0 until width && y in 0 until height) {
                    pixelColors[x][y] = f.color
                }
            }
            val tailX = centerX - (f.size * f.direction)
            val tailWag = (sin(f.tailPhase) * 0.5).toInt()
            val tailY = centerY + tailWag
            if (tailX in 0 until width && tailY in 0 until height) {
                val tailColor = ColorUtils.scaleBrightness(f.color, 0.7)
                pixelColors[tailX][tailY] = tailColor
            }
            val eyeX = centerX + ((f.size - 1) * f.direction)
            val eyeY = centerY
            if (eyeX in 0 until width && eyeY in 0 until height) {
                val eyeColor = ColorUtils.blend(f.color, RgbColor.WHITE, 128)
                pixelColors[eyeX][eyeY] = eyeColor
            }
        }
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun cleanup() {
        fish.clear()
        bubbles.clear()
    }
}
