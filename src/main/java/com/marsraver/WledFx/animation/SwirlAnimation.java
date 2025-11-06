package com.marsraver.WledFx.animation;

import javax.sound.sampled.*;

/**
 * Swirl animation - Audio-reactive swirling pixels with mirrored patterns
 * Based on WLED mode_2DSwirl by Mark Kriegsman, modified by Andrew Tuline
 */
public class SwirlAnimation implements LedAnimation {
    
    private int combinedWidth;
    private int combinedHeight;
    
    // Pixel colors: RGB values
    private int[][][] pixelColors;
    
    // Configuration parameters
    private int speed = 128;            // Controls swirl speed
    private int intensity = 64;         // Controls brightness sensitivity
    private int custom1 = 16;           // Controls blur amount
    private int fadeAmount = 4;         // Gentle fade to prevent white saturation (very light fade)
    
    private static final int BORDER_WIDTH = 2;
    private static final int NOISE_FLOOR = 100;  // Minimum audio level to display pixels (very aggressive to filter ambient noise)
    
    // Audio data for reactivity
    private TargetDataLine microphone;
    private byte[] audioBuffer;
    private float volumeSmth = 0.0f;    // Smoothed volume (0.0-1.0 range)
    private int volumeRaw = 0;          // Raw volume (0-255 range)
    private int smoothedVolumeRaw = 0;  // Smoothed raw volume to prevent brief spikes
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.pixelColors = new int[combinedWidth][combinedHeight][3];
        
        // Initialize to black
        for (int x = 0; x < combinedWidth; x++) {
            for (int y = 0; y < combinedHeight; y++) {
                pixelColors[x][y][0] = 0;
                pixelColors[x][y][1] = 0;
                pixelColors[x][y][2] = 0;
            }
        }
        
