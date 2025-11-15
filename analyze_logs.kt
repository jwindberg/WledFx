#!/usr/bin/env kotlin

import java.io.File

fun analyzeGrid01Logs() {
    val directTestLog = File("grid01_led19_direct_test.log")
    val artnetLog = File("grid01_artnet.log")
    
    println("=== Grid01 LED19 Test Analysis ===\n")
    
    if (directTestLog.exists()) {
        println("Found rgbData log:")
        directTestLog.readLines().forEach { println("  $it") }
    } else {
        println("rgbData log not found - run 'Direct LED19 Test' animation")
    }
    
    println()
    
    if (artnetLog.exists()) {
        println("Found ArtNet log:")
        artnetLog.readLines().forEach { println("  $it") }
    } else {
        println("ArtNet log not found")
    }
    
    // Analysis
    if (directTestLog.exists()) {
        val lines = directTestLog.readText()
        val nonZeroMatch = Regex("Total non-zero LEDs: (\\d+)").find(lines)
        val led19Match = Regex("rgbData\\[57\\] = (\\d+).*rgbData\\[58\\] = (\\d+).*rgbData\\[59\\] = (\\d+)", RegexOption.DOT_MATCHES_ALL).find(lines)
        
        println("\n=== Analysis ===")
        nonZeroMatch?.let {
            val count = it.groupValues[1].toInt()
            if (count == 1) {
                println("✓ Data is correct: Only 1 LED has non-zero RGB")
            } else {
                println("✗ Issue: $count LEDs have non-zero RGB (should be 1)")
            }
        }
        
        led19Match?.let {
            val r = it.groupValues[1].toInt()
            val g = it.groupValues[2].toInt()
            val b = it.groupValues[3].toInt()
            println("LED 19 RGB: R=$r, G=$g, B=$b")
            if (r == 255 && g == 255 && b == 255) {
                println("✓ LED 19 is correctly set to white")
            } else {
                println("✗ LED 19 RGB is wrong (expected 255,255,255)")
            }
        }
        
        println("\nConclusion:")
        if (lines.contains("Total non-zero LEDs: 1")) {
            println("The data being sent is correct. If hardware shows 2 pixels,")
            println("the issue is in the hardware LED mapping - LED index 19")
            println("doesn't correspond to a single physical LED.")
        }
    }
}

analyzeGrid01Logs()

