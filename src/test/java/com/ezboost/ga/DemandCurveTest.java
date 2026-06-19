package com.ezboost.ga;

import com.ezboost.model.MonthlySeasonData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemandCurveTest {

    @Test
    void testDefaultCurve() {
        DemandCurve curve = DemandCurve.createDefault();
        assertNotNull(curve);
        assertEquals(100.0, curve.getIntercept(), 0.01);
        assertEquals(-0.05, curve.getSlope(), 0.001);
        assertEquals(0.0, curve.getRSquared(), 0.01);
    }

    @Test
    void testDefaultCurveOccupancyAtKnownPrices() {
        DemandCurve curve = DemandCurve.createDefault();
        // At RM400: 100 + (-0.05)*400 = 80%
        assertEquals(80.0, curve.getOccupancy(400), 0.01);
        // At RM1000: 100 + (-0.05)*1000 = 50%
        assertEquals(50.0, curve.getOccupancy(1000), 0.01);
    }

    @Test
    void testOccupancyClampsToMinimum() {
        DemandCurve curve = DemandCurve.createDefault();
        // At extremely high price, occupancy should not go below 5%
        double occ = curve.getOccupancy(50000);
        assertEquals(5.0, occ, 0.01);
    }

    @Test
    void testOccupancyClampsToMaximum() {
        DemandCurve curve = DemandCurve.createDefault();
        // At price 0: 100 + 0 = 100, but clamped to 98
        double occ = curve.getOccupancy(0);
        assertEquals(98.0, occ, 0.01);
    }

    @Test
    void testOptimalPriceForDefaultCurve() {
        DemandCurve curve = DemandCurve.createDefault();
        // Optimal = -100 / (2 * -0.05) = 1000
        assertEquals(1000.0, curve.getOptimalPrice(), 0.01);
    }

    @Test
    void testFitFromDataWithInsufficientData() {
        List<MonthlySeasonData> data = new ArrayList<>();
        // Less than 3 points -> should return default
        MonthlySeasonData d1 = new MonthlySeasonData();
        d1.setAvgRoomRate(300);
        d1.setOccupancyRate(80);
        data.add(d1);

        DemandCurve curve = DemandCurve.fitFromData(data);
        // Should be default curve
        assertEquals(100.0, curve.getIntercept(), 0.01);
        assertEquals(-0.05, curve.getSlope(), 0.001);
    }

    @Test
    void testFitFromNullData() {
        DemandCurve curve = DemandCurve.fitFromData(null);
        assertNotNull(curve);
        assertEquals(100.0, curve.getIntercept(), 0.01);
    }

    @Test
    void testFitFromValidData() {
        List<MonthlySeasonData> data = new ArrayList<>();
        // Create data with clear negative slope
        double[][] points = {
            {200, 90}, {300, 85}, {400, 80}, {500, 75}, {600, 70}
        };
        for (double[] p : points) {
            MonthlySeasonData d = new MonthlySeasonData();
            d.setAvgRoomRate(p[0]);
            d.setOccupancyRate(p[1]);
            data.add(d);
        }

        DemandCurve curve = DemandCurve.fitFromData(data);
        assertTrue(curve.getSlope() < 0, "Slope should be negative");
        assertTrue(curve.getRSquared() > 0.9, "R-squared should be high for linear data");
    }

    @Test
    void testToString() {
        DemandCurve curve = DemandCurve.createDefault();
        String str = curve.toString();
        assertTrue(str.contains("DemandCurve"));
        assertTrue(str.contains("intercept"));
        assertTrue(str.contains("slope"));
    }
}
