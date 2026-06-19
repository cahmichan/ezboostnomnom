package com.ezboost.model;

public enum Season {
    LOW(-0.1, 0.2, 0.9),        // Scale factor -0.1, Min 20%, Max 90%
    NORMAL(0.0, 0.5, 1.1),      // Scale factor 0.0, Min 50%, Max 110%  
    PEAK(0.1, 0.7, 1.4),        // Scale factor 0.1, Min 70%, Max 140%
    SUPER_PEAK(0.2, 0.9, 1.8);  // Scale factor 0.2, Min 90%, Max 180%
    
    private final double scaleFactor; // make season price lower and higher than base price
    private final double minMultiplier; //prevent price from going too low
    private final double maxMultiplier; //prevent price from going too high
    
    Season(double scaleFactor, double minMultiplier, double maxMultiplier) {
        this.scaleFactor = scaleFactor;
        this.minMultiplier = minMultiplier;
        this.maxMultiplier = maxMultiplier;
    }
    
    public double getScaleFactor() {
        return scaleFactor;
    }
    
    public double getMinMultiplier() {
        return minMultiplier;
    }
    
    public double getMaxMultiplier() {
        return maxMultiplier;
    }
}