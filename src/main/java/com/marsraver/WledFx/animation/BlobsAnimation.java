package com.marsraver.WledFx.animation;

import java.util.Random;

/**
 * Floating Blobs animation - Blobs that move around, bounce off edges, and grow/shrink
 * Based on WLED mode_2Dfloatingblobs by stepko, adapted by Blaz Kristan
 */
public class BlobsAnimation implements LedAnimation {
    
    private static final int MAX_BLOBS = 8;
    
    private int combinedWidth;
    private int combinedHeight;
    
    // Pixel state: RGB values per pixel
    private int[][][] pixelColors; // [x][y][rgb] = 0=R, 1=G, 2=B
    
    // Animation parameters
    private int speed = 128;        // 0-255, affects movement speed
    private int intensity = 128;    // 0-255, affects number of blobs: (intensity>>5) + 1
    private int custom1 = 32;       // Blur amount
    private int custom2 = 32;       // Fade/trail amount
    
    // Blob data
    private static class Blob {
        float x, y;
        float sX, sY; // speed
        float r;      // radius
        boolean grow;
        int color;    // color index (0-255)
    }
    
    private Blob[] blobs;
    private int amount;
    private Random random;
    private long startTime;
    private long lastColorChange;
    
    /**
     * Get color from simple HSV palette (rainbow)
     */
    private int[] colorFromPalette(int hue, boolean wrap, boolean blend, int brightness) {
        // Simple rainbow palette implementation
        if (!wrap) {
            hue = hue % 256;
        }
        float h = (hue % 256) / 255.0f * 360.0f;
        float s = 1.0f;
        float v = brightness == 0 ? 1.0f : brightness / 255.0f;
        
        return hsvToRgb(h, s, v);
    }
    
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
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.pixelColors = new int[combinedWidth][combinedHeight][3];
        this.random = new Random();
        this.startTime = System.currentTimeMillis();
        this.lastColorChange = startTime;
        
        // Calculate number of blobs: (intensity>>5) + 1
        this.amount = Math.min(MAX_BLOBS, (intensity >> 5) + 1);
        this.blobs = new Blob[MAX_BLOBS];
        
