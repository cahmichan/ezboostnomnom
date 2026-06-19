package com.ezboost.service;

import com.ezboost.dao.FutureEventDAO;
import com.ezboost.dao.SeasonalityDAO;
import com.ezboost.ga.DemandCurve;
import com.ezboost.model.FutureEvent;
import com.ezboost.model.MonthlySeasonData;
import com.ezboost.model.Room;
import com.ezboost.model.Season;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.util.*;

public class EventSeasonService {

    private static final Logger logger = LoggerFactory.getLogger(EventSeasonService.class);

    // Season hierarchy for comparison (higher = stronger)
    private static final Map<String, Integer> SEASON_RANK = new HashMap<>();
    static {
        SEASON_RANK.put("LOW", 0);
        SEASON_RANK.put("NORMAL", 1);
        SEASON_RANK.put("PEAK", 2);
        SEASON_RANK.put("SUPER_PEAK", 3);
    }

    private static final String[] MONTH_NAMES = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    /**
     * Generate monthly forecast with event-adjusted seasons.
     *
     * Returns a list of 12 maps (one per month), each containing:
     * - "month" (String): month name
     * - "monthNum" (Integer): 1-12
     * - "events" (List<FutureEvent>): events affecting this month
     * - "baseSeason" (String): historical season classification
     * - "adjustedSeason" (String): season after event overrides
     * - "seasonChanged" (Boolean): true if events bumped the season
     * - "roomPrices" (Map<String, Double>): room name -> adjusted price
     */
    public List<Map<String, Object>> generateMonthlyForecast(List<Room> rooms, int userId, int year) {
        return generateMonthlyForecast(rooms, userId, year, null);
    }

    public List<Map<String, Object>> generateMonthlyForecast(List<Room> rooms, int userId, int year, DemandCurve demandCurve) {
        List<Map<String, Object>> forecast = new ArrayList<>();

        // Load historical monthly data to determine base season per month
        Map<Integer, String> baseSeasonByMonth = getBaseSeasonByMonth(userId);

        // Load active events for the year
        List<FutureEvent> yearEvents = FutureEventDAO.getEventsByYear(userId, year);

        logger.debug("Generating forecast for {} with {} events{}", year, yearEvents.size(),
                demandCurve != null ? " (demand curve active)" : "");

        for (int month = 1; month <= 12; month++) {
            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", MONTH_NAMES[month - 1]);
            monthData.put("monthNum", month);

            // Find events that affect this month
            List<FutureEvent> monthEvents = getEventsForMonth(yearEvents, year, month);
            monthData.put("events", monthEvents);

            // Base season from historical data
            String baseSeason = baseSeasonByMonth.getOrDefault(month, "NORMAL");
            monthData.put("baseSeason", baseSeason);

            // Determine adjusted season: highest override among events, but only bump UP
            String adjustedSeason = baseSeason;
            for (FutureEvent event : monthEvents) {
                String override = event.getSeasonOverride();
                if (SEASON_RANK.getOrDefault(override, 0) > SEASON_RANK.getOrDefault(adjustedSeason, 0)) {
                    adjustedSeason = override;
                }
            }
            monthData.put("adjustedSeason", adjustedSeason);
            monthData.put("seasonChanged", !adjustedSeason.equals(baseSeason));

            // Look up GA-optimized prices for the adjusted season
            Season adjustedSeasonEnum = Season.valueOf(adjustedSeason);
            Map<String, Double> roomPrices = new LinkedHashMap<>();
            Map<String, Double> roomOccupancies = new LinkedHashMap<>();
            for (Room room : rooms) {
                Double price = room.getSeasonalPrices().get(adjustedSeasonEnum);
                if (price == null) price = room.getBaseAdr();
                roomPrices.put(room.getName(), price);

                // Per-room occupancy from demand curve or room data
                if (demandCurve != null) {
                    roomOccupancies.put(room.getName(), demandCurve.getOccupancy(price));
                } else {
                    roomOccupancies.put(room.getName(), room.getOccupancyForSeason(adjustedSeasonEnum));
                }
            }
            monthData.put("roomPrices", roomPrices);
            monthData.put("roomOccupancies", roomOccupancies);

            forecast.add(monthData);
        }

        return forecast;
    }

    /**
     * Determine base season for each month (1-12) from historical data.
     * Uses the most common season classification for each calendar month across all years.
     */
    private Map<Integer, String> getBaseSeasonByMonth(int userId) {
        Map<Integer, String> result = new HashMap<>();
        List<MonthlySeasonData> allData = SeasonalityDAO.getMonthlyDataByUser(userId);

        // Group by month number and count season occurrences
        Map<Integer, Map<String, Integer>> monthSeasonCounts = new HashMap<>();

        for (MonthlySeasonData data : allData) {
            int monthNum = extractMonthNumber(data.getMonthYear(), data.getMonthName());
            if (monthNum < 1 || monthNum > 12) continue;

            monthSeasonCounts.computeIfAbsent(monthNum, k -> new HashMap<>());
            String season = data.getClassifiedSeason();
            if (season != null) {
                monthSeasonCounts.get(monthNum).merge(season, 1, Integer::sum);
            }
        }

        // For each month, pick the most common season
        for (int m = 1; m <= 12; m++) {
            Map<String, Integer> counts = monthSeasonCounts.get(m);
            if (counts != null && !counts.isEmpty()) {
                result.put(m, counts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .get().getKey());
            } else {
                result.put(m, "NORMAL"); // default if no data
            }
        }

        return result;
    }

    /**
     * Extract month number from monthYear (format "2024-01") or monthName ("January")
     */
    private int extractMonthNumber(String monthYear, String monthName) {
        // Try parsing from monthYear format "YYYY-MM"
        if (monthYear != null && monthYear.contains("-")) {
            try {
                String[] parts = monthYear.split("-");
                return Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
        // Fallback to monthName
        if (monthName != null) {
            for (int i = 0; i < MONTH_NAMES.length; i++) {
                if (MONTH_NAMES[i].equalsIgnoreCase(monthName.trim())) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    /**
     * Filter events that overlap with a given month.
     */
    private List<FutureEvent> getEventsForMonth(List<FutureEvent> allEvents, int year, int month) {
        List<FutureEvent> result = new ArrayList<>();
        // Month boundaries
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1, 0, 0, 0);
        long monthStart = cal.getTimeInMillis();

        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        long monthEnd = cal.getTimeInMillis();

        for (FutureEvent event : allEvents) {
            Date startDate = event.getEventDate();
            Date endDate = event.getEventEndDate();

            if (endDate == null) {
                // Single-day event: check if it falls within the month
                long eventTime = startDate.getTime();
                if (eventTime >= monthStart && eventTime <= monthEnd) {
                    result.add(event);
                }
            } else {
                // Multi-day event: check if date range overlaps with month
                long eventStart = startDate.getTime();
                long eventEnd = endDate.getTime();
                if (eventStart <= monthEnd && eventEnd >= monthStart) {
                    result.add(event);
                }
            }
        }
        return result;
    }
}
