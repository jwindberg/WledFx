package com.marsraver.WledFx.animation;

import javax.sound.sampled.*;
import java.util.Random;

/**
 * Blurz animation - Random pixels with blur effect
 * Based on WLED mode_blurz by Andrew Tuline
 */
public class BlurzAnimation implements LedAnimation {
    
    private static final Random random = new Random();
    private int combinedWidth;
    private int combinedHeight;
    
    // Pixel state: 0-255 brightness per pixel
    private byte[][] pixelBrightness;
    
    // Color hue per pixel
    private byte[][] pixelHue;
    
    // Color hue tracking
    private int currentHue = 0;
    
    // Configuration
    private int fadeSpeed = 20;        // How fast pixels fade (0-255) - higher = faster fade
    private int blurIntensity = 10;    // Blur amount (0-255) - less blur for brighter pixels
    private long lastPixelTime;
    private long pixelInterval = 50_000_000L; // 50ms between new pixels
    private int pixelsPerFrame = 5;     // Add multiple pixels at once
    
    private int audioIndex = 0; // Simulated audio FFT index
    private long lastFadeTime;
    
    // Microphone capture
    private TargetDataLine microphone;
    private byte[] audioBuffer;
    private Thread audioThread;
    private int currentSoundLevel = 0;
    
    // Sound level tracking - keep a window of recent samples
    private int[] soundLevelWindow;
    private int soundLevelIndex = 0;
    private static final int WINDOW_SIZE = 40; // Keep last 2 seconds (40 samples at 50ms intervals)
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.pixelBrightness = new byte[combinedWidth][combinedHeight];
        this.pixelHue = new byte[combinedWidth][combinedHeight];
        this.lastPixelTime = 0;
        this.lastFadeTime = 0;
        this.soundLevelWindow = new int[WINDOW_SIZE];
        this.soundLevelIndex = 0;
        
