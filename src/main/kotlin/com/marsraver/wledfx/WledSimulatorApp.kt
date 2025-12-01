package com.marsraver.wledfx

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.marsraver.wledfx.animation.LedAnimation
import com.marsraver.wledfx.AnimationRegistry
import com.marsraver.wledfx.color.RgbColor
import com.marsraver.wledfx.color.Palettes
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
import javafx.scene.control.ColorPicker
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
    private val animationColorStates = mutableMapOf<String, com.marsraver.wledfx.color.RgbColor>()  // Store color state per animation
    private val animationPaletteStates = mutableMapOf<String, String>()  // Store palette state per animation
    private lateinit var startButton: Button
    private lateinit var statusLabel: Label
    private lateinit var retryButton: Button
    private lateinit var animationComboBox: ComboBox<String>
    private val failedDevices = mutableListOf<DeviceConfig>()
    private lateinit var brightnessSlider: Slider
    private lateinit var brightnessLabel: Label
    private var brightnessScale: Double = 1.0
    private lateinit var speedSlider: Slider
    private lateinit var speedLabel: Label
    private lateinit var textInputLabel: Label
    private lateinit var textInputField: TextField
    private lateinit var textSpeedLabel: Label
    private lateinit var textSpeedSlider: Slider
    private lateinit var randomCheckBox: CheckBox
    private lateinit var randomIntervalField: TextField
    private lateinit var randomIntervalLabel: Label
    private lateinit var colorPicker: ColorPicker
    private lateinit var colorLabel: Label
    private lateinit var paletteComboBox: ComboBox<String>
    private lateinit var paletteLabel: Label
    private lateinit var candleMultiCheckBox: CheckBox
    private lateinit var twinkleCheckBox: CheckBox
    private lateinit var puddlesPeakCheckBox: CheckBox
    private var lastRandomSwitchNs: Long = 0L

    private val combinedWidth = VIRTUAL_GRID_WIDTH
    private val combinedHeight = VIRTUAL_GRID_HEIGHT

    override fun start(primaryStage: Stage) {
        primaryStage.title = "WLED 4x4 Grid Simulator"
        println("Loaded layout '$LAYOUT_SOURCE' with ${DEVICES.size} panels; virtual grid ${combinedWidth}x$combinedHeight.")

        val spacing = CELL_SIZE * 1.2
        canvas = Canvas((combinedWidth * spacing + spacing / 2), (combinedHeight * spacing + spacing / 2))
        gc = canvas.graphicsContext2D

        statusLabel = Label("Connecting to WLED devices...")
        
        retryButton = Button("Retry Connection").apply {
            isVisible = false
            isManaged = false
            setOnAction {
                retryConnections()
            }
        }

        animationComboBox = ComboBox<String>().apply {
            val animationNames = AnimationRegistry.getNames()
            items.addAll(animationNames)
            value = animationNames.firstOrNull() ?: ""
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
        
        speedLabel = Label("Speed: 128")
        speedSlider = Slider(0.0, 255.0, 128.0).apply {
            isShowTickMarks = true
            isShowTickLabels = true
            majorTickUnit = 64.0
            blockIncrement = 8.0
            orientation = Orientation.VERTICAL
            // Invert so faster (255) is at top, slower (0) is at bottom
            valueProperty().addListener { _, _, newValue ->
                val newValueDouble = newValue.toDouble()
                val invertedValue = 255.0 - newValueDouble
                val speedValue = invertedValue.coerceIn(0.0, 255.0).roundToInt()
                speedLabel.text = "Speed: $speedValue"
                currentAnimation?.let { animation ->
                    if (animation.supportsSpeed()) {
                        animation.setSpeed(speedValue)
                    }
                }
            }
        }

        textInputLabel = Label("Scroll Text:")
        textInputField = TextField("HELLO WLED").apply {
            prefColumnCount = 14
            textProperty().addListener { _, _, newValue ->
                val animation = currentAnimation
                if (animation != null && animation.supportsTextInput()) {
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
                if (animation != null && animation.supportsSpeedFactor()) {
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

        // Color picker - default to black for TwinkleUp, white for others
        colorLabel = Label("Color:")
        colorPicker = ColorPicker(Color.WHITE).apply {
            isDisable = true
            valueProperty().addListener { _, _, newColor ->
                val animation = currentAnimation
                if (animation != null && animation.supportsColor()) {
                    val rgbColor = RgbColor(
                        (newColor.red * 255).toInt().coerceIn(0, 255),
                        (newColor.green * 255).toInt().coerceIn(0, 255),
                        (newColor.blue * 255).toInt().coerceIn(0, 255)
                    )
                    animation.setColor(rgbColor)
                    // Store the color state for this animation
                    val animationName = animation.getName()
                    animationColorStates[animationName] = rgbColor
                }
            }
        }
        colorLabel.isDisable = true

        // Palette selector
        paletteLabel = Label("Palette:")
        paletteComboBox = ComboBox<String>().apply {
            items.addAll(Palettes.getNames())
            value = Palettes.DEFAULT_PALETTE_NAME
            isDisable = true
            setOnAction {
                val selectedPalette = value
                val animation = currentAnimation
                if (animation != null && animation.supportsPalette() && selectedPalette != null) {
                    val palette = Palettes.get(selectedPalette)
                    if (palette != null) {
                        animation.setPalette(palette)
                        // Store the palette state for this animation
                        val animationName = animation.getName()
                        animationPaletteStates[animationName] = selectedPalette
                    }
                }
            }
        }
        paletteLabel.isDisable = true

        val controlsBox = HBox(10.0,
            statusLabel,
            retryButton,
            Label("Animation:"),
            animationComboBox,
            startButton,
            randomCheckBox,
            randomIntervalField,
            randomIntervalLabel
        )
        
        val colorPaletteBox = HBox(10.0,
            colorLabel,
            colorPicker,
            paletteLabel,
            paletteComboBox
        ).apply {
            padding = Insets(10.0)
        }
        
        val mainContent = VBox(10.0, controlsBox, canvas, colorPaletteBox)
        val root = BorderPane()
        root.center = mainContent
        // Candle multi-mode checkbox
        candleMultiCheckBox = CheckBox("Multi Candle").apply {
            isVisible = false
            isManaged = false
            selectedProperty().addListener { _, _, isSelected ->
                val animation = currentAnimation
                if (animation != null && animation.supportsMultiMode()) {
                    animation.setMultiMode(isSelected)
                }
            }
        }
        
        // Twinkle checkbox
        twinkleCheckBox = CheckBox("Twinkle").apply {
            isVisible = false
            isManaged = false
            selectedProperty().addListener { _, _, isSelected ->
                val animation = currentAnimation
                if (animation != null && animation.supportsCatMode()) {
                    animation.setCatMode(isSelected)
                }
            }
        }
        
        // Puddles peak detection checkbox
        puddlesPeakCheckBox = CheckBox("Peak Detect").apply {
            isVisible = false
            isManaged = false
            selectedProperty().addListener { _, _, isSelected ->
                val animation = currentAnimation
                if (animation != null && animation.supportsPeakDetect()) {
                    animation.setPeakDetect(isSelected)
                }
            }
        }
        
        val brightnessSpeedBox = HBox(12.0).apply {
            padding = Insets(10.0)
            children.addAll(
                VBox(12.0, brightnessLabel, brightnessSlider),
                VBox(12.0, speedLabel, speedSlider)
            )
        }
        val rightControlsBox = VBox(12.0, brightnessSpeedBox, textInputLabel, textInputField, textSpeedLabel, textSpeedSlider, candleMultiCheckBox, twinkleCheckBox, puddlesPeakCheckBox).apply {
            padding = Insets(10.0)
        }
        root.right = rightControlsBox

        updateAnimationControls(animationComboBox.value ?: "")

        primaryStage.scene = Scene(root, 860.0, 900.0)
        primaryStage.centerOnScreen()
        primaryStage.show()
        primaryStage.toFront()
        primaryStage.requestFocus()
        println("JavaFX window opened - ready to control WLED devices")

        Thread.sleep(500)

        connectToDevices()

        primaryStage.setOnCloseRequest {
            stopAnimation()
            devices.forEach { connection ->
                sendBlackFrame(connection)
                connection.artNetClient.disconnect()
            }
            println("Disconnected from all WLED devices")
        }
    }

    private fun connectToDevices() {
        failedDevices.clear()
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
                    failedDevices.add(deviceConfig)
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
                    retryButton.isVisible = false
                    retryButton.isManaged = false
                    startSelectedAnimation()
                    startButton.text = "Stop Animation"
                }
                println("Successfully connected to all ${connectedCount[0]} devices")
            } else {
                Platform.runLater {
                    val failedNames = failedDevices.joinToString(", ") { it.name }
                    statusLabel.text = "Connected to ${connectedCount[0]}/${DEVICES.size} devices. Failed: $failedNames"
                    animationComboBox.isDisable = false
                    randomCheckBox.isDisable = false
                    retryButton.isVisible = true
                    retryButton.isManaged = true
                }
                System.err.println("Warning: Only ${connectedCount[0]}/${DEVICES.size} devices connected")
            }
        }.start()
    }

    private fun retryConnections() {
        if (failedDevices.isEmpty()) {
            return
        }

        Platform.runLater {
            retryButton.isDisable = true
            statusLabel.text = "Retrying connection to ${failedDevices.size} device(s)..."
        }

        Thread {
            val devicesToRetry = failedDevices.toList()
            failedDevices.clear()
            val newlyConnected = mutableListOf<DeviceConnection>()

            for (deviceConfig in devicesToRetry) {
                // Check if device is already connected
                val alreadyConnected = devices.any { it.config.ip == deviceConfig.ip }
                if (alreadyConnected) {
                    println("Device ${deviceConfig.name} is already connected, skipping retry")
                    continue
                }

                try {
                    println("Retrying connection to ${deviceConfig.name} at ${deviceConfig.ip}...")
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
                    newlyConnected.add(device)
                } catch (ex: Exception) {
                    System.err.println("Retry failed for ${deviceConfig.name}: ${ex.message}")
                    ex.printStackTrace()
                    failedDevices.add(deviceConfig)
                }
            }

            Platform.runLater {
                retryButton.isDisable = false
                
                if (failedDevices.isEmpty()) {
                    // All devices now connected
                    querySyncSettings(DEVICES)
                    statusLabel.text = "All devices connected!"
                    retryButton.isVisible = false
                    retryButton.isManaged = false
                    if (devices.size == DEVICES.size) {
                        startButton.isDisable = false
                        animationComboBox.isDisable = false
                        randomCheckBox.isDisable = false
                        // If animation was already running, restart it to include newly connected devices
                        // Otherwise, start it for the first time (matching initial connection behavior)
                        if (running.get()) {
                            stopAnimation()
                        }
                        startSelectedAnimation()
                        startButton.text = "Stop Animation"
                    }
                } else {
                    val failedNames = failedDevices.joinToString(", ") { it.name }
                    statusLabel.text = "Connected to ${devices.size}/${DEVICES.size} devices. Failed: $failedNames"
                }
            }

            if (newlyConnected.isNotEmpty()) {
                println("Successfully reconnected to ${newlyConnected.size} device(s)")
            }
        }.start()
    }

    private fun startSelectedAnimation() {
        if (devices.isEmpty()) {
            System.err.println("No devices connected!")
            return
        }

        val selectedAnimation = animationComboBox.value ?: run {
            System.err.println("No animation selected")
            return
        }
        
        // Create animation using registry
        val newAnimation: LedAnimation = AnimationRegistry.create(selectedAnimation) ?: run {
            System.err.println("Unknown animation: $selectedAnimation")
            return
        }
        
        // Apply animation-specific settings if supported
        if (newAnimation.supportsMultiMode() && ::candleMultiCheckBox.isInitialized) {
            newAnimation.setMultiMode(candleMultiCheckBox.isSelected)
        }
        if (newAnimation.supportsCatMode() && ::twinkleCheckBox.isInitialized) {
            newAnimation.setCatMode(twinkleCheckBox.isSelected)
        }
        if (newAnimation.supportsTextInput()) {
            newAnimation.setText(textInputField.text)
        }
        if (newAnimation.supportsSpeedFactor()) {
            newAnimation.setSpeedFactor(textSpeedSlider.value / 100.0)
        }
        if (newAnimation.supportsPeakDetect() && ::puddlesPeakCheckBox.isInitialized) {
            newAnimation.setPeakDetect(puddlesPeakCheckBox.isSelected)
        }

        currentAnimation = newAnimation

        val animation = newAnimation
        animation.init(combinedWidth, combinedHeight)
        
        // Update UI controls based on animation capabilities
        val supportsColor = animation.supportsColor()
        val supportsPalette = animation.supportsPalette()
        val supportsSpeed = animation.supportsSpeed()
        
        // Update speed control visibility immediately
        speedLabel.isVisible = supportsSpeed
        speedLabel.isManaged = supportsSpeed
        speedSlider.isVisible = supportsSpeed
        speedSlider.isManaged = supportsSpeed
        
        // Set initial speed if animation supports it
        if (supportsSpeed) {
            val currentSpeed = animation.getSpeed() ?: 128
            speedSlider.value = 255.0 - currentSpeed  // Invert for slider
            speedLabel.text = "Speed: $currentSpeed"
        }
        
        colorPicker.isDisable = !supportsColor
        colorLabel.isDisable = !supportsColor
        paletteComboBox.isDisable = !supportsPalette
        paletteLabel.isDisable = !supportsPalette
        
        // Apply color and palette if animation supports them
        if (supportsColor) {
            // Check if we have a stored color state for this animation
            val storedColor = animationColorStates[selectedAnimation]
            val colorToUse = if (storedColor != null) {
                // Use stored color state
                storedColor
            } else {
                // First time - get the animation's current color (its default)
                val animColor = animation.getColor()
                if (animColor != null) {
                    animColor
                } else {
                    // Fallback: use animation-specific default
                    when (selectedAnimation) {
                        "TwinkleUp" -> RgbColor.BLACK
                        "Tetrix" -> RgbColor.BLACK
                        else -> {
                            val currentColor = colorPicker.value
                            RgbColor(
                                (currentColor.red * 255).toInt().coerceIn(0, 255),
                                (currentColor.green * 255).toInt().coerceIn(0, 255),
                                (currentColor.blue * 255).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                }
            }
            // Update color picker to show the animation's current color
            colorPicker.value = Color.rgb(colorToUse.r, colorToUse.g, colorToUse.b)
            animation.setColor(colorToUse)
            // Store the color state
            animationColorStates[selectedAnimation] = colorToUse
        }
        if (supportsPalette) {
            // Check if we have a stored palette state for this animation
            val storedPaletteName = animationPaletteStates[selectedAnimation]
            val paletteNameToUse = if (storedPaletteName != null) {
                // Use stored palette state
                storedPaletteName
            } else {
                // First time - check if animation has its own default, otherwise use global default
                animation.getDefaultPaletteName() ?: Palettes.DEFAULT_PALETTE_NAME
            }
            // Update palette combo box to show the animation's current palette
            paletteComboBox.value = paletteNameToUse
            val palette = Palettes.get(paletteNameToUse)
            if (palette != null) {
                animation.setPalette(palette)
                // Store the palette state
                animationPaletteStates[selectedAnimation] = paletteNameToUse
            }
        }
        
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
                                // Grid01 is mirrored, so invert X coordinate for LED index
                                val mappedX = if (device.config.name == "Grid01") {
                                    device.width - 1 - localX
                                } else {
                                    localX
                                }
                                val localLedIndex = localY * device.width + mappedX
                                rgbData[localLedIndex * 3] = rgb.r
                                rgbData[localLedIndex * 3 + 1] = rgb.g
                                rgbData[localLedIndex * 3 + 2] = rgb.b
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
        currentAnimation?.cleanup()
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
                val color = Color.rgb(rgb.r, rgb.g, rgb.b)
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

    private fun applyBrightness(rgb: RgbColor): RgbColor {
        if (brightnessScale >= 0.999) return rgb
        val scale = brightnessScale.coerceIn(0.0, 1.0)
        return RgbColor(
            (rgb.r * scale).roundToInt().coerceIn(0, 255),
            (rgb.g * scale).roundToInt().coerceIn(0, 255),
            (rgb.b * scale).roundToInt().coerceIn(0, 255)
        )
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
        // Check if current animation supports these features, or create a temporary instance to check
        val animation = currentAnimation
        val tempAnimation = if (animation != null && animation.getName() == animationName) {
            animation
        } else {
            // Create temporary instance to check capabilities
            AnimationRegistry.create(animationName)
        }
        
        val supportsTextInput = tempAnimation?.supportsTextInput() ?: false
        val supportsSpeedFactor = tempAnimation?.supportsSpeedFactor() ?: false
        val supportsMultiMode = tempAnimation?.supportsMultiMode() ?: false
        val supportsCatMode = tempAnimation?.supportsCatMode() ?: false
        val supportsPeakDetect = tempAnimation?.supportsPeakDetect() ?: false
        
        textInputLabel.isVisible = supportsTextInput
        textInputField.isVisible = supportsTextInput
        textInputLabel.isManaged = supportsTextInput
        textInputField.isManaged = supportsTextInput
        textSpeedLabel.isVisible = supportsSpeedFactor
        textSpeedLabel.isManaged = supportsSpeedFactor
        textSpeedSlider.isVisible = supportsSpeedFactor
        textSpeedSlider.isManaged = supportsSpeedFactor
        
        candleMultiCheckBox.isVisible = supportsMultiMode
        candleMultiCheckBox.isManaged = supportsMultiMode
        
        twinkleCheckBox.isVisible = supportsCatMode
        twinkleCheckBox.isManaged = supportsCatMode
        
        puddlesPeakCheckBox.isVisible = supportsPeakDetect
        puddlesPeakCheckBox.isManaged = supportsPeakDetect
        
        // Check if animation supports speed (self-identified by animation)
        val supportsSpeed = currentAnimation?.supportsSpeed() ?: false
        speedLabel.isVisible = supportsSpeed
        speedLabel.isManaged = supportsSpeed
        speedSlider.isVisible = supportsSpeed
        speedSlider.isManaged = supportsSpeed
        
        // Set initial speed value if animation supports it
        if (supportsSpeed) {
            val anim = currentAnimation
            if (anim != null) {
                val currentSpeed = anim.getSpeed() ?: 128
                // Invert for slider (255 at top = 0.0, 0 at bottom = 255.0)
                speedSlider.value = 255.0 - currentSpeed
                speedLabel.text = "Speed: $currentSpeed"
            }
        }
        
        // Always update palette combo box to show the correct state for the selected animation
        // This ensures the UI reflects the correct state even when switching animations
        // We need to check if the animation supports palettes, but we can't know that without the instance
        // So we'll update it based on stored state, and it will be properly set when the animation starts
        val storedPaletteName = animationPaletteStates[animationName]
        if (storedPaletteName != null) {
            // Only update if we have a stored state for this animation
            // This prevents showing wrong state when switching to an animation that doesn't support palettes
            paletteComboBox.value = storedPaletteName
        }
        
        // Update color and palette controls based on current animation (if it exists)
        // If animation doesn't exist yet, controls will be updated when animation starts
        val currentAnim = currentAnimation
        // Only update if the current animation matches the selected animation name
        if (currentAnim != null && currentAnim.getName() == animationName) {
            val supportsColor = currentAnim.supportsColor()
            val supportsPalette = currentAnim.supportsPalette()
            
            colorPicker.isDisable = !supportsColor
            colorLabel.isDisable = !supportsColor
            // Palette combo box is already updated above, just enable/disable it
            paletteComboBox.isDisable = !supportsPalette
            paletteLabel.isDisable = !supportsPalette
            
            // Apply current selections if animation supports them
            if (supportsColor) {
                // Check if we have a stored color state for this animation
                val storedColor = animationColorStates[animationName]
                val colorToUse = if (storedColor != null) {
                    // Use stored color state
                    storedColor
                } else {
                    // First time - get the animation's current color (its default)
                    val animColor = currentAnim.getColor()
                    if (animColor != null) {
                        animColor
                    } else {
                        // Fallback: use animation-specific default
                        when (animationName) {
                            "TwinkleUp" -> RgbColor.BLACK
                            "Tetrix" -> RgbColor.BLACK
                            else -> {
                                val currentColor = colorPicker.value
                                RgbColor(
                                    (currentColor.red * 255).toInt().coerceIn(0, 255),
                                    (currentColor.green * 255).toInt().coerceIn(0, 255),
                                    (currentColor.blue * 255).toInt().coerceIn(0, 255)
                                )
                            }
                        }
                    }
                }
                // Update color picker to show the animation's current color
                colorPicker.value = Color.rgb(colorToUse.r, colorToUse.g, colorToUse.b)
                currentAnim.setColor(colorToUse)
                // Store the color state
                animationColorStates[animationName] = colorToUse
            }
            
            if (supportsPalette) {
                // Check if we have a stored palette state for this animation
                val storedPaletteName = animationPaletteStates[animationName]
                val paletteNameToUse = if (storedPaletteName != null) {
                    // Use stored palette state
                    storedPaletteName
                } else {
                    // First time - check if animation has its own default, otherwise use global default
                    currentAnim.getDefaultPaletteName() ?: Palettes.DEFAULT_PALETTE_NAME
                }
                // Update palette combo box to show the animation's current palette
                paletteComboBox.value = paletteNameToUse
                val palette = Palettes.get(paletteNameToUse)
                if (palette != null) {
                    currentAnim.setPalette(palette)
                    // Store the palette state
                    animationPaletteStates[animationName] = paletteNameToUse
                }
            } else {
                // Animation doesn't support palette - disable the combo box
                paletteComboBox.isDisable = true
                paletteLabel.isDisable = true
            }
        }
        // Note: If animation doesn't exist yet or doesn't match, the state will be restored
        // when the animation is created in startSelectedAnimation()
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

