package com.ezboost.service;

import com.ezboost.model.FutureEvent;
import com.ezboost.model.MarketSegment;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExcelExportService {

    public byte[] generateReport(List<Room> rooms,
                                 double targetRevenue,
                                 double estimatedRevenue,
                                 List<MarketSegment> marketSegments,
                                 List<Map<String, Object>> monthlyForecast,
                                 Integer forecastYear,
                                 String demandCurveMode,
                                 Double achievableMinRevenue,
                                 Double achievableMaxRevenue,
                                 List<String> constraintHighlights) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle highlightStyle = createHighlightStyle(workbook);

            createOptimizedPricingSheet(workbook.createSheet("Optimized Pricing"), rooms, targetRevenue,
                    estimatedRevenue, demandCurveMode, achievableMinRevenue, achievableMaxRevenue,
                    constraintHighlights, headerStyle, moneyStyle, percentStyle, titleStyle, subtitleStyle);

            createSegmentPricingSheet(workbook.createSheet("Segment Pricing"), rooms, marketSegments,
                    headerStyle, moneyStyle, titleStyle, subtitleStyle);

            if (monthlyForecast != null && !monthlyForecast.isEmpty()) {
                createMonthlyForecastSheet(workbook.createSheet("Monthly Forecast"), monthlyForecast, forecastYear,
                        rooms, headerStyle, moneyStyle, titleStyle, subtitleStyle, highlightStyle);
            }

            createAssumptionsSheet(workbook.createSheet("Assumptions"), demandCurveMode,
                    achievableMinRevenue, achievableMaxRevenue, constraintHighlights,
                    headerStyle, titleStyle, subtitleStyle);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createOptimizedPricingSheet(Sheet sheet, List<Room> rooms, double targetRevenue,
                                             double estimatedRevenue, String demandCurveMode,
                                             Double achievableMinRevenue, Double achievableMaxRevenue,
                                             List<String> constraintHighlights,
                                             CellStyle headerStyle, CellStyle moneyStyle,
                                             CellStyle percentStyle, CellStyle titleStyle,
                                             CellStyle subtitleStyle) {
        int rowNum = 0;
        String[] headers = {
                "Room Type", "Total Rooms", "Base ADR",
                "Low (RM)", "Low Occ%",
                "Normal (RM)", "Normal Occ%",
                "Peak (RM)", "Peak Occ%",
                "Super Peak (RM)", "Super Peak Occ%",
                "Estimated Revenue (RM)"
        };

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("EzBoost Revenue Optimization Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));

        Row subtitleRow = sheet.createRow(rowNum++);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Demand mode: " + demandCurveMode);
        subtitleCell.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.length - 1));

        rowNum++;

        rowNum = writeMoneySummaryRow(sheet, rowNum, "Target Revenue", targetRevenue, moneyStyle);
        rowNum = writeMoneySummaryRow(sheet, rowNum, "Estimated Revenue", estimatedRevenue, moneyStyle);
        if (achievableMinRevenue != null && achievableMaxRevenue != null) {
            rowNum = writeMoneySummaryRow(sheet, rowNum, "Achievable Revenue Floor", achievableMinRevenue, moneyStyle);
            rowNum = writeMoneySummaryRow(sheet, rowNum, "Achievable Revenue Ceiling", achievableMaxRevenue, moneyStyle);
        }
        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Season[] seasons = {Season.LOW, Season.NORMAL, Season.PEAK, Season.SUPER_PEAK};
        for (Room room : rooms) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            row.createCell(col++).setCellValue(room.getName());
            row.createCell(col++).setCellValue(room.getTotalRooms());

            Cell baseAdrCell = row.createCell(col++);
            baseAdrCell.setCellValue(room.getBaseAdr());
            baseAdrCell.setCellStyle(moneyStyle);

            for (Season season : seasons) {
                Cell priceCell = row.createCell(col++);
                priceCell.setCellValue(room.getSeasonalPrices().get(season));
                priceCell.setCellStyle(moneyStyle);

                Cell occCell = row.createCell(col++);
                occCell.setCellValue(room.getOccupancyForSeason(season) / 100.0);
                occCell.setCellStyle(percentStyle);
            }

            Cell revenueCell = row.createCell(col);
            revenueCell.setCellValue(room.getEstimatedRevenue());
            revenueCell.setCellStyle(moneyStyle);
        }

        rowNum += 2;
        Row notesTitle = sheet.createRow(rowNum++);
        notesTitle.createCell(0).setCellValue("Constraint Highlights");
        if (constraintHighlights != null) {
            for (String note : constraintHighlights) {
                Row noteRow = sheet.createRow(rowNum++);
                noteRow.createCell(0).setCellValue(note);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSegmentPricingSheet(Sheet sheet, List<Room> rooms, List<MarketSegment> segments,
                                           CellStyle headerStyle, CellStyle moneyStyle,
                                           CellStyle titleStyle, CellStyle subtitleStyle) {
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Segment Pricing Matrix");
        titleCell.setCellStyle(titleStyle);

        Row subtitleRow = sheet.createRow(rowNum++);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Base seasonal price multiplied by each segment multiplier.");
        subtitleCell.setCellStyle(subtitleStyle);

        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Room Type");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.createCell(1).setCellValue("Season");
        headerRow.getCell(1).setCellStyle(headerStyle);
        headerRow.createCell(2).setCellValue("Base Price (RM)");
        headerRow.getCell(2).setCellStyle(headerStyle);

        int col = 3;
        for (MarketSegment segment : segments) {
            Cell cell = headerRow.createCell(col++);
            cell.setCellValue(segment.getSegmentCode() + " (" + segment.getRateMultiplier() + "x)");
            cell.setCellStyle(headerStyle);
        }

        Season[] seasons = {Season.LOW, Season.NORMAL, Season.PEAK, Season.SUPER_PEAK};
        for (Room room : rooms) {
            for (Season season : seasons) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(room.getName());
                row.createCell(1).setCellValue(season.name());

                double basePrice = room.getSeasonalPrices().get(season);
                Cell baseCell = row.createCell(2);
                baseCell.setCellValue(basePrice);
                baseCell.setCellStyle(moneyStyle);

                col = 3;
                for (MarketSegment segment : segments) {
                    Cell priceCell = row.createCell(col++);
                    priceCell.setCellValue(basePrice * segment.getRateMultiplier());
                    priceCell.setCellStyle(moneyStyle);
                }
            }
        }

        for (int i = 0; i < 3 + segments.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    @SuppressWarnings("unchecked")
    private void createMonthlyForecastSheet(Sheet sheet, List<Map<String, Object>> forecast, Integer forecastYear,
                                            List<Room> rooms, CellStyle headerStyle, CellStyle moneyStyle,
                                            CellStyle titleStyle, CellStyle subtitleStyle,
                                            CellStyle highlightStyle) {
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Monthly Forecast" + (forecastYear != null ? " (" + forecastYear + ")" : ""));
        titleCell.setCellStyle(titleStyle);

        Row subtitleRow = sheet.createRow(rowNum++);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Event-adjusted monthly season overrides and room prices.");
        subtitleCell.setCellStyle(subtitleStyle);

        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        String[] baseHeaders = {"Month", "Events", "Base Season", "Adjusted Season"};
        int col = 0;
        for (String header : baseHeaders) {
            Cell cell = headerRow.createCell(col++);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
        }
        for (Room room : rooms) {
            Cell cell = headerRow.createCell(col++);
            cell.setCellValue(room.getName() + " (RM)");
            cell.setCellStyle(headerStyle);
        }

        for (Map<String, Object> monthData : forecast) {
            Row row = sheet.createRow(rowNum++);
            boolean seasonChanged = Boolean.TRUE.equals(monthData.get("seasonChanged"));
            int rowCol = 0;

            row.createCell(rowCol++).setCellValue((String) monthData.get("month"));
            row.createCell(rowCol++).setCellValue(buildEventLabel((List<FutureEvent>) monthData.get("events")));
            row.createCell(rowCol++).setCellValue((String) monthData.get("baseSeason"));

            Cell adjustedCell = row.createCell(rowCol++);
            adjustedCell.setCellValue((String) monthData.get("adjustedSeason"));
            if (seasonChanged) {
                adjustedCell.setCellStyle(highlightStyle);
            }

            Map<String, Double> roomPrices = (Map<String, Double>) monthData.get("roomPrices");
            for (Room room : rooms) {
                Double price = roomPrices.get(room.getName());
                Cell priceCell = row.createCell(rowCol++);
                priceCell.setCellValue(price != null ? price : 0.0);
                priceCell.setCellStyle(seasonChanged ? highlightStyle : moneyStyle);
            }
        }

        for (int i = 0; i < 4 + rooms.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createAssumptionsSheet(Sheet sheet, String demandCurveMode, Double achievableMinRevenue,
                                        Double achievableMaxRevenue, List<String> constraintHighlights,
                                        CellStyle headerStyle, CellStyle titleStyle, CellStyle subtitleStyle) {
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Assumptions and Constraints");
        titleCell.setCellStyle(titleStyle);

        Row subtitleRow = sheet.createRow(rowNum++);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("This sheet mirrors the reasoning shown in the EzBoost results page.");
        subtitleCell.setCellStyle(subtitleStyle);

        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Item");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.createCell(1).setCellValue("Value");
        headerRow.getCell(1).setCellStyle(headerStyle);

        rowNum = writeTextSummaryRow(sheet, rowNum, "Demand Curve Mode", demandCurveMode);
        if (achievableMinRevenue != null && achievableMaxRevenue != null) {
            rowNum = writeTextSummaryRow(sheet, rowNum, "Achievable Range",
                    String.format("RM %,.2f to RM %,.2f", achievableMinRevenue, achievableMaxRevenue));
        }
        rowNum = writeTextSummaryRow(sheet, rowNum, "Multiplier Scope",
                "Global seasonal multipliers only. No room-specific or segment-specific optimizer overrides.");
        rowNum = writeTextSummaryRow(sheet, rowNum, "Event Policy",
                "Fetched API/preset events replace earlier rows for the same source and year; manual events are preserved.");

        rowNum++;
        Row constraintTitle = sheet.createRow(rowNum++);
        constraintTitle.createCell(0).setCellValue("Constraint Highlights");
        if (constraintHighlights != null) {
            for (String highlight : constraintHighlights) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(highlight);
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private int writeMoneySummaryRow(Sheet sheet, int rowNum, String label, double value, CellStyle moneyStyle) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(moneyStyle);
        return rowNum;
    }

    private int writeTextSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowNum;
    }

    private String buildEventLabel(List<FutureEvent> events) {
        if (events == null || events.isEmpty()) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        for (FutureEvent event : events) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(event.getEventName());
        }
        return builder.toString();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("RM #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createSubtitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createHighlightStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("RM #,##0.00"));
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
