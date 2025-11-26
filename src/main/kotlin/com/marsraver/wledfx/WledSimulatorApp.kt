package com.marsraver.wledfx

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.marsraver.wledfx.animation.AkemiAnimation
import com.marsraver.wledfx.animation.BlackHoleAnimation
import com.marsraver.wledfx.animation.BlobsAnimation
import com.marsraver.wledfx.animation.BlurzAnimation
import com.marsraver.wledfx.animation.BouncingBallAnimation
import com.marsraver.wledfx.animation.ColoredBurstsAnimation
import com.marsraver.wledfx.animation.CrazyBeesAnimation
import com.marsraver.wledfx.animation.DistortionWavesAnimation
import com.marsraver.wledfx.animation.DnaAnimation
import com.marsraver.wledfx.animation.DnaSpiralAnimation
import com.marsraver.wledfx.animation.DriftAnimation
import com.marsraver.wledfx.animation.DriftRoseAnimation
import com.marsraver.wledfx.animation.FrizzlesAnimation
import com.marsraver.wledfx.animation.GeqAnimation
import com.marsraver.wledfx.animation.LedAnimation
import com.marsraver.wledfx.animation.ScrollingTextAnimation
import com.marsraver.wledfx.animation.SinDotsAnimation
import com.marsraver.wledfx.animation.SnakeAnimation
import com.marsraver.wledfx.animation.RippleRainbowAnimation
import com.marsraver.wledfx.animation.RainAnimation
import com.marsraver.wledfx.animation.SwirlAnimation
import com.marsraver.wledfx.animation.TartanAnimation
import com.marsraver.wledfx.animation.WavingCellAnimation
import com.marsraver.wledfx.animation.WaverlyAnimation
import com.marsraver.wledfx.animation.SquareSwirlAnimation
import com.marsraver.wledfx.animation.SoapAnimation
import com.marsraver.wledfx.animation.SunRadiationAnimation
import com.marsraver.wledfx.wled.WledDdpClient
import com.marsraver.wledfx.wled.WledClient
import com.marsraver.wledfx.wled.model.WledConfig
import com.marsraver.wledfx.wled.model.WledInfo
import com.marsraver.wledfx.wled.model.WledState
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.CheckBox
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.roundToInt

class WledSimulatorApp : Application() {

    private data class DeviceConfig(
        val ip: String,
        val name: String,
        val gridX: Int,
        val gridY: Int,
        val logicalWidth: Int,
        val logicalHeight: Int,
        val offsetX: Int = 0,
        val offsetY: Int = 0,
    )

    private data class DeviceConnection(
        val config: DeviceConfig,
        val info: WledInfo,
        val wledConfig: WledConfig,
        val wledState: WledState,
        val artNetClient: WledDdpClient,
        val width: Int,
        val height: Int,
    )

    private lateinit var canvas: Canvas
    private lateinit var gc: GraphicsContext
    private val devices = mutableListOf<DeviceConnection>()
    private var animationTimer: AnimationTimer? = null
    private var currentAnimation: LedAnimation? = null
    private val running = AtomicBoolean(false)
    private lateinit var startButton: Button
    private lateinit var statusLabel: Label
    private lateinit var animationComboBox: ComboBox<String>
    private lateinit var brightnessSlider: Slider
    private lateinit var brightnessLabel: Label
    private var brightnessScale: Double = 1.0
    private lateinit var textInputLabel: Label
    private lateinit var textInputField: TextField
    private lateinit var textSpeedLabel: Label
    private lateinit var textSpeedSlider: Slider
    private lateinit var randomCheckBox: CheckBox
    private lateinit var randomIntervalField: TextField
    private lateinit var randomIntervalLabel: Label
    private var lastRandomSwitchNs: Long = 0L
    private var debugStartNs: Long = 0L
    private var gridDebugLogged = false

    private val combinedWidth = VIRTUAL_GRID_WIDTH
    private val combinedHeight = VIRTUAL_GRID_HEIGHT

