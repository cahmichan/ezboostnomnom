package com.ezboost.model;

import java.sql.Timestamp;

/**
 * UserMultiplierSettings Model - Stores user-defined price multipliers
 * Enhancement iii: User-adjustable room rate multipliers based on seasonality
 */
public class UserMultiplierSettings {
    private int settingId;
    private int userId;
    private String roomType;            // Optional: specific room type, null = all rooms
    private String seasonName;          // LOW, NORMAL, PEAK, SUPER_PEAK
    private String segmentName;         // Optional: specific segment, null = all segments
    private double customMultiplier;    // User-defined multiplier
    private double minBound;            // Minimum allowed multiplier
    private double maxBound;            // Maximum allowed multiplier
    private boolean isLocked;           // If locked, GA cannot modify this setting
    private Timestamp lastUpdated;

    // Default constructor
    public UserMultiplierSettings() {
        this.minBound = 0.5;
        this.maxBound = 2.0;
        this.isLocked = false;
    }

    // Full constructor
    public UserMultiplierSettings(int settingId, int userId, String roomType, 
                                  String seasonName, String segmentName,
                                  double customMultiplier, double minBound, 
                                  double maxBound, boolean isLocked, 
                                  Timestamp lastUpdated) {
        this.settingId = settingId;
        this.userId = userId;
        this.roomType = roomType;
        this.seasonName = seasonName;
        this.segmentName = segmentName;
        this.customMultiplier = customMultiplier;
        this.minBound = minBound;
        this.maxBound = maxBound;
        this.isLocked = isLocked;
        this.lastUpdated = lastUpdated;
    }

    // Constructor for new setting
    public UserMultiplierSettings(int userId, String roomType, String seasonName,
                                  String segmentName, double customMultiplier) {
        this.userId = userId;
        this.roomType = roomType;
        this.seasonName = seasonName;
        this.segmentName = segmentName;
        this.customMultiplier = customMultiplier;
        this.minBound = 0.5;
        this.maxBound = 2.0;
        this.isLocked = false;
    }

    /**
     * Validate that the custom multiplier is within bounds
     */
    public boolean isValidMultiplier() {
        return customMultiplier >= minBound && customMultiplier <= maxBound;
    }

    /**
     * Clamp the multiplier to valid bounds
     */
    public void clampMultiplier() {
        if (customMultiplier < minBound) {
            customMultiplier = minBound;
        } else if (customMultiplier > maxBound) {
            customMultiplier = maxBound;
        }
    }

    /**
     * Apply the multiplier to a base price
     * @param basePrice The base ADR price
     * @return Adjusted price
     */
    public double applyMultiplier(double basePrice) {
        return basePrice * customMultiplier;
    }

    /**
     * Check if this setting applies to a specific context
     */
    public boolean appliesTo(String roomType, String seasonName, String segmentName) {
        boolean roomMatches = (this.roomType == null || this.roomType.equals(roomType));
        boolean seasonMatches = (this.seasonName == null || this.seasonName.equals(seasonName));
        boolean segmentMatches = (this.segmentName == null || this.segmentName.equals(segmentName));
        return roomMatches && seasonMatches && segmentMatches;
    }

    /**
     * Get specificity score (more specific = higher priority)
     * Used when multiple settings might apply to the same context
     */
    public int getSpecificityScore() {
        int score = 0;
        if (roomType != null && !roomType.isEmpty()) score += 4;
        if (seasonName != null && !seasonName.isEmpty()) score += 2;
        if (segmentName != null && !segmentName.isEmpty()) score += 1;
        return score;
    }

    // Getters and Setters
    public int getSettingId() {
        return settingId;
    }

    public void setSettingId(int settingId) {
        this.settingId = settingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getSeasonName() {
        return seasonName;
    }

    public void setSeasonName(String seasonName) {
        this.seasonName = seasonName;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public double getCustomMultiplier() {
        return customMultiplier;
    }

    public void setCustomMultiplier(double customMultiplier) {
        this.customMultiplier = customMultiplier;
    }

    public double getMinBound() {
        return minBound;
    }

    public void setMinBound(double minBound) {
        this.minBound = minBound;
    }

    public double getMaxBound() {
        return maxBound;
    }

    public void setMaxBound(double maxBound) {
        this.maxBound = maxBound;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "UserMultiplierSettings{" +
                "roomType='" + (roomType != null ? roomType : "ALL") + '\'' +
                ", seasonName='" + seasonName + '\'' +
                ", segmentName='" + (segmentName != null ? segmentName : "ALL") + '\'' +
                ", customMultiplier=" + customMultiplier +
                ", bounds=[" + minBound + ", " + maxBound + "]" +
                ", isLocked=" + isLocked +
                '}';
    }
}
