package com.ezboost.service;

import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoostMePageServiceTest {

    @Test
    void preparesEscapableDisplayRowsWithoutLeavingCalculationsInTheView() {
        Room room = new Room("Deluxe <Suite>", 220, 180, 260, 75, 10);
        room.setPriceForSeason(Season.LOW, 190);
        room.setPriceForSeason(Season.NORMAL, 220);
        room.setPriceForSeason(Season.PEAK, 250);
        room.setPriceForSeason(Season.SUPER_PEAK, 260);
        room.setOccupancyForSeason(Season.LOW, 65);

        MarketSegment segment = new MarketSegment("Corporate", "CORP", "FIT", 1.1, "Corporate guests");
        Map<String, Object> values = new HashMap<>();
        values.put("roomTypeCount", 1);
        values.put("monthlyCount", 12);
        values.put("readyForOptimization", true);
        values.put("expectedRevenue", 100000.0);
        values.put("estimatedRevenue", 99000.0);
        values.put("bestSolution", List.of(room));
        values.put("marketSegments", List.of(segment));
        values.put("priceConstraintStates", Map.of("Deluxe <Suite>|LOW", "FLOOR",
                "Deluxe <Suite>|SUPER_PEAK", "CEILING"));
        values.put("achievableMinRevenue", 90000.0);
        values.put("achievableMaxRevenue", 110000.0);
        values.put("accuracy", 99.0);
        values.put("targetDifficulty", "Comfortable");
        values.put("demandCurveMode", "Historical fit");
        values.put("demandCurveSummary", "Historical data used.");
        values.put("constraintHighlights", Arrays.asList("Low price constrained."));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(invocation -> values.get(invocation.getArgument(0)));

        BoostMePageService.View view = BoostMePageService.buildView(request);

        assertTrue(view.isHasResults());
        assertEquals("100,000.00", view.getExpectedRevenue());
        assertEquals("Deluxe <Suite>", view.getRoomRows().get(0).getName());
        assertEquals("190.00", view.getRoomRows().get(0).getLow().getPrice());
        assertEquals("65.00", view.getRoomRows().get(0).getLow().getOccupancy());
        assertTrue(view.getRoomRows().get(0).getLow().isFloor());
        assertTrue(view.getRoomRows().get(0).getSuperPeak().isCeiling());
        assertEquals("premium", view.getSegments().get(0).getBadgeClass());
        assertEquals("209.00", view.getSegmentPriceRows().get(0).getSegmentPrices().get(0));
        assertFalse(view.isAnyForecastChanges());
    }
}