    override fun start(primaryStage: Stage) {
        primaryStage.title = "WLED 4x4 Grid Simulator"
        println("Loaded layout '$LAYOUT_SOURCE' with ${DEVICES.size} panels; virtual grid ${combinedWidth}x$combinedHeight.")

        val spacing = CELL_SIZE * 1.2
        canvas = Canvas((combinedWidth * spacing + spacing / 2), (combinedHeight * spacing + spacing / 2))
        gc = canvas.graphicsContext2D

        statusLabel = Label("Connecting to WLED devices...")

        animationComboBox = ComboBox<String>().apply {
            val animationNames = listOf(
                "Akemi",
                "Black Hole",
                "Blobs",
                "Blurz",
                "Bouncing Ball",
                "Colored Bursts",
                "Crazy Bees",
                "DNA",
                "DNA Spiral",
                "Distortion Waves",
                "Drift",
                "Drift Rose",
                "Frizzles",
                "GEQ",
                "Rain",
                "Ripple Rainbow",
                "Scrolling Text",
                "SinDots",
                "Snake",
                "Soap",
                "Square Swirl",
                "Sun Radiation",
                "Swirl",
                "Tartan",
                "Waverly",
                "Waving Cell",
            ).sorted()
            items.addAll(animationNames)
            value = when {
                animationNames.contains("Snake") -> "Snake"
                else -> animationNames.firstOrNull() ?: ""
            }
            isDisable = true
            setOnAction {
                if (running.get()) {
                    stopAnimation()
                    startSelectedAnimation()
                    startButton.text = "Stop Animation"
                }
            }
            valueProperty().addListener { _, _, newValue ->
                updateAnimationControls(newValue ?: "")
            }
        }

        startButton = Button("Start Animation").apply {
            isDisable = true
            setOnAction {
                if (!running.get()) {
                    startSelectedAnimation()
                    text = "Stop Animation"
                } else {
                    stopAnimation()
                    text = "Start Animation"
                }
            }
        }

        randomCheckBox = CheckBox("Random").apply {
            isDisable = true
            setOnAction {
                val enabled = isSelected
                updateRandomControls(enabled)
                if (enabled) lastRandomSwitchNs = System.nanoTime()
            }
        }
        randomIntervalField = TextField("10").apply {
            prefColumnCount = 3
            isDisable = true
            textProperty().addListener { _, oldValue, newValue ->
                if (newValue.isNullOrEmpty()) {
                    if (text != oldValue) text = oldValue ?: "10"
                    return@addListener
                }
                if (!newValue.matches(Regex("\\d+(?:\\.\\d+)?"))) {
                    if (text != oldValue) text = oldValue ?: "10"
                }
            }
        }
        randomIntervalLabel = Label("sec").apply { isDisable = true }

        brightnessLabel = Label("Brightness: 100%")
        brightnessSlider = Slider(0.0, 100.0, 100.0).apply {
            isShowTickMarks = true
            isShowTickLabels = true
            majorTickUnit = 25.0
            blockIncrement = 5.0
            orientation = Orientation.VERTICAL
            valueProperty().addListener { _, _, newValue ->
                val percent = newValue.toDouble().coerceIn(0.0, 100.0)
                brightnessScale = percent / 100.0
                brightnessLabel.text = "Brightness: ${percent.roundToInt()}%"
            }
        }

        textInputLabel = Label("Scroll Text:")
        textInputField = TextField("HELLO WLED").apply {
            prefColumnCount = 14
            textProperty().addListener { _, _, newValue ->
                val animation = currentAnimation
                if (animation is ScrollingTextAnimation) {
                    animation.setText(newValue)
                }
            }
        }
        textSpeedLabel = Label("Scroll Speed: 100%")
        textSpeedSlider = Slider(10.0, 100.0, 100.0).apply {
            isShowTickMarks = true
            isShowTickLabels = true
            majorTickUnit = 30.0
            blockIncrement = 5.0
            valueProperty().addListener { _, _, newValue ->
                val percent = newValue.toDouble().coerceIn(10.0, 100.0)
                textSpeedLabel.text = "Scroll Speed: ${percent.roundToInt()}%"
                val animation = currentAnimation
                if (animation is ScrollingTextAnimation) {
                    animation.setSpeedFactor(percent / 100.0)
                }
            }
        }
        textInputLabel.isVisible = false
        textInputLabel.isManaged = false
        textInputField.isVisible = false
        textInputField.isManaged = false
        textSpeedLabel.isVisible = false
        textSpeedLabel.isManaged = false
        textSpeedSlider.isVisible = false
        textSpeedSlider.isManaged = false

        val controlsBox = HBox(10.0,
            statusLabel,
            Label("Animation:"),
            animationComboBox,
            startButton,
            randomCheckBox,
            randomIntervalField,
            randomIntervalLabel
        )
        val vbox = VBox(10.0, controlsBox, canvas)
        val root = BorderPane(vbox)
        val brightnessBox = VBox(12.0, brightnessLabel, brightnessSlider, textInputLabel, textInputField, textSpeedLabel, textSpeedSlider).apply {
            padding = Insets(10.0)
        }
        root.right = brightnessBox

        updateAnimationControls(animationComboBox.value ?: "")

        primaryStage.scene = Scene(root, 860.0, 900.0)
        primaryStage.centerOnScreen()
        primaryStage.show()
        primaryStage.toFront()
        primaryStage.requestFocus()
        println("JavaFX window opened - ready to control WLED devices")

        Thread.sleep(500)

        Thread {
            val connectedCount = intArrayOf(0)
            for (deviceConfig in DEVICES) {
                try {
                    println("Connecting to ${deviceConfig.name} at ${deviceConfig.ip}...")
                    val client = WledClient(deviceConfig.ip)
                    val info = client.getInfo()
                    val config = client.getConfig()
                    val state = client.getState()

                    val width = info.leds?.matrix?.w ?: deviceConfig.logicalWidth
                    val height = info.leds?.matrix?.h ?: deviceConfig.logicalHeight

                    println("Connected! Device: ${info.name}")
                    println("Matrix size: ${width}x$height")
                    println("Total LEDs: ${info.leds?.count}")

                    val dmxConfig = config.iface?.live?.dmx
                    val port = config.iface?.live?.port ?: 6454
                    val universe = dmxConfig?.uni ?: 0
                    val dmxStartAddress = dmxConfig?.addr ?: 0

                    println("DMX Config - Universe: $universe, Port: $port, Start Address: $dmxStartAddress")

                    // Using DDP protocol instead of Art-Net to avoid secondary color issues
                    val artNetClient = WledDdpClient(info)
                    artNetClient.connect()
                    println("Connected via DDP on port 4048")

                    val device = DeviceConnection(deviceConfig, info, config, state, artNetClient, width, height)
                    devices.add(device)
                    connectedCount[0]++

                    Platform.runLater {
                        statusLabel.text = "Connected to ${connectedCount[0]}/${DEVICES.size} devices"
                    }
                } catch (ex: Exception) {
                    System.err.println("Failed to connect to ${deviceConfig.name}: ${ex.message}")
                    ex.printStackTrace()
                }
            }

            if (connectedCount[0] == DEVICES.size) {
                // Query and display sync/dmx settings for all panels
                querySyncSettings(DEVICES)
                
                Platform.runLater {
                    statusLabel.text = "All devices connected!"
                    startButton.isDisable = false
                    animationComboBox.isDisable = false
                    randomCheckBox.isDisable = false
                    startSelectedAnimation()
                    startButton.text = "Stop Animation"
                }
                println("Successfully connected to all ${connectedCount[0]} devices")
            } else {
                Platform.runLater {
                    statusLabel.text = "Connected to ${connectedCount[0]}/${DEVICES.size} devices"
                    animationComboBox.isDisable = false
                    randomCheckBox.isDisable = false
                }
                System.err.println("Warning: Only ${connectedCount[0]}/${DEVICES.size} devices connected")
            }
        }.start()

        primaryStage.setOnCloseRequest {
            stopAnimation()
            devices.forEach { connection ->
                sendBlackFrame(connection)
                connection.artNetClient.disconnect()
            }
            println("Disconnected from all WLED devices")
        }
    }

