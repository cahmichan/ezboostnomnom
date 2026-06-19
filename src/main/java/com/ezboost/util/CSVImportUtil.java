package com.ezboost.util;

import com.ezboost.model.MonthlySeasonData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSVImportUtil - Utility for parsing CSV and Excel files
 * Enhancement i: Data-driven seasonality from file import
 */
public class CSVImportUtil {

    private static final Logger logger = LoggerFactory.getLogger(CSVImportUtil.class);

    /**
     * Parse CSV file containing monthly hotel data
     * Expected columns: MonthYear, MonthName, OccupancyRate, TotalRevenue, AvgRoomRate
     */
    public static List<MonthlySeasonData> parseCSV(InputStream inputStream, int userId) {
        return parseCSVResult(inputStream, userId).getData();
    }

    public static ParseResult parseCSVResult(InputStream inputStream, int userId) {
        List<MonthlySeasonData> dataList = new ArrayList<>();
        int rejectedRows = 0;
        int zeroAdrRows = 0;
        int zeroRevenueRows = 0;
        List<String> warnings = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                // Skip header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                String[] values = line.split(",");
                if (values.length < 5) {
                    rejectedRows++;
                    logger.error("Skipping monthly row {} because it has too few columns: {}", lineNumber, line);
                    continue;
                }

                try {
                    MonthlySeasonData data = new MonthlySeasonData();
                    data.setUserId(userId);
                    data.setMonthYear(values[0].trim());
                    data.setMonthName(values[1].trim());
                    data.setOccupancyRate(parseDouble(values[2]));
                    data.setTotalRevenue(parseDouble(values[3]));
                    data.setAvgRoomRate(parseDouble(values[4]));

                    if (data.getMonthYear().isEmpty() || data.getMonthName().isEmpty()) {
                        rejectedRows++;
                        logger.error("Skipping monthly row {} because month fields are missing: {}", lineNumber, line);
                        continue;
                    }
                    if (data.getOccupancyRate() < 0 || data.getOccupancyRate() > 100) {
                        rejectedRows++;
                        logger.error("Skipping monthly row {} because occupancy is out of range: {}", lineNumber, line);
                        continue;
                    }
                    if (data.getTotalRevenue() < 0 || data.getAvgRoomRate() < 0) {
                        rejectedRows++;
                        logger.error("Skipping monthly row {} because revenue or ADR is negative: {}", lineNumber, line);
                        continue;
                    }

                    data.autoClassifySeason();
                    dataList.add(data);

                    if (data.getAvgRoomRate() == 0) {
                        zeroAdrRows++;
                    }
                    if (data.getTotalRevenue() == 0) {
                        zeroRevenueRows++;
                    }
                } catch (NumberFormatException e) {
                    rejectedRows++;
                    logger.error("Skipping invalid monthly row {}: {}", lineNumber, line);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing CSV: {}", e.getMessage(), e);
        }

        if (dataList.size() < 4) {
            warnings.add("Fewer than 4 valid month rows were found, so threshold preview will fall back to the default thresholds.");
        }
        if (zeroAdrRows > 0) {
            warnings.add(zeroAdrRows + " month rows have an ADR of 0, so the demand-curve fit may fall back to the default model.");
        }
        if (zeroRevenueRows > 0) {
            warnings.add(zeroRevenueRows + " month rows have zero revenue. Double-check whether those months are complete before importing.");
        }

        return new ParseResult(dataList, rejectedRows, warnings);
    }

    /**
     * Parse Excel file (.xlsx) containing monthly hotel data
     * Searches for the correct sheet and columns
     */
    public static List<MonthlySeasonData> parseExcel(InputStream inputStream, int userId) {
        List<MonthlySeasonData> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            // Try to find a sheet with monthly data
            Sheet sheet = findMonthlyDataSheet(workbook);
            if (sheet == null) {
                logger.error("Could not find monthly data sheet in Excel file");
                return dataList;
            }

            logger.debug("[CSVImportUtil] Processing sheet: {}", sheet.getSheetName());

            // Find header row and column indices
            int[] columnIndices = findColumnIndices(sheet);
            if (columnIndices == null) {
                logger.error("Could not find required columns in Excel file");
                return dataList;
            }

            int headerRow = columnIndices[0];
            int monthCol = columnIndices[1];
            int occCol = columnIndices[2];
            int revCol = columnIndices[3];
            int adrCol = columnIndices[4];

            // Parse data rows
            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    MonthlySeasonData data = parseExcelRow(row, userId, monthCol, occCol, revCol, adrCol);
                    if (data != null) {
                        dataList.add(data);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing row {}: {}", i, e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing Excel file: {}", e.getMessage(), e);
        }

