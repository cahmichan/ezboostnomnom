package com.ezboost.service;

import com.ezboost.model.FutureEvent;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON snapshot used to regenerate an export without relying on HTTP session state. */
public final class OptimizationReportSnapshot {

    private final List<Room> rooms;
    private final double targetRevenue;
    private final double estimatedRevenue;
    private final List<MarketSegment> marketSegments;
    private final List<Map<String, Object>> monthlyForecast;
    private final Integer forecastYear;
    private final String demandCurveMode;
    private final Double achievableMinRevenue;
    private final Double achievableMaxRevenue;
    private final List<String> constraintHighlights;

    public OptimizationReportSnapshot(List<Room> rooms, double targetRevenue, double estimatedRevenue,
                                      List<MarketSegment> marketSegments, List<Map<String, Object>> monthlyForecast,
                                      Integer forecastYear, String demandCurveMode, Double achievableMinRevenue,
                                      Double achievableMaxRevenue, List<String> constraintHighlights) {
        this.rooms = rooms == null ? Collections.emptyList() : rooms;
        this.targetRevenue = targetRevenue;
        this.estimatedRevenue = estimatedRevenue;
        this.marketSegments = marketSegments == null ? Collections.emptyList() : marketSegments;
        this.monthlyForecast = monthlyForecast == null ? Collections.emptyList() : monthlyForecast;
        this.forecastYear = forecastYear;
        this.demandCurveMode = demandCurveMode == null ? "Historical fit" : demandCurveMode;
        this.achievableMinRevenue = achievableMinRevenue;
        this.achievableMaxRevenue = achievableMaxRevenue;
        this.constraintHighlights = constraintHighlights == null ? Collections.emptyList() : constraintHighlights;
    }

    public String toJson() {
        JSONObject root = new JSONObject();
        root.put("targetRevenue", targetRevenue);
        root.put("estimatedRevenue", estimatedRevenue);
        root.put("forecastYear", forecastYear == null ? JSONObject.NULL : forecastYear);
        root.put("demandCurveMode", demandCurveMode);
        root.put("achievableMinRevenue", achievableMinRevenue == null ? JSONObject.NULL : achievableMinRevenue);
        root.put("achievableMaxRevenue", achievableMaxRevenue == null ? JSONObject.NULL : achievableMaxRevenue);
        root.put("constraintHighlights", new JSONArray(constraintHighlights));

        JSONArray roomArray = new JSONArray();
        for (Room room : rooms) {
            JSONObject item = new JSONObject();
            item.put("name", room.getName());
            item.put("baseAdr", room.getBaseAdr());
            item.put("minAdr", room.getMinAdr());
            item.put("maxAdr", room.getMaxAdr());
            item.put("occupancy", room.getOccupancy());
            item.put("totalRooms", room.getTotalRooms());
            JSONObject prices = new JSONObject();
            JSONObject occupancies = new JSONObject();
            for (Season season : Season.values()) {
                prices.put(season.name(), room.getSeasonalPrices().get(season));
                occupancies.put(season.name(), room.getOccupancyForSeason(season));
            }
            item.put("prices", prices);
            item.put("occupancies", occupancies);
            roomArray.put(item);
        }
        root.put("rooms", roomArray);

        JSONArray segmentArray = new JSONArray();
        for (MarketSegment segment : marketSegments) {
            JSONObject item = new JSONObject();
            item.put("id", segment.getId());
            item.put("userId", segment.getUserId());
            item.put("name", segment.getSegmentName());
            item.put("code", segment.getSegmentCode());
            item.put("category", segment.getCategory());
            item.put("multiplier", segment.getRateMultiplier());
            item.put("description", segment.getDescription());
            item.put("active", segment.isActive());
            segmentArray.put(item);
        }
        root.put("segments", segmentArray);
        root.put("forecast", encodeForecast(monthlyForecast));
        return root.toString();
    }

    public static OptimizationReportSnapshot fromJson(String json) {
        JSONObject root = new JSONObject(json);
        List<Room> rooms = decodeRooms(root.getJSONArray("rooms"));
        List<MarketSegment> segments = decodeSegments(root.optJSONArray("segments"));
        List<Map<String, Object>> forecast = decodeForecast(root.optJSONArray("forecast"));
        List<String> highlights = new ArrayList<>();
        JSONArray highlightsArray = root.optJSONArray("constraintHighlights");
        if (highlightsArray != null) {
            for (int i = 0; i < highlightsArray.length(); i++) {
                highlights.add(highlightsArray.getString(i));
            }
        }
        return new OptimizationReportSnapshot(rooms, root.getDouble("targetRevenue"), root.getDouble("estimatedRevenue"),
                segments, forecast, root.isNull("forecastYear") ? null : root.getInt("forecastYear"),
                root.optString("demandCurveMode", "Historical fit"),
                root.isNull("achievableMinRevenue") ? null : root.getDouble("achievableMinRevenue"),
                root.isNull("achievableMaxRevenue") ? null : root.getDouble("achievableMaxRevenue"), highlights);
    }

