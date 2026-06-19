package com.ezboost.service;

import com.ezboost.dao.MarketSegmentDAO;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import com.ezboost.model.SegmentPricingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SegmentPricingService - Calculates segment-specific prices from GA-optimized base prices
 *
 * Simplified version: Only applies rate multipliers to generate segment prices.
 * Does NOT calculate revenue distribution (since we can't predict future segment proportions).
 *
 * All methods are user-scoped: segments are fetched per-user.
 */
public class SegmentPricingService {

    /**
     * Get all active market segments for a user
     */
    public List<MarketSegment> getSegments(int userId) {
        return MarketSegmentDAO.getAllSegments(userId);
    }

    /**
     * Calculate segment-specific prices for all room types and seasons
     *
     * @param rooms List of optimized Room objects from GA
     * @param userId The user whose segments to use
     * @return Map of roomType -> Season -> SegmentPricingResult
     */
    public Map<String, Map<Season, SegmentPricingResult>> calculateSegmentPrices(List<Room> rooms, int userId) {
        Map<String, Map<Season, SegmentPricingResult>> results = new LinkedHashMap<>();
        List<MarketSegment> segments = MarketSegmentDAO.getAllSegments(userId);

        for (Room room : rooms) {
            Map<Season, SegmentPricingResult> seasonMap = new LinkedHashMap<>();

            for (Season season : Season.values()) {
                Double basePrice = room.getSeasonalPrices().get(season);
                if (basePrice == null) basePrice = 0.0;

                // Create result for each season (segments applied in JSP/Excel)
                SegmentPricingResult result = new SegmentPricingResult();
                result.setRoomType(room.getName());
                result.setBasePrice(basePrice);
                result.setSegmentName("Base");
                result.setSegmentCode("BASE");
                result.setCategory("N/A");
                result.setMultiplier(1.0);
                result.setSegmentPrice(basePrice);
                result.setRevenueContribution(0.0);

                seasonMap.put(season, result);
            }

            results.put(room.getName(), seasonMap);
        }

        return results;
    }

    /**
     * Calculate segment prices from a simple map of room type -> price
     *
     * @param optimizedPrices Map of room type -> optimized base price from GA
     * @param userId The user whose segments to use
     * @return List of SegmentPricingResult for each segment/room combination
     */
    public static List<SegmentPricingResult> calculateSegmentPricesFromMap(Map<String, Double> optimizedPrices, int userId) {
        List<SegmentPricingResult> results = new ArrayList<>();
        List<MarketSegment> segments = MarketSegmentDAO.getAllSegments(userId);

        for (Map.Entry<String, Double> entry : optimizedPrices.entrySet()) {
            String roomType = entry.getKey();
            double basePrice = entry.getValue();

            for (MarketSegment segment : segments) {
                SegmentPricingResult result = new SegmentPricingResult();
                result.setRoomType(roomType);
                result.setSegmentName(segment.getSegmentName());
                result.setSegmentCode(segment.getSegmentCode());
                result.setCategory(segment.getCategory());
                result.setBasePrice(basePrice);
                result.setMultiplier(segment.getRateMultiplier());
                result.setSegmentPrice(basePrice * segment.getRateMultiplier());
                result.setRevenueContribution(0.0);
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Generate pricing table for easy JSP display
     * Returns list of maps with room info and segment prices
     *
     * @param rooms List of optimized Room objects
     * @param userId The user whose segments to use
     * @return List of pricing data maps
     */
    public List<Map<String, Object>> generatePricingTable(List<Room> rooms, int userId) {
        List<Map<String, Object>> table = new ArrayList<>();
        List<MarketSegment> segments = MarketSegmentDAO.getAllSegments(userId);

        for (Room room : rooms) {
            Map<String, Object> rowData = new LinkedHashMap<>();
            rowData.put("roomName", room.getName());
            rowData.put("totalRooms", room.getTotalRooms());
            rowData.put("occupancy", room.getOccupancy());

            // Add base prices by season
            Map<String, Double> basePrices = new LinkedHashMap<>();
            Map<String, Map<String, Double>> segmentPrices = new LinkedHashMap<>();

            for (Season season : Season.values()) {
                Double basePrice = room.getSeasonalPrices().get(season);
                if (basePrice == null) basePrice = 0.0;
                basePrices.put(season.name(), basePrice);

                // Calculate segment prices for this season
                Map<String, Double> seasonSegmentPrices = new LinkedHashMap<>();
                for (MarketSegment segment : segments) {
                    double segmentPrice = basePrice * segment.getRateMultiplier();
                    seasonSegmentPrices.put(segment.getSegmentCode(), segmentPrice);
                }
                segmentPrices.put(season.name(), seasonSegmentPrices);
            }

            rowData.put("basePrices", basePrices);
            rowData.put("segmentPrices", segmentPrices);
            rowData.put("estimatedRevenue", room.getEstimatedRevenue());

            table.add(rowData);
        }

        return table;
    }

    /**
     * Get all segments with their multipliers for a user (for UI display)
     */
    public static List<MarketSegment> getAllSegments(int userId) {
        return MarketSegmentDAO.getAllSegments(userId);
    }

    /**
     * Calculate average multiplier across all segments for a user
     */
    public static double getAverageMultiplier(int userId) {
        List<MarketSegment> segments = MarketSegmentDAO.getAllSegments(userId);
        if (segments.isEmpty()) return 1.0;

        double sum = 0;
        for (MarketSegment seg : segments) {
            sum += seg.getRateMultiplier();
        }
        return sum / segments.size();
    }
}
