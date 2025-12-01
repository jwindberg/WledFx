package com.marsraver.wledfx.animation
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.ColorUtils
import com.marsraver.wledfx.color.Palette

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Scrolling text animation rendered with a 5x7 bitmap font.
 */
class ScrollingTextAnimation : LedAnimation {

    private var combinedWidth: Int = 0
    private var combinedHeight: Int = 0
    private lateinit var pixels: Array<Array<RgbColor>>

    private var text: String = DEFAULT_TEXT
    private var scrollOffset: Double = 0.0
    private var totalTextWidth: Int = 0
    private var lastUpdateNanos: Long = 0L
    private var hueOffset: Int = 0
    private var speedFactor: Double = 1.0
    private var currentPalette: Palette? = null

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
        pixels = Array(combinedWidth) { Array(combinedHeight) { RgbColor.BLACK } }
        scrollOffset = combinedWidth.toDouble()
        computeTextMetrics()
        lastUpdateNanos = System.nanoTime()
        hueOffset = 0
        speedFactor = speedFactor.coerceIn(MIN_SPEED_FACTOR, 1.0)
    }

    override fun update(now: Long): Boolean {
        if (!::pixels.isInitialized) return true

        val deltaMs = (now - lastUpdateNanos) / 1_000_000.0
        lastUpdateNanos = now

        clear()

        val scrollSpeed = BASE_SCROLL_SPEED * speedFactor * (combinedWidth / 16.0).coerceAtLeast(1.0)
        scrollOffset -= scrollSpeed * (deltaMs / 16.0)
        if (scrollOffset < -totalTextWidth) {
            scrollOffset = combinedWidth.toDouble()
        }

        val baseY = (combinedHeight - CHAR_HEIGHT) / 2
        val yStart = baseY.coerceAtLeast(0)

        hueOffset = (hueOffset + 1) and 0xFF

        var currentX = scrollOffset
        text.forEachIndexed { index, ch ->
            val pattern = FONT[ch] ?: FONT['?'] ?: return@forEachIndexed
            val hue = (hueOffset + index * 12) and 0xFF
            val rgb = hsvToRgb(hue, 255, 255)
            for (row in pattern.indices) {
                val actualY = yStart + row
                if (actualY !in 0 until combinedHeight) continue
                val rowBits = pattern[row]
                for (col in 0 until CHAR_WIDTH) {
                    val actualX = (currentX + col).roundToInt()
                    if (actualX !in 0 until combinedWidth) continue
                    if ((rowBits shr (CHAR_WIDTH - 1 - col)) and 1 == 1) {
                        pixels[actualX][actualY] = rgb
                    }
                }
            }
            currentX += CHAR_WIDTH + CHAR_SPACING
        }

        return true
    }

    override fun getPixelColor(x: Int, y: Int): RgbColor {
        return if (::pixels.isInitialized && x in 0 until combinedWidth && y in 0 until combinedHeight) {
            pixels[x][y]
        } else {
            RgbColor.BLACK
        }
    }

    override fun getName(): String = "Scrolling Text"

    override fun cleanup() {
        // no resources to release
    }

    override fun supportsTextInput(): Boolean = true

    override fun setText(value: String?) {
        text = (value?.ifBlank { DEFAULT_TEXT } ?: DEFAULT_TEXT)
            .uppercase()
        computeTextMetrics()
    }

    override fun supportsSpeedFactor(): Boolean = true

    override fun setSpeedFactor(value: Double) {
        speedFactor = value.coerceIn(MIN_SPEED_FACTOR, MAX_SPEED_FACTOR)
    }

    private fun clear() {
        for (x in 0 until combinedWidth) {
            for (y in 0 until combinedHeight) {
                pixels[x][y] = RgbColor.BLACK
            }
        }
    }

    private fun computeTextMetrics() {
        val characterCount = text.length
        totalTextWidth = characterCount * (CHAR_WIDTH + CHAR_SPACING)
        if (characterCount > 0) {
            totalTextWidth -= CHAR_SPACING
        }
        scrollOffset = min(scrollOffset, combinedWidth.toDouble())
    }

    private fun hsvToRgb(hue: Int, saturation: Int, value: Int): RgbColor {
        val currentPalette = this.currentPalette?.colors
        if (currentPalette != null && currentPalette.isNotEmpty()) {
            val paletteIndex = ((hue % 256) / 256.0 * currentPalette.size).toInt().coerceIn(0, currentPalette.size - 1)
            val baseColor = currentPalette[paletteIndex]
            val brightnessFactor = value / 255.0
            return ColorUtils.scaleBrightness(baseColor, brightnessFactor)
        } else {
            return ColorUtils.hsvToRgb(hue, saturation, value)
        }
    }

    companion object {
        private const val CHAR_WIDTH = 5
        private const val CHAR_HEIGHT = 7
        private const val CHAR_SPACING = 1
        private const val BASE_SCROLL_SPEED = 0.6
        private const val MIN_SPEED_FACTOR = 0.1
        private const val MAX_SPEED_FACTOR = 1.0
        private const val DEFAULT_TEXT = "HELLO WLED"

        private fun pattern(vararg rows: String): IntArray {
            return rows.map { row ->
                row.fold(0) { acc, c -> (acc shl 1) or if (c == '1') 1 else 0 }
            }.toIntArray()
        }

        internal val FONT: Map<Char, IntArray> = mapOf(
            ' ' to pattern("00000","00000","00000","00000","00000","00000","00000"),
            'A' to pattern("01110","10001","10001","11111","10001","10001","10001"),
            'B' to pattern("11110","10001","11110","10001","10001","10001","11110"),
            'C' to pattern("01110","10001","10000","10000","10000","10001","01110"),
            'D' to pattern("11110","10001","10001","10001","10001","10001","11110"),
            'E' to pattern("11111","10000","11110","10000","10000","10000","11111"),
            'F' to pattern("11111","10000","11110","10000","10000","10000","10000"),
            'G' to pattern("01110","10001","10000","10011","10001","10001","01110"),
            'H' to pattern("10001","10001","11111","10001","10001","10001","10001"),
            'I' to pattern("01110","00100","00100","00100","00100","00100","01110"),
            'J' to pattern("00111","00010","00010","00010","10010","10010","01100"),
            'K' to pattern("10001","10010","11100","11000","10100","10010","10001"),
            'L' to pattern("10000","10000","10000","10000","10000","10000","11111"),
            'M' to pattern("10001","11011","10101","10101","10001","10001","10001"),
            'N' to pattern("10001","11001","10101","10011","10001","10001","10001"),
            'O' to pattern("01110","10001","10001","10001","10001","10001","01110"),
            'P' to pattern("11110","10001","10001","11110","10000","10000","10000"),
            'Q' to pattern("01110","10001","10001","10001","10101","10010","01101"),
            'R' to pattern("11110","10001","10001","11110","10100","10010","10001"),
            'S' to pattern("01110","10001","01000","00100","00010","10001","01110"),
            'T' to pattern("11111","00100","00100","00100","00100","00100","00100"),
            'U' to pattern("10001","10001","10001","10001","10001","10001","01110"),
            'V' to pattern("10001","10001","10001","10001","10001","01010","00100"),
            'W' to pattern("10001","10001","10001","10101","10101","10101","01010"),
            'X' to pattern("10001","10001","01010","00100","01010","10001","10001"),
            'Y' to pattern("10001","10001","01010","00100","00100","00100","00100"),
            'Z' to pattern("11111","00001","00010","00100","01000","10000","11111"),
            '0' to pattern("01110","10001","10011","10101","11001","10001","01110"),
            '1' to pattern("00100","01100","00100","00100","00100","00100","01110"),
            '2' to pattern("01110","10001","00001","00010","00100","01000","11111"),
            '3' to pattern("11110","00001","00001","01110","00001","00001","11110"),
            '4' to pattern("00010","00110","01010","10010","11111","00010","00010"),
            '5' to pattern("11111","10000","11110","00001","00001","10001","01110"),
            '6' to pattern("01110","10000","11110","10001","10001","10001","01110"),
            '7' to pattern("11111","00010","00100","01000","01000","01000","01000"),
            '8' to pattern("01110","10001","10001","01110","10001","10001","01110"),
            '9' to pattern("01110","10001","10001","01111","00001","00010","01100"),
            '!' to pattern("00100","00100","00100","00100","00100","00000","00100"),
            '?' to pattern("01110","10001","00001","00010","00100","00000","00100"),
            ',' to pattern("00000","00000","00000","00000","00000","00110","00100")
        )
    }
}
