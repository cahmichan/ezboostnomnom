package com.ezboost.service;

import com.ezboost.dao.RoomDataDAO;
import com.ezboost.dao.SeasonalityDAO;
import com.ezboost.model.FutureEvent;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import com.ezboost.model.User;
import jakarta.servlet.http.HttpServletRequest;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Prepares setup and display data rendered by the BoostMe page. */
public final class BoostMePageService {

    private BoostMePageService() {
    }

    public static void prepare(HttpServletRequest request, User user) {
        int roomTypeCount = 0;
        int monthlyCount = 0;
        boolean readyForOptimization = false;

        if (user != null) {
            roomTypeCount = RoomDataDAO.getRoomTypeCount(user.getUserId());
            monthlyCount = SeasonalityDAO.getMonthlyDataByUser(user.getUserId()).size();
            readyForOptimization = roomTypeCount > 0
                    && monthlyCount > 0
                    && SeasonalityDAO.getThresholdsByUser(user.getUserId()) != null;
        }

        request.setAttribute("roomTypeCount", roomTypeCount);
        request.setAttribute("monthlyCount", monthlyCount);
        request.setAttribute("readyForOptimization", readyForOptimization);
        request.setAttribute("boostView", buildView(request));
    }

    @SuppressWarnings("unchecked")
    private static View buildView(HttpServletRequest request) {
        DecimalFormat money = new DecimalFormat("#,##0.00");
        DecimalFormat number = new DecimalFormat("0.00");
        int roomTypeCount = intAttribute(request, "roomTypeCount");
        int monthlyCount = intAttribute(request, "monthlyCount");
        boolean ready = Boolean.TRUE.equals(request.getAttribute("readyForOptimization"));
        Double expectedRevenue = doubleAttribute(request, "expectedRevenue");
        Double estimatedRevenue = doubleAttribute(request, "estimatedRevenue");
        List<Room> rooms = listAttribute(request, "bestSolution");
        boolean hasResults = expectedRevenue != null && estimatedRevenue != null && !rooms.isEmpty();
        Map<String, String> constraintStates = mapAttribute(request, "priceConstraintStates");
        List<MarketSegment> segments = listAttribute(request, "marketSegments");
        List<Map<String, Object>> forecast = listAttribute(request, "monthlyForecast");

        List<RoomRow> roomRows = new ArrayList<>();
        for (Room room : rooms) {
            roomRows.add(new RoomRow(room, money, number, constraintStates));
        }

        List<Segment> segmentRows = new ArrayList<>();
        for (MarketSegment segment : segments) {
            segmentRows.add(new Segment(segment, number));
        }

        List<SegmentPriceRow> segmentPriceRows = new ArrayList<>();
        for (Room room : rooms) {
            boolean first = true;
            for (Season season : Season.values()) {
                Double basePrice = room.getSeasonalPrices().get(season);
                List<String> prices = new ArrayList<>();
                for (MarketSegment segment : segments) {
                    prices.add(money.format(value(basePrice) * segment.getRateMultiplier()));
                }
                segmentPriceRows.add(new SegmentPriceRow(room.getName(), first, season.name().replace('_', ' '),
                        money.format(value(basePrice)), prices));
                first = false;
            }
        }

        List<String> forecastRoomNames = new ArrayList<>();
        List<ForecastRow> forecastRows = new ArrayList<>();
        boolean anyChanged = false;
        if (!forecast.isEmpty()) {
            Object firstPrices = forecast.get(0).get("roomPrices");
            if (firstPrices instanceof Map) {
                for (Object roomName : ((Map<?, ?>) firstPrices).keySet()) {
                    forecastRoomNames.add(String.valueOf(roomName));
                }
            }
            for (Map<String, Object> month : forecast) {
                List<String> eventNames = new ArrayList<>();
                Object rawEvents = month.get("events");
                if (rawEvents instanceof List) {
                    for (Object rawEvent : (List<?>) rawEvents) {
                        if (rawEvent instanceof FutureEvent) {
                            eventNames.add(((FutureEvent) rawEvent).getEventName());
                        }
                    }
                }
                List<String> prices = new ArrayList<>();
                Object rawPrices = month.get("roomPrices");
                Map<?, ?> roomPrices = rawPrices instanceof Map ? (Map<?, ?>) rawPrices : Collections.emptyMap();
                for (String roomName : forecastRoomNames) {
                    Object price = roomPrices.get(roomName);
                    prices.add(money.format(price instanceof Number ? ((Number) price).doubleValue() : 0));
                }
                boolean changed = Boolean.TRUE.equals(month.get("seasonChanged"));
                anyChanged |= changed;
                forecastRows.add(new ForecastRow(String.valueOf(month.get("month")), eventNames,
                        formatSeason(month.get("baseSeason")), formatSeason(month.get("adjustedSeason")), changed, prices));
            }
        }

        Double minimum = doubleAttribute(request, "achievableMinRevenue");
        Double maximum = doubleAttribute(request, "achievableMaxRevenue");
        Integer forecastYear = integerAttribute(request, "forecastYear");
        Integer adjustedMonths = integerAttribute(request, "eventAdjustedMonthCount");
        List<String> highlights = listAttribute(request, "constraintHighlights");
        return new View(ready, roomTypeCount, monthlyCount, hasResults,
                stringAttribute(request, "error"), stringAttribute(request, "optimizationWarning"),
                expectedRevenue == null ? "" : String.format(Locale.US, "%.2f", expectedRevenue),
                hasResults ? money.format(expectedRevenue) : "0.00", hasResults ? money.format(estimatedRevenue) : "0.00",
                money.format(value(minimum)), money.format(value(maximum)), number.format(value(doubleAttribute(request, "accuracy"))),
                defaultString(stringAttribute(request, "targetDifficulty"), "n/a"),
                defaultString(stringAttribute(request, "demandCurveMode"), "Awaiting run"),
                defaultString(stringAttribute(request, "demandCurveSummary"), ""), highlights, roomRows, segmentRows,
                segmentPriceRows, forecastRoomNames, forecastRows, anyChanged,
                forecastYear == null ? Calendar.getInstance().get(Calendar.YEAR) : forecastYear,
                adjustedMonths == null ? 0 : adjustedMonths);
    }