        // Try to start microphone capture for audio reactivity
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                audioBuffer = new byte[4096];
                System.out.println("Swirl: Microphone initialized successfully");
            } else {
                System.err.println("Swirl: Microphone not supported");
            }
        } catch (Exception e) {
            System.err.println("Swirl: Failed to initialize microphone: " + e.getMessage());
        }
    }
    
    /**
     * Update audio data for reactivity
     */
    private void updateAudio() {
        if (microphone != null && microphone.isOpen()) {
            try {
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead > 0) {
                    // Calculate RMS (root mean square) for volume
                    long sum = 0;
                    int count = 0;
                    for (int i = 0; i < bytesRead; i += 2) {
                        int sample = (int) audioBuffer[i] | ((int) audioBuffer[i + 1] << 8);
                        sum += sample * sample;
                        count++;
                    }
                    
                    if (count > 0) {
                        double rms = Math.sqrt(sum / count);
                        // Normalize to 0-1 range for smoothed volume
                        volumeSmth = (float) Math.min(1.0, rms / 10000.0);
                        // Raw volume in 0-255 range
                        // Increase divisor to make values smaller (less sensitive)
                        int currentRaw = (int) Math.min(255, rms / 60.0);
                        // Apply exponential moving average to smooth out brief spikes
                        // Use 5% of new value + 95% of old value for very aggressive smoothing
                        smoothedVolumeRaw = (smoothedVolumeRaw * 19 + currentRaw) / 20;
                        volumeRaw = smoothedVolumeRaw;
                    } else {
                        volumeSmth = 0.0f;
                        volumeRaw = 0;
                        smoothedVolumeRaw = 0;
                    }
                }
            } catch (Exception e) {
                // Error reading audio, set to quiet and decay smoothed value
                volumeSmth = 0.0f;
                smoothedVolumeRaw = (smoothedVolumeRaw * 19) / 20; // Decay smoothed value (match smoothing ratio)
                volumeRaw = smoothedVolumeRaw;
            }
        } else {
            // No microphone, set to quiet (no simulation - quiet room should be black)
            volumeSmth = 0.0f;
            volumeRaw = 0;
            smoothedVolumeRaw = 0;
        }
    }
    
    /**
     * Fast 8-bit sine function (beatsin8_t equivalent)
     * Returns sine wave oscillating between min and max
     * frequency: beats per minute (0-255), controls oscillation speed
     */
    private int beatsin8(int frequency, int min, int max, long timebase) {
        double phase = timebase * frequency / 255.0;
        double sine = Math.sin(phase);
        // Map from [-1, 1] to [min, max]
        int range = max - min;
        return min + (int) ((sine + 1.0) * range / 2.0);
    }
    
    /**
     * Fade all pixels slightly to prevent white saturation
     * @param amount Amount to fade (0-255), higher = more aggressive fade
     */
    private void fadeToBlack(int amount) {
        if (amount <= 0) return;
        if (amount >= 255) {
            // Complete fade to black
            for (int x = 0; x < combinedWidth; x++) {
                for (int y = 0; y < combinedHeight; y++) {
                    pixelColors[x][y][0] = 0;
                    pixelColors[x][y][1] = 0;
                    pixelColors[x][y][2] = 0;
                }
            }
            return;
        }
        
        // Fade each pixel by multiplying by (255 - amount) / 255
        int fadeFactor = 255 - amount;
        for (int x = 0; x < combinedWidth; x++) {
            for (int y = 0; y < combinedHeight; y++) {
                pixelColors[x][y][0] = (pixelColors[x][y][0] * fadeFactor) / 255;
                pixelColors[x][y][1] = (pixelColors[x][y][1] * fadeFactor) / 255;
                pixelColors[x][y][2] = (pixelColors[x][y][2] * fadeFactor) / 255;
            }
        }
    }
    
    /**
     * Apply blur effect
     */
    private void applyBlur(int amount) {
        if (amount <= 0) return;
        
        // Simple box blur
        int[][][] temp = new int[combinedWidth][combinedHeight][3];
        
        for (int x = 0; x < combinedWidth; x++) {
            for (int y = 0; y < combinedHeight; y++) {
                for (int c = 0; c < 3; c++) {
                    int sum = 0;
                    int count = 0;
                    
                    // Average with neighbors (1-pixel radius)
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < combinedWidth && ny >= 0 && ny < combinedHeight) {
                                sum += pixelColors[nx][ny][c];
                                count++;
                            }
                        }
                    }
                    
                    if (count > 0) {
                        int avg = sum / count;
                        // Blend original with blurred based on amount
                        int original = pixelColors[x][y][c];
                        temp[x][y][c] = (original * (255 - amount) + avg * amount) / 255;
                    } else {
                        temp[x][y][c] = pixelColors[x][y][c];
                    }
                }
            }
        }
        
        // Copy back
        for (int x = 0; x < combinedWidth; x++) {
            for (int y = 0; y < combinedHeight; y++) {
                System.arraycopy(temp[x][y], 0, pixelColors[x][y], 0, 3);
            }
        }
    }
    
    /**
     * Add color to pixel (additive blending)
     */
    private void addPixelColor(int x, int y, int[] rgb) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            pixelColors[x][y][0] = Math.min(255, pixelColors[x][y][0] + rgb[0]);
            pixelColors[x][y][1] = Math.min(255, pixelColors[x][y][1] + rgb[1]);
            pixelColors[x][y][2] = Math.min(255, pixelColors[x][y][2] + rgb[2]);
        }
    }
    
    /**
     * Get color from palette with brightness control
     */
    private int[] colorFromPalette(int colorIndex, int brightness) {
        // Wrap color index to 0-255 range
        colorIndex = colorIndex % 256;
        if (colorIndex < 0) colorIndex += 256;
        
        // Convert to HSV and then RGB (rainbow palette)
        float h = (colorIndex / 255.0f) * 360.0f;
        float s = 1.0f;
        float v = brightness / 255.0f;  // Use brightness parameter
        
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
    
    @Override
    public boolean update(long now) {
        // Update audio data
        updateAudio();
        
        // If audio is below noise floor, fade to black and don't add new pixels
        if (volumeRaw < NOISE_FLOOR) {
            // Fade existing pixels to black gradually
            fadeToBlack(15); // More aggressive fade when quiet
            // Apply blur even when quiet to smooth the fade
            if (custom1 > 0) {
                applyBlur(custom1);
            }
            return true;
        }
        
        // Gentle fade to prevent white saturation when music is playing
        fadeToBlack(fadeAmount);
        
        // Apply blur: custom1
        if (custom1 > 0) {
            applyBlur(custom1);
        }
        
        // Timebase in milliseconds (similar to strip.now)
        long timeMs = now / 1_000_000; // Convert nanoseconds to milliseconds
        
        // Calculate positions using sine waves with speed-based frequencies
        // Frequency is scaled by speed: 27*speed/255 and 41*speed/255
        int freq1 = (27 * speed) / 255;
        int freq2 = (41 * speed) / 255;
        if (freq1 < 1) freq1 = 1;
        if (freq2 < 1) freq2 = 1;
        
        int i = beatsin8(freq1, BORDER_WIDTH, combinedWidth - BORDER_WIDTH, timeMs);
        int j = beatsin8(freq2, BORDER_WIDTH, combinedHeight - BORDER_WIDTH, timeMs);
        
        // Mirrored positions
        int ni = (combinedWidth - 1) - i;
        int nj = (combinedHeight - 1) - j;
        
        // Calculate color index and brightness based on time and audio
        // Color index = (strip.now / divisor + volumeSmth*4)
        // Brightness = volumeRaw * intensity / 64
        // Boost brightness significantly - use volumeRaw as a multiplier, not the main brightness
        int baseBrightness = 200; // Base brightness level (fairly bright)
        int audioBoost = volumeRaw / 2; // Add audio-based boost
        int brightness = Math.min(255, baseBrightness + audioBoost);
        brightness = Math.max(150, brightness); // Ensure minimum brightness of 150
        
        // Add 6 pixels at different mirrored/swapped positions
        // Each with different time divisors for color variation
        int[] color1 = colorFromPalette((int)(timeMs / 11 + volumeSmth * 4), brightness);
        addPixelColor(i, j, color1);
        
        int[] color2 = colorFromPalette((int)(timeMs / 13 + volumeSmth * 4), brightness);
        addPixelColor(j, i, color2);  // Swapped X and Y
        
        int[] color3 = colorFromPalette((int)(timeMs / 17 + volumeSmth * 4), brightness);
        addPixelColor(ni, nj, color3);  // Mirrored
        
        int[] color4 = colorFromPalette((int)(timeMs / 29 + volumeSmth * 4), brightness);
        addPixelColor(nj, ni, color4);  // Swapped and mirrored
        
        int[] color5 = colorFromPalette((int)(timeMs / 37 + volumeSmth * 4), brightness);
        addPixelColor(i, nj, color5);  // Mirrored vertically
        
        int[] color6 = colorFromPalette((int)(timeMs / 41 + volumeSmth * 4), brightness);
        addPixelColor(ni, j, color6);  // Mirrored horizontally
        
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
        return "Swirl";
    }
    
    /**
     * Cleanup method to close microphone when animation stops
     */
    public void cleanup() {
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
            System.out.println("Swirl: Microphone closed");
        }
    }
}
