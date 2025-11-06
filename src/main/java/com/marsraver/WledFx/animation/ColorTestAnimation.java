package com.marsraver.WledFx.animation;

/**
 * Color test animation with red, green, and blue circles on different grids
 */
public class ColorTestAnimation implements LedAnimation {
    
    private int combinedWidth;
    private int combinedHeight;
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
    }
    
    @Override
    public boolean update(long now) {
        return true; // Static colors
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        // Device 1 (top-left, 0-15, 0-15): Red circle at center
        if (x < 16 && y < 16) {
            int centerX = 8;
            int centerY = 8;
            double distance = Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
            if (distance < 6) {
                return new int[]{255, 0, 0}; // Red
            }
        }
        
        // Device 2 (top-right, 16-31, 0-15): Green circle at center
        if (x >= 16 && x < 32 && y < 16) {
            int localX = x - 16;
            int centerX = 8;
            int centerY = 8;
            double distance = Math.sqrt((localX - centerX) * (localX - centerX) + (y - centerY) * (y - centerY));
            if (distance < 6) {
                return new int[]{0, 255, 0}; // Green
            }
        }
        
        // Device 3 (bottom-left, 0-15, 16-31): Blue circle at center
        if (x < 16 && y >= 16 && y < 32) {
            int localY = y - 16;
            int centerX = 8;
            int centerY = 8;
            double distance = Math.sqrt((x - centerX) * (x - centerX) + (localY - centerY) * (localY - centerY));
            if (distance < 6) {
                return new int[]{0, 0, 255}; // Blue
            }
        }
        
        // Everything else is off
        return new int[]{0, 0, 0};
    }
    
    @Override
    public String getName() {
        return "Color Test";
    }
}

