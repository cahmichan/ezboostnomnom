package com.ezboost.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    private Room room;
    private Room explicitBaseRoom;

    @BeforeEach
    void setUp() {
        room = new Room("Deluxe", 200.0, 400.0, 75.0, 50);
        explicitBaseRoom = new Room("Premier", 320.0, 250.0, 450.0, 80.0, 12);
    }

    @Test
    void testConstructorSetsFields() {
        assertEquals("Deluxe", room.getName());
        assertEquals(200.0, room.getMinAdr());
        assertEquals(400.0, room.getMaxAdr());
        assertEquals(75.0, room.getOccupancy());
        assertEquals(50, room.getTotalRooms());
    }

    @Test
    void testBaseAdrIsMidpoint() {
        assertEquals(300.0, room.getBaseAdr(), 0.01);
    }

    @Test
    void testExplicitBaseAdrIsPreserved() {
        assertEquals(320.0, explicitBaseRoom.getBaseAdr(), 0.01);
        assertEquals(250.0, explicitBaseRoom.getMinAdr(), 0.01);
        assertEquals(450.0, explicitBaseRoom.getMaxAdr(), 0.01);
    }

    @Test
    void testExplicitBaseAdrClampsToBounds() {
        Room clamped = new Room("Suite", 500.0, 200.0, 450.0, 70.0, 5);
        assertEquals(450.0, clamped.getBaseAdr(), 0.01);
    }

    @Test
    void testSeasonalPricesGeneratedOnConstruction() {
        assertNotNull(room.getSeasonalPrices());
        assertEquals(4, room.getSeasonalPrices().size());
        for (Season season : Season.values()) {
            assertTrue(room.getSeasonalPrices().containsKey(season));
            assertTrue(room.getSeasonalPrices().get(season) > 0);
        }
    }

    @Test
    void testSetPriceForSeason() {
        room.setPriceForSeason(Season.PEAK, 500.0);
        assertEquals(500.0, room.getSeasonalPrices().get(Season.PEAK), 0.01);
    }

    @Test
    void testSetOccupancy() {
        room.setOccupancy(85.0);
        assertEquals(85.0, room.getOccupancy(), 0.01);
    }

    @Test
    void testSetOccupancyClampsToZero() {
        room.setOccupancy(-10.0);
        assertEquals(0.0, room.getOccupancy(), 0.01);
    }

    @Test
    void testSetOccupancyClampsToHundred() {
        room.setOccupancy(150.0);
        assertEquals(100.0, room.getOccupancy(), 0.01);
    }

    @Test
    void testSeasonalOccupancyDefaultsToFlat() {
        assertFalse(room.hasSeasonalOccupancies());
        assertEquals(75.0, room.getOccupancyForSeason(Season.LOW), 0.01);
        assertEquals(75.0, room.getOccupancyForSeason(Season.PEAK), 0.01);
    }

    @Test
    void testSetAndGetSeasonalOccupancy() {
        room.setOccupancyForSeason(Season.PEAK, 90.0);
        room.setOccupancyForSeason(Season.LOW, 50.0);

        assertTrue(room.hasSeasonalOccupancies());
        assertEquals(90.0, room.getOccupancyForSeason(Season.PEAK), 0.01);
        assertEquals(50.0, room.getOccupancyForSeason(Season.LOW), 0.01);
        // Unset season falls back to flat occupancy
        assertEquals(75.0, room.getOccupancyForSeason(Season.NORMAL), 0.01);
    }

    @Test
    void testGetEstimatedRevenuePositive() {
        double revenue = room.getEstimatedRevenue();
        assertTrue(revenue > 0, "Estimated revenue should be positive");
    }

    @Test
    void testGetAverageSeasonalPrice() {
        double avg = room.getAverageSeasonalPrice();
        assertTrue(avg > 0);
        // Average of seasonal prices should be near base ADR
        assertEquals(room.getBaseAdr(), avg, room.getBaseAdr() * 0.5);
    }
}
