package com.ezboost.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportDisplaySafetyTest {

    @Test
    void rejectsMonthlyLabelsThatWouldBeUnsafeToRender() {
        String csv = "MonthYear,MonthName,OccupancyRate,TotalRevenue,AvgRoomRate\n" +
                "2024-01,<script>,70,100000,180\n";

        CSVImportUtil.ParseResult result = CSVImportUtil.parseCSVResult(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 1);

        assertEquals(0, result.getAcceptedRows());
        assertEquals(1, result.getRejectedRows());
    }

    @Test
    void rejectsRoomNamesThatWouldBeUnsafeToRender() {
        String csv = "RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n" +
                "<img>,10,200,180,240,70\n";

        RoomDataImportUtil.ParseResult result = RoomDataImportUtil.parseRoomDataCSV(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertTrue(result.getRooms().isEmpty());
        assertEquals(1, result.getRejectedRows());
    }
}
