package com.marsraver.WledFx.animation;

/**
 * Snake animation that travels across the grid
 */
public class SnakeAnimation implements LedAnimation {
    
    private int combinedWidth;
    private int combinedHeight;
    private int snakePosition;
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        this.snakePosition = 0;
    }
    
    @Override
    public boolean update(long now) {
        // Move snake forward
        snakePosition = (snakePosition + 1) % (combinedWidth * combinedHeight);
        return true; // Keep running
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        int ledIndex = y * combinedWidth + x;
        int distance = Math.abs(ledIndex - snakePosition);
        
        // Create snake effect with tail
        if (distance == 0) {
            // Head - bright green
            return new int[]{0, 255, 0};
        } else if (distance <= 5) {
            // Tail - fade to dark green
            int intensity = 255 - (distance * 40);
            intensity = Math.max(0, Math.min(255, intensity));
            return new int[]{0, intensity, 0};
        } else {
            // Off
            return new int[]{0, 0, 0};
        }
    }
    
    @Override
    public String getName() {
        return "Snake";
    }
}

