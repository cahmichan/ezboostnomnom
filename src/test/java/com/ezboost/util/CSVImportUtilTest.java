package com.ezboost.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CSVImportUtilTest {

    @Test
    void testParseMonthlyCsvResultTracksRejectedRows() {
        String csv = "MonthYear,MonthName,OccupancyRate,TotalRevenue,AvgRoomRate\n" +
                "2024-01,January,75,100000,180\n" +
                ",February,80,120000,190\n" +
                "2024-03,March,150,90000,170\n";

        CSVImportUtil.ParseResult result = CSVImportUtil.parseCSVResult(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 7);

        assertEquals(1, result.getAcceptedRows());
        assertEquals(2, result.getRejectedRows());
    }

    @Test
    void testParseMonthlyCsvResultWarnsWhenThresholdFallbackLikely() {
        String csv = "MonthYear,MonthName,OccupancyRate,TotalRevenue,AvgRoomRate\n" +
                "2024-01,January,75,100000,0\n" +
                "2024-02,February,78,120000,0\n" +
                "2024-03,March,74,118000,0\n";

        CSVImportUtil.ParseResult result = CSVImportUtil.parseCSVResult(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 7);

        assertEquals(3, result.getAcceptedRows());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("Fewer than 4 valid month rows")));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("ADR of 0")));
    }
}
