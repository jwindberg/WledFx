package com.marsraver.WledFx

import com.marsraver.WledFx.animation.AkemiAnimation
import com.marsraver.WledFx.animation.BlackHoleAnimation
import com.marsraver.WledFx.animation.BlobsAnimation
import com.marsraver.WledFx.animation.BlurzAnimation
import com.marsraver.WledFx.animation.BouncingBallAnimation
import com.marsraver.WledFx.animation.ColorTestAnimation
import com.marsraver.WledFx.animation.ColoredBurstsAnimation
import com.marsraver.WledFx.animation.CrazyBeesAnimation
import com.marsraver.WledFx.animation.DistortionWavesAnimation
import com.marsraver.WledFx.animation.GeqAnimation
import com.marsraver.WledFx.animation.LedAnimation
import com.marsraver.WledFx.animation.ScrollingTextAnimation
import com.marsraver.WledFx.animation.SinDotsAnimation
import com.marsraver.WledFx.animation.SnakeAnimation
import com.marsraver.WledFx.animation.RippleRainbowAnimation
import com.marsraver.WledFx.animation.RainAnimation
import com.marsraver.WledFx.animation.SwirlAnimation
import com.marsraver.WledFx.animation.TartanAnimation
import com.marsraver.WledFx.animation.WavingCellAnimation
import com.marsraver.WledFx.animation.WaverlyAnimation
import com.marsraver.WledFx.animation.SquareSwirlAnimation
import com.marsraver.WledFx.animation.SoapAnimation
import com.marsraver.WledFx.animation.SunRadiationAnimation
import com.marsraver.WledFx.wled.WledArtNetClient
import com.marsraver.WledFx.wled.WledClient
import com.marsraver.WledFx.wled.model.WledConfig
import com.marsraver.WledFx.wled.model.WledInfo
import com.marsraver.WledFx.wled.model.WledState
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
import kotlin.math.roundToInt

class WledSimulatorApp : Application() {

    private data class DeviceConfig(
        val ip: String,
        val name: String,
        val gridX: Int,
        val gridY: Int,
    )

    private data class DeviceConnection(
        val config: DeviceConfig,
        val info: WledInfo,
        val wledConfig: WledConfig,
        val wledState: WledState,
        val artNetClient: WledArtNetClient,
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

    private val combinedWidth = 32
    private val combinedHeight = 32

    override fun start(primaryStage: Stage) {
        primaryStage.title = "WLED 4x4 Grid Simulator"

        val spacing = CELL_SIZE * 1.2
        canvas = Canvas((combinedWidth * spacing + spacing / 2), (combinedHeight * spacing + spacing / 2))
        gc = canvas.graphicsContext2D

        statusLabel = Label("Connecting to WLED devices...")

        animationComboBox = ComboBox<String>().apply {
            items.addAll(
                "Snake",
                "Bouncing Ball",
                "Colored Bursts",
                "Black Hole",
                "Blobs",
                "Blurz",
                "Crazy Bees",
                "Distortion Waves",
                "GEQ",
                "Swirl",
                "Akemi",
                "Waving Cell",
                "Tartan",
                "Sun Radiation",
                "Square Swirl",
                "Soap",
                "Scrolling Text",
                "SinDots",
                "Ripple Rainbow",
                "Rain",
                "Waverly",
                "Color Test",
            )
            value = "Snake"
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

                    val width = info.leds?.matrix?.w ?: 16
                    val height = info.leds?.matrix?.h ?: 16

                    println("Connected! Device: ${info.name}")
                    println("Matrix size: ${width}x$height")
                    println("Total LEDs: ${info.leds?.count}")

                    val dmxConfig = config.iface?.live?.dmx
                    val port = config.iface?.live?.port ?: 6454
                    val universe = dmxConfig?.uni ?: 0
                    val dmxStartAddress = dmxConfig?.addr ?: 0

                    println("DMX Config - Universe: $universe, Port: $port, Start Address: $dmxStartAddress")

                    val artNetClient = WledArtNetClient(info, universe, port, dmxStartAddress)
                    artNetClient.connect()
                    println("Connected to Art-Net on port $port")

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
            "Color Test" -> ColorTestAnimation()
            "Snake" -> SnakeAnimation()
            "Bouncing Ball" -> BouncingBallAnimation()
            "Colored Bursts" -> ColoredBurstsAnimation()
            "Black Hole" -> BlackHoleAnimation()
            "Blobs" -> BlobsAnimation()
            "Blurz" -> BlurzAnimation()
            "Crazy Bees" -> CrazyBeesAnimation()
            "Distortion Waves" -> DistortionWavesAnimation()
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
                        val deviceStartX = device.config.gridX * device.width
                        val deviceStartY = device.config.gridY * device.height

                        for (y in 0 until device.height) {
                            for (x in 0 until device.width) {
                                val globalX = deviceStartX + x
                                val globalY = deviceStartY + y
                                val localLedIndex = x * device.height + (device.height - 1 - y)
                                val rgb = applyBrightness(animation.getPixelColor(globalX, globalY))
                                rgbData[localLedIndex * 3] = rgb[0]
                                rgbData[localLedIndex * 3 + 1] = rgb[1]
                                rgbData[localLedIndex * 3 + 2] = rgb[2]
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
        val lineX = combinedWidth / 2 * spacing + startX
        gc.strokeLine(lineX, 0.0, lineX, canvas.height)
        val lineY = combinedHeight / 2 * spacing + startY
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

        private val DEVICES = arrayOf(
            DeviceConfig("192.168.7.113", "Grid01", 0, 0),
            DeviceConfig("192.168.7.226", "Grid02", 1, 0),
            DeviceConfig("192.168.7.181", "Grid03", 0, 1),
            DeviceConfig("192.168.7.167", "Grid04", 1, 1),
        )

        @JvmStatic
        fun main(args: Array<String>) {
            launch(WledSimulatorApp::class.java, *args)
        }
    }
}