        // Initialize blobs
        for (int i = 0; i < MAX_BLOBS; i++) {
            Blob blob = new Blob();
            
            // Random radius between 1 and cols/4 (minimum 2)
            int maxR = Math.max(2, combinedWidth / 4);
            blob.r = random.nextFloat() * (maxR - 1) + 1;
            
            // Speed based on random and speed parameter
            float speedDiv = Math.max(1, 256 - speed);
            blob.sX = (random.nextInt(combinedWidth - 3) + 3) / speedDiv;
            blob.sY = (random.nextInt(combinedHeight - 3) + 3) / speedDiv;
            
            // Random starting position
            blob.x = random.nextInt(combinedWidth);
            blob.y = random.nextInt(combinedHeight);
            
            // Random color
            blob.color = random.nextInt(256);
            
            // Grow flag: true if radius is very small
            blob.grow = blob.r < 1.0f;
            
            // Ensure non-zero speeds
            if (blob.sX == 0) blob.sX = 1;
            if (blob.sY == 0) blob.sY = 1;
            
            blobs[i] = blob;
        }
    }
    
    @Override
    public boolean update(long now) {
        long currentTime = System.currentTimeMillis();
        
        // Fade all pixels: (custom2>>3) + 1
        int fadeAmount = (custom2 >> 3) + 1;
        fadeToBlack(fadeAmount);
        
        // Slowly change colors every 2 seconds
        if (currentTime - lastColorChange >= 2000) {
            for (int i = 0; i < amount; i++) {
                blobs[i].color = (blobs[i].color + 4) % 256;
            }
            lastColorChange = currentTime;
        }
        
        // Update and draw blobs
        for (int i = 0; i < amount; i++) {
            Blob blob = blobs[i];
            
            // Change radius (grow or shrink)
            float speedFactor = Math.max(Math.abs(blob.sX), Math.abs(blob.sY));
            if (blob.grow) {
                // Enlarge radius until it is >= min(cols/4, 2)
                blob.r += speedFactor * 0.05f;
                float maxRadius = Math.min(combinedWidth / 4.0f, 2.0f);
                if (blob.r >= maxRadius) {
                    blob.grow = false;
                }
            } else {
                // Reduce radius until it is < 1
                blob.r -= speedFactor * 0.05f;
                if (blob.r < 1.0f) {
                    blob.grow = true;
                }
            }
            
            // Get color from palette
            int[] c = colorFromPalette(blob.color, false, false, 0);
            
            // Draw blob (circle if radius > 1, single pixel otherwise)
            int xPos = Math.round(blob.x);
            int yPos = Math.round(blob.y);
            if (blob.r > 1.0f) {
                fillCircle(xPos, yPos, Math.round(blob.r), c);
            } else {
                setPixelColor(xPos, yPos, c);
            }
            
            // Move x
            if (blob.x + blob.r >= combinedWidth - 1) {
                blob.x += blob.sX * ((combinedWidth - 1 - blob.x) / blob.r + 0.005f);
            } else if (blob.x - blob.r <= 0) {
                blob.x += blob.sX * (blob.x / blob.r + 0.005f);
            } else {
                blob.x += blob.sX;
            }
            
            // Move y
            if (blob.y + blob.r >= combinedHeight - 1) {
                blob.y += blob.sY * ((combinedHeight - 1 - blob.y) / blob.r + 0.005f);
            } else if (blob.y - blob.r <= 0) {
                blob.y += blob.sY * (blob.y / blob.r + 0.005f);
            } else {
                blob.y += blob.sY;
            }
            
            // Bounce x
            if (blob.x < 0.01f) {
                float speedDiv = Math.max(1, 256 - speed);
                blob.sX = (random.nextInt(combinedWidth - 3) + 3) / speedDiv;
                blob.x = 0.01f;
            } else if (blob.x > combinedWidth - 1.01f) {
                float speedDiv = Math.max(1, 256 - speed);
                blob.sX = (random.nextInt(combinedWidth - 3) + 3) / speedDiv;
                blob.sX = -blob.sX;
                blob.x = combinedWidth - 1.01f;
            }
            
            // Bounce y
            if (blob.y < 0.01f) {
                float speedDiv = Math.max(1, 256 - speed);
                blob.sY = (random.nextInt(combinedHeight - 3) + 3) / speedDiv;
                blob.y = 0.01f;
            } else if (blob.y > combinedHeight - 1.01f) {
                float speedDiv = Math.max(1, 256 - speed);
                blob.sY = (random.nextInt(combinedHeight - 3) + 3) / speedDiv;
                blob.sY = -blob.sY;
                blob.y = combinedHeight - 1.01f;
            }
        }
        
        // Apply blur
        int blurAmount = custom1 >> 2;
        if (blurAmount > 0) {
            applyBlur(blurAmount);
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
        return "Blobs";
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
    
    private void fillCircle(int centerX, int centerY, int radius, int[] rgb) {
        if (radius <= 0) {
            setPixelColor(centerX, centerY, rgb);
            return;
        }
        
        // Draw filled circle using distance check
        int minX = Math.max(0, centerX - radius);
        int maxX = Math.min(combinedWidth - 1, centerX + radius);
        int minY = Math.max(0, centerY - radius);
        int maxY = Math.min(combinedHeight - 1, centerY + radius);
        
        float radiusSq = radius * radius;
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                int dx = x - centerX;
                int dy = y - centerY;
                float distSq = dx * dx + dy * dy;
                
                if (distSq <= radiusSq) {
                    addPixelColor(x, y, rgb);
                }
            }
        }
    }
    
    private void addPixelColor(int x, int y, int[] rgb) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            pixelColors[x][y][0] = Math.min(255, pixelColors[x][y][0] + rgb[0]);
            pixelColors[x][y][1] = Math.min(255, pixelColors[x][y][1] + rgb[1]);
            pixelColors[x][y][2] = Math.min(255, pixelColors[x][y][2] + rgb[2]);
        }
    }
}
