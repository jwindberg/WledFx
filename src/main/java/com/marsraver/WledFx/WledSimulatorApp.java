package com.marsraver.WledFx;

import com.marsraver.WledFx.animation.AkemiAnimation;
import com.marsraver.WledFx.animation.BlackHoleAnimation;
import com.marsraver.WledFx.animation.BlobsAnimation;
import com.marsraver.WledFx.animation.BlurzAnimation;
import com.marsraver.WledFx.animation.BouncingBallAnimation;
import com.marsraver.WledFx.animation.ColoredBurstsAnimation;
import com.marsraver.WledFx.animation.ColorTestAnimation;
import com.marsraver.WledFx.animation.CrazyBeesAnimation;
import com.marsraver.WledFx.animation.DistortionWavesAnimation;
import com.marsraver.WledFx.animation.GeqAnimation;
import com.marsraver.WledFx.animation.SwirlAnimation;

import com.marsraver.WledFx.animation.LedAnimation;
import com.marsraver.WledFx.animation.SnakeAnimation;
import com.marsraver.WledFx.wled.WledArtNetClient;
import com.marsraver.WledFx.wled.WledClient;
import com.marsraver.WledFx.wled.model.WledConfig;
import com.marsraver.WledFx.wled.model.WledInfo;
import com.marsraver.WledFx.wled.model.WledState;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaFX application to simulate multiple WLED devices in a 2x2 grid
 */
public class WledSimulatorApp extends Application {
    
    private static final int CELL_SIZE = 10;
    private static final int FPS = 60; // Frames per second
    private static final long FRAME_INTERVAL_NS = 1_000_000_000 / FPS;
    
    // Device configuration
    private static class DeviceConfig {
        String ip;
        String name;
        int gridX;  // Position in 2x2 grid: 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
        int gridY;
    }
    
    private static final DeviceConfig[] DEVICES = {
        new DeviceConfig() {{ ip = "192.168.7.113"; name = "Grid01"; gridX = 0; gridY = 0; }}, // Top-left
        new DeviceConfig() {{ ip = "192.168.7.226"; name = "Grid02"; gridX = 1; gridY = 0; }}, // Top-right
        new DeviceConfig() {{ ip = "192.168.7.181"; name = "Grid03"; gridX = 0; gridY = 1; }}, // Bottom-left
        new DeviceConfig() {{ ip = "192.168.7.167"; name = "Grid04"; gridX = 1; gridY = 1; }}  // Bottom-right
    };
    
    private Canvas canvas;
    private GraphicsContext gc;
    private List<DeviceConnection> devices = new ArrayList<>();
    private AnimationTimer animationTimer;
    private LedAnimation currentAnimation;
    private AtomicBoolean running = new AtomicBoolean(false);
    private Button startButton;
    private Label statusLabel;
    private ComboBox<String> animationComboBox;
    private int combinedWidth = 32;
    private int combinedHeight = 32;
    
    // Container for device connection info
    private static class DeviceConnection {
        DeviceConfig config;
        WledInfo info;
        WledConfig wledConfig;
        WledState wledState;
        WledArtNetClient artNetClient;
        int width;
        int height;
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("WLED 4x4 Grid Simulator");
        
        // Create canvas for combined 32x32 grid
        // Account for spacing (CELL_SIZE * 1.2) used in drawing
        double spacing = CELL_SIZE * 1.2;
        canvas = new Canvas((int)(combinedWidth * spacing + spacing / 2), (int)(combinedHeight * spacing + spacing / 2));
        gc = canvas.getGraphicsContext2D();
        
        // Create controls
        statusLabel = new Label("Connecting to WLED devices...");
        
        // Animation selection combo box
        animationComboBox = new ComboBox<>();
        animationComboBox.getItems().addAll("Snake", "Bouncing Ball", "Colored Bursts", "Black Hole", "Blobs", "Blurz", "Crazy Bees", "Distortion Waves", "GEQ", "Swirl", "Akemi", "Color Test");
        animationComboBox.setValue("Snake");
        animationComboBox.setDisable(true);
        
        // Add event handler to switch animations when selection changes
        animationComboBox.setOnAction(e -> {
            if (running.get()) {
                // Stop current animation and start the new one
                stopAnimation();
                startSelectedAnimation();
                startButton.setText("Stop Animation");
            }
            // When not running, just update the selection - animation will start when button is clicked
        });
        
        startButton = new Button("Start Animation");
        startButton.setDisable(true);
        startButton.setOnAction(e -> {
            if (!running.get()) {
                startSelectedAnimation();
                startButton.setText("Stop Animation");
            } else {
                stopAnimation();
                startButton.setText("Start Animation");
            }
        });
        
