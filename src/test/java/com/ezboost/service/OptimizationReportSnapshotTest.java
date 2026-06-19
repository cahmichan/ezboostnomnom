package com.ezboost.service;

import com.ezboost.model.FutureEvent;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimizationReportSnapshotTest {

    @Test
    void preservesReportInputsAcrossJsonRoundTrip() {
        Room room = new Room("Deluxe", 200, 150, 300, 75, 20);
        room.setPriceForSeason(Season.PEAK, 260);
        room.setOccupancyForSeason(Season.PEAK, 72);
        MarketSegment segment = new MarketSegment("Corporate", "CORP", "GIT", 0.9, "Contract rate");
        FutureEvent event = new FutureEvent(1, "Holiday", Date.valueOf("2026-01-01"), null,
                "PUBLIC_HOLIDAY", "PEAK", "CALENDARIFIC");

        Map<String, Object> month = new LinkedHashMap<>();
        month.put("month", "January");
        month.put("monthNum", 1);
        month.put("baseSeason", "NORMAL");
        month.put("adjustedSeason", "PEAK");
        month.put("seasonChanged", true);
        month.put("events", Arrays.asList(event));
        Map<String, Double> prices = new LinkedHashMap<>();
        prices.put("Deluxe", 260.0);
        month.put("roomPrices", prices);
        Map<String, Double> occupancies = new LinkedHashMap<>();
        occupancies.put("Deluxe", 72.0);
        month.put("roomOccupancies", occupancies);

        OptimizationReportSnapshot snapshot = new OptimizationReportSnapshot(Arrays.asList(room), 100000, 99900,
                Arrays.asList(segment), Arrays.asList(month), 2026, "Historical fit", 80000.0, 120000.0,
                Arrays.asList("Deluxe / PEAK is free."));
        OptimizationReportSnapshot restored = OptimizationReportSnapshot.fromJson(snapshot.toJson());

        assertEquals(1, restored.getRooms().size());
        assertEquals(260.0, restored.getRooms().get(0).getSeasonalPrices().get(Season.PEAK));
        assertEquals(1, restored.getMonthlyForecast().size());
        assertTrue((Boolean) restored.getMonthlyForecast().get(0).get("seasonChanged"));
        assertEquals("Holiday", ((List<FutureEvent>) restored.getMonthlyForecast().get(0).get("events")).get(0).getEventName());
        assertEquals(0.9, restored.getMarketSegments().get(0).getRateMultiplier());
    }
}
