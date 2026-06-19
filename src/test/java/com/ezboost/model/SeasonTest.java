package com.ezboost.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeasonTest {

    @Test
    void testFourSeasons() {
        assertEquals(4, Season.values().length);
    }

    @Test
    void testSeasonOrder() {
        Season[] seasons = Season.values();
        assertEquals(Season.LOW, seasons[0]);
        assertEquals(Season.NORMAL, seasons[1]);
        assertEquals(Season.PEAK, seasons[2]);
        assertEquals(Season.SUPER_PEAK, seasons[3]);
    }

    @Test
    void testLowSeasonMultipliers() {
        assertEquals(0.2, Season.LOW.getMinMultiplier(), 0.01);
        assertEquals(0.9, Season.LOW.getMaxMultiplier(), 0.01);
    }

    @Test
    void testMultipliersIncreaseWithSeason() {
        assertTrue(Season.LOW.getMinMultiplier() < Season.NORMAL.getMinMultiplier());
        assertTrue(Season.NORMAL.getMinMultiplier() < Season.PEAK.getMinMultiplier());
        assertTrue(Season.PEAK.getMinMultiplier() < Season.SUPER_PEAK.getMinMultiplier());

        assertTrue(Season.LOW.getMaxMultiplier() < Season.NORMAL.getMaxMultiplier());
        assertTrue(Season.NORMAL.getMaxMultiplier() < Season.PEAK.getMaxMultiplier());
        assertTrue(Season.PEAK.getMaxMultiplier() < Season.SUPER_PEAK.getMaxMultiplier());
    }

    @Test
    void testScaleFactorsIncreaseWithSeason() {
        assertTrue(Season.LOW.getScaleFactor() < Season.NORMAL.getScaleFactor());
        assertTrue(Season.NORMAL.getScaleFactor() < Season.PEAK.getScaleFactor());
        assertTrue(Season.PEAK.getScaleFactor() < Season.SUPER_PEAK.getScaleFactor());
    }
}