        // Setup UI
        VBox vbox = new VBox(10);
        HBox controlsBox = new HBox(10);
        Label animationLabel = new Label("Animation:");
        controlsBox.getChildren().addAll(statusLabel, animationLabel, animationComboBox, startButton);
        vbox.getChildren().addAll(controlsBox, canvas);
        
        BorderPane root = new BorderPane();
        root.setCenter(vbox);
        Scene scene = new Scene(root, 800, 900);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
        primaryStage.toFront();
        primaryStage.requestFocus();
        System.out.println("JavaFX window opened - ready to control WLED devices");
        
        // Small delay to ensure window is displayed before starting connection
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Connect to all WLED devices in background thread
        new Thread(() -> {
            final int[] connectedCount = {0};
            for (DeviceConfig deviceConfig : DEVICES) {
                try {
                    System.out.println("Connecting to " + deviceConfig.name + " at " + deviceConfig.ip + "...");
                    WledClient client = new WledClient(deviceConfig.ip);
                    WledInfo info = client.getInfo();
                    WledConfig config = client.getConfig();
                    WledState state = client.getState();
                    
                    int width = info.getLeds().getMatrix().getW();
                    int height = info.getLeds().getMatrix().getH();
                    
                    System.out.println("Connected! Device: " + info.getName());
                    System.out.println("Matrix size: " + width + "x" + height);
                    System.out.println("Total LEDs: " + info.getLeds().getCount());
                    
                    // Get DMX configuration
                    var dmxConfig = config.getIface().getLive().getDmx();
                    int port = config.getIface().getLive().getPort();
                    int universe = dmxConfig.getUni();
                    int dmxStartAddress = dmxConfig.getAddr();
                    
                    System.out.println("DMX Config - Universe: " + universe + ", Port: " + port + ", Start Address: " + dmxStartAddress);
                    
                    // Setup Art-Net client
                    WledArtNetClient artNetClient = new WledArtNetClient(info, universe, port, dmxStartAddress);
                    artNetClient.connect();
                    System.out.println("Connected to Art-Net on port " + port);
                    
                    // Create device connection
                    DeviceConnection device = new DeviceConnection();
                    device.config = deviceConfig;
                    device.info = info;
                    device.wledConfig = config;
                    device.wledState = state;
                    device.artNetClient = artNetClient;
                    device.width = width;
                    device.height = height;
                    devices.add(device);
                    connectedCount[0]++;
                    
                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        statusLabel.setText("Connected to " + connectedCount[0] + "/" + DEVICES.length + " devices");
                    });
                } catch (Exception e) {
                    System.err.println("Failed to connect to " + deviceConfig.name + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            if (connectedCount[0] == DEVICES.length) {
                Platform.runLater(() -> {
                    statusLabel.setText("All devices connected!");
                    startButton.setDisable(false);
                    animationComboBox.setDisable(false);
                    // Auto-start the selected animation for easier testing
                    startSelectedAnimation();
                    startButton.setText("Stop Animation");
                });
                System.out.println("Successfully connected to all " + connectedCount[0] + " devices");
            } else {
                Platform.runLater(() -> {
                    statusLabel.setText("Connected to " + connectedCount[0] + "/" + DEVICES.length + " devices");
                    animationComboBox.setDisable(false);
                });
                System.err.println("Warning: Only " + connectedCount[0] + "/" + DEVICES.length + " devices connected");
            }
        }).start();
        
        // Cleanup on close
        primaryStage.setOnCloseRequest(e -> {
            stopAnimation();
            for (DeviceConnection device : devices) {
                if (device.artNetClient != null) {
                    device.artNetClient.disconnect();
                }
            }
            System.out.println("Disconnected from all WLED devices");
        });
    }
    
    private void stopAnimation() {
        running.set(false);
        if (animationTimer != null) {
            animationTimer.stop();
        }
        // Cleanup animation-specific resources
        if (currentAnimation instanceof BlurzAnimation) {
            ((BlurzAnimation) currentAnimation).cleanup();
        }
        if (currentAnimation instanceof GeqAnimation) {
            ((GeqAnimation) currentAnimation).cleanup();
        }
        if (currentAnimation instanceof SwirlAnimation) {
            ((SwirlAnimation) currentAnimation).cleanup();
        }
        if (currentAnimation instanceof AkemiAnimation) {
            ((AkemiAnimation) currentAnimation).cleanup();
        }
    }
    