        // Try to start microphone capture
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                audioBuffer = new byte[4096];
                System.out.println("Microphone initialized successfully");
            } else {
                System.err.println("Microphone not supported");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize microphone: " + e.getMessage());
        }
    }
    
    @Override
    public boolean update(long now) {
        // Fade all pixels - but only every 100ms
        if (now - lastFadeTime > 100_000_000L) {
            for (int x = 0; x < combinedWidth; x++) {
                for (int y = 0; y < combinedHeight; y++) {
                    int brightness = pixelBrightness[x][y] & 0xFF;
                    if (brightness > 0) {
                        brightness = Math.max(0, brightness - fadeSpeed);
                        pixelBrightness[x][y] = (byte) brightness;
                    }
                }
            }
            lastFadeTime = now;
        }
        
        // Add new random pixels
        if (now - lastPixelTime > pixelInterval) {
            int brightness;
            
            // Use real audio if available, otherwise simulated
            if (microphone != null && microphone.isOpen()) {
                try {
                    int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                    if (bytesRead > 0) {
                        // Calculate RMS (root mean square) for volume
                        long sum = 0;
                        for (int i = 0; i < bytesRead; i += 2) {
                            int sample = (int) audioBuffer[i] | ((int) audioBuffer[i + 1] << 8);
                            sum += sample * sample;
                        }
                        double rms = Math.sqrt(sum / (bytesRead / 2));
                        currentSoundLevel = (int) Math.min(255, rms / 128);
                        brightness = Math.min(255, currentSoundLevel);
                    } else {
                        currentSoundLevel = 0;
                    }
                } catch (Exception e) {
                    currentSoundLevel = 0;
                }
            } else {
                // Simulated audio data - generate random spikes
                currentSoundLevel = random.nextInt(100);
            }
            
            // Add current sound level to window
            soundLevelWindow[soundLevelIndex] = currentSoundLevel;
            soundLevelIndex = (soundLevelIndex + 1) % WINDOW_SIZE;
            
            // Calculate percentile threshold to avoid saturation with sustained music
            // Sort the window to find the 75th percentile (threshold for "loud" samples)
            int[] sortedWindow = soundLevelWindow.clone();
            java.util.Arrays.sort(sortedWindow);
            int percentileIndex = (int) (WINDOW_SIZE * 0.75); // 75th percentile
            int threshold = sortedWindow[percentileIndex];
            
            int adjustedBrightness;
            
            // Check if current sound is above 75th percentile
            if (currentSoundLevel < threshold) {
                return true;
            }
            
            // Scale brightness based on how much above threshold it is
            int spikeAmount = currentSoundLevel - threshold;
            adjustedBrightness = Math.min(255, Math.max(150, spikeAmount * 5)); // Increased min from 50 to 150, multiplier from 3 to 5
            
            // Rotate color hue for this burst
            currentHue = (currentHue + 15) % 255;
            
            // Add multiple pixels at once with larger spots
            for (int i = 0; i < pixelsPerFrame; i++) {
                int randomX = random.nextInt(combinedWidth);
                int randomY = random.nextInt(combinedHeight);
                
                // Create a spot with radius 1-2 pixels
                int spotRadius = 1 + random.nextInt(2);
                for (int dx = -spotRadius; dx <= spotRadius; dx++) {
                    for (int dy = -spotRadius; dy <= spotRadius; dy++) {
                        int px = randomX + dx;
                        int py = randomY + dy;
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (px >= 0 && px < combinedWidth && py >= 0 && py < combinedHeight && distance <= spotRadius) {
                            // Fade brightness based on distance from center
                            double fadeFactor = 1.0 - (distance / spotRadius);
                            int spotBrightness = (int) (adjustedBrightness * fadeFactor);
                            pixelBrightness[px][py] = (byte) Math.max(pixelBrightness[px][py] & 0xFF, spotBrightness);
                            pixelHue[px][py] = (byte) currentHue;
                        }
                    }
                }
            }
            
            audioIndex = (audioIndex + 1) % 16;
            lastPixelTime = now;
        }
        
        // Apply blur effect - disabled for now
        // applyBlur();
        
        return true; // Keep running
    }
    
    private void applyBlur() {
        // Simple box blur approximation
        byte[][] blurred = new byte[combinedWidth][combinedHeight];
        
        for (int x = 0; x < combinedWidth; x++) {
            for (int y = 0; y < combinedHeight; y++) {
                int sum = 0;
                int count = 0;
                
                // Average neighboring pixels (3x3 kernel)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < combinedWidth && ny >= 0 && ny < combinedHeight) {
                            sum += pixelBrightness[nx][ny] & 0xFF;
                            count++;
                        }
                    }
                }
                
                if (count > 0) {
                    int avg = sum / count;
                    // Blend original with blurred based on intensity
                    int original = pixelBrightness[x][y] & 0xFF;
                    int blended = (original * (255 - blurIntensity) + avg * blurIntensity) / 255;
                    blurred[x][y] = (byte) blended;
                } else {
                    blurred[x][y] = pixelBrightness[x][y];
                }
            }
        }
        
        // Copy back
        for (int x = 0; x < combinedWidth; x++) {
            System.arraycopy(blurred[x], 0, pixelBrightness[x], 0, combinedHeight);
        }
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        int brightness = pixelBrightness[x][y] & 0xFF;
        int hue = pixelHue[x][y] & 0xFF;
        
        // Convert HSV to RGB
        // Hue is 0-255 representing 0-360 degrees
        // Saturation and Value are both 255 (full saturation, brightness controlled by pixel brightness)
        
        // Calculate RGB from HSV
        double h = (hue / 255.0) * 360.0;
        double s = 1.0;
        double v = brightness / 255.0;
        
        double c = v * s;
        double x_h = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = v - c;
        
        double r = 0, g = 0, b = 0;
        
        if (h >= 0 && h < 60) {
            r = c; g = x_h; b = 0;
        } else if (h >= 60 && h < 120) {
            r = x_h; g = c; b = 0;
        } else if (h >= 120 && h < 180) {
            r = 0; g = c; b = x_h;
        } else if (h >= 180 && h < 240) {
            r = 0; g = x_h; b = c;
        } else if (h >= 240 && h < 300) {
            r = x_h; g = 0; b = c;
        } else {
            r = c; g = 0; b = x_h;
        }
        
        // Apply brightness boost (multiply by 1.5x and cap at 255)
        int rFinal = (int) Math.round((r + m) * 255);
        int gFinal = (int) Math.round((g + m) * 255);
        int bFinal = (int) Math.round((b + m) * 255);
        
        // Boost brightness by 1.5x for brighter colors
        rFinal = Math.min(255, (rFinal * 3) / 2);
        gFinal = Math.min(255, (gFinal * 3) / 2);
        bFinal = Math.min(255, (bFinal * 3) / 2);
        
        return new int[]{rFinal, gFinal, bFinal};
    }
    
    @Override
    public String getName() {
        return "Blurz";
    }
    
    /**
     * Cleanup method to close microphone when animation stops
     */
    public void cleanup() {
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
            System.out.println("Microphone closed");
        }
    }
    
    /**
     * Get the current sound level (0-255)
     */
    public int getSoundLevel() {
        return currentSoundLevel;
    }
    
    /**
     * Check if microphone is active
     */
    public boolean isMicrophoneActive() {
        return microphone != null && microphone.isOpen();
    }
}
