package com.ezboost.model;
import java.util.EnumMap;

public class Room {
    private String name;
    private double baseADR;
    private double minADR;
    private double maxADR;
    private double occupancy;
    private int totalRooms;
    private EnumMap<Season, Double> seasonalPrices = new EnumMap<>(Season.class);
    private EnumMap<Season, Double> seasonalOccupancies;
    
    public Room(String name, double minADR, double maxADR, double occupancy, int totalRooms) {
        this(name, (minADR + maxADR) / 2.0, minADR, maxADR, occupancy, totalRooms);
    }

    public Room(String name, double baseADR, double minADR, double maxADR, double occupancy, int totalRooms) {
        this.name = name;
        this.minADR = Math.min(minADR, maxADR);
        this.maxADR = Math.max(minADR, maxADR);
        this.baseADR = baseADR <= 0 ? this.minADR : baseADR;
        this.baseADR = Math.max(this.minADR, Math.min(this.maxADR, this.baseADR));
        this.occupancy = occupancy;
        this.totalRooms = totalRooms;
        generateSeasonalPrices(); // default seasonal prices
    }
    
    private void generateSeasonalPrices() {
        for (Season season : Season.values()) {
            double multiplier = (season.getMinMultiplier() + season.getMaxMultiplier()) / 2.0;
            double baseAdr = getBaseAdr();
            seasonalPrices.put(season, baseAdr * multiplier);
        }
    }
    
    public String getName() { 
        return name; 
    }
    
    public double getMinAdr() { 
        return minADR; 
    }
    
    public double getMaxAdr() { 
        return maxADR; 
    }
    
    public double getBaseAdr() { 
        return baseADR;
    }
    
    public double getOccupancy() { 
        return occupancy; 
    }
    
    public int getTotalRooms() { 
        return totalRooms; 
    }
    
    public EnumMap<Season, Double> getSeasonalPrices() { 
        return seasonalPrices; 
    }
    
    public void setPriceForSeason(Season season, double newPrice) {
        seasonalPrices.put(season, newPrice);
    }
    
    public void setOccupancyForSeason(Season season, double occ) {
        if (seasonalOccupancies == null) {
            seasonalOccupancies = new EnumMap<>(Season.class);
        }
        seasonalOccupancies.put(season, occ);
    }

    public double getOccupancyForSeason(Season season) {
        if (seasonalOccupancies != null && seasonalOccupancies.containsKey(season)) {
            return seasonalOccupancies.get(season);
        }
        return occupancy; // fallback to flat occupancy
    }

    public boolean hasSeasonalOccupancies() {
        return seasonalOccupancies != null && !seasonalOccupancies.isEmpty();
    }

    public EnumMap<Season, Double> getSeasonalOccupancies() {
        return seasonalOccupancies;
    }

    public double getEstimatedRevenue() {
        double total = 0;
        for (Season season : Season.values()) {
            double price = seasonalPrices.getOrDefault(season, getBaseAdr());
            double occ = getOccupancyForSeason(season);
            total += price * (occ / 100.0) * totalRooms * (365.0 / 4.0);
        }
        return total;
    }
    
    public double getAverageSeasonalPrice() {
        return seasonalPrices.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(getBaseAdr());
    }
    
    // FIXED: Implement setOccupancy method instead of throwing exception
    public void setOccupancy(double newOccupancy) {
        // Add bounds checking to ensure occupancy stays within reasonable limits
        if (newOccupancy < 0) {
            this.occupancy = 0;
        } else if (newOccupancy > 100) {
            this.occupancy = 100;
        } else {
            this.occupancy = newOccupancy;
        }
    }
}
