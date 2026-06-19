package com.ezboost.util;

import com.ezboost.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomDataImportUtil {

    private static final Logger logger = LoggerFactory.getLogger(RoomDataImportUtil.class);

    public static ParseResult parseRoomDataCSV(InputStream inputStream) {
        List<Room> rooms = new ArrayList<>();
        List<PreviewRow> previewRows = new ArrayList<>();
        int rejectedRows = 0;
        int backfilledBaseRateCount = 0;
        int floorAnchoredCount = 0;
        int suspiciousConstraintCount = 0;
        List<String> warnings = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (isFirstLine) {
                    isFirstLine = false;
                    logger.debug("[RoomDataImportUtil] Header: {}", line);
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");
                if (values.length < 6) {
                    rejectedRows++;
                    logger.error("[RoomDataImportUtil] Insufficient columns at row {} (need 6, got {}): {}",
                            lineNumber, values.length, line);
                    continue;
                }

                try {
                    String roomType = values[0].trim();
                    int numberOfRooms = Integer.parseInt(values[1].trim());
                    double parsedBaseRate = parseOptionalDouble(values[2]);
                    double minRate = parseOptionalDouble(values[3]);
                    double maxRate = parseOptionalDouble(values[4]);
                    double avgOccupancy = parseOptionalDouble(values[5]);

                    if (roomType.isEmpty() || roomType.length() > 100 || numberOfRooms <= 0 || numberOfRooms > 100000) {
                        rejectedRows++;
                        logger.error("[RoomDataImportUtil] Skipping invalid row {}: {}", lineNumber, line);
                        continue;
                    }

                    if (minRate > maxRate) {
                        double temp = minRate;
                        minRate = maxRate;
                        maxRate = temp;
                    }

                    boolean backfilledBaseRate = parsedBaseRate <= 0;
                    double baseRate = parsedBaseRate;
                    if (baseRate <= 0) {
                        baseRate = minRate > 0 ? minRate : maxRate;
                    }
                    if (minRate <= 0) {
                        minRate = baseRate;
                    }
                    if (maxRate <= 0) {
                        maxRate = Math.max(baseRate, minRate);
                    }

                    if (minRate <= 0 || maxRate <= 0 || avgOccupancy < 0 || avgOccupancy > 100) {
                        rejectedRows++;
                        logger.error("[RoomDataImportUtil] Skipping out-of-range row {}", lineNumber);
                        continue;
                    }

                    if (baseRate < minRate) {
                        baseRate = minRate;
                    }
                    if (baseRate > maxRate) {
                        maxRate = baseRate;
                    }

                    Room room = new Room(roomType, baseRate, minRate, maxRate, avgOccupancy, numberOfRooms);
                    rooms.add(room);

                    boolean floorAnchored = Math.abs(baseRate - minRate) < 0.0001;
                    boolean tightBounds = (maxRate - minRate) < 10;
                    boolean unusualOccupancy = avgOccupancy < 10 || avgOccupancy > 95;

                    if (backfilledBaseRate) {
                        backfilledBaseRateCount++;
                    }
                    if (floorAnchored) {
                        floorAnchoredCount++;
                    }
                    if (tightBounds || unusualOccupancy) {
                        suspiciousConstraintCount++;
                    }
                    previewRows.add(new PreviewRow(lineNumber, room, backfilledBaseRate, floorAnchored,
                            tightBounds, unusualOccupancy));

                    logger.debug("[RoomDataImportUtil] Parsed: {} | Rooms: {} | Base: {} | Range: {}-{} | Occ: {}%",
                            roomType, numberOfRooms, baseRate, minRate, maxRate, avgOccupancy);
                } catch (NumberFormatException e) {
                    rejectedRows++;
                    logger.error("[RoomDataImportUtil] Number format error at row {}: {}", lineNumber, line);
                }
            }
        } catch (Exception e) {
            logger.error("[RoomDataImportUtil] Error parsing CSV: {}", e.getMessage(), e);
        }

        if (backfilledBaseRateCount > 0) {
            warnings.add(backfilledBaseRateCount + " room rows had no base rate and were backfilled from min rate.");
        }
        if (floorAnchoredCount > 0) {
            warnings.add(floorAnchoredCount + " room rows have Base ADR equal to Min ADR, so low-season pricing cannot move below that floor.");
        }
        if (suspiciousConstraintCount > 0) {
            warnings.add(suspiciousConstraintCount + " room rows have very tight rate bounds or unusual occupancy values.");
        }

        logger.debug("[RoomDataImportUtil] Successfully parsed {} room types", rooms.size());
        return new ParseResult(rooms, previewRows, rejectedRows, backfilledBaseRateCount,
                floorAnchoredCount, suspiciousConstraintCount, warnings);
    }

    private static double parseOptionalDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0;

        String cleaned = value
                .replace(",", "")
                .replace("MYR", "")
                .replace("RM", "")
                .replace("$", "")
                .replace("%", "")
                .trim();

        double parsed = Double.parseDouble(cleaned);
        if (!Double.isFinite(parsed)) {
            throw new NumberFormatException("Non-finite number");
        }
        return parsed;
    }

    public static boolean validateRoomData(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return false;
        }

        for (Room room : rooms) {
            if (room.getName() == null || room.getName().isEmpty()) return false;
            if (room.getTotalRooms() <= 0) return false;
            if (!Double.isFinite(room.getBaseAdr()) || !Double.isFinite(room.getMinAdr())
                    || !Double.isFinite(room.getMaxAdr()) || !Double.isFinite(room.getOccupancy())) return false;
            if (room.getMinAdr() <= 0 || room.getMaxAdr() <= 0) return false;
            if (room.getMinAdr() > room.getMaxAdr()) return false;
            if (room.getBaseAdr() < room.getMinAdr() || room.getBaseAdr() > room.getMaxAdr()) return false;
            if (room.getOccupancy() < 0 || room.getOccupancy() > 100) return false;
        }

        return true;
    }

    public static String generateRoomDataTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n");
        sb.append("Standard Room,50,150.00,130.00,200.00,75.00\n");
        sb.append("Deluxe Room,30,200.00,175.00,280.00,70.00\n");
        sb.append("Suite,10,350.00,300.00,500.00,55.00\n");
        return sb.toString();
    }

    public static String generateRoyaleChulanSample() {
        StringBuilder sb = new StringBuilder();
        sb.append("RoomType,NumberOfRooms,BaseRate,MinRate,MaxRate,AvgOccupancy\n");
        sb.append("Superior Room,64,155.00,140.00,220.00,81.99\n");
        sb.append("Deluxe Room,52,165.00,150.00,230.00,81.63\n");
        sb.append("Studio Hollywood,27,145.00,130.00,200.00,74.76\n");
        sb.append("Studio Suite,48,155.00,140.00,220.00,76.01\n");
        sb.append("Premier Room,78,175.00,160.00,240.00,75.76\n");
        sb.append("Royale Club Room,67,170.00,155.00,240.00,75.30\n");
        sb.append("1 Bedroom Apartment,20,170.00,150.00,245.00,66.51\n");
        sb.append("2 Bedroom Apartment,5,290.00,260.00,390.00,54.64\n");
        sb.append("2 Bedroom Executive,3,360.00,320.00,490.00,39.78\n");
        sb.append("Premier Suite,14,180.00,160.00,245.00,81.20\n");
        sb.append("Executive Suite,23,200.00,180.00,270.00,59.12\n");
        sb.append("Royale Suite,1,400.00,360.00,600.00,2.92\n");
        return sb.toString();
    }

    public static int getTotalRoomCount(List<Room> rooms) {
        return rooms.stream().mapToInt(Room::getTotalRooms).sum();
    }

    public static double getWeightedAverageBaseRate(List<Room> rooms) {
        double totalWeightedRate = 0;
        int totalRooms = 0;

        for (Room room : rooms) {
            totalWeightedRate += room.getBaseAdr() * room.getTotalRooms();
            totalRooms += room.getTotalRooms();
        }

        return totalRooms > 0 ? totalWeightedRate / totalRooms : 0;
    }

    public static final class ParseResult {
        private final List<Room> rooms;
        private final List<PreviewRow> previewRows;
        private final int rejectedRows;
        private final int backfilledBaseRateCount;
        private final int floorAnchoredCount;
        private final int suspiciousConstraintCount;
        private final List<String> warnings;

        private ParseResult(List<Room> rooms, List<PreviewRow> previewRows, int rejectedRows,
                            int backfilledBaseRateCount, int floorAnchoredCount,
                            int suspiciousConstraintCount, List<String> warnings) {
            this.rooms = new ArrayList<>(rooms);
            this.previewRows = new ArrayList<>(previewRows);
            this.rejectedRows = rejectedRows;
            this.backfilledBaseRateCount = backfilledBaseRateCount;
            this.floorAnchoredCount = floorAnchoredCount;
            this.suspiciousConstraintCount = suspiciousConstraintCount;
            this.warnings = new ArrayList<>(warnings);
        }

        public List<Room> getRooms() {
            return rooms;
        }

        public int getAcceptedRows() {
            return rooms.size();
        }

        public int getRejectedRows() {
            return rejectedRows;
        }

        public int getBackfilledBaseRateCount() {
            return backfilledBaseRateCount;
        }

        public int getFloorAnchoredCount() {
            return floorAnchoredCount;
        }

        public int getSuspiciousConstraintCount() {
            return suspiciousConstraintCount;
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public List<PreviewRow> getPreviewRows() {
            return Collections.unmodifiableList(previewRows);
        }
    }

    public static final class PreviewRow {
        private final int lineNumber;
        private final Room room;
        private final boolean baseRateBackfilled;
        private final boolean floorAnchored;
        private final boolean tightBounds;
        private final boolean unusualOccupancy;

        private PreviewRow(int lineNumber, Room room, boolean baseRateBackfilled,
                           boolean floorAnchored, boolean tightBounds, boolean unusualOccupancy) {
            this.lineNumber = lineNumber;
            this.room = room;
            this.baseRateBackfilled = baseRateBackfilled;
            this.floorAnchored = floorAnchored;
            this.tightBounds = tightBounds;
            this.unusualOccupancy = unusualOccupancy;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public Room getRoom() {
            return room;
        }

        public boolean isBaseRateBackfilled() {
            return baseRateBackfilled;
        }

        public boolean isFloorAnchored() {
            return floorAnchored;
        }

        public boolean isTightBounds() {
            return tightBounds;
        }

        public boolean isUnusualOccupancy() {
            return unusualOccupancy;
        }

        public List<String> getFlags() {
            List<String> flags = new ArrayList<>();
            if (baseRateBackfilled) {
                flags.add("Base ADR backfilled");
            }
            if (floorAnchored) {
                flags.add("Base = floor");
            }
            if (tightBounds) {
                flags.add("Tight rate bounds");
            }
            if (unusualOccupancy) {
                flags.add("Occupancy outlier");
            }
            return flags;
        }
    }
}
