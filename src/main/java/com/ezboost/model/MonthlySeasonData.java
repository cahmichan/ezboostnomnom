package com.ezboost.model;

import java.sql.Timestamp;

/**
 * MonthlySeasonData Model - Stores historical monthly hotel performance data
 * Enhancement i: Data-driven seasonality from CSV import
 */
public class MonthlySeasonData {
    private int dataId;
    private int userId;
    private String monthYear;           // Format: "2024-01"
    private String monthName;           // Full month name: "January"
    private double occupancyRate;       // Percentage (e.g., 75.19)
    private double totalRevenue;        // Total room revenue for the month
    private double avgRoomRate;         // Average Daily Rate (ADR)
    private String classifiedSeason;    // AUTO-CLASSIFIED: LOW, NORMAL, PEAK, SUPER_PEAK
    private Timestamp importDate;       // When this data was imported

    // Default constructor
    public MonthlySeasonData() {
    }

    // Full constructor
    public MonthlySeasonData(int dataId, int userId, String monthYear, String monthName,
                             double occupancyRate, double totalRevenue, double avgRoomRate,
                             String classifiedSeason, Timestamp importDate) {
        this.dataId = dataId;
        this.userId = userId;
        this.monthYear = monthYear;
        this.monthName = monthName;
        this.occupancyRate = occupancyRate;
        this.totalRevenue = totalRevenue;
        this.avgRoomRate = avgRoomRate;
        this.classifiedSeason = classifiedSeason;
        this.importDate = importDate;
    }

    // Constructor for CSV import (without ID and importDate)
    public MonthlySeasonData(int userId, String monthYear, String monthName,
                             double occupancyRate, double totalRevenue, double avgRoomRate) {
        this.userId = userId;
        this.monthYear = monthYear;
        this.monthName = monthName;
        this.occupancyRate = occupancyRate;
        this.totalRevenue = totalRevenue;
        this.avgRoomRate = avgRoomRate;
        this.classifiedSeason = null; // Will be auto-classified
    }

    /**
     * Auto-classify season based on occupancy thresholds
     * Based on Royale Chulan 2024 data analysis:
     * - SUPER_PEAK: >= 85% (July, August)
     * - PEAK: 75% - 85% (Feb, Jun, Sep, Nov, Dec)
     * - NORMAL: 65% - 75% (Jan, Mar, May, Oct)
     * - LOW: < 65% (April)
     */
    public void autoClassifySeason() {
        if (occupancyRate >= 85.0) {
            this.classifiedSeason = "SUPER_PEAK";
        } else if (occupancyRate >= 75.0) {
            this.classifiedSeason = "PEAK";
        } else if (occupancyRate >= 65.0) {
            this.classifiedSeason = "NORMAL";
        } else {
            this.classifiedSeason = "LOW";
        }
    }

    /**
     * Classify season with custom thresholds
     */
    public void classifySeasonWithThresholds(double lowMax, double normalMax, double peakMax) {
        if (occupancyRate >= peakMax) {
            this.classifiedSeason = "SUPER_PEAK";
        } else if (occupancyRate >= normalMax) {
            this.classifiedSeason = "PEAK";
        } else if (occupancyRate >= lowMax) {
            this.classifiedSeason = "NORMAL";
        } else {
            this.classifiedSeason = "LOW";
        }
    }

    /**
     * Get the Season enum equivalent
     */
    public Season getSeasonEnum() {
        if (classifiedSeason == null) {
            autoClassifySeason();
        }
        switch (classifiedSeason) {
            case "SUPER_PEAK": return Season.SUPER_PEAK;
            case "PEAK": return Season.PEAK;
            case "NORMAL": return Season.NORMAL;
            case "LOW": return Season.LOW;
            default: return Season.NORMAL;
        }
    }

    /**
     * Calculate RevPAR (Revenue Per Available Room)
     * @param totalRooms Total rooms in hotel
     * @param daysInMonth Days in the month
     */
    public double calculateRevPAR(int totalRooms, int daysInMonth) {
        int availableRoomNights = totalRooms * daysInMonth;
        if (availableRoomNights == 0) return 0;
        return totalRevenue / availableRoomNights;
    }

    // Getters and Setters
    public int getDataId() {
        return dataId;
    }

    public void setDataId(int dataId) {
        this.dataId = dataId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getAvgRoomRate() {
        return avgRoomRate;
    }

    public void setAvgRoomRate(double avgRoomRate) {
        this.avgRoomRate = avgRoomRate;
    }

    public String getClassifiedSeason() {
        return classifiedSeason;
    }

    public void setClassifiedSeason(String classifiedSeason) {
        this.classifiedSeason = classifiedSeason;
    }

    public Timestamp getImportDate() {
        return importDate;
    }

    public void setImportDate(Timestamp importDate) {
        this.importDate = importDate;
    }

    @Override
    public String toString() {
        return "MonthlySeasonData{" +
                "monthYear='" + monthYear + '\'' +
                ", monthName='" + monthName + '\'' +
                ", occupancyRate=" + occupancyRate + "%" +
                ", totalRevenue=MYR " + String.format("%.2f", totalRevenue) +
                ", avgRoomRate=MYR " + String.format("%.2f", avgRoomRate) +
                ", classifiedSeason='" + classifiedSeason + '\'' +
                '}';
    }
}
