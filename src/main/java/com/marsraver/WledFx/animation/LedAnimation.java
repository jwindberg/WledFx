package com.marsraver.WledFx.animation;

/**
 * Interface for LED animations across multiple devices
 */
public interface LedAnimation {
    
    /**
     * Initialize the animation state
     * @param combinedWidth Total width of the combined grid
     * @param combinedHeight Total height of the combined grid
     */
    void init(int combinedWidth, int combinedHeight);
    
    /**
     * Update the animation for the current frame
     * @param now Current time in nanoseconds
     * @return True if the animation should continue, false to stop
     */
    boolean update(long now);
    
    /**
     * Get the color for a specific pixel in the combined grid
     * @param x X coordinate in the combined grid (0 to combinedWidth-1)
     * @param y Y coordinate in the combined grid (0 to combinedHeight-1)
     * @return RGB color values as [R, G, B] each in range 0-255
     */
    int[] getPixelColor(int x, int y);
    
    /**
     * Get the name of the animation for display in UI
     * @return Animation name
     */
    String getName();
    
    /**
     * Draw the animation on the canvas (optional, for custom rendering)
     * @param gc Graphics context for drawing
     * @param combinedWidth Total width of the combined grid
     * @param combinedHeight Total height of the combined grid
     * @param cellSize Size of each cell
     */
    default void draw(javafx.scene.canvas.GraphicsContext gc, int combinedWidth, int combinedHeight, double cellSize) {
        // Default implementation: draw pixels based on getPixelColor
        double spacing = cellSize * 1.2;
        double radius = cellSize / 2.0;
        double startX = spacing / 2;
        double startY = spacing / 2;
        
        for (int y = 0; y < combinedHeight; y++) {
            for (int x = 0; x < combinedWidth; x++) {
                int[] rgb = getPixelColor(x, y);
                double pixelX = x * spacing + startX;
                double pixelY = y * spacing + startY;
                
                gc.setFill(javafx.scene.paint.Color.rgb(rgb[0], rgb[1], rgb[2]));
                gc.fillOval(pixelX - radius, pixelY - radius, radius * 2, radius * 2);
            }
        }
    }
}

