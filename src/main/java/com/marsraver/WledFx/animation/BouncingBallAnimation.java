package com.marsraver.WledFx.animation;

import java.util.Random;

/**
 * Bouncing ball animation with random behavior
 */
public class BouncingBallAnimation implements LedAnimation {
    
    private static final Random random = new Random();
    private static final double GRAVITY = 0.3;
    private static final double BOUNCE_DAMPING = 0.95;
    private static final double RANDOM_CHANGE_PROBABILITY = 0.02;
    
    private int combinedWidth;
    private int combinedHeight;
    
    // Ball state
    private double ballX;
    private double ballY;
    private double velocityX;
    private double velocityY;
    private int ballRadius;
    
    // Trail effect
    private static final int MAX_TRAIL = 5;
    
    @Override
    public void init(int combinedWidth, int combinedHeight) {
        this.combinedWidth = combinedWidth;
        this.combinedHeight = combinedHeight;
        
        // Initialize ball at random position
        this.ballX = combinedWidth / 2.0 + (random.nextDouble() - 0.5) * 10;
        this.ballY = combinedHeight / 2.0 + (random.nextDouble() - 0.5) * 10;
        
        // Random initial velocity
        this.velocityX = (random.nextDouble() - 0.5) * 4;
        this.velocityY = (random.nextDouble() - 0.5) * 4;
        
        this.ballRadius = 2;
    }
    
    @Override
    public boolean update(long now) {
        // Add random velocity changes
        if (random.nextDouble() < RANDOM_CHANGE_PROBABILITY) {
            velocityX += (random.nextDouble() - 0.5) * 2;
            velocityY += (random.nextDouble() - 0.5) * 2;
        }
        
        // Update position
        ballX += velocityX;
        ballY += velocityY;
        
        // Bounce off walls
        if (ballX < ballRadius) {
            ballX = ballRadius;
            velocityX = -velocityX * BOUNCE_DAMPING;
        } else if (ballX >= combinedWidth - ballRadius) {
            ballX = combinedWidth - ballRadius - 1;
            velocityX = -velocityX * BOUNCE_DAMPING;
        }
        
        if (ballY < ballRadius) {
            ballY = ballRadius;
            velocityY = -velocityY * BOUNCE_DAMPING;
        } else if (ballY >= combinedHeight - ballRadius) {
            ballY = combinedHeight - ballRadius - 1;
            velocityY = -velocityY * BOUNCE_DAMPING;
        }
        
        // Clamp velocities to prevent excessive speed
        velocityX = Math.max(-8, Math.min(8, velocityX));
        velocityY = Math.max(-8, Math.min(8, velocityY));
        
        return true; // Keep running
    }
    
    @Override
    public int[] getPixelColor(int x, int y) {
        // Calculate distance from ball center
        double dx = x - ballX;
        double dy = y - ballY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance <= ballRadius) {
            // Ball itself - bright color
            return new int[]{0, 255, 255}; // Cyan (to test GRB mapping)
        } else if (distance <= ballRadius + MAX_TRAIL) {
            // Trail effect
            double trailDistance = distance - ballRadius;
            double intensity = 255 * (1.0 - trailDistance / MAX_TRAIL);
            intensity = Math.max(0, Math.min(255, intensity));
            
            // Fade from cyan to black
            int r = (int)(0 * (1.0 - trailDistance / MAX_TRAIL));
            int g = (int)(255 * (1.0 - trailDistance / MAX_TRAIL));
            int b = (int)(255 * (1.0 - trailDistance / MAX_TRAIL));
            
            return new int[]{r, g, b};
        } else {
            // Off
            return new int[]{0, 0, 0};
        }
    }
    
    @Override
    public String getName() {
        return "Bouncing Ball";
    }
}

