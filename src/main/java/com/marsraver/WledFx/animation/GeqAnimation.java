package com.marsraver.WledFx.animation;

import javax.sound.sampled.*;
import java.util.Random;

/**
 * GEQ (Graphic Equalizer) animation - 2D frequency band visualization
 * Based on WLED mode_2DGEQ by Will Tatam, code reduction by Ewoud Wijma
 */
public class GeqAnimation implements LedAnimation {
    
    private static final Random random = new Random();
    private int combinedWidth;
    private int combinedHeight;
    
    // Pixel colors: RGB values
    private int[][][] pixelColors;
    
    // Configuration parameters
    private int numBands = 16;          // Number of frequency bands (1-16)
    private int centerBin = 0;          // Center frequency bin (0-15)
    private int speed = 128;            // Fade speed (0-255, higher = faster fade)
    private int intensity = 128;        // Ripple decay time (256 - intensity)
    private boolean colorBars = false;  // Toggle for vertical color gradient (check1)
    private int[] peakColor = {255, 255, 255}; // Peak indicator color (white)
    private int noiseFloor = 50;        // Minimum FFT value before bars appear (subtracted from FFT)
    private int maxFFTValue = 255;      // FFT value that maps to max bar height (increased for louder music)
    
    // Previous bar heights for each column (for peak tracking)
    private int[] previousBarHeight;
    
    // Ripple timing
    private long lastRippleTime = 0;
    private long callCount = 0;
    
    // FFT data - 16 frequency bands
    private int[] fftResult = new int[16];
    
    // Microphone capture for real audio
    private TargetDataLine microphone;
    private byte[] audioBuffer;
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.pixelColors = new int[combinedWidth][combinedHeight][3];
        this.previousBarHeight = new int[combinedWidth];
        this.lastRippleTime = 0;
        this.callCount = 0;
        
        // Initialize previous bar heights
        for (int i = 0; i < combinedWidth; i++) {
            previousBarHeight[i] = 0;
        }
        
