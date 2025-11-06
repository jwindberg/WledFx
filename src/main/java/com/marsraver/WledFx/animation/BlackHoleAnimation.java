package com.marsraver.WledFx.animation;

/**
 * Black Hole animation - Orbiting stars around a central point
 * Based on WLED mode_2DBlackHole by Stepko, Modified by Andrew Tuline
 */
public class BlackHoleAnimation implements LedAnimation {
    
    private int combinedWidth;
    private int combinedHeight;
    
    // Pixel state: RGB values per pixel
    private int[][][] pixelColors; // [x][y][rgb] = 0=R, 1=G, 2=B
    
    // Animation parameters
    private int speed = 128;        // 0-255, affects fade rate
    private int intensity = 128;    // 0-255, affects outer Y frequency
    private int custom1 = 128;      // Outer X frequency
    private int custom2 = 128;      // Inner X frequency
    private int custom3 = 128;      // Inner Y frequency
    private boolean solid = false;  // check1 - solid color mode
    private boolean blur = false;   // check3 - blur mode
    
    // Time tracking
    private long startTime;
    
    /**
     * Fast 8-bit sine function (beatsin8_t equivalent)
     * Returns sine wave oscillating between min and max
     * frequency: beats per minute (0-255), controls oscillation speed
     * phaseOffset: 0-255, represents phase shift (128 = 180 degrees, 64 = 90 degrees)
     * timebase: time value (from strip.now/128 in original)
     */
    private int beatsin8(int frequency, int min, int max, int phaseOffset, long timebase) {
        // Convert phaseOffset (0-255) to radians (0-255 maps to 0-2π)
        // Add timebase with frequency scaling
        double phase = (timebase * frequency / 255.0) + (phaseOffset / 255.0 * 2 * Math.PI);
        double sine = Math.sin(phase);
        // Map from [-1, 1] to [min, max]
        int range = max - min;
        return min + (int) ((sine + 1.0) * range / 2.0);
    }
    
    /**
     * Get color from simple HSV palette (rainbow)
     */
    private int[] colorFromPalette(int hue, boolean wrap, int brightness) {
        // Simple rainbow palette implementation
        if (!wrap) {
            hue = hue % 256;
        }
        float h = (hue % 256) / 255.0f * 360.0f;
        float s = 1.0f;
        float v = brightness / 255.0f;
        
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
        this.startTime = System.currentTimeMillis();
    }
    
    @Override
    public boolean update(long now) {
        long timeMs = System.currentTimeMillis() - startTime;
        
        // Fade all pixels to black: 16 + (speed>>3)
        // Reduce fade amount to create longer, more visible trails
        int fadeAmount = Math.max(2, (16 + (speed >> 3)) / 4); // Divide by 4 for longer trails
        fadeToBlack(fadeAmount);
        
        // Timebase: strip.now/128
        long t = timeMs / 128;
        
        // Outer stars (8 of them)
        for (int i = 0; i < 8; i++) {
            int phaseOffsetX = ((i % 2) == 1) ? 128 : 0;
            int phaseOffsetY = ((i % 2) == 1) ? 192 : 64;
            int x = beatsin8(custom1 >> 3, 0, combinedWidth - 1, phaseOffsetX + (int)(t * i), t);
            int y = beatsin8(intensity >> 3, 0, combinedHeight - 1, phaseOffsetY + (int)(t * i), t);
            
            // Get color from palette: i*32, false, solid?0:255
            int paletteIndex = i * 32;
            int brightness = solid ? 0 : 255;
            int[] color = colorFromPalette(paletteIndex, false, brightness);
            addPixelColor(x, y, color);
        }
        
        // Inner stars (4 of them)
        // For circular orbits, X and Y need the same frequency with 90-degree phase offset
        // Use custom2>>3 for frequency, but ensure X and Y have proper phase offset for circles
        int innerFreq = custom2 >> 3;
        for (int i = 0; i < 4; i++) {
            int phaseOffsetX = ((i % 2) == 1) ? 128 : 0;
            int phaseOffsetY = ((i % 2) == 1) ? 192 : 64; // 64 units = 90 degrees out of phase with X
            int x = beatsin8(innerFreq, combinedWidth/4, combinedWidth - 1 - combinedWidth/4, 
                            phaseOffsetX + (int)(t * i), t);
            int y = beatsin8(innerFreq, combinedHeight/4, combinedHeight - 1 - combinedHeight/4, 
                            phaseOffsetY + (int)(t * i), t);
            
            // Get color from palette: 255-i*64, false, solid?0:255
            int paletteIndex = 255 - i * 64;
            int brightness = solid ? 0 : 255;
            int[] color = colorFromPalette(paletteIndex, false, brightness);
            addPixelColor(x, y, color);
        }
        
        // Central white dot
        int centerX = combinedWidth / 2;
        int centerY = combinedHeight / 2;
        setPixelColor(centerX, centerY, new int[]{255, 255, 255});
        
        // Blur everything if enabled
        if (blur) {
            int blurAmount = 16;
            boolean useSmear = (combinedWidth * combinedHeight) < 100;
            applyBlur(blurAmount, useSmear);
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
        return "Black Hole";
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
    
    private void applyBlur(int amount, boolean smear) {
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
    
    private void addPixelColor(int x, int y, int[] rgb) {
        if (x >= 0 && x < combinedWidth && y >= 0 && y < combinedHeight) {
            pixelColors[x][y][0] = Math.min(255, pixelColors[x][y][0] + rgb[0]);
            pixelColors[x][y][1] = Math.min(255, pixelColors[x][y][1] + rgb[1]);
            pixelColors[x][y][2] = Math.min(255, pixelColors[x][y][2] + rgb[2]);
        }
    }
}