    public List<Room> getRooms() { return rooms; }
    public double getTargetRevenue() { return targetRevenue; }
    public double getEstimatedRevenue() { return estimatedRevenue; }
    public List<MarketSegment> getMarketSegments() { return marketSegments; }
    public List<Map<String, Object>> getMonthlyForecast() { return monthlyForecast; }
    public Integer getForecastYear() { return forecastYear; }
    public String getDemandCurveMode() { return demandCurveMode; }
    public Double getAchievableMinRevenue() { return achievableMinRevenue; }
    public Double getAchievableMaxRevenue() { return achievableMaxRevenue; }
    public List<String> getConstraintHighlights() { return constraintHighlights; }

    private JSONArray encodeForecast(List<Map<String, Object>> forecast) {
        JSONArray array = new JSONArray();
        for (Map<String, Object> month : forecast) {
            JSONObject item = new JSONObject();
            item.put("month", month.get("month"));
            item.put("monthNum", month.get("monthNum"));
            item.put("baseSeason", month.get("baseSeason"));
            item.put("adjustedSeason", month.get("adjustedSeason"));
            item.put("seasonChanged", month.get("seasonChanged"));
            item.put("events", encodeEvents((List<FutureEvent>) month.get("events")));
            item.put("roomPrices", new JSONObject((Map<String, Object>) month.get("roomPrices")));
            item.put("roomOccupancies", new JSONObject((Map<String, Object>) month.get("roomOccupancies")));
            array.put(item);
        }
        return array;
    }

    private JSONArray encodeEvents(List<FutureEvent> events) {
        JSONArray array = new JSONArray();
        if (events == null) return array;
        for (FutureEvent event : events) {
            JSONObject item = new JSONObject();
            item.put("name", event.getEventName());
            item.put("date", event.getEventDate() == null ? JSONObject.NULL : event.getEventDate().toString());
            item.put("endDate", event.getEventEndDate() == null ? JSONObject.NULL : event.getEventEndDate().toString());
            item.put("type", event.getEventType());
            item.put("seasonOverride", event.getSeasonOverride());
            item.put("source", event.getSource());
            item.put("active", event.isActive());
            array.put(item);
        }
        return array;
    }

    private static List<Room> decodeRooms(JSONArray array) {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Room room = new Room(item.getString("name"), item.getDouble("baseAdr"), item.getDouble("minAdr"),
                    item.getDouble("maxAdr"), item.getDouble("occupancy"), item.getInt("totalRooms"));
            JSONObject prices = item.getJSONObject("prices");
            JSONObject occupancies = item.getJSONObject("occupancies");
            for (Season season : Season.values()) {
                room.setPriceForSeason(season, prices.getDouble(season.name()));
                room.setOccupancyForSeason(season, occupancies.getDouble(season.name()));
            }
            rooms.add(room);
        }
        return rooms;
    }

    private static List<MarketSegment> decodeSegments(JSONArray array) {
        List<MarketSegment> segments = new ArrayList<>();
        if (array == null) return segments;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            MarketSegment segment = new MarketSegment();
            segment.setId(item.optInt("id"));
            segment.setUserId(item.optInt("userId"));
            segment.setSegmentName(item.optString("name"));
            segment.setSegmentCode(item.optString("code"));
            segment.setCategory(item.optString("category"));
            segment.setRateMultiplier(item.optDouble("multiplier", 1.0));
            segment.setDescription(item.optString("description"));
            segment.setActive(item.optBoolean("active", true));
            segments.add(segment);
        }
        return segments;
    }

    private static List<Map<String, Object>> decodeForecast(JSONArray array) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (array == null) return result;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Map<String, Object> month = new LinkedHashMap<>();
            month.put("month", item.optString("month"));
            month.put("monthNum", item.optInt("monthNum"));
            month.put("baseSeason", item.optString("baseSeason"));
            month.put("adjustedSeason", item.optString("adjustedSeason"));
            month.put("seasonChanged", item.optBoolean("seasonChanged"));
            month.put("events", decodeEvents(item.optJSONArray("events")));
            month.put("roomPrices", decodeDoubleMap(item.optJSONObject("roomPrices")));
            month.put("roomOccupancies", decodeDoubleMap(item.optJSONObject("roomOccupancies")));
            result.add(month);
        }
        return result;
    }

    private static List<FutureEvent> decodeEvents(JSONArray array) {
        List<FutureEvent> events = new ArrayList<>();
        if (array == null) return events;
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            FutureEvent event = new FutureEvent();
            event.setEventName(item.optString("name"));
            if (!item.isNull("date")) event.setEventDate(Date.valueOf(item.getString("date")));
            if (!item.isNull("endDate")) event.setEventEndDate(Date.valueOf(item.getString("endDate")));
            event.setEventType(item.optString("type"));
            event.setSeasonOverride(item.optString("seasonOverride"));
            event.setSource(item.optString("source"));
            event.setActive(item.optBoolean("active", true));
            events.add(event);
        }
        return events;
    }

    private static Map<String, Double> decodeDoubleMap(JSONObject object) {
        Map<String, Double> values = new LinkedHashMap<>();
        if (object == null) return values;
        for (String key : object.keySet()) values.put(key, object.getDouble(key));
        return values;
    }
}
