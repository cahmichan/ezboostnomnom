package com.ezboost.model;

/**
 * SegmentPricingResult - Holds segment-specific pricing for a room type
 * 
 * Simplified version: Only tracks pricing, not revenue contribution
 */
public class SegmentPricingResult {
    private String roomType;
    private String segmentName;
    private String segmentCode;
    private String category;
    private double basePrice;
    private double multiplier;
    private double segmentPrice;
    private double revenueContribution; // Kept for backward compatibility, always 0

    // Getters and Setters
    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
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

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getSegmentPrice() {
        return segmentPrice;
    }

    public void setSegmentPrice(double segmentPrice) {
        this.segmentPrice = segmentPrice;
    }

    /**
     * @deprecated Revenue contribution is no longer calculated. Always returns 0.
     */
    @Deprecated
    public double getRevenueContribution() {
        return 0.0;
    }

    /**
     * @deprecated Revenue contribution is no longer used.
     */
    @Deprecated
    public void setRevenueContribution(double revenueContribution) {
        this.revenueContribution = 0.0;
    }

    @Override
    public String toString() {
        return "SegmentPricingResult{" +
                "roomType='" + roomType + '\'' +
                ", segmentName='" + segmentName + '\'' +
                ", basePrice=" + basePrice +
                ", multiplier=" + multiplier +
                ", segmentPrice=" + segmentPrice +
                '}';
    }
}