    private static String formatSeason(Object season) {
        return season == null ? "" : String.valueOf(season).replace('_', ' ');
    }

    private static double value(Double number) {
        return number == null ? 0 : number;
    }

    private static int intAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static Integer integerAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private static Double doubleAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private static String stringAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> listAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof List ? (List<T>) value : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> mapAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof Map ? (Map<String, String>) value : Collections.emptyMap();
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    public static final class View {
        private final boolean readyForOptimization, hasResults, anyForecastChanges;
        private final int roomTypeCount, monthlyCount, forecastYear, eventAdjustedMonthCount;
        private final String error, optimizationWarning, expectedRevenueInput, expectedRevenue, estimatedRevenue,
                achievableMinimum, achievableMaximum, accuracy, targetDifficulty, demandCurveMode, demandCurveSummary;
        private final List<String> constraintHighlights, forecastRoomNames;
        private final List<RoomRow> roomRows;
        private final List<Segment> segments;
        private final List<SegmentPriceRow> segmentPriceRows;
        private final List<ForecastRow> forecastRows;

        private View(boolean readyForOptimization, int roomTypeCount, int monthlyCount, boolean hasResults, String error,
                     String optimizationWarning, String expectedRevenueInput, String expectedRevenue, String estimatedRevenue,
                     String achievableMinimum, String achievableMaximum, String accuracy, String targetDifficulty,
                     String demandCurveMode, String demandCurveSummary, List<String> constraintHighlights,
                     List<RoomRow> roomRows, List<Segment> segments, List<SegmentPriceRow> segmentPriceRows,
                     List<String> forecastRoomNames, List<ForecastRow> forecastRows, boolean anyForecastChanges,
                     int forecastYear, int eventAdjustedMonthCount) {
            this.readyForOptimization = readyForOptimization;
            this.roomTypeCount = roomTypeCount;
            this.monthlyCount = monthlyCount;
            this.hasResults = hasResults;
            this.error = error;
            this.optimizationWarning = optimizationWarning;
            this.expectedRevenueInput = expectedRevenueInput;
            this.expectedRevenue = expectedRevenue;
            this.estimatedRevenue = estimatedRevenue;
            this.achievableMinimum = achievableMinimum;
            this.achievableMaximum = achievableMaximum;
            this.accuracy = accuracy;
            this.targetDifficulty = targetDifficulty;
            this.demandCurveMode = demandCurveMode;
            this.demandCurveSummary = demandCurveSummary;
            this.constraintHighlights = constraintHighlights;
            this.roomRows = roomRows;
            this.segments = segments;
            this.segmentPriceRows = segmentPriceRows;
            this.forecastRoomNames = forecastRoomNames;
            this.forecastRows = forecastRows;
            this.anyForecastChanges = anyForecastChanges;
            this.forecastYear = forecastYear;
            this.eventAdjustedMonthCount = eventAdjustedMonthCount;
        }
        public boolean isReadyForOptimization() { return readyForOptimization; }
        public int getRoomTypeCount() { return roomTypeCount; }
        public int getMonthlyCount() { return monthlyCount; }
        public boolean isHasResults() { return hasResults; }
        public String getError() { return error; }
        public String getOptimizationWarning() { return optimizationWarning; }
        public String getExpectedRevenueInput() { return expectedRevenueInput; }
        public String getExpectedRevenue() { return expectedRevenue; }
        public String getEstimatedRevenue() { return estimatedRevenue; }
        public String getAchievableMinimum() { return achievableMinimum; }
        public String getAchievableMaximum() { return achievableMaximum; }
        public String getAccuracy() { return accuracy; }
        public String getTargetDifficulty() { return targetDifficulty; }
        public String getDemandCurveMode() { return demandCurveMode; }
        public String getDemandCurveSummary() { return demandCurveSummary; }
        public List<String> getConstraintHighlights() { return constraintHighlights; }
        public List<RoomRow> getRoomRows() { return roomRows; }
        public List<Segment> getSegments() { return segments; }
        public List<SegmentPriceRow> getSegmentPriceRows() { return segmentPriceRows; }
        public List<String> getForecastRoomNames() { return forecastRoomNames; }
        public List<ForecastRow> getForecastRows() { return forecastRows; }
        public boolean isAnyForecastChanges() { return anyForecastChanges; }
        public int getForecastYear() { return forecastYear; }
        public int getEventAdjustedMonthCount() { return eventAdjustedMonthCount; }
    }

