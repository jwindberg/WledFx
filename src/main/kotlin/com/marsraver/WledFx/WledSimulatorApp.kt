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
import com.marsraver.WledFx.animation.SnakeAnimation
import com.marsraver.WledFx.animation.SwirlAnimation
import com.marsraver.WledFx.wled.WledArtNetClient
import com.marsraver.WledFx.wled.WledClient
import com.marsraver.WledFx.wled.model.WledConfig
import com.marsraver.WledFx.wled.model.WledInfo
import com.marsraver.WledFx.wled.model.WledState
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.util.concurrent.atomic.AtomicBoolean

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

        val controlsBox = HBox(10.0, statusLabel, Label("Animation:"), animationComboBox, startButton)
        val vbox = VBox(10.0, controlsBox, canvas)
        val root = BorderPane(vbox)

        primaryStage.scene = Scene(root, 800.0, 900.0)
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
                    startSelectedAnimation()
                    startButton.text = "Stop Animation"
                }
                println("Successfully connected to all ${connectedCount[0]} devices")
            } else {
                Platform.runLater {
                    statusLabel.text = "Connected to ${connectedCount[0]}/${DEVICES.size} devices"
                    animationComboBox.isDisable = false
                }
                System.err.println("Warning: Only ${connectedCount[0]}/${DEVICES.size} devices connected")
            }
        }.start()

        primaryStage.setOnCloseRequest {
            stopAnimation()
            devices.forEach { connection ->
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
        currentAnimation = when (selectedAnimation) {
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
            else -> {
                System.err.println("Unknown animation: $selectedAnimation")
                return
            }
        }

        currentAnimation?.init(combinedWidth, combinedHeight)
        running.set(true)
        val lastUpdateTime = longArrayOf(System.nanoTime())
        val totalLeds = combinedWidth * combinedHeight

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
                                val rgb = animation.getPixelColor(globalX, globalY)
                                rgbData[localLedIndex * 3] = rgb[0]
                                rgbData[localLedIndex * 3 + 1] = rgb[1]
                                rgbData[localLedIndex * 3 + 2] = rgb[2]
                            }
                        }

                        device.artNetClient.sendRgb(rgbData, deviceLeds)
                    }

                    drawCombinedSimulation()
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
                val rgb = animation.getPixelColor(globalX, globalY)
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

