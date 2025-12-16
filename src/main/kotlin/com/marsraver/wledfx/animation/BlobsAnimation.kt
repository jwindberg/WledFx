package com.marsraver.wledfx.animation

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import java.util.Random
import kotlin.math.*

/**
 * Floating Blobs animation
 */
class BlobsAnimation : BaseAnimation() {
    
    // blobs
    private class Blob {
        var x: Float = 0f
        var y: Float = 0f
        var speedX: Float = 0f
        var speedY: Float = 0f
        var radius: Float = 1f
        var grow: Boolean = false
        var color: Int = 0
    }
    
    private lateinit var pixelColors: Array<Array<RgbColor>>
    private lateinit var blobs: Array<Blob>
    private var amount: Int = 0
    private val random = kotlin.random.Random.Default
    
    private var lastColorChange: Long = 0
    private val MAX_BLOBS = 8
    
    override fun getName(): String = "Blobs"
    override fun is1D(): Boolean = false
    override fun is2D(): Boolean = true

    override fun onInit() {
        pixelColors = Array(width) { Array(height) { RgbColor.BLACK } }
        val startTime = System.currentTimeMillis()
        lastColorChange = startTime

        amount = min(MAX_BLOBS, (paramIntensity shr 5) + 1)
        blobs = Array(MAX_BLOBS) { Blob() }

        for (i in 0 until MAX_BLOBS) {
            val blob = blobs[i]
            val maxRadius = max(2, width / 4)
            blob.radius = random.nextFloat() * (maxRadius - 1) + 1

            val speedDiv = max(1, 256 - paramSpeed).toFloat()
            blob.speedX = (random.nextInt(width.coerceAtLeast(4) - 3) + 3) / speedDiv
            blob.speedY = (random.nextInt(height.coerceAtLeast(4) - 3) + 3) / speedDiv

            blob.x = random.nextInt(width).toFloat()
            blob.y = random.nextInt(height).toFloat()
            blob.color = random.nextInt(256)
            blob.grow = blob.radius < 1.0f

            if (blob.speedX == 0f) blob.speedX = 1f
            if (blob.speedY == 0f) blob.speedY = 1f
        }
    }

    override fun update(now: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val custom2 = 32 // default
        val fadeAmount = (custom2 shr 3) + 1
        fadeToBlack(fadeAmount)

        if (currentTime - lastColorChange >= 2000) {
            for (i in 0 until amount) blobs[i].color = (blobs[i].color + 4) % 256
            lastColorChange = currentTime
        }

        for (i in 0 until amount) {
            val blob = blobs[i]
            val speedFactor = max(abs(blob.speedX), abs(blob.speedY))

            if (blob.grow) {
                blob.radius += speedFactor * 0.05f
                val maxRadius = min(width / 4.0f, 2.0f)
                if (blob.radius >= maxRadius) blob.grow = false
            } else {
                blob.radius -= speedFactor * 0.05f
                if (blob.radius < 1.0f) blob.grow = true
            }

            val color = getColorFromPalette(blob.color)
            val xPos = blob.x.roundToInt()
            val yPos = blob.y.roundToInt()
            
            if (blob.radius > 1.0f) {
                fillCircle(xPos, yPos, blob.radius.roundToInt(), color)
            } else {
                setPixelColor(xPos, yPos, color)
            }
            
            // Movement logic
            blob.x = when {
                blob.x + blob.radius >= width - 1 -> blob.x + blob.speedX * ((width - 1 - blob.x) / blob.radius + 0.005f)
                blob.x - blob.radius <= 0 -> blob.x + blob.speedX * (blob.x / blob.radius + 0.005f)
                else -> blob.x + blob.speedX
            }

            blob.y = when {
                blob.y + blob.radius >= height - 1 -> blob.y + blob.speedY * ((height - 1 - blob.y) / blob.radius + 0.005f)
                blob.y - blob.radius <= 0 -> blob.y + blob.speedY * (blob.y / blob.radius + 0.005f)
                else -> blob.y + blob.speedY
            }

            if (blob.x < 0.01f || blob.x > width - 1.01f) {
                 val speedDiv = max(1, 256 - paramSpeed).toFloat()
                 val dir = if (blob.x < 0.01f) 1 else -1
                 blob.speedX = dir * ((random.nextInt(width.coerceAtLeast(4) - 3) + 3) / speedDiv)
                 blob.x = if (dir == 1) 0.01f else width - 1.01f
            }
            if (blob.y < 0.01f || blob.y > height - 1.01f) {
                 val speedDiv = max(1, 256 - paramSpeed).toFloat()
                 val dir = if (blob.y < 0.01f) 1 else -1
                 blob.speedY = dir * ((random.nextInt(height.coerceAtLeast(4) - 3) + 3) / speedDiv)
                 blob.y = if (dir == 1) 0.01f else height - 1.01f
            }
        }
        
        // Blur
        val custom1 = 32
        val blurAmount = custom1 shr 2
        if (blurAmount > 0) applyBlur(blurAmount)

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y]
        } else {
            RgbColor.BLACK
        }
    }
    
    private fun fadeToBlack(amount: Int) {
        val factor = (255 - amount).coerceIn(0, 255) / 255.0
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixelColors[x][y] = ColorUtils.scaleBrightness(pixelColors[x][y], factor)
            }
        }
    }
    
    private fun setPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            pixelColors[x][y] = rgb
        }
    }
    
    private fun fillCircle(centerX: Int, centerY: Int, radius: Int, rgb: RgbColor) {
        if (radius <= 0) {
            setPixelColor(centerX, centerY, rgb)
            return
        }
        val minX = max(0, centerX - radius)
        val maxX = min(width - 1, centerX + radius)
        val minY = max(0, centerY - radius)
        val maxY = min(height - 1, centerY + radius)
        val radiusSq = radius * radius

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val dx = x - centerX
                val dy = y - centerY
                if (dx*dx + dy*dy <= radiusSq) {
                    addPixelColor(x, y, rgb)
                }
            }
        }
    }
    
    private fun addPixelColor(x: Int, y: Int, rgb: RgbColor) {
        if (x in 0 until width && y in 0 until height) {
            val current = pixelColors[x][y]
            pixelColors[x][y] = RgbColor(
                (current.r + rgb.r).coerceAtMost(255),
                (current.g + rgb.g).coerceAtMost(255),
                (current.b + rgb.b).coerceAtMost(255)
            )
        }
    }
    
    private fun applyBlur(amount: Int) {
        // Simple box blur implementation similar to previous
        // reuse centralized blur if possible, but Blobs has specific logic
        // For speed, just implement 3x3
        val temp = Array(width) { Array(height) { RgbColor.BLACK } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0; var sumG = 0; var sumB = 0; var count = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            val c = pixelColors[nx][ny]
                            sumR += c.r; sumG += c.g; sumB += c.b
                            count++
                        }
                    }
                }
                val current = pixelColors[x][y]
                val br = if (count > 0) sumR/count else current.r
                val bg = if (count > 0) sumG/count else current.g
                val bb = if (count > 0) sumB/count else current.b
                
                temp[x][y] = RgbColor(
                    (current.r * (255 - amount) + br * amount) / 255,
                    (current.g * (255 - amount) + bg * amount) / 255,
                    (current.b * (255 - amount) + bb * amount) / 255
                )
            }
        }
        for (x in 0 until width) for (y in 0 until height) pixelColors[x][y] = temp[x][y]
    }
}