        return dataList;
    }

    /**
     * Find the sheet containing monthly data
     */
    private static Sheet findMonthlyDataSheet(Workbook workbook) {
        // Priority sheets to check
        String[] prioritySheets = {"ARR 2024 by Monthly", "Monthly", "ARR", "Revenue"};

        for (String sheetName : prioritySheets) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getSheetName().toLowerCase().contains(sheetName.toLowerCase())) {
                    return sheet;
                }
            }
        }

        // Return first sheet as fallback
        return workbook.getSheetAt(0);
    }

    /**
     * Find column indices for required data
     * Returns: [headerRow, monthCol, occupancyCol, revenueCol, adrCol]
     */
    private static int[] findColumnIndices(Sheet sheet) {
        for (int rowNum = 0; rowNum <= Math.min(10, sheet.getLastRowNum()); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            int monthCol = -1, occCol = -1, revCol = -1, adrCol = -1;

            for (int colNum = 0; colNum < row.getLastCellNum(); colNum++) {
                Cell cell = row.getCell(colNum);
                if (cell == null) continue;

                String value = getCellStringValue(cell).toLowerCase();

                if (value.contains("room category") || value.contains("month")) {
                    monthCol = colNum;
                }
                if (value.contains("usage") || value.contains("occ")) {
                    occCol = colNum;
                }
                if (value.contains("revenue") && !value.contains("revpar")) {
                    revCol = colNum;
                }
                if (value.contains("arr") || value.contains("adr")) {
                    adrCol = colNum;
                }
            }

            // If we found the key columns, return them
            if (occCol >= 0 && revCol >= 0) {
                return new int[]{rowNum, monthCol, occCol, revCol, adrCol};
            }
        }
        return null;
    }

    /**
     * Parse a single Excel row into MonthlySeasonData
     */
    private static MonthlySeasonData parseExcelRow(Row row, int userId, 
                                                    int monthCol, int occCol, 
                                                    int revCol, int adrCol) {
        // Get room type / month identifier
        Cell monthCell = row.getCell(monthCol);
        if (monthCell == null) return null;

        String monthValue = getCellStringValue(monthCell);
        if (monthValue.isEmpty() || monthValue.equalsIgnoreCase("total")) return null;

        // Get occupancy
        Cell occCell = row.getCell(occCol);
        if (occCell == null) return null;
        double occupancy = getCellNumericValue(occCell);
        if (occupancy <= 0 || occupancy > 100) return null;

        // Get revenue
        Cell revCell = row.getCell(revCol);
        double revenue = revCell != null ? getCellNumericValue(revCell) : 0;

        // Get ADR
        Cell adrCell = adrCol >= 0 ? row.getCell(adrCol) : null;
        double adr = adrCell != null ? getCellNumericValue(adrCell) : 0;

        MonthlySeasonData data = new MonthlySeasonData();
        data.setUserId(userId);
        data.setMonthYear(extractMonthYear(monthValue));
        data.setMonthName(extractMonthName(monthValue));
        data.setOccupancyRate(occupancy);
        data.setTotalRevenue(revenue);
        data.setAvgRoomRate(adr);
        data.autoClassifySeason();

        return data;
    }

    /**
     * Get string value from cell
     */
    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date date = cell.getDateCellValue();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM");
                    return sdf.format(date);
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * Get numeric value from cell
     */
    private static double getCellNumericValue(Cell cell) {
        if (cell == null) return 0;

        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String value = cell.getStringCellValue()
                            .replace("%", "")
                            .replace(",", "")
                            .replace("MYR", "")
                            .replace("RM", "")
                            .trim();
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }

    /**
     * Extract month-year format from various input formats
     */
    private static String extractMonthYear(String input) {
        // If already in YYYY-MM format
        if (input.matches("\\d{4}-\\d{2}")) {
            return input;
        }

        // Try to extract from date string
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        
        for (int i = 0; i < months.length; i++) {
            if (input.toLowerCase().contains(months[i].toLowerCase())) {
                // Default to 2024 if year not specified
                return String.format("2024-%02d", i + 1);
            }
        }

        return input;
    }

    /**
     * Extract month name from input
     */
    private static String extractMonthName(String input) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        
        for (String month : months) {
            if (input.toLowerCase().contains(month.toLowerCase())) {
                return month;
            }
        }

        // Try to extract from YYYY-MM format
        if (input.matches("\\d{4}-\\d{2}")) {
            int monthNum = Integer.parseInt(input.substring(5, 7));
            if (monthNum >= 1 && monthNum <= 12) {
                return months[monthNum - 1];
            }
        }

        return input;
    }

    /**
     * Parse double from string, handling various formats
     */
    private static double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0;

        String cleaned = value
                .replace("%", "")
                .replace(",", "")
                .replace("MYR", "")
                .replace("RM", "")
                .replace("$", "")
                .trim();

        return Double.parseDouble(cleaned);
    }

    /**
     * Generate sample CSV template
     */
    public static String generateCSVTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("MonthYear,MonthName,OccupancyRate,TotalRevenue,AvgRoomRate\n");
        sb.append("2024-01,January,75.19,1773865,189.43\n");
        sb.append("2024-02,February,76.93,1677469,187.05\n");
        sb.append("2024-03,March,65.97,1532097,186.36\n");
        sb.append("2024-04,April,60.44,1417392,194.46\n");
        sb.append("2024-05,May,72.15,1750000,190.00\n");
        sb.append("2024-06,June,78.33,2050000,195.00\n");
        sb.append("2024-07,July,87.04,2680000,210.00\n");
        sb.append("2024-08,August,84.91,2520000,205.00\n");
        sb.append("2024-09,September,76.22,1980000,192.00\n");
        sb.append("2024-10,October,71.88,1720000,188.00\n");
        sb.append("2024-11,November,77.45,2020000,193.00\n");
        sb.append("2024-12,December,79.12,2150000,198.00\n");
        return sb.toString();
    }

    public static final class ParseResult {
        private final List<MonthlySeasonData> data;
        private final int rejectedRows;
        private final List<String> warnings;

        private ParseResult(List<MonthlySeasonData> data, int rejectedRows, List<String> warnings) {
            this.data = new ArrayList<>(data);
            this.rejectedRows = rejectedRows;
            this.warnings = new ArrayList<>(warnings);
        }

        public List<MonthlySeasonData> getData() {
            return Collections.unmodifiableList(data);
        }

        public int getAcceptedRows() {
            return data.size();
        }

        public int getRejectedRows() {
            return rejectedRows;
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }
    }
}
