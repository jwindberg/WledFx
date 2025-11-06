package com.marsraver.WledFx.animation;

/**
 * Colored Bursts animation - rainbow rays radiating from random centers
 * Based on WLED's mode #136 Colored Bursts effect
 */
public class ColoredBurstsAnimation implements LedAnimation {
    
    private int combinedWidth;
    private int combinedHeight;
    private int time;
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.time = 0;
    }
    
    @Override
    public boolean update(long now) {
        time++;
        return true; // Keep running
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        // Animated center that moves around the field
        // Use sinusoidal motion for smooth movement
        double t = time * 0.05; // Slow down the movement
        double centerX = combinedWidth / 2.0 + Math.cos(t * 0.5) * combinedWidth * 0.3;
        double centerY = combinedHeight / 2.0 + Math.sin(t * 0.3) * combinedHeight * 0.3;
        
        // Calculate angle from center to this pixel
        double dx = x - centerX;
        double dy = y - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 1) {
            // Center pixel - bright white
            int intensity = 128 + (time % 128);
            return new int[]{intensity, intensity, intensity};
        }
        
        // Calculate angle in degrees (0-360)
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360;
        }
        
        // Create discrete rays instead of filling everything
        // Use modulo to create distinct rays with gaps between them
        double numRays = 16; // Number of rays
        double rayWidth = 360.0 / numRays;
        
        // Check if this pixel is within a ray
        double rayPos = (angle + time * 2) % 360;
        double rayIndex = Math.floor(rayPos / rayWidth);
        double rayOffset = (rayPos % rayWidth) / rayWidth; // Position within the ray (0-1)
        
        // Only draw pixels that are within a certain distance of the ray center
        double rayCenterOffset = Math.abs(rayOffset - 0.5);
        if (rayCenterOffset > 0.3) {
            // Gap between rays - black
            return new int[]{0, 0, 0};
        }
        
        // Create rainbow effect based on ray index
        // Map ray to hue in HSV color space
        double hue = (rayIndex * rayWidth + time * 2) % 360;
        double saturation = 1.0;
        
        // Fade based on distance from center - further out should be dimmer
        double maxDist = Math.sqrt(combinedWidth * combinedWidth + combinedHeight * combinedHeight);
        double distFactor = 1.0 - (distance / maxDist) * 0.8; // Keep some minimum brightness
        double value = distFactor;
        
        // Add pulsing effect
        double pulse = 0.5 + 0.5 * Math.sin(time * 0.1);
        value *= pulse;
        
        // Convert HSV to RGB
        return hsvToRgb(hue, saturation, value);
    }
    
    @Override
    public String getName() {
        return "Colored Bursts";
    }
    
    private int[] hsvToRgb(double h, double s, double v) {
        int c = (int)(v * s * 255);
        double hPrime = h / 60.0;
        int x = (int)(c * (1 - Math.abs(hPrime % 2 - 1)));
        int m = (int)(v * 255 - c);
        
        int r = 0, g = 0, b = 0;
        
        if (hPrime < 1) {
            r = c; g = x; b = 0;
        } else if (hPrime < 2) {
            r = x; g = c; b = 0;
        } else if (hPrime < 3) {
            r = 0; g = c; b = x;
        } else if (hPrime < 4) {
            r = 0; g = x; b = c;
        } else if (hPrime < 5) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        return new int[]{r + m, g + m, b + m};
    }
}
