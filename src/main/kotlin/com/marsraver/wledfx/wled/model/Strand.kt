package com.marsraver.wledfx.wled.model

import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.wled.WledDdpClient
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Represents an animation with initialization and drawing phases,
 * similar to Processing's setup() and draw() pattern.
 */
interface StrandAnimation {
    /**
     * Called once at the start of the animation to initialize state.
     */
    fun init(strand: Strand)
    
    /**
     * Called repeatedly each frame to update pixels.
     */
    fun draw(strand: Strand)
}

/**
 * Represents a strand of LEDs with a clean API that internally uses
 * a flat array for efficient network transmission.
 *
 * @param length Number of LEDs in the strand
 * @param strandAnimation The animation with init and draw methods
 */
class Strand(
    val length: Int, 
    val ddpClient: WledDdpClient,
    private val strandAnimation: StrandAnimation
) : AutoCloseable {
    private val data: IntArray = IntArray(length * 3)
    private var running = false
    private var executor: ScheduledExecutorService? = null

    init {
        require(length > 0) { "Strand length must be greater than 0, got $length" }
    }

    /**
     * Sets the pixel at the specified position.
     *
     * @param position LED index (0 to length-1)
     * @param pixel The pixel color to set
     * @throws IndexOutOfBoundsException if position is out of range
     */
    fun set(position: Int, pixel: RgbColor) {
        require(position in 0 until length) {
            "Position $position is out of range [0, $length)"
        }
        val index = position * 3
        data[index] = pixel.r
        data[index + 1] = pixel.g
        data[index + 2] = pixel.b
    }

    /**
     * Gets the pixel at the specified position.
     *
     * @param position LED index (0 to length-1)
     * @return The pixel color at that position
     * @throws IndexOutOfBoundsException if position is out of range
     */
    fun get(position: Int): RgbColor {
        require(position in 0 until length) {
            "Position $position is out of range [0, $length)"
        }
        val index = position * 3
        return RgbColor(
            r = data[index],
            g = data[index + 1],
            b = data[index + 2]
        )
    }

    /**
     * Clears all pixels to black (0, 0, 0).
     */
    fun clear() {
        data.fill(0)
    }

    /**
     * Sets all pixels to the specified color.
     *
     * @param pixel The color to fill all pixels with
     */
    fun fill(pixel: RgbColor) {
        for (i in 0 until length) {
            set(i, pixel)
        }
    }

    /**
     * Gets the internal IntArray for direct use with WledDdpClient.
     * This is a zero-copy operation - returns the actual internal array.
     *
     * @return The flat RGB array [R, G, B, R, G, B, ...]
     */
    fun toIntArray(): IntArray = data

    /**
     * Sends the current pixel data to the device.
     * Animation functions should call this after updating pixels.
     */
    fun sendFrame() {
        ddpClient.sendRgb(this)
    }

    fun run() {
        try {
            ddpClient.connect()
            println("DDP client connected on port 4048")

            // Run the animation loop
            running = true
            animate()

        } catch (e: Exception) {
            System.err.println("Error: ${e.message}")
            e.printStackTrace()
        } finally {
            // Cleanup
            stop()
        }
    }

    /**
     * Main animation loop using a timer-based callback approach.
     * Uses ScheduledExecutorService to call draw() at precise intervals,
     * similar to Arduino timer callbacks.
     */
    private fun animate() {
        println("Starting animation...")
        println("Press Ctrl+C to stop")

        // Initialize the animation once
        strandAnimation.init(this)

        // Create a single-threaded scheduled executor for precise timing
        executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "Strand-Animation").apply {
                isDaemon = true
            }
        }

        // Schedule draw() to be called at fixed rate (timer-based callback)
        executor?.scheduleAtFixedRate(
            {
                if (running) {
                    try {
                        // Call the draw function
                        strandAnimation.draw(this@Strand)

                        // Send the frame to the device
                        sendFrame()
                    } catch (e: Exception) {
                        System.err.println("Error in animation frame: ${e.message}")
                        e.printStackTrace()
                        running = false
                    }
                }
            },
            0,  // Initial delay: start immediately
            FRAME_DELAY_MS,  // Period: call every N milliseconds
            TimeUnit.MILLISECONDS
        )

        // Keep the main thread alive while animation is running
        try {
            while (running) {
                Thread.sleep(100)  // Check running status every 100ms
            }
        } catch (e: InterruptedException) {
            println("\nAnimation interrupted by user")
            Thread.currentThread().interrupt()
        }
    }

    fun stop() {
        running = false
        
        // Shutdown the executor (stops timer callbacks)
        executor?.let { exec ->
            exec.shutdown()
            try {
                if (!exec.awaitTermination(1, TimeUnit.SECONDS)) {
                    exec.shutdownNow()  // Force shutdown if needed
                }
            } catch (e: InterruptedException) {
                exec.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        executor = null

        try {
            clear()  // Already black, but being explicit
            ddpClient.sendRgb(this)
            println("Sent blackout frame")
        } catch (e: Exception) {
            System.err.println("Error sending blackout: ${e.message}")
        }

        // Disconnect
        ddpClient.disconnect()
        println("Disconnected from Grid00")
    }

    /**
     * Implements AutoCloseable for use with try-with-resources (use blocks).
     * Calls stop() to ensure proper cleanup.
     */
    override fun close() {
        stop()
    }
}

// Animation settings
private const val FRAME_DELAY_MS = 33L  // ~30 FPS (1000ms / 30 = 33.33ms)