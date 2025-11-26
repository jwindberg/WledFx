package com.marsraver.wledfx.wled

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.marsraver.wledfx.wled.model.WledInfo
import com.marsraver.wledfx.wled.model.WledState
import java.nio.file.Paths

/**
 * Utility that queries each configured WLED panel and compares its reported configuration
 * to the expected layout. Run it from the command line to print a diagnostic report.
 *
 * Usage:
 *   kotlin -classpath build/classes/... com.marsraver.wledfx.wled.WledPanelConfigVerifier [layoutPath]
 * or configure a Gradle run target.
 */
object WledPanelConfigVerifier {

    private const val DEFAULT_LAYOUT_PATH = "src/main/resources/layouts/four_grid.json"
    private val mapper = jacksonObjectMapper()

    @JvmStatic
    fun main(args: Array<String>) {
        val layoutPath = args.firstOrNull()
            ?: System.getProperty("wledfx.layout", DEFAULT_LAYOUT_PATH)

        val layout = loadLayout(layoutPath)
        val virtualWidth = layout.virtualGrid?.width ?: 0
        val virtualHeight = layout.virtualGrid?.height ?: 0

        println("Loaded layout from $layoutPath")
        println("Virtual grid: ${if (virtualWidth > 0 && virtualHeight > 0) "${virtualWidth}x$virtualHeight" else "unspecified"}")
        println("Discovered ${layout.panels.size} panels\n")

        layout.panels.forEachIndexed { index, panel ->
            println("---- Panel ${index + 1}: ${panel.name} (${panel.ip}) ----")
            println("Expected position: (${panel.position.x}, ${panel.position.y})")
            println("Expected size: ${panel.size.width}x${panel.size.height}")
            println(
                "Expected orientation: startCorner=${panel.startCorner ?: "default"}, " +
                    "order=${panel.order ?: "default"}, reverseX=${panel.reverseX ?: false}, " +
                    "reverseY=${panel.reverseY ?: false}, serpentine=${panel.serpentine ?: false}",
            )

            val issues = mutableListOf<String>()
            try {
                val client = WledClient(panel.ip)

                val info = client.getInfo()
                val state = client.getState()
                val cfg = client.getConfigJson()

                validateLedInfo(panel, info, issues)
                validateLedConfig(panel, cfg, issues)
                validateSegments(panel, state, issues)
            } catch (ex: Exception) {
                issues += "Failed to query device: ${ex.message ?: ex.javaClass.simpleName}"
            }

            if (issues.isEmpty()) {
                println("No configuration mismatches detected for ${panel.name}.")
            } else {
                println("Detected issues for ${panel.name}:")
                issues.forEach { println("  - $it") }
            }
            println()
        }
    }

    private fun loadLayout(path: String): Layout {
        val file = Paths.get(path).toFile()
        if (!file.exists()) {
            throw IllegalArgumentException("Layout file not found: $path")
        }
        return mapper.readValue(file)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Layout(
        val panels: List<Panel>,
        val virtualGrid: GridSize? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Panel(
        val name: String,
        val ip: String,
        val position: GridPosition,
        val size: GridSize,
        val startCorner: String? = null,
        val order: String? = null,
        val reverseX: Boolean? = null,
        val reverseY: Boolean? = null,
        val serpentine: Boolean? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GridPosition(val x: Int, val y: Int)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GridSize(val width: Int, val height: Int)

    private fun validateLedInfo(panel: Panel, info: WledInfo, issues: MutableList<String>) {
        val expectedCount = panel.size.width * panel.size.height
        val leds = info.leds
        if (leds == null) {
            issues += "WLED info.leds is missing."
            return
        }

        if (leds.count != expectedCount) {
            issues += "Device reports ${leds.count} LEDs but expected $expectedCount."
        }

        val matrix = leds.matrix
        if (matrix == null) {
            issues += "WLED info.leds.matrix missing (device may not be in 2D mode)."
        } else {
            if (matrix.w != panel.size.width || matrix.h != panel.size.height) {
                issues += "Matrix dimensions ${matrix.w}x${matrix.h} do not match expected ${panel.size.width}x${panel.size.height}."
            }
        }
    }

    private fun validateLedConfig(panel: Panel, cfg: JsonNode, issues: MutableList<String>) {
        val ledNode = cfg.path("led")
        if (ledNode.isMissingNode || ledNode.isNull) {
            issues += "cfg.led missing from device."
            return
        }

        val expectedCount = panel.size.width * panel.size.height
        val count = ledNode.path("count").asInt(-1)
        if (count != expectedCount) {
            issues += "cfg.led.count=$count but expected $expectedCount."
        }

        val arrayNode = ledNode.path("array")
        if (!arrayNode.isArray || arrayNode.size() == 0) {
            issues += "cfg.led.array missing or empty."
            return
        }

        var activeEntry: JsonNode? = null
        val iterator = arrayNode.elements()
        while (iterator.hasNext()) {
            val node = iterator.next()
            if (node.path("len").asInt(0) > 0) {
                activeEntry = node
                break
            }
        }

        if (activeEntry == null) {
            issues += "cfg.led.array has no entry with len > 0."
            return
        }

        val expectedOffsetX = panel.position.x * panel.size.width
        val expectedOffsetY = panel.position.y * panel.size.height

        val len = activeEntry.path("len").asInt(-1)
        val start = activeEntry.path("start").asInt(0)
        val offsetX = activeEntry.path("offsetX").asInt(0)
        val offsetY = activeEntry.path("offsetY").asInt(0)

        if (len != expectedCount) {
            issues += "cfg.led.array primary entry len=$len expected $expectedCount."
        }
        if (start != 0) {
            issues += "cfg.led.array primary entry start=$start expected 0."
        }
        if (offsetX != expectedOffsetX) {
            issues += "cfg.led.array primary entry offsetX=$offsetX expected $expectedOffsetX."
        }
        if (offsetY != expectedOffsetY) {
            issues += "cfg.led.array primary entry offsetY=$offsetY expected $expectedOffsetY."
        }
    }

    private fun validateSegments(panel: Panel, state: WledState, issues: MutableList<String>) {
        println("Segments reported (${state.seg.size} total):")
        if (state.seg.isEmpty()) {
            println("  <none>")
            issues += "No segments defined on device."
            return
        }

        state.seg.sortedBy { it.id }.forEach { seg ->
            println(
                "  id=${seg.id} start=${seg.start} stop=${seg.stop} startY=${seg.startY} stopY=${seg.stopY} " +
                    "len=${seg.len} revX=${seg.rev} revY=${seg.rY} mirrorY=${seg.mY}",
            )
        }

        val expectedCount = panel.size.width * panel.size.height
        val expectedOffsetY = panel.position.y * panel.size.height
        val expectedStopY = expectedOffsetY + panel.size.height

        val segmentsCovering = state.seg.filter {
            it.start == 0 &&
                it.stop == expectedCount &&
                it.startY == expectedOffsetY &&
                it.stopY == expectedStopY
        }

        if (segmentsCovering.isEmpty()) {
            issues += "No segment spans start=0, stop=$expectedCount, startY=$expectedOffsetY, stopY=$expectedStopY."
            return
        }

        val expectedReverseX = panel.reverseX ?: false
        val expectedReverseY = panel.reverseY ?: false
        segmentsCovering.forEach { seg ->
            if (seg.rev != expectedReverseX) {
                issues += "Segment ${seg.id} revX=${seg.rev} but expected $expectedReverseX."
            }
            if (seg.rY != expectedReverseY) {
                issues += "Segment ${seg.id} revY=${seg.rY} but expected $expectedReverseY."
            }
        }
    }
}