        // Try to start microphone capture
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                audioBuffer = new byte[4096];
                System.out.println("GEQ: Microphone initialized successfully");
            } else {
                System.err.println("GEQ: Microphone not supported");
            }
        } catch (Exception e) {
            System.err.println("GEQ: Failed to initialize microphone: " + e.getMessage());
        }
    }
    
    /**
     * Update FFT data from microphone or simulate it
     */
    private void updateFFT() {
        if (microphone != null && microphone.isOpen()) {
            try {
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead > 0) {
                    // Simplified FFT simulation - divide audio into 16 bands
                    int samplesPerBand = bytesRead / 2 / 16;
                    
                    for (int band = 0; band < 16; band++) {
                        long sum = 0;
                        int count = 0;
                        int startIdx = band * samplesPerBand * 2;
                        int endIdx = Math.min(startIdx + samplesPerBand * 2, bytesRead);
                        
                        for (int i = startIdx; i < endIdx; i += 2) {
                            int sample = (int) audioBuffer[i] | ((int) audioBuffer[i + 1] << 8);
                            sum += sample * sample;
                            count++;
                        }
                        
                        if (count > 0) {
                            double rms = Math.sqrt(sum / count);
                            // Scale audio more aggressively for louder music response
                            // Use / 180.0 for better response to loud music
                            fftResult[band] = (int) Math.min(255, rms / 180.0);
                        } else {
                            fftResult[band] = 0;
                        }
                    }
                } else {
                    // No audio data, fade out
                    for (int i = 0; i < 16; i++) {
                        fftResult[i] = Math.max(0, fftResult[i] - 5);
                    }
                }
            } catch (Exception e) {
                // Error reading audio, use simulated data
                simulateFFT();
            }
        } else {
            // No microphone, simulate FFT data
            simulateFFT();
        }
    }
    
    /**
     * Simulate FFT data with animated variations
     */
    private void simulateFFT() {
        long time = System.currentTimeMillis();
        for (int i = 0; i < 16; i++) {
            double phase = time / 80.0 + i * 0.5;
            // Reduced base level for quieter simulation when no real audio
            // Base around 30-40 (below noise floor), with variations up to 150-180
            double value = Math.sin(phase) * 60 + 70;  // Base around 70, swing +/-60
            value += Math.sin(phase * 2.3) * 30;
            value += Math.sin(phase * 0.7 + i) * 20;
            value += Math.sin(phase * 3.1 + i * 2) * 15;  // Additional variation
            // Keep values lower for simulated data - only go up to ~150 max
            fftResult[i] = (int) Math.max(0, Math.min(255, value));
        }
    }
    
    /**
     * Map value from one range to another (like Arduino map())
     */
    private int map(int value, int inMin, int inMax, int outMin, int outMax) {
        if (inMax == inMin) return outMin;
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }
    
    /**
     * Constrain value to range (like Arduino constrain())
     */
    private int constrain(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Fade all pixels to black
     */
    private void fadeToBlack(int fadeAmount) {
        fadeAmount = Math.min(255, Math.max(0, fadeAmount));
        for (int x = 0; x < combinedWidth; x++) {
            for (int y = 0; y < combinedHeight; y++) {
                for (int c = 0; c < 3; c++) {
                    int faded = pixelColors[x][y][c] * (255 - fadeAmount) / 255;
                    pixelColors[x][y][c] = Math.max(0, Math.min(255, faded));
                }
            }
        }
    }
    
    /**
     * Get color from palette (simple rainbow implementation)
     */
    private int[] colorFromPalette(int colorIndex, boolean wrap) {
        // Wrap color index to 0-255 range
        if (!wrap) {
            colorIndex = colorIndex % 256;
        }
        if (colorIndex < 0) colorIndex += 256;
        colorIndex = colorIndex % 256;
        
        // Convert to HSV and then RGB (rainbow palette)
        float h = (colorIndex / 255.0f) * 360.0f;
        float s = 1.0f;
        float v = 1.0f;
        
        return hsvToRgb(h, s, v);
    }
    
    /**
     * Convert HSV to RGB
     */
    private int[] hsvToRgb(float h, float s, float v) {
        int hi = (int) (h / 60.0f) % 6;
        float f = (h / 60.0f) - hi;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        
        float r, g, b;
        switch (hi) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        
        return new int[]{
            Math.round(r * 255),
            Math.round(g * 255),
            Math.round(b * 255)
        };
    }
    
    /**
     * Set pixel color
     */
    private void setPixelColor(int x, int y, int[] rgb) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            // Clamp RGB values to 0-255 range before storing
            pixelColors[x][y][0] = Math.max(0, Math.min(255, rgb[0]));
            pixelColors[x][y][1] = Math.max(0, Math.min(255, rgb[1]));
            pixelColors[x][y][2] = Math.max(0, Math.min(255, rgb[2]));
        }
    }
    
    @Override
    public boolean update(long now) {
        callCount++;
        
        // Update FFT data
        updateFFT();
        
        // Check for ripple time
        boolean rippleTime = false;
        long rippleInterval = 256 - intensity;
        if (rippleInterval < 1) rippleInterval = 1;
        
        if (now - lastRippleTime >= rippleInterval * 1_000_000L) { // Convert to nanoseconds
            lastRippleTime = now;
            rippleTime = true;
        }
        
        // Apply fade to black
        int fadeoutDelay = (256 - speed) / 64;
        if (fadeoutDelay <= 1 || (callCount % fadeoutDelay == 0)) {
            fadeToBlack(speed);
        }
        
        // Draw bars for each column
        for (int x = 0; x < combinedWidth; x++) {
            // Map column to frequency band
            int band = map(x, 0, combinedWidth, 0, numBands);
            
            // Handle band selection based on numBands and centerBin
            if (numBands < 16) {
                int startBin = constrain(centerBin - numBands / 2, 0, 15 - numBands + 1);
                if (numBands <= 1) {
                    band = centerBin;
                } else {
                    band = map(band, 0, numBands - 1, startBin, startBin + numBands - 1);
                }
            }
            
            band = constrain(band, 0, 15);
            
            // Get color index for this band (band * 17 gives good spread across palette)
            int colorIndex = band * 17;
            
            // Map FFT value to bar height (0 to rows)
            // Apply noise floor - subtract threshold so quiet rooms show no bars
            int adjustedFFT = Math.max(0, fftResult[band] - noiseFloor);
            // Boost adjusted FFT to make bars taller - multiply by 2 for roughly 2x height
            int boostedFFT = adjustedFFT * 2;
            // Map from (0, maxFFTValue-noiseFloor) to (0, combinedHeight), but with boosted values
            // Cap boosted FFT at effectiveMax to prevent overflow, but bars will be taller
            int effectiveMax = maxFFTValue - noiseFloor;
            int barHeight = 0;
            if (boostedFFT > 0) {
                // Map boosted FFT, but cap it so we don't exceed screen height
                int cappedBoosted = Math.min(boostedFFT, effectiveMax);
                barHeight = map(cappedBoosted, 0, effectiveMax, 0, combinedHeight);
                barHeight = Math.min(combinedHeight, Math.max(0, barHeight));
            }
            
            // Drive peak up (only increase, don't decrease immediately)
            if (barHeight > previousBarHeight[x]) {
                previousBarHeight[x] = barHeight;
            }
            
            // Draw the bar from bottom up
            int[] ledColor = new int[]{0, 0, 0}; // BLACK
            for (int y = 0; y < barHeight; y++) {
                // If color bars mode, use vertical gradient
                if (colorBars) {
                    colorIndex = map(y, 0, combinedHeight - 1, 0, 255);
                }
                
                ledColor = colorFromPalette(colorIndex, false);
                
                // Set pixel (rows-1 - y to draw from bottom)
                setPixelColor(x, combinedHeight - 1 - y, ledColor);
            }
            
            // Draw peak indicator at top
            if (previousBarHeight[x] > 0) {
                setPixelColor(x, combinedHeight - previousBarHeight[x], peakColor);
            }
            
            // Ripple effect - decay peak height
            if (rippleTime && previousBarHeight[x] > 0) {
                previousBarHeight[x]--;
            }
        }
        
        return true;
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            int[] color = pixelColors[x][y].clone();
            // Clamp RGB values to 0-255 range
            color[0] = Math.max(0, Math.min(255, color[0]));
            color[1] = Math.max(0, Math.min(255, color[1]));
            color[2] = Math.max(0, Math.min(255, color[2]));
            return color;
        }
        return new int[]{0, 0, 0};
    }
    
    @Override
    public String getName() {
        return "GEQ";
    }
    
    /**
     * Cleanup method to close microphone when animation stops
     */
    public void cleanup() {
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
            System.out.println("GEQ: Microphone closed");
        }
    }
}
