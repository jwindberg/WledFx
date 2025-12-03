package com.marsraver.wledfx.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Simple class to learn how audio works.
 * Prints the current volume every second for 10 seconds.
 */
class VolumePrinter {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val printer = VolumePrinter()
            printer.printVolumeForTenSeconds()
        }
    }

    fun printVolumeForTenSeconds() {
        println("Starting audio volume monitoring...")
        println("Will print volume every second for 10 seconds")
        println("Make some noise to see the volume change!\n")

        val meter = LoudnessMeter()

        // Print volume every second for 10 seconds
        runBlocking {
            for (second in 1..10) {
                delay(1000) // Wait 1 second

                val loudness = meter.getLoudness()
                println("Second $second: Loudness = $loudness/1024")
            }
        }

        // Cleanup
        meter.stop()
        println("\nDone! Audio monitoring stopped.")
    }
}

