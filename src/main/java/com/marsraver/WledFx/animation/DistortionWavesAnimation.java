package com.marsraver.WledFx.animation;

/**
 * Distortion Waves animation - Wave distortion effects with oscillating centers
 * Based on WLED mode_2Ddistortionwaves by ldirko, adapted by blazoncek
 */
public class DistortionWavesAnimation implements LedAnimation {
    
    private int combinedWidth;
    private int combinedHeight;
    
    // Animation parameters
    private int speed = 128;        // 0-255, affects wave speed
    private int scale = 64;         // 0-255, affects wave scale
    private boolean fill = false;   // check1 - fill mode for palette
    private boolean zoom = false;   // check2 - zoom out mode
    private boolean alt = false;    // check3 - alternate distortion mode
    private int paletteMode = 0;    // 0 = RGB, >0 = palette
    
    // Time tracking
    private long startTime;
    
    // Helper functions to match WLED behavior
    
    /**
     * Fast 8-bit cosine function (cos8_t equivalent)
     * Returns cosine value in range 0-255
     */
    private int cos8(int theta) {
        // theta is 0-255, map to 0-360 degrees
        double radians = (theta / 255.0) * 2 * Math.PI;
        double cosValue = Math.cos(radians);
        // Map from [-1, 1] to [0, 255]
        return (int) ((cosValue + 1.0) * 127.5);
    }
    
    /**
     * Fast sine function for beatsin16_t equivalent
     * Returns sine wave oscillating between min and max
     */
    private int beatsin(int frequency, int min, int max, long timeMs) {
        // frequency affects how fast it oscillates
        double phase = (timeMs * frequency) / 1000.0;
        double sine = Math.sin(phase);
        // Map from [-1, 1] to [min, max]
        int range = max - min;
        return min + (int) ((sine + 1.0) * range / 2.0);
    }
    
    /**
     * Approximate RGB to HSV conversion
     */
    private int[] rgb2hsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;
        
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        
        float h = 0;
        if (delta != 0) {
            if (max == rf) {
                h = 60 * (((gf - bf) / delta) % 6);
            } else if (max == gf) {
                h = 60 * (((bf - rf) / delta) + 2);
            } else {
                h = 60 * (((rf - gf) / delta) + 4);
            }
        }
        if (h < 0) h += 360;
        
        float s = max == 0 ? 0 : delta / max;
        float v = max;
        
        return new int[]{
            (int) (h * 255 / 360),  // hue 0-255
            (int) (s * 255),         // saturation 0-255
            (int) (v * 255)          // brightness 0-255
        };
    }
    
    /**
     * Get color from simple HSV palette (for simplicity, using rainbow)
     */
    private int[] colorFromPalette(int hue, int brightness) {
        // Simple rainbow palette implementation
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
        this.startTime = System.currentTimeMillis();
    }
    
    @Override
    public boolean update(long now) {
        // Animation updates on every frame, so we just return true
        return true;
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        if (x < 0 || x >= combinedWidth || y < 0 || y >= combinedHeight) {
            return new int[]{0, 0, 0};
        }
        
        long timeMs = System.currentTimeMillis() - startTime;
        
        // Calculate speed and scale parameters
        int speedVal = speed / 32;  // speed/32
        int scaleVal = scale / 32;  // intensity/32
        
        if (zoom) {
            scaleVal += 192 / (combinedWidth + combinedHeight);
        }
        
        // Time-based values
        long a = timeMs / 32;   // strip.now/32
        long a2 = a / 2;
        long a3 = a / 3;
        
        int colsScaled = combinedWidth * scaleVal;
        int rowsScaled = combinedHeight * scaleVal;
        
        // Oscillating center points (beatsin16_t equivalent)
        int cx = beatsin(10 - speedVal, 0, colsScaled, timeMs);
        int cy = beatsin(12 - speedVal, 0, rowsScaled, timeMs);
        int cx1 = beatsin(13 - speedVal, 0, colsScaled, timeMs);
        int cy1 = beatsin(15 - speedVal, 0, rowsScaled, timeMs);
        int cx2 = beatsin(17 - speedVal, 0, colsScaled, timeMs);
        int cy2 = beatsin(14 - speedVal, 0, rowsScaled, timeMs);
        
        // Calculate offsets for this pixel
        int xoffs = x * scaleVal;
        int yoffs = y * scaleVal;
        
        int rdistort, gdistort, bdistort;
        
        if (alt) {
            // Alternate mode from original code
            rdistort = cos8((int) (((x + y) * 8 + a2) & 255)) >> 1;
            gdistort = cos8((int) (((x + y) * 8 + a3 + 32) & 255)) >> 1;
            bdistort = cos8((int) (((x + y) * 8 + a + 64) & 255)) >> 1;
        } else {
            // Normal mode
            rdistort = cos8((int) ((cos8((int) (((x << 3) + a) & 255)) + 
                            cos8((int) (((y << 3) - a2) & 255)) + a3) & 255)) >> 1;
            gdistort = cos8((int) ((cos8((int) (((x << 3) - a2) & 255)) + 
                            cos8((int) (((y << 3) + a3) & 255)) + a + 32) & 255)) >> 1;
            bdistort = cos8((int) ((cos8((int) (((x << 3) + a3) & 255)) + 
                            cos8((int) (((y << 3) - a) & 255)) + a2 + 64) & 255)) >> 1;
        }
        
        // Calculate distance-based wave effects
        int distR = ((xoffs - cx) * (xoffs - cx) + (yoffs - cy) * (yoffs - cy)) >> 7;
        int distG = ((xoffs - cx1) * (xoffs - cx1) + (yoffs - cy1) * (yoffs - cy1)) >> 7;
        int distB = ((xoffs - cx2) * (xoffs - cx2) + (yoffs - cy2) * (yoffs - cy2)) >> 7;
        
        int valueR = rdistort + ((int)(a - distR) << 1);
        int valueG = gdistort + ((int)(a2 - distG) << 1);
        int valueB = bdistort + ((int)(a3 - distB) << 1);
        
        // Apply cosine to final values
        valueR = cos8((int)(valueR & 255));
        valueG = cos8((int)(valueG & 255));
        valueB = cos8((int)(valueB & 255));
        
        if (paletteMode == 0) {
            // Use RGB values (original color mode)
            return new int[]{valueR, valueG, valueB};
        } else {
            // Use palette
            int brightness = (valueR + valueG + valueB) / 3;
            
            if (fill) {
                // Map brightness to palette index
                return colorFromPalette(brightness, 255);
            } else {
                // Color mapping: calculate hue from pixel color, map it to palette index
                int[] hsv = rgb2hsv(valueR >> 2, valueG >> 2, valueB >> 2);
                return colorFromPalette(hsv[0], brightness);
            }
        }
    }
    
    @Override
    public String getName() {
        return "Distortion Waves";
    }
}