    public static final class RoomRow {
        private final String name, roomCount, baseAdr, minAdr, maxAdr, estimatedRevenue;
        private final SeasonPrice low, normal, peak, superPeak;
        private RoomRow(Room room, DecimalFormat money, DecimalFormat number, Map<String, String> states) {
            this.name = room.getName();
            this.roomCount = String.valueOf(room.getTotalRooms());
            this.baseAdr = money.format(room.getBaseAdr()); this.minAdr = money.format(room.getMinAdr());
            this.maxAdr = money.format(room.getMaxAdr()); this.estimatedRevenue = money.format(room.getEstimatedRevenue());
            this.low = price(room, Season.LOW, money, number, states);
            this.normal = price(room, Season.NORMAL, money, number, states);
            this.peak = price(room, Season.PEAK, money, number, states);
            this.superPeak = price(room, Season.SUPER_PEAK, money, number, states);
        }
        private static SeasonPrice price(Room room, Season season, DecimalFormat money, DecimalFormat number, Map<String, String> states) {
            return new SeasonPrice(money.format(value(room.getSeasonalPrices().get(season))),
                    number.format(room.getOccupancyForSeason(season)), states.get(room.getName() + "|" + season.name()));
        }
        public String getName() { return name; } public String getRoomCount() { return roomCount; }
        public String getBaseAdr() { return baseAdr; } public String getMinAdr() { return minAdr; }
        public String getMaxAdr() { return maxAdr; } public String getEstimatedRevenue() { return estimatedRevenue; }
        public SeasonPrice getLow() { return low; } public SeasonPrice getNormal() { return normal; }
        public SeasonPrice getPeak() { return peak; } public SeasonPrice getSuperPeak() { return superPeak; }
    }

    public static final class SeasonPrice {
        private final String price, occupancy, constraint;
        private SeasonPrice(String price, String occupancy, String constraint) { this.price = price; this.occupancy = occupancy; this.constraint = constraint; }
        public String getPrice() { return price; } public String getOccupancy() { return occupancy; }
        public boolean isFloor() { return "FLOOR".equals(constraint); } public boolean isCeiling() { return "CEILING".equals(constraint); }
    }

    public static final class Segment {
        private final String code, description, multiplier, badgeClass;
        private Segment(MarketSegment segment, DecimalFormat number) {
            this.code = segment.getSegmentCode();
            this.description = segment.getDescription() == null ? segment.getSegmentName() : segment.getDescription();
            this.multiplier = number.format(segment.getRateMultiplier());
            this.badgeClass = segment.getRateMultiplier() > 1 ? "premium" : segment.getRateMultiplier() < 1 ? "discount" : "neutral";
        }
        public String getCode() { return code; } public String getDescription() { return description; }
        public String getMultiplier() { return multiplier; } public String getBadgeClass() { return badgeClass; }
    }

    public static final class SegmentPriceRow {
        private final String roomName, season, basePrice; private final boolean firstSeason; private final List<String> segmentPrices;
        private SegmentPriceRow(String roomName, boolean firstSeason, String season, String basePrice, List<String> segmentPrices) {
            this.roomName = roomName; this.firstSeason = firstSeason; this.season = season; this.basePrice = basePrice; this.segmentPrices = segmentPrices;
        }
        public String getRoomName() { return roomName; } public boolean isFirstSeason() { return firstSeason; }
        public String getSeason() { return season; } public String getBasePrice() { return basePrice; }
        public List<String> getSegmentPrices() { return segmentPrices; }
    }

    public static final class ForecastRow {
        private final String month, baseSeason, adjustedSeason; private final List<String> events, prices; private final boolean changed;
        private ForecastRow(String month, List<String> events, String baseSeason, String adjustedSeason, boolean changed, List<String> prices) {
            this.month = month; this.events = events; this.baseSeason = baseSeason; this.adjustedSeason = adjustedSeason; this.changed = changed; this.prices = prices;
        }
        public String getMonth() { return month; } public List<String> getEvents() { return events; }
        public String getBaseSeason() { return baseSeason; } public String getAdjustedSeason() { return adjustedSeason; }
        public boolean isChanged() { return changed; } public List<String> getPrices() { return prices; }
    }
}
