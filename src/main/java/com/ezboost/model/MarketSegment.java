package com.ezboost.model;

/**
 * MarketSegment model - Represents a customer segment with pricing multiplier
 * 
 * Simplified version: Only tracks pricing multipliers, NOT revenue percentages
 * (since we're predicting future prices, we don't know what % each segment will contribute)
 */
public class MarketSegment {
    private int id;
    private int userId;
    private String segmentName;
    private String segmentCode;
    private String category;        // FIT or GIT
    private double rateMultiplier;  // Pricing multiplier (0.5 - 2.0)
    private String description;
    private boolean active;

    // Constructors
    public MarketSegment() {
        this.active = true;
        this.rateMultiplier = 1.0;
    }

    public MarketSegment(String segmentName, String segmentCode, String category, 
                         double rateMultiplier, String description) {
        this.segmentName = segmentName;
        this.segmentCode = segmentCode;
        this.category = category;
        this.rateMultiplier = rateMultiplier;
        this.description = description;
        this.active = true;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public String getSegmentCode() {
        return segmentCode;
    }

    public void setSegmentCode(String segmentCode) {
        this.segmentCode = segmentCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getRateMultiplier() {
        return rateMultiplier;
    }

    public void setRateMultiplier(double rateMultiplier) {
        this.rateMultiplier = rateMultiplier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @deprecated Revenue share is no longer used. Returns 0.0.
     */
    @Deprecated
    public double getRevenueShare() {
        return 0.0;
    }

    /**
     * @deprecated Revenue share is no longer used. This method does nothing.
     */
    @Deprecated
    public void setRevenueShare(double revenueShare) {
        // No-op - revenue share is no longer tracked
    }

    @Override
    public String toString() {
        return "MarketSegment{" +
                "segmentName='" + segmentName + '\'' +
                ", segmentCode='" + segmentCode + '\'' +
                ", category='" + category + '\'' +
                ", rateMultiplier=" + rateMultiplier +
                '}';
    }
}
