package com.marsraver.WledFx.animation;

import java.util.Random;

/**
 * Crazy Bees animation - Bees flying to random targets with flowers
 * Based on WLED mode_2Dcrazybees by stepko, adapted by Blaz Kristan
 */
public class CrazyBeesAnimation implements LedAnimation {
    
    private static final Random random = new Random();
    private static final int MAX_BEES = 5;
    
    private int combinedWidth;
    private int combinedHeight;
    
    // Pixel state: RGB values per pixel
    private int[][][] pixelColors; // [x][y][rgb] = 0=R, 1=G, 2=B
    
    // Bee structure
    private static class Bee {
        int posX, posY;      // Current position
        int aimX, aimY;      // Target position
        int hue;             // Color hue (0-255)
        int deltaX, deltaY;  // Distance to target
        int signX, signY;    // Direction (-1 or 1)
        int error;           // Bresenham error term
        
        void setAim(int w, int h) {
            aimX = random.nextInt(w);
            aimY = random.nextInt(h);
            hue = random.nextInt(256);
            deltaX = Math.abs(aimX - posX);
            deltaY = Math.abs(aimY - posY);
            signX = posX < aimX ? 1 : -1;
            signY = posY < aimY ? 1 : -1;
            error = deltaX - deltaY;
        }
    }
    
    private Bee[] bees;
    private int numBees;
    private long lastUpdateTime;
    private long updateInterval; // Frame interval based on speed
    
    // Animation parameters
    private int fadeAmount = 32;
    private int blurAmount = 10;
    private int speed = 128; // 0-255, higher = faster
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.pixelColors = new int[combinedWidth][combinedHeight][3];
        this.lastUpdateTime = 0;
        
        // Calculate number of bees: MIN(MAX_BEES, (rows * cols) / 256 + 1)
        this.numBees = Math.min(MAX_BEES, (combinedWidth * combinedHeight) / 256 + 1);
        this.bees = new Bee[numBees];
        
        // Initialize bees at random positions
        random.setSeed(System.currentTimeMillis());
        for (int i = 0; i < numBees; i++) {
            bees[i] = new Bee();
            bees[i].posX = random.nextInt(combinedWidth);
            bees[i].posY = random.nextInt(combinedHeight);
            bees[i].setAim(combinedWidth, combinedHeight);
        }
        
        // Calculate update interval: FRAMETIME * 16 / ((speed>>4)+1)
        // Assuming 60fps, FRAMETIME = 16ms = 16_000_000ns
        // We'll use a similar formula: 16_000_000 * 16 / ((speed>>4)+1)
        int speedFactor = (speed >> 4) + 1;
        this.updateInterval = 16_000_000L * 16L / speedFactor;
    }
    
    @Override
    public boolean update(long now) {
        // Only update at the specified interval
        if (now - lastUpdateTime < updateInterval) {
            return true;
        }
        lastUpdateTime = now;
        
        // Fade all pixels to black
        fadeToBlack(fadeAmount);
        
        // Apply blur
        applyBlur(blurAmount);
        
        // Update bees
        for (int i = 0; i < numBees; i++) {
            Bee bee = bees[i];
            
            // Draw flower at target (4 pixels around aimX, aimY)
            int[] flowerColor = hsvToRgb(bee.hue, 255, 255); // Full saturation, full brightness
            addPixelColor(bee.aimX + 1, bee.aimY, flowerColor);
            addPixelColor(bee.aimX, bee.aimY + 1, flowerColor);
            addPixelColor(bee.aimX - 1, bee.aimY, flowerColor);
            addPixelColor(bee.aimX, bee.aimY - 1, flowerColor);
            
            // Move bee using Bresenham's line algorithm
            if (bee.posX != bee.aimX || bee.posY != bee.aimY) {
                // Draw bee at current position
                int[] beeColor = hsvToRgb(bee.hue, 153, 255); // 60% saturation = 153/255
                setPixelColor(bee.posX, bee.posY, beeColor);
                
                // Move toward target using Bresenham's algorithm
                int error2 = bee.error * 2;
                if (error2 > -bee.deltaY) {
                    bee.error -= bee.deltaY;
                    bee.posX += bee.signX;
                }
                if (error2 < bee.deltaX) {
                    bee.error += bee.deltaX;
                    bee.posY += bee.signY;
                }
            } else {
                // Reached target, set new target
                bee.setAim(combinedWidth, combinedHeight);
            }
        }
        
        return true;
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            return pixelColors[x][y].clone();
        }
        return new int[]{0, 0, 0};
    }
    
    @Override
    public String getName() {
        return "Crazy Bees";
    }
    
    private void fadeToBlack(int amount) {
        for (int x = 0; x < combinedWidth; x++) {
            for (int y = 0; y < combinedHeight; y++) {
                for (int c = 0; c < 3; c++) {
                    pixelColors[x][y][c] = Math.max(0, pixelColors[x][y][c] - amount);
                }
            }
        }
    }
    
    private void applyBlur(int amount) {
        if (amount <= 0) return;
        
        // Simple box blur: average neighboring pixels
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
                    
                    // Blend original with blurred version
                    int blurred = count > 0 ? sum / count : pixelColors[x][y][c];
                    temp[x][y][c] = (pixelColors[x][y][c] * (255 - amount) + blurred * amount) / 255;
                }
            }
        }
        
        pixelColors = temp;
    }
    
    private void setPixelColor(int x, int y, int[] rgb) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            pixelColors[x][y][0] = Math.min(255, Math.max(0, rgb[0]));
            pixelColors[x][y][1] = Math.min(255, Math.max(0, rgb[1]));
            pixelColors[x][y][2] = Math.min(255, Math.max(0, rgb[2]));
        }
    }
    
    private void addPixelColor(int x, int y, int[] rgb) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            pixelColors[x][y][0] = Math.min(255, pixelColors[x][y][0] + rgb[0]);
            pixelColors[x][y][1] = Math.min(255, pixelColors[x][y][1] + rgb[1]);
            pixelColors[x][y][2] = Math.min(255, pixelColors[x][y][2] + rgb[2]);
        }
    }
    
    private int[] hsvToRgb(int h, int s, int v) {
        // Convert HSV to RGB
        // h: 0-255 (hue)
        // s: 0-255 (saturation)
        // v: 0-255 (value/brightness)
        
        float hue = (h % 256) / 255.0f * 360.0f;
        float saturation = s / 255.0f;
        float value = v / 255.0f;
        
        int hi = (int) (hue / 60.0f) % 6;
        float f = (hue / 60.0f) - hi;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);
        
        float r, g, b;
        switch (hi) {
            case 0: r = value; g = t; b = p; break;
            case 1: r = q; g = value; b = p; break;
            case 2: r = p; g = value; b = t; break;
            case 3: r = p; g = q; b = value; break;
            case 4: r = t; g = p; b = value; break;
            default: r = value; g = p; b = q; break;
        }
        
        return new int[]{
            Math.round(r * 255),
            Math.round(g * 255),
            Math.round(b * 255)
        };
    }
}