    private void startSelectedAnimation() {
        if (devices.isEmpty()) {
            System.err.println("No devices connected!");
            return;
        }
        
        // Create animation based on selection
        String selectedAnimation = animationComboBox.getValue();
        if ("Color Test".equals(selectedAnimation)) {
            currentAnimation = new ColorTestAnimation();
        } else if ("Snake".equals(selectedAnimation)) {
            currentAnimation = new SnakeAnimation();
        } else if ("Bouncing Ball".equals(selectedAnimation)) {
            currentAnimation = new BouncingBallAnimation();
        } else if ("Colored Bursts".equals(selectedAnimation)) {
            currentAnimation = new ColoredBurstsAnimation();
        } else if ("Black Hole".equals(selectedAnimation)) {
            currentAnimation = new BlackHoleAnimation();
        } else if ("Blobs".equals(selectedAnimation)) {
            currentAnimation = new BlobsAnimation();
        } else if ("Blurz".equals(selectedAnimation)) {
            currentAnimation = new BlurzAnimation();
        } else if ("Crazy Bees".equals(selectedAnimation)) {
            currentAnimation = new CrazyBeesAnimation();
        } else if ("Distortion Waves".equals(selectedAnimation)) {
            currentAnimation = new DistortionWavesAnimation();
        } else if ("GEQ".equals(selectedAnimation)) {
            currentAnimation = new GeqAnimation();
        } else if ("Swirl".equals(selectedAnimation)) {
            currentAnimation = new SwirlAnimation();
        } else if ("Akemi".equals(selectedAnimation)) {
            currentAnimation = new AkemiAnimation();
        } else {
            System.err.println("Unknown animation: " + selectedAnimation);
            return;
        }
        
        currentAnimation.init(combinedWidth, combinedHeight);
        
        running.set(true);
        long[] lastUpdateTime = {System.nanoTime()};
        int totalLeds = combinedWidth * combinedHeight;
        
        System.out.println("Starting " + currentAnimation.getName() + " animation with " + totalLeds + " LEDs total (" + combinedWidth + "x" + combinedHeight + " combined grid)");
        
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running.get()) {
                    return;
                }
                
                // Control frame rate
                if (now - lastUpdateTime[0] < FRAME_INTERVAL_NS) {
                    return;
                }
                lastUpdateTime[0] = now;
                
                // Update animation
                if (!currentAnimation.update(now)) {
                    stopAnimation();
                    return;
                }
                
                try {
                    // Send data to each device
                    for (DeviceConnection device : devices) {
                        int deviceLeds = device.width * device.height;
                        int[] rgbData = new int[deviceLeds * 3];
                        
                        // Calculate device position in combined grid
                        int deviceStartX = device.config.gridX * device.width;
                        int deviceStartY = device.config.gridY * device.height;
                        
                        // Get pixel colors from animation
                        for (int y = 0; y < device.height; y++) {
                            for (int x = 0; x < device.width; x++) {
                                int globalX = deviceStartX + x;
                                int globalY = deviceStartY + y;
                                
                                // Map to LED index for top-right, vertical serpentine
                                // LED 0 is at top-right corner (x=width-1, y=0)
                                int localLedIndex = x * device.height + (device.height - 1 - y);
                                
                                int[] rgb = currentAnimation.getPixelColor(globalX, globalY);
                                
                                rgbData[localLedIndex * 3] = rgb[0];
                                rgbData[localLedIndex * 3 + 1] = rgb[1];
                                rgbData[localLedIndex * 3 + 2] = rgb[2];
                            }
                        }
                        
                        // Send to device
                        device.artNetClient.sendRgb(rgbData, deviceLeds);
                    }
                    
                    // Draw combined simulation
                    drawCombinedSimulation();
                } catch (Exception e) {
                    System.err.println("Error sending frame: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        animationTimer.start();
    }
    
    private void drawCombinedSimulation() {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Calculate spacing and radius for circular pixels
        double spacing = CELL_SIZE * 1.2;
        double radius = CELL_SIZE / 2.0;
        double startX = spacing / 2;
        double startY = spacing / 2;
        
        // Draw combined grid across all devices
        for (int globalY = 0; globalY < combinedHeight; globalY++) {
            for (int globalX = 0; globalX < combinedWidth; globalX++) {
                int[] rgb = currentAnimation.getPixelColor(globalX, globalY);
                Color color = Color.rgb(rgb[0], rgb[1], rgb[2]);
                
                // Draw circular pixel
                double pixelX = globalX * spacing + startX;
                double pixelY = globalY * spacing + startY;
                gc.setFill(color);
                gc.fillOval(pixelX - radius, pixelY - radius, radius * 2, radius * 2);
            }
        }
        
        // Draw grid lines to separate the 4 devices
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);
        
        // Vertical line in middle
        double lineX = combinedWidth / 2 * spacing + startX;
        gc.strokeLine(lineX, 0, lineX, canvas.getHeight());
        
        // Horizontal line in middle
        double lineY = combinedHeight / 2 * spacing + startY;
        gc.strokeLine(0, lineY, canvas.getWidth(), lineY);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

