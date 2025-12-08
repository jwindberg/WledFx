package com.marsraver.wledfx

import com.marsraver.wledfx.strandanimation.CandyCaneAnimation
import com.marsraver.wledfx.strandanimation.ChristmasStrandAnimation
import com.marsraver.wledfx.strandanimation.ColorWheelAnimation
import com.marsraver.wledfx.strandanimation.CylonAnimation
import com.marsraver.wledfx.strandanimation.FftAnimation
import com.marsraver.wledfx.strandanimation.PaletteStrandAnimation
import com.marsraver.wledfx.wled.WledClient
import com.marsraver.wledfx.wled.WledDdpClient
import com.marsraver.wledfx.wled.model.Strand

/**
 * Simple demo showing how to control a WLED device using WledDdpClient.
 *
 * This demo connects to Grid00 and animates a red dot moving back and forth
 * along the LED strip (treating the 16x16 grid as a linear 256-LED strip).
 *
 * Usage: Run the main() function
 */
class StrandAnimationPlayer {
    companion object {

//        private const val IP_ADDRESS = "192.168.7.170"
//        private const val NUM_LEDS =  512

        private const val IP_ADDRESS = "192.168.7.231"
//        private const val NUM_LEDS =  256

        @JvmStatic
        fun main(args: Array<String>) {
            var strand: Strand? = null
            try {
                println("Connecting to Grid00 at $IP_ADDRESS...")

                // Step 1: Create WledClient to get device info
                val client = WledClient(IP_ADDRESS)
                val info = client.getInfo()

                val numLeds: Int = info.leds?.count ?: 0;

                println("Connected! Device: ${info.name}")
                println("Total LEDs: ${info.leds?.count ?: numLeds}")

                // Step 2: Create WledDdpClient with the device info
                val ddpClient = WledDdpClient(info)

                // Step 3: Create the Strand with the animation
                strand = Strand(numLeds, ddpClient, CandyCaneAnimation())
                
                // Add shutdown hook to ensure cleanup on Ctrl+C
                val shutdownHook = Thread {
                    println("\nShutdown signal received, cleaning up...")
                    strand?.stop()
                }
                Runtime.getRuntime().addShutdownHook(shutdownHook)

                // Step 4: start the strand
                strand.use {
                    it.run()
                }
            } catch (e: Exception) {
                System.err.println("Error: ${e.message}")
                e.printStackTrace()
            } finally {
                // Ensure cleanup happens
                strand?.stop()
            }
        }
    }
}

