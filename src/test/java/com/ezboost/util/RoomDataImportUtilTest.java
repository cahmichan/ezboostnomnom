package com.ezboost.util;

import com.ezboost.model.Room;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomDataImportUtilTest {

    @Test
    void testParseRoomDataWithExplicitBaseRate() {
        String csv = "RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n" +
                "Deluxe,10,210,180,260,75\n";

        RoomDataImportUtil.ParseResult result = RoomDataImportUtil.parseRoomDataCSV(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        List<Room> rooms = result.getRooms();
        assertEquals(1, rooms.size());
        assertEquals(210.0, rooms.get(0).getBaseAdr(), 0.01);
        assertEquals(0, result.getBackfilledBaseRateCount());
        assertEquals(0, result.getRejectedRows());
    }

    @Test
    void testParseRoomDataBackfillsMissingBaseRate() {
        String csv = "RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n" +
                "Suite,5,,300,450,62\n";

        RoomDataImportUtil.ParseResult result = RoomDataImportUtil.parseRoomDataCSV(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, result.getRooms().size());
        assertEquals(300.0, result.getRooms().get(0).getBaseAdr(), 0.01);
        assertEquals(1, result.getBackfilledBaseRateCount());
        assertFalse(result.getWarnings().isEmpty());
    }

    @Test
    void testParseRoomDataFlagsBaseEqualsFloor() {
        String csv = "RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n" +
                "Studio,8,180,180,220,68\n";

        RoomDataImportUtil.ParseResult result = RoomDataImportUtil.parseRoomDataCSV(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, result.getFloorAnchoredCount());
        assertEquals(1, result.getPreviewRows().size());
        assertTrue(result.getPreviewRows().get(0).isFloorAnchored());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("Base ADR equal to Min ADR")));
    }

    @Test
    void testParseRoomDataFlagsSuspiciousRows() {
        String csv = "RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n" +
                "Suite,2,450,445,450,98\n";

        RoomDataImportUtil.ParseResult result = RoomDataImportUtil.parseRoomDataCSV(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, result.getSuspiciousConstraintCount());
        assertTrue(result.getPreviewRows().get(0).isTightBounds());
        assertTrue(result.getPreviewRows().get(0).isUnusualOccupancy());
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeInputs() {
        String csv = "RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n" +
                "Bad NaN,2,NaN,100,200,75\n" +
                "Bad Occupancy,2,150,100,200,101\n";

        RoomDataImportUtil.ParseResult result = RoomDataImportUtil.parseRoomDataCSV(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertTrue(result.getRooms().isEmpty());
        assertEquals(2, result.getRejectedRows());
    }
}
