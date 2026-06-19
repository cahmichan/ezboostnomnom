package com.ezboost.service;

import com.ezboost.model.FutureEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalendarificService {

    private static final Logger logger = LoggerFactory.getLogger(CalendarificService.class);

    private static final String API_BASE_URL = "https://calendarific.com/api/v2/holidays";

    // Major holidays that deserve SUPER_PEAK classification
    private static final Set<String> SUPER_PEAK_KEYWORDS = new HashSet<>(Arrays.asList(
            "hari raya", "aidilfitri", "aidiladha", "chinese new year",
            "deepavali", "diwali", "christmas", "new year"
    ));

    /**
     * Fetch Malaysian public holidays from Calendarific API
     */
    public List<FutureEvent> fetchHolidays(String apiKey, int year, int userId) throws Exception {
        List<FutureEvent> events = new ArrayList<>();

        String urlStr = API_BASE_URL + "?api_key=" + URLEncoder.encode(apiKey, "UTF-8")
                + "&country=MY&year=" + year;

        logger.debug("Fetching holidays for year {}", year);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            String errorMsg = "API returned status " + responseCode;
            // Read error stream for more details
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                errorMsg += ": " + sb.toString();
            } catch (Exception ignored) {}
            throw new Exception(errorMsg);
        }

        StringBuilder responseBody = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                responseBody.append(line);
            }
        }

        JSONObject json = new JSONObject(responseBody.toString());
        JSONObject response = json.getJSONObject("response");
        JSONArray holidays = response.getJSONArray("holidays");

        logger.debug("Received {} holidays", holidays.length());

        for (int i = 0; i < holidays.length(); i++) {
            JSONObject holiday = holidays.getJSONObject(i);
            String name = holiday.getString("name");
            String description = holiday.optString("description", "");

            // Get date
            JSONObject dateObj = holiday.getJSONObject("date");
            JSONObject iso = dateObj.optJSONObject("datetime");
            if (iso == null) continue;

            int y = iso.getInt("year");
            int m = iso.getInt("month");
            int d = iso.getInt("day");
            Date eventDate = Date.valueOf(String.format("%d-%02d-%02d", y, m, d));

            // Check holiday type
            JSONArray types = holiday.optJSONArray("type");
            boolean isNationalHoliday = false;
            if (types != null) {
                for (int t = 0; t < types.length(); t++) {
                    String type = types.getString(t);
                    if ("National holiday".equalsIgnoreCase(type) ||
                        "Public holiday".equalsIgnoreCase(type) ||
                        "Gazetted holiday".equalsIgnoreCase(type)) {
                        isNationalHoliday = true;
                        break;
                    }
                }
            }

            // Only include national/public holidays (skip observances etc.)
            if (!isNationalHoliday) continue;

            // Determine season override
            String seasonOverride = determineSuperPeak(name) ? "SUPER_PEAK" : "PEAK";

            FutureEvent event = new FutureEvent();
            event.setUserId(userId);
            event.setEventName(name);
            event.setEventDate(eventDate);
            event.setEventEndDate(null);
            event.setEventType("PUBLIC_HOLIDAY");
            event.setSeasonOverride(seasonOverride);
            event.setSource("CALENDARIFIC");
            event.setActive(true);

            events.add(event);
        }

        logger.debug("Filtered to {} national/public holidays", events.size());
        return events;
    }

    /**
     * Check if a holiday name matches a major holiday that should be SUPER_PEAK
     */
    private boolean determineSuperPeak(String holidayName) {
        String lower = holidayName.toLowerCase();
        for (String keyword : SUPER_PEAK_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate Malaysian school holiday presets for a given year.
     * Based on typical Malaysian school calendar:
     * - Term 1 break: ~mid March (1 week)
     * - Mid-year break: ~late May to mid June (2-3 weeks)
     * - Term 3 break: ~late August (1 week)
     * - Year-end break: ~late November to end December (5-6 weeks)
     */
    public List<FutureEvent> getSchoolHolidayPresets(int year, int userId) {
        List<FutureEvent> events = new ArrayList<>();

        // Term 1 break: March 15-23 (approx)
        events.add(createSchoolHoliday(userId, "Term 1 School Break", year,
                3, 15, 3, 23, "PEAK"));

        // Mid-year break: May 24 - June 9 (approx)
        events.add(createSchoolHoliday(userId, "Mid-Year School Break", year,
                5, 24, 6, 9, "PEAK"));

        // Term 3 break: August 23-31 (approx)
        events.add(createSchoolHoliday(userId, "Term 3 School Break", year,
                8, 23, 8, 31, "PEAK"));

        // Year-end break: November 22 - December 31 (approx)
        events.add(createSchoolHoliday(userId, "Year-End School Break", year,
                11, 22, 12, 31, "SUPER_PEAK"));

        logger.debug("Generated {} school holiday presets for {}", events.size(), year);
        return events;
    }

    private FutureEvent createSchoolHoliday(int userId, String name, int year,
                                            int startMonth, int startDay,
                                            int endMonth, int endDay,
                                            String seasonOverride) {
        FutureEvent event = new FutureEvent();
        event.setUserId(userId);
        event.setEventName(name);
        event.setEventDate(Date.valueOf(String.format("%d-%02d-%02d", year, startMonth, startDay)));
        event.setEventEndDate(Date.valueOf(String.format("%d-%02d-%02d", year, endMonth, endDay)));
        event.setEventType("SCHOOL_BREAK");
        event.setSeasonOverride(seasonOverride);
        event.setSource("PRESET");
        event.setActive(true);
        return event;
    }
}
