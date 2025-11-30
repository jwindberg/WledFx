package com.marsraver.wledfx.animation

import com.marsraver.wledfx.audio.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Waverly animation - mirrored rainbow columns driven by 3D Perlin noise.
 * Audio RMS controls column height, blur strength, and brightness.
 */
class WaverlyAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private var pixelColors: Array<Array<IntArray>> = emptyArray()
    private var timeValue: Double = 0.0

    @Volatile
    private var smoothedLevel: Double = 0.0
    private val audioLock = Any()
    private var audioScope: CoroutineScope? = null

    override fun init(combinedWidth: Int, combinedHeight: Int) {
        this.combinedWidth = combinedWidth
        this.combinedHeight = combinedHeight
        pixelColors = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }
        timeValue = 0.0
        synchronized(audioLock) {
            smoothedLevel = 0.0
        }
        audioScope?.cancel()
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope ->
            scope.launch {
                AudioPipeline.rmsFlow().collectLatest { level ->
                    synchronized(audioLock) {
                        // Amplify incoming audio for better sensitivity to normal speech
                        // Multiply by 3-4x to make normal room sounds detectable
                        val amplified = level.level.toDouble() * 3.5
                        val incoming = amplified.coerceIn(0.0, 255.0)
                        
                        // More responsive: faster attack, faster decay
                        // When audio is present, respond quickly (30% new)
                        // When audio stops, decay quickly (only 70% retention)
                        if (incoming > smoothedLevel) {
                            // Attack: respond quickly to rising audio
                            smoothedLevel = smoothedLevel * 0.70 + incoming * 0.30
                        } else {
                            // Decay: fade out quickly when audio stops
                            smoothedLevel = smoothedLevel * 0.85
                        }
                        // Apply threshold - very low levels become 0 for true black
                        // Lower threshold so normal speech can trigger it
                        if (smoothedLevel < 3.0) {
                            smoothedLevel = 0.0
                        }
                    }
                }
            }
        }
    }

    override fun update(now: Long): Boolean {
        timeValue = now / 1_000_000.0 / 2.0
        clearPixels()

        val level = synchronized(audioLock) { smoothedLevel }.coerceIn(0.0, 255.0)
        
        // When level is 0, should be completely black (no base values)
        if (level <= 0.0) {
            // Everything is already cleared, just return
            return true
        }
        
        // Scale everything from 0 when quiet to full when loud
        val levelFactor = level / 255.0
        // Increase height multiplier so music fills closer to 50% of screen
        // With normal music levels, should reach ~50% height
        val heightMultiplier = levelFactor * 3.5 // Increased from 1.7 to 3.5 for better fill
        // Increase brightness - make it brighter, especially at lower levels
        // Scale from 0 to 255, but boost lower levels for better visibility
        val brightnessBase = (levelFactor * 255.0).coerceIn(0.0, 255.0)
        val blurAmount = (levelFactor * 127.0).roundToInt().coerceIn(0, 127) // 0 to 127 (was 32 to 127)

        for (i in 0 until combinedWidth) {
            val noiseVal = inoise8(i * 45.0, timeValue, timeValue * 0.6)
            val baseHeight = mapRange(noiseVal, 0.0, 255.0, 0.0, combinedHeight.toDouble())
            val thisMax = (baseHeight * heightMultiplier)
                .roundToInt()
                .coerceIn(0, combinedHeight)
            if (thisMax <= 0) continue

            // Increase brightness - ensure it's bright enough to be visible
            // Minimum brightness when audio is present, scale up with level
            val minBrightness = 180 // Minimum brightness when audio detected
            val brightness = (minBrightness + (brightnessBase - minBrightness) * 0.3).roundToInt().coerceIn(180, 255)
            for (j in 0 until thisMax) {
                val paletteIndex = mapRange(j.toDouble(), 0.0, thisMax.toDouble(), 250.0, 0.0)
                val color = colorFromRainbowPalette(paletteIndex, brightness)
                addPixelColor(i, j, color)

                val mirrorX = combinedWidth - 1 - i
                val mirrorY = combinedHeight - 1 - j
                addPixelColor(mirrorX, mirrorY, color)
            }
        }

        blur2d(blurAmount)
        return true
    }

    override fun getPixelColor(x: Int, y: Int): IntArray {
        return if (x in 0 until combinedWidth && y in 0 until combinedHeight) {
            val color = pixelColors[x][y].clone()
            color[0] = color[0].coerceIn(0, 255)
            color[1] = color[1].coerceIn(0, 255)
            color[2] = color[2].coerceIn(0, 255)
            color
        } else {
            intArrayOf(0, 0, 0)
        }
    }

    override fun getName(): String = "Waverly"

    fun cleanup() {
        audioScope?.cancel()
        audioScope = null
        synchronized(audioLock) {
            smoothedLevel = 0.0
        }
    }

    private fun clearPixels() {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = 0
                pixelColors[x][y][1] = 0
                pixelColors[x][y][2] = 0
            }
        }
    }

    private fun addPixelColor(x: Int, y: Int, rgb: IntArray) {
        if (x !in 0 until combinedWidth || y !in 0 until combinedHeight) return
        pixelColors[x][y][0] = (pixelColors[x][y][0] + rgb[0]).coerceAtMost(255)
        pixelColors[x][y][1] = (pixelColors[x][y][1] + rgb[1]).coerceAtMost(255)
        pixelColors[x][y][2] = (pixelColors[x][y][2] + rgb[2]).coerceAtMost(255)
    }

    private fun blur2d(amount: Int) {
        if (amount <= 0) return
        val factor = amount.coerceIn(0, 255)
        val temp = Array(combinedWidth) { Array(combinedHeight) { IntArray(3) } }

        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                for (channel in 0 until 3) {
                    var sum = 0
                    var weight = 0
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until combinedWidth && ny in 0 until combinedHeight) {
                                val w = if (dx == 0 && dy == 0) 4 else 1
                                sum += pixelColors[nx][ny][channel] * w
                                weight += w
                            }
                        }
                    }
                    val average = if (weight == 0) pixelColors[x][y][channel] else sum / weight
                    temp[x][y][channel] =
                        (pixelColors[x][y][channel] * (255 - factor) + average * factor) / 255
                }
            }
        }

        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixelColors[x][y][0] = temp[x][y][0]
                pixelColors[x][y][1] = temp[x][y][1]
                pixelColors[x][y][2] = temp[x][y][2]
            }
        }
    }

    private fun colorFromRainbowPalette(indexValue: Double, brightness: Int): IntArray {
        var index = indexValue % 256.0
        if (index < 0) index += 256.0
        val hue = index / 255.0 * 360.0
        val saturation = 1.0
        val value = brightness / 255.0
        return hsvToRgb(hue, saturation, value)
    }

    private fun hsvToRgb(h: Double, s: Double, v: Double): IntArray {
        if (s <= 0.0) {
            val value = (v * 255).roundToInt().coerceIn(0, 255)
            return intArrayOf(value, value, value)
        }

        val hMod = (h % 360 + 360) % 360
        val c = v * s
        val x = c * (1 - abs((hMod / 60.0) % 2 - 1))
        val m = v - c

        val (r1, g1, b1) = when {
            hMod < 60 -> Triple(c, x, 0.0)
            hMod < 120 -> Triple(x, c, 0.0)
            hMod < 180 -> Triple(0.0, c, x)
            hMod < 240 -> Triple(0.0, x, c)
            hMod < 300 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }

        val r = ((r1 + m) * 255).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255).roundToInt().coerceIn(0, 255)
        return intArrayOf(r, g, b)
    }

    private fun mapRange(value: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        if (abs(inMax - inMin) < 1e-9) return outMin
        val clamped = value.coerceIn(inMin, inMax)
        val ratio = (clamped - inMin) / (inMax - inMin)
        return outMin + ratio * (outMax - outMin)
    }

    private fun inoise8(x: Double, y: Double, z: Double): Double {
        val scaledX = x / 128.0
        val scaledY = y / 128.0
        val scaledZ = z / 128.0
        val noise = perlinNoise(scaledX, scaledY, scaledZ)
        return ((noise + 1.0) * 127.5).coerceIn(0.0, 255.0)
    }

    private fun perlinNoise(x: Double, y: Double, z: Double): Double {
        val xi = floor(x).toInt() and PERM_MASK
        val yi = floor(y).toInt() and PERM_MASK
        val zi = floor(z).toInt() and PERM_MASK

        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)

        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val a = PERM[xi] + yi
        val aa = PERM[a and PERM_MASK] + zi
        val ab = PERM[(a + 1) and PERM_MASK] + zi
        val b = PERM[(xi + 1) and PERM_MASK] + yi
        val ba = PERM[b and PERM_MASK] + zi
        val bb = PERM[(b + 1) and PERM_MASK] + zi

        val x1 = lerp(
            grad(PERM[aa and PERM_MASK], xf, yf, zf),
            grad(PERM[ba and PERM_MASK], xf - 1, yf, zf),
            u
        )
        val x2 = lerp(
            grad(PERM[ab and PERM_MASK], xf, yf - 1, zf),
            grad(PERM[bb and PERM_MASK], xf - 1, yf - 1, zf),
            u
        )
        val y1 = lerp(x1, x2, v)

        val x3 = lerp(
            grad(PERM[(aa + 1) and PERM_MASK], xf, yf, zf - 1),
            grad(PERM[(ba + 1) and PERM_MASK], xf - 1, yf, zf - 1),
            u
        )
        val x4 = lerp(
            grad(PERM[(ab + 1) and PERM_MASK], xf, yf - 1, zf - 1),
            grad(PERM[(bb + 1) and PERM_MASK], xf - 1, yf - 1, zf - 1),
            u
        )
        val y2 = lerp(x3, x4, v)

        return lerp(y1, y2, w)
    }

    private fun fade(t: Double): Double = t.pow(3.0) * (t * (t * 6 - 15) + 10)

    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    private fun grad(hash: Int, x: Double, y: Double, z: Double): Double {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }

    companion object {
        private const val PERM_MASK = 255

        private val PERM_BASE = intArrayOf(
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
        )

        private val PERM = IntArray(512) { PERM_BASE[it and PERM_MASK] }
    }
}