    private fun startSelectedAnimation() {
        if (devices.isEmpty()) {
            System.err.println("No devices connected!")
            return
        }

        val selectedAnimation = animationComboBox.value
        val newAnimation: LedAnimation = when (selectedAnimation) {
            "Snake" -> SnakeAnimation()
            "Bouncing Ball" -> BouncingBallAnimation()
            "Colored Bursts" -> ColoredBurstsAnimation()
            "Black Hole" -> BlackHoleAnimation()
            "Blobs" -> BlobsAnimation()
            "Blurz" -> BlurzAnimation()
            "Crazy Bees" -> CrazyBeesAnimation()
            "Distortion Waves" -> DistortionWavesAnimation()
            "DNA" -> DnaAnimation()
            "DNA Spiral" -> DnaSpiralAnimation()
            "Drift" -> DriftAnimation()
            "Drift Rose" -> DriftRoseAnimation()
            "Frizzles" -> FrizzlesAnimation()
            "GEQ" -> GeqAnimation()
            "Swirl" -> SwirlAnimation()
            "Akemi" -> AkemiAnimation()
            "Waving Cell" -> WavingCellAnimation()
            "Tartan" -> TartanAnimation()
            "Sun Radiation" -> SunRadiationAnimation()
            "Square Swirl" -> SquareSwirlAnimation()
            "Soap" -> SoapAnimation()
            "Scrolling Text" -> ScrollingTextAnimation().apply {
                setText(textInputField.text)
                setSpeedFactor(textSpeedSlider.value / 100.0)
            }
            "SinDots" -> SinDotsAnimation()
            "Ripple Rainbow" -> RippleRainbowAnimation()
            "Rain" -> RainAnimation()
            "Waverly" -> WaverlyAnimation()
            else -> {
                System.err.println("Unknown animation: $selectedAnimation")
                return
            }
        }

        currentAnimation = newAnimation

        currentAnimation?.init(combinedWidth, combinedHeight)
        running.set(true)
        val lastUpdateTime = longArrayOf(System.nanoTime())
        val totalLeds = combinedWidth * combinedHeight

        lastRandomSwitchNs = System.nanoTime()
        debugStartNs = lastRandomSwitchNs
        gridDebugLogged = false

        println("Starting ${currentAnimation?.getName()} animation with $totalLeds LEDs total (${combinedWidth}x$combinedHeight combined grid)")

        animationTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (!running.get()) return

                if (now - lastUpdateTime[0] < FRAME_INTERVAL_NS) return
                lastUpdateTime[0] = now

                val animation = currentAnimation ?: return
                if (!animation.update(now)) {
                    stopAnimation()
                    return
                }

                try {
                    devices.forEach { device ->
                        val deviceLeds = device.width * device.height
                        val rgbData = IntArray(deviceLeds * 3)
                        
                        // All animations use the same standard rendering path
                        // Standard mapping: gridX = column (X), gridY = row (Y)
                        val baseX = device.config.gridX * device.width
                        val baseY = device.config.gridY * device.height

                        for (localY in 0 until device.height) {
                            for (localX in 0 until device.width) {
                                val physicalX = baseX + localX
                                val physicalY = baseY + localY

                                // Direct mapping - match exactly how the simulator displays
                                // Simulator draws: for (globalY in 0..height) for (globalX in 0..width) 
                                //   animation.getPixelColor(globalX, globalY) at pixel (globalX, globalY)
                                // Physical panels should sample the same coordinates
                                val sampleX = (physicalX + device.config.offsetX).coerceIn(0, combinedWidth - 1)
                                val sampleY = (physicalY + device.config.offsetY).coerceIn(0, combinedHeight - 1)

                                val rgb = applyBrightness(animation.getPixelColor(sampleX, sampleY))
                                // Standard row-major mapping: LED 0 at top-left (0,0), goes left-to-right, top-to-bottom
                                // All grids use the same standard mapping
                                val localLedIndex = localY * device.width + localX
                                rgbData[localLedIndex * 3] = rgb[0]
                                rgbData[localLedIndex * 3 + 1] = rgb[1]
                                rgbData[localLedIndex * 3 + 2] = rgb[2]
                            }
                        }

                        if (device.config.name.equals("Grid01", ignoreCase = true) && now - debugStartNs >= 5_000_000_000L && !gridDebugLogged) {
                            gridDebugLogged = true
                            val litIndices = mutableListOf<String>()
                            for (idx in 0 until deviceLeds) {
                                val r = rgbData[idx * 3]
                                val g = rgbData[idx * 3 + 1]
                                val b = rgbData[idx * 3 + 2]
                                if ((r or g or b) != 0) {
                                    // Use standard row-major mapping: LED index = y * width + x
                                    val localX = idx % device.width
                                    val localY = idx / device.width
                                    litIndices += "LED[$idx](localX=$localX,localY=$localY)->RGB($r,$g,$b)"
                                }
                            }
                            println("Grid01 rgbData check: Total LEDs=${deviceLeds}, Non-black LEDs=${litIndices.size}")
                            if (litIndices.isNotEmpty()) {
                                println("  Lit LEDs: ${litIndices.joinToString()}")
                            } else {
                                println("  All LEDs are black (0,0,0)")
                            }
                            // Also show raw bytes for LED 19 and neighbors
                            println("  Raw bytes around LED[19]:")
                            for (idx in 17..21) {
                                val r = rgbData[idx * 3]
                                val g = rgbData[idx * 3 + 1]
                                val b = rgbData[idx * 3 + 2]
                                println("    LED[$idx]: bytes[${idx*3}]=$r, bytes[${idx*3+1}]=$g, bytes[${idx*3+2}]=$b")
                            }
                        }
                        device.artNetClient.sendRgb(rgbData, deviceLeds)
                    }


                    drawCombinedSimulation()
                    handleRandomSelection(now)
                } catch (ex: Exception) {
                    System.err.println("Error sending frame: ${ex.message}")
                    ex.printStackTrace()
                }
            }
        }.also { it.start() }
    }

    private fun stopAnimation() {
        running.set(false)
        animationTimer?.stop()
        when (val animation = currentAnimation) {
            is BlurzAnimation -> animation.cleanup()
            is GeqAnimation -> animation.cleanup()
            is SwirlAnimation -> animation.cleanup()
            is AkemiAnimation -> animation.cleanup()
            is WavingCellAnimation -> animation.cleanup()
            is TartanAnimation -> animation.cleanup()
            is WaverlyAnimation -> animation.cleanup()
            is SunRadiationAnimation -> animation.cleanup()
            is SquareSwirlAnimation -> animation.cleanup()
            is SoapAnimation -> animation.cleanup()
            is SinDotsAnimation -> animation.cleanup()
            is RippleRainbowAnimation -> animation.cleanup()
            is RainAnimation -> animation.cleanup()
            is ScrollingTextAnimation -> animation.cleanup()
        }
    }

    private fun drawCombinedSimulation() {
        gc.fill = Color.BLACK
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)

        val spacing = CELL_SIZE * 1.2
        val radius = CELL_SIZE / 2.0
        val startX = spacing / 2
        val startY = spacing / 2

        val animation = currentAnimation ?: return
        for (globalY in 0 until combinedHeight) {
            for (globalX in 0 until combinedWidth) {
                val rgb = applyBrightness(animation.getPixelColor(globalX, globalY))
                val color = Color.rgb(rgb[0], rgb[1], rgb[2])
                val pixelX = globalX * spacing + startX
                val pixelY = globalY * spacing + startY
                gc.fill = color
                gc.fillOval(pixelX - radius, pixelY - radius, radius * 2, radius * 2)
            }
        }

        gc.stroke = Color.DARKGRAY
        gc.lineWidth = 0.5
        val lineX = startX + (combinedWidth / 2 * spacing) - spacing / 2
        gc.strokeLine(lineX, 0.0, lineX, canvas.height)
        val lineY = startY + (combinedHeight / 2 * spacing) - spacing / 2
        gc.strokeLine(0.0, lineY, canvas.width, lineY)
    }

    private fun applyBrightness(rgb: IntArray): IntArray {
        if (brightnessScale >= 0.999) return rgb
        val scale = brightnessScale.coerceIn(0.0, 1.0)
        val r = (rgb[0] * scale).roundToInt().coerceIn(0, 255)
        val g = (rgb[1] * scale).roundToInt().coerceIn(0, 255)
        val b = (rgb[2] * scale).roundToInt().coerceIn(0, 255)
        return intArrayOf(r, g, b)
    }

    private fun sendBlackFrame(connection: DeviceConnection) {
        val deviceLeds = connection.width * connection.height
        val blackData = IntArray(deviceLeds * 3)
        try {
            connection.artNetClient.sendRgb(blackData, deviceLeds)
        } catch (ex: Exception) {
            System.err.println("Failed to send blackout frame to ${connection.config.name}: ${ex.message}")
        }
    }

    private fun updateAnimationControls(animationName: String) {
        val isScrollingText = animationName == "Scrolling Text"
        textInputLabel.isVisible = isScrollingText
        textInputField.isVisible = isScrollingText
        textInputLabel.isManaged = isScrollingText
        textInputField.isManaged = isScrollingText
        textSpeedLabel.isVisible = isScrollingText
        textSpeedLabel.isManaged = isScrollingText
        textSpeedSlider.isVisible = isScrollingText
        textSpeedSlider.isManaged = isScrollingText
    }

    private fun updateRandomControls(enabled: Boolean) {
        randomIntervalField.isDisable = !enabled
        randomIntervalLabel.isDisable = !enabled
        if (!enabled && !randomIntervalField.text.isNullOrBlank()) {
            val sanitized = randomIntervalSeconds()
            randomIntervalField.text = sanitized.roundToInt().toString()
        }
    }

    private fun randomIntervalSeconds(): Double {
        val value = randomIntervalField.text.toDoubleOrNull()
        val sanitized = when {
            value == null -> 10.0
            value < 1.0 -> 1.0
            value > 3600.0 -> 3600.0
            else -> value
        }
        if (value == null || value != sanitized) {
            randomIntervalField.text = sanitized.roundToInt().toString()
        }
        return sanitized
    }

    private fun handleRandomSelection(now: Long) {
        if (!randomCheckBox.isSelected) return
        val items = animationComboBox.items
        if (items.isEmpty()) return

        val intervalSeconds = randomIntervalSeconds()
        val intervalNs = (intervalSeconds * 1_000_000_000L).toLong()
        if (now - lastRandomSwitchNs < intervalNs) return

        lastRandomSwitchNs = now

        val current = animationComboBox.value
        val candidates = items.filter { it != current }
        val selectionPool = if (candidates.isNotEmpty()) candidates else items
        val newSelection = selectionPool.random()

        Platform.runLater {
            animationComboBox.value = newSelection
            if (running.get()) {
                stopAnimation()
                startSelectedAnimation()
                startButton.text = "Stop Animation"
            }
        }
    }

    companion object {
        private const val CELL_SIZE = 10.0
        private const val FPS = 60
        private const val FRAME_INTERVAL_NS = 1_000_000_000L / FPS
        private const val DEFAULT_LAYOUT_RESOURCE = "layouts/four_grid.json"
        private const val FALLBACK_GRID_WIDTH = 32
        private const val FALLBACK_GRID_HEIGHT = 32

        private val mapper = jacksonObjectMapper()

        private data class LayoutLoadResult(val layout: LayoutConfig, val source: String)

        private val layoutLoadResult = loadLayout()
        private val LAYOUT = layoutLoadResult.layout
        private val LAYOUT_SOURCE = layoutLoadResult.source

        private val VIRTUAL_GRID_WIDTH: Int = computeVirtualWidth(LAYOUT)
        private val VIRTUAL_GRID_HEIGHT: Int = computeVirtualHeight(LAYOUT)

        private val DEVICES: Array<DeviceConfig> = LAYOUT.panels.map { panel ->
            DeviceConfig(
                ip = panel.ip,
                name = panel.name,
                gridX = panel.position.x,
                gridY = panel.position.y,
                logicalWidth = panel.size.width,
                logicalHeight = panel.size.height,
                offsetX = panel.offset?.x ?: 0,
                offsetY = panel.offset?.y ?: 0,
            )
        }.toTypedArray()

        @JvmStatic
        fun main(args: Array<String>) {
            launch(WledSimulatorApp::class.java, *args)
        }
        
        private fun querySyncSettings(devices: Array<DeviceConfig>) {
            println("\n========== Querying Sync/DMX Settings ==========")
            for (deviceConfig in devices) {
                try {
                    println("\n--- ${deviceConfig.name} (${deviceConfig.ip}) ---")
                    val client = WledClient(deviceConfig.ip)
                    val info = client.getInfo()
                    val configJson = client.getConfigJson()
                    
                    // Extract LED configuration from info endpoint
                    val infoLeds = info.leds
                    if (infoLeds != null) {
                        println("LED Info (from /json/info):")
                        println("  count: ${infoLeds.count}")
                        println("  rgbw: ${infoLeds.rgbw}")
                        println("  wv: ${infoLeds.wv}")
                        val matrix = infoLeds.matrix
                        if (matrix != null) {
                            println("\n2D Matrix Info:")
                            println("  w: ${matrix.w}, h: ${matrix.h}")
                        }
                    }
                    
                    // Check config JSON for LED settings
                    val configLeds = configJson["leds"]
                    if (configLeds != null) {
                        println("\nLED Configuration (from /json/cfg):")
                        println("  $configLeds")
                    } else {
                        println("\nLED Configuration: (not found in /json/cfg)")
                        // Try to find color order in other places
                        val hw = configJson["hw"]
                        if (hw != null) {
                            println("  Hardware config: $hw")
                        }
                    }
                    
                    // Extract sync settings
                    val iface = configJson["if"]
                    if (iface != null) {
                        val sync = iface["sync"]
                        val live = iface["live"]
                        
                        println("\nSync Settings:")
                        if (sync != null) {
                            println("  sync: $sync")
                        } else {
                            println("  sync: (null)")
                        }
                        
                        println("\nLive/E131/Art-Net Settings:")
                        if (live != null) {
                            println("  enabled: ${live["en"]}")
                            println("  port: ${live["port"]}")
                            println("  timeout: ${live["timeout"]}")
                            println("  maxbri: ${live["maxbri"]}")
                            println("  offset: ${live["offset"]}")
                            println("  rlm: ${live["rlm"]}")
                            println("  mso: ${live["mso"]}")
                            println("  mc: ${live["mc"]}")
                            
                            val dmx = live["dmx"]
                            if (dmx != null) {
                                println("\nDMX Settings:")
                                println("  uni (universe): ${dmx["uni"]}")
                                println("  addr (DMX start address): ${dmx["addr"]}")
                                println("  mode: ${dmx["mode"]} (4=Multi RGB)")
                                println("  dss: ${dmx["dss"]}")
                                println("  seqskip: ${dmx["seqskip"]}")
                                println("  e131prio: ${dmx["e131prio"]}")
                            } else {
                                println("\nDMX Settings: (null)")
                            }
                        } else {
                            println("  live: (null)")
                        }
                    } else {
                        println("Interface config: (null)")
                    }
                } catch (ex: Exception) {
                    System.err.println("Failed to query ${deviceConfig.name}: ${ex.message}")
                    ex.printStackTrace()
                }
            }
            println("\n========== End Sync/DMX Settings ==========\n")
        }

        private fun computeVirtualWidth(layout: LayoutConfig): Int {
            val explicit = layout.virtualGrid?.width ?: 0
            if (explicit > 0) return explicit
            return layout.panels.maxOfOrNull { panel ->
                (panel.position.x * panel.size.width) + panel.size.width
            } ?: FALLBACK_GRID_WIDTH
        }

        private fun computeVirtualHeight(layout: LayoutConfig): Int {
            val explicit = layout.virtualGrid?.height ?: 0
            if (explicit > 0) return explicit
            return layout.panels.maxOfOrNull { panel ->
                (panel.position.y * panel.size.height) + panel.size.height
            } ?: FALLBACK_GRID_HEIGHT
        }

        private fun loadLayout(): LayoutLoadResult {
            val layoutSettingRaw = System.getProperty("wledfx.layout", DEFAULT_LAYOUT_RESOURCE)
            val (layoutSetting, explicitClasspath) = if (layoutSettingRaw.startsWith("classpath:")) {
                layoutSettingRaw.removePrefix("classpath:") to true
            } else {
                layoutSettingRaw to false
            }
            val normalized = layoutSetting.removePrefix("/")

            if (!explicitClasspath) {
                val path = runCatching { Paths.get(layoutSetting) }.getOrNull()
                if (path != null && Files.exists(path)) {
                    Files.newInputStream(path).use { input ->
                        val layout: LayoutConfig = mapper.readValue(input)
                        return LayoutLoadResult(layout, path.toString())
                    }
                }
            }

            val resourceStream = Thread.currentThread().contextClassLoader?.getResourceAsStream(normalized)
                ?: WledSimulatorApp::class.java.classLoader?.getResourceAsStream(normalized)
            if (resourceStream != null) {
                resourceStream.use { input ->
                    val layout: LayoutConfig = mapper.readValue(input)
                    return LayoutLoadResult(layout, "classpath:$normalized")
                }
            }

            println("[Layout] Unable to find layout at '$layoutSettingRaw'. Falling back to default configuration.")
            return LayoutLoadResult(defaultLayout(), "default")
        }

        private fun defaultLayout(): LayoutConfig {
            val panels = listOf(
                PanelConfig("Grid01", "192.168.7.113", GridPosition(0, 0), GridSize(16, 16), startCorner = "bottom-left", order = "column-major", serpentine = true),
                PanelConfig("Grid02", "192.168.7.226", GridPosition(1, 0), GridSize(16, 16), startCorner = "bottom-left", order = "column-major", serpentine = true),
                PanelConfig("Grid03", "192.168.7.181", GridPosition(0, 1), GridSize(16, 16), startCorner = "bottom-left", order = "column-major", serpentine = true),
                PanelConfig("Grid04", "192.168.7.167", GridPosition(1, 1), GridSize(16, 16), startCorner = "bottom-left", order = "column-major", serpentine = true),
            )
            return LayoutConfig(panels, GridSize(FALLBACK_GRID_WIDTH, FALLBACK_GRID_HEIGHT))
        }

        private data class LayoutConfig(
            val panels: List<PanelConfig>,
            val virtualGrid: GridSize? = null,
        )

        private data class PanelConfig(
            val name: String,
            val ip: String,
            val position: GridPosition,
            val size: GridSize,
            val startCorner: String? = null,
            val order: String? = null,
            val serpentine: Boolean? = null,
            val offset: GridPosition? = null,
        )

        private data class GridPosition(val x: Int, val y: Int)
        private data class GridSize(val width: Int, val height: Int)
    }
}

