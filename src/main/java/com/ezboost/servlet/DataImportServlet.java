package com.ezboost.servlet;

import com.ezboost.dao.RoomDataDAO;
import com.ezboost.dao.SeasonalityDAO;
import com.ezboost.dao.AuditEventDAO;
import com.ezboost.ga.SeasonClassifierGA;
import com.ezboost.model.MonthlySeasonData;
import com.ezboost.model.Room;
import com.ezboost.model.SeasonThreshold;
import com.ezboost.model.User;
import com.ezboost.util.CSVImportUtil;
import com.ezboost.util.OnboardingUtil;
import com.ezboost.util.RoomDataImportUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataImportServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(DataImportServlet.class);
    private static final String MONTHLY_PREVIEW_SESSION_KEY = "pendingMonthlyImportPreview";
    private static final String ROOM_PREVIEW_SESSION_KEY = "pendingRoomImportPreview";
    private static final long MAX_UPLOAD_BYTES = 10L * 1024L * 1024L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp?error=Please login first");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        String action = request.getParameter("action");

        if ("downloadMonthlyTemplate".equals(action)) {
            downloadFile(response, "monthly_data_template.csv", CSVImportUtil.generateCSVTemplate());
            return;
        } else if ("downloadRoomTemplate".equals(action)) {
            downloadFile(response, "room_data_template.csv", RoomDataImportUtil.generateRoomDataTemplate());
            return;
        } else if ("downloadMonthlySample".equals(action)) {
            downloadFile(response, "monthly_data_royale_chulan_2024.csv", generateMonthlySample());
            return;
        } else if ("downloadRoomSample".equals(action)) {
            downloadFile(response, "room_data_royale_chulan.csv", RoomDataImportUtil.generateRoyaleChulanSample());
            return;
        }

        loadDataForDisplay(request, session, userId);
        request.getRequestDispatcher("data-import.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp?error=Please login first");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();
        String action = request.getParameter("action");

        try {
            switch (action != null ? action : "") {
                case "previewMonthly":
                    previewMonthlyImport(request, session, userId);
                    break;
                case "commitMonthlyImport":
                    commitMonthlyPreview(request, session, userId);
                    break;
                case "previewRooms":
                    previewRoomImport(request, session, userId);
                    break;
                case "commitRoomImport":
                    commitRoomPreview(request, session, userId);
                    break;
                case "discardMonthlyPreview":
                    clearPreview(session, MONTHLY_PREVIEW_SESSION_KEY);
                    request.setAttribute("success", "Monthly import preview cleared.");
                    break;
                case "discardRoomPreview":
                    clearPreview(session, ROOM_PREVIEW_SESSION_KEY);
                    request.setAttribute("success", "Room import preview cleared.");
                    break;
                case "importMonthly":
                case "importRooms":
                    request.setAttribute("error", "Preview the import before committing it.");
                    break;
                case "deleteMonthly":
                    handleDeleteMonthly(request, userId);
                    AuditEventDAO.record(userId, "MONTHLY_DELETE", "MonthlySeasonData", "SUCCESS");
                    break;
                case "deleteRooms":
                    RoomDataDAO.deleteAllRoomData(userId);
                    AuditEventDAO.record(userId, "ROOM_DELETE", "ActualRoomData", "SUCCESS");
                    request.setAttribute("success", "Your room data was deleted successfully.");
                    break;
                case "deleteAllData":
                    SeasonalityDAO.deleteAllUserMonthlyData(userId);
                    SeasonalityDAO.deleteThresholds(userId);
                    RoomDataDAO.deleteAllRoomData(userId);
                    AuditEventDAO.record(userId, "IMPORT_DATA_DELETE", "ImportedData", "SUCCESS");
                    clearPreview(session, MONTHLY_PREVIEW_SESSION_KEY);
                    clearPreview(session, ROOM_PREVIEW_SESSION_KEY);
                    request.setAttribute("success", "All of your imported setup data was deleted successfully.");
                    break;
                case "reclassifySeasons":
                    handleReclassifySeasons(request, userId);
                    break;
                case "updateThresholds":
                    handleManualThresholds(request, userId);
                    break;
                case "continueOnboardingImport":
                    if (handleContinueOnboardingImport(request, session, userId)) {
                        response.sendRedirect("MultiplierSettings");
                        return;
                    }
                    break;
                default:
                    request.setAttribute("error", "Unknown action: " + action);
            }
        } catch (Exception e) {
            logger.error("Error processing action: {}", e.getMessage(), e);
            request.setAttribute("error", e instanceof IllegalArgumentException
                    ? e.getMessage() : "The requested data action could not be completed. Please try again.");
        }

        loadDataForDisplay(request, session, userId);
        request.getRequestDispatcher("data-import.jsp").forward(request, response);
    }

    private void previewMonthlyImport(HttpServletRequest request, HttpSession session, int userId) throws Exception {
        Part filePart = request.getPart("monthlyFile");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Please select a monthly data file to preview.");
            return;
        }
        validateCsvUpload(filePart, "monthly data");

        String fileName = getFileName(filePart);
        boolean replaceExisting = !"append".equals(request.getParameter("monthlyImportMode"));
        CSVImportUtil.ParseResult parseResult;
        try (InputStream inputStream = filePart.getInputStream()) {
            parseResult = CSVImportUtil.parseCSVResult(inputStream, userId);
        }

        if (parseResult.getData().isEmpty()) {
            request.setAttribute("error", "No valid monthly rows were found in the uploaded file.");
            return;
        }

        SeasonThreshold threshold = buildThresholdPreview(userId, parseResult.getData());
        boolean gaUsed = parseResult.getData().size() >= 4;
        MonthlyImportPreview preview = new MonthlyImportPreview(userId,
                fileName,
                replaceExisting,
                parseResult.getData(),
                threshold,
                gaUsed,
                parseResult.getRejectedRows(),
                parseResult.getWarnings()
        );

        session.setAttribute(MONTHLY_PREVIEW_SESSION_KEY, preview);
        request.setAttribute("success", "Monthly data parsed successfully. Review the preview below before importing.");
    }

    private void commitMonthlyPreview(HttpServletRequest request, HttpSession session, int userId) {
        MonthlyImportPreview preview = (MonthlyImportPreview) session.getAttribute(MONTHLY_PREVIEW_SESSION_KEY);
        if (preview == null || !preview.isValidFor(userId)) {
            request.setAttribute("error", "Your monthly import preview expired. Preview the file again before importing.");
            return;
        }

        saveMonthlyData(preview, request, userId);
        clearPreview(session, MONTHLY_PREVIEW_SESSION_KEY);
    }

    private void previewRoomImport(HttpServletRequest request, HttpSession session, int userId) throws Exception {
        Part filePart = request.getPart("roomFile");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Please select a room data file to preview.");
            return;
        }
        validateCsvUpload(filePart, "room data");

        String fileName = getFileName(filePart);
        boolean replaceExisting = !"append".equals(request.getParameter("roomImportMode"));
        RoomDataImportUtil.ParseResult parseResult;
        try (InputStream inputStream = filePart.getInputStream()) {
            parseResult = RoomDataImportUtil.parseRoomDataCSV(inputStream);
        }

        List<Room> rooms = parseResult.getRooms();
        if (rooms.isEmpty()) {
            request.setAttribute("error", "No valid room rows were found in the uploaded file.");
            return;
        }
        if (!RoomDataImportUtil.validateRoomData(rooms)) {
            request.setAttribute("error", "Room data validation failed. Please check the file format and rate bounds.");
            return;
        }

        RoomImportPreview preview = new RoomImportPreview(userId, fileName, replaceExisting, parseResult);
        session.setAttribute(ROOM_PREVIEW_SESSION_KEY, preview);
        request.setAttribute("success", "Room data parsed successfully. Review the preview below before importing.");
    }

    private void commitRoomPreview(HttpServletRequest request, HttpSession session, int userId) {
        RoomImportPreview preview = (RoomImportPreview) session.getAttribute(ROOM_PREVIEW_SESSION_KEY);
        if (preview == null || !preview.isValidFor(userId)) {
            request.setAttribute("error", "Your room import preview expired. Preview the file again before importing.");
            return;
        }

        saveRoomData(preview, request, userId);
        clearPreview(session, ROOM_PREVIEW_SESSION_KEY);
    }

    private void handleMonthlyDataImport(HttpServletRequest request, int userId) throws Exception {
        Part filePart = request.getPart("monthlyFile");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Please select a monthly data file to upload.");
            return;
        }
        validateCsvUpload(filePart, "monthly data");

        String fileName = getFileName(filePart);
        boolean replaceExisting = !"append".equals(request.getParameter("monthlyImportMode"));
        logger.debug("Monthly file received: {} (replaceExisting={})", fileName, replaceExisting);

        CSVImportUtil.ParseResult parseResult;
        try (InputStream inputStream = filePart.getInputStream()) {
            parseResult = CSVImportUtil.parseCSVResult(inputStream, userId);
        }

        if (parseResult.getData().isEmpty()) {
            request.setAttribute("error", "No valid monthly rows were found in the uploaded file.");
            return;
        }

        MonthlyImportPreview preview = new MonthlyImportPreview(userId,
                fileName,
                replaceExisting,
                parseResult.getData(),
                buildThresholdPreview(userId, parseResult.getData()),
                parseResult.getData().size() >= 4,
                parseResult.getRejectedRows(),
                parseResult.getWarnings()
        );
        saveMonthlyData(preview, request, userId);
    }

    private void saveMonthlyData(MonthlyImportPreview preview, HttpServletRequest request, int userId) {
        int savedCount = SeasonalityDAO.saveImportedMonthlyData(userId, preview.getImportedData(),
                preview.getThreshold(), preview.isReplaceExisting());
        AuditEventDAO.record(userId, "MONTHLY_IMPORT", "MonthlySeasonData", "SUCCESS");

        request.setAttribute("thresholds", preview.getThreshold());
        request.setAttribute("gaUsed", preview.isGaUsed());
        request.setAttribute("monthlyImportMode", preview.isReplaceExisting() ? "replace" : "append");
        request.setAttribute("monthlyImportedCount", savedCount);
        request.setAttribute("monthlyRejectedRows", preview.getRejectedRows());
        request.setAttribute("monthlyWarnings", preview.getWarnings());
        request.setAttribute("monthlySuccess",
                "Imported " + savedCount + " month rows from " + preview.getFileName() + " and " +
                        (preview.isGaUsed() ? "generated fresh AI thresholds." : "used the default thresholds because fewer than 4 valid rows were available."));
    }

    private void handleRoomDataImport(HttpServletRequest request, int userId) throws Exception {
        Part filePart = request.getPart("roomFile");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Please select a room data file to upload.");
            return;
        }
        validateCsvUpload(filePart, "room data");

        String fileName = getFileName(filePart);
        boolean replaceExisting = !"append".equals(request.getParameter("roomImportMode"));
        logger.debug("Room file received: {} (replaceExisting={})", fileName, replaceExisting);

        RoomDataImportUtil.ParseResult parseResult;
        try (InputStream inputStream = filePart.getInputStream()) {
            parseResult = RoomDataImportUtil.parseRoomDataCSV(inputStream);
        }

        if (parseResult.getRooms().isEmpty()) {
            request.setAttribute("error", "No valid room rows were found in the uploaded file.");
            return;
        }
        if (!RoomDataImportUtil.validateRoomData(parseResult.getRooms())) {
            request.setAttribute("error", "Room data validation failed. Please check the file format and rate bounds.");
            return;
        }

        saveRoomData(new RoomImportPreview(userId, fileName, replaceExisting, parseResult), request, userId);
    }

    private void saveRoomData(RoomImportPreview preview, HttpServletRequest request, int userId) {
        RoomDataImportUtil.ParseResult parseResult = preview.getParseResult();
        List<Room> rooms = parseResult.getRooms();

        int savedCount = RoomDataDAO.saveRoomData(userId, rooms, preview.isReplaceExisting());
        AuditEventDAO.record(userId, "ROOM_IMPORT", "ActualRoomData", "SUCCESS");
        int totalRooms = RoomDataImportUtil.getTotalRoomCount(rooms);
        double avgBaseRate = RoomDataImportUtil.getWeightedAverageBaseRate(rooms);

        request.setAttribute("roomImportMode", preview.isReplaceExisting() ? "replace" : "append");
        request.setAttribute("roomImportedCount", savedCount);
        request.setAttribute("roomRejectedRows", parseResult.getRejectedRows());
        request.setAttribute("roomBackfilledBaseRateCount", parseResult.getBackfilledBaseRateCount());
        request.setAttribute("roomFloorAnchoredCount", parseResult.getFloorAnchoredCount());
        request.setAttribute("roomSuspiciousConstraintCount", parseResult.getSuspiciousConstraintCount());
        request.setAttribute("roomImportWarnings", parseResult.getWarnings());
        request.setAttribute("roomSuccess", "Imported " + savedCount + " room types from " + preview.getFileName() +
                " (" + totalRooms + " total rooms, weighted average base rate: RM " + String.format("%.2f", avgBaseRate) + ").");
    }

    private SeasonThreshold buildThresholdPreview(int userId, List<MonthlySeasonData> importedData) {
        boolean gaUsed = importedData.size() >= 4;
        SeasonThreshold threshold;
        if (gaUsed) {
            logger.debug("Running Season Classifier GA preview...");
            SeasonClassifierGA classifier = new SeasonClassifierGA(importedData);
            double[] thresholds = classifier.runGA();
            threshold = new SeasonThreshold(userId, thresholds[0], thresholds[1], thresholds[2], true);
        } else {
            threshold = new SeasonThreshold(userId, 65.0, 75.0, 85.0, false);
        }

        for (MonthlySeasonData data : importedData) {
            data.setClassifiedSeason(threshold.classifyOccupancy(data.getOccupancyRate()));
        }
        return threshold;
    }

    private void handleDeleteMonthly(HttpServletRequest request, int userId) {
        String dataIdStr = request.getParameter("dataId");
        if (dataIdStr != null && !dataIdStr.isEmpty()) {
            int dataId = Integer.parseInt(dataIdStr);
            if (!SeasonalityDAO.deleteMonthlyDataById(dataId, userId)) {
                request.setAttribute("error", "Monthly record was not found for your account.");
                return;
            }
            request.setAttribute("success", "Monthly record deleted.");
        } else {
            SeasonalityDAO.deleteAllUserMonthlyData(userId);
            SeasonalityDAO.deleteThresholds(userId);
            request.setAttribute("success", "All monthly data deleted.");
        }
    }

    private void handleReclassifySeasons(HttpServletRequest request, int userId) {
        List<MonthlySeasonData> existingData = SeasonalityDAO.getMonthlyDataByUser(userId);

        if (existingData.size() < 4) {
            request.setAttribute("error", "Need at least 4 months of data for AI classification.");
            return;
        }

        SeasonClassifierGA classifier = new SeasonClassifierGA(existingData);
        double[] thresholds = classifier.runGA();

        SeasonThreshold threshold = new SeasonThreshold(userId,
                thresholds[0], thresholds[1], thresholds[2], true);
        SeasonalityDAO.saveThresholds(threshold);
        SeasonalityDAO.reclassifyAllSeasons(userId, threshold);

        request.setAttribute("thresholds", threshold);
        request.setAttribute("success", "Seasons reclassified with fresh AI-generated thresholds.");
    }

    private void handleManualThresholds(HttpServletRequest request, int userId) {
        try {
            double t1 = Double.parseDouble(request.getParameter("t1"));
            double t2 = Double.parseDouble(request.getParameter("t2"));
            double t3 = Double.parseDouble(request.getParameter("t3"));

            if (t1 >= t2 || t2 >= t3) {
                request.setAttribute("error", "Thresholds must be in order: T1 < T2 < T3.");
                return;
            }

            SeasonThreshold threshold = new SeasonThreshold(userId, t1, t2, t3, false);
            SeasonalityDAO.saveThresholds(threshold);
            SeasonalityDAO.reclassifyAllSeasons(userId, threshold);

            request.setAttribute("thresholds", threshold);
            request.setAttribute("success", "Manual thresholds applied and existing data reclassified.");
        } catch (NumberFormatException e) {
            request.setAttribute("error", "Invalid threshold values.");
        }
    }

    private boolean handleContinueOnboardingImport(HttpServletRequest request, HttpSession session, int userId) {
        boolean hasMonthlyData = !SeasonalityDAO.getMonthlyDataByUser(userId).isEmpty();
        boolean hasRoomData = RoomDataDAO.hasRoomData(userId);
        boolean hasThresholds = SeasonalityDAO.getThresholdsByUser(userId) != null;

        if (hasMonthlyData && hasRoomData && hasThresholds) {
            return OnboardingUtil.advanceToStep(userId, OnboardingUtil.STEP_MULTIPLIERS, session);
        }

        List<String> missing = new ArrayList<>();
        if (!hasMonthlyData) {
            missing.add("monthly demand history");
        }
        if (!hasRoomData) {
            missing.add("room inventory");
        }
        if (!hasThresholds) {
            missing.add("season thresholds");
        }
        request.setAttribute("error", "Finish Import before continuing. Missing: " + String.join(", ", missing) + ".");
        return false;
    }

    private void loadDataForDisplay(HttpServletRequest request, HttpSession session, int userId) {
        List<MonthlySeasonData> monthlyData = SeasonalityDAO.getMonthlyDataByUser(userId);
        Map<String, Integer> seasonDistribution = SeasonalityDAO.getSeasonDistribution(userId);
        SeasonThreshold thresholds = SeasonalityDAO.getThresholdsByUser(userId);
        List<Room> rooms = RoomDataDAO.getAllRooms(userId);
        int totalRooms = RoomDataDAO.getTotalRoomCount(userId);
        double weightedAverageBaseRate = RoomDataDAO.getWeightedAverageBaseRate(userId);

        request.setAttribute("monthlyData", monthlyData);
        request.setAttribute("seasonDistribution", seasonDistribution);
        request.setAttribute("thresholds", thresholds);
        request.setAttribute("rooms", rooms);
        request.setAttribute("totalRoomCount", totalRooms);
        request.setAttribute("roomTypeCount", rooms.size());
        request.setAttribute("weightedAverageBaseRate", weightedAverageBaseRate);

        boolean hasMonthlyData = !monthlyData.isEmpty();
        boolean hasRoomData = !rooms.isEmpty();
        boolean hasThresholds = thresholds != null;
        boolean readyForOptimization = hasMonthlyData && hasRoomData && hasThresholds;

        request.setAttribute("hasMonthlyData", hasMonthlyData);
        request.setAttribute("hasRoomData", hasRoomData);
        request.setAttribute("hasThresholds", hasThresholds);
        request.setAttribute("readyForOptimization", readyForOptimization);
        User onboardingUser = (User) session.getAttribute("user");
        boolean onboardingActive = OnboardingUtil.isOnboardingActive(onboardingUser);
        request.setAttribute("onboardingActive", onboardingActive);
        request.setAttribute("nextStepUrl", readyForOptimization
                ? (onboardingActive ? "MultiplierSettings" : "BoostMe")
                : "DataImport");
        request.setAttribute("nextStepLabel", readyForOptimization
                ? (onboardingActive ? "Continue to Multipliers" : "Open BoostMe")
                : "Finish setup");

        request.setAttribute("monthlyPreview", session.getAttribute(MONTHLY_PREVIEW_SESSION_KEY));
        request.setAttribute("roomPreview", session.getAttribute(ROOM_PREVIEW_SESSION_KEY));
    }

    private void clearPreview(HttpSession session, String key) {
        if (session != null) {
            session.removeAttribute(key);
        }
    }

    private void downloadFile(HttpServletResponse response, String filename, String content) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.getWriter().write(content);
    }

    private String generateMonthlySample() {
        StringBuilder sb = new StringBuilder();
        sb.append("MonthYear,MonthName,OccupancyRate,TotalRevenue,AvgRoomRate\n");
        sb.append("2024-01,January,75.19,1773864.97,189.43\n");
        sb.append("2024-02,February,76.93,1677469.22,187.05\n");
        sb.append("2024-03,March,65.97,1532096.62,186.36\n");
        sb.append("2024-04,April,60.44,1417391.77,194.46\n");
        sb.append("2024-05,May,72.15,1750000.00,190.00\n");
        sb.append("2024-06,June,78.33,2050000.00,195.00\n");
        sb.append("2024-07,July,87.04,2680000.00,210.00\n");
        sb.append("2024-08,August,84.91,2520000.00,205.00\n");
        sb.append("2024-09,September,76.22,1980000.00,192.00\n");
        sb.append("2024-10,October,71.88,1720000.00,188.00\n");
        sb.append("2024-11,November,77.45,2020000.00,193.00\n");
        sb.append("2024-12,December,79.12,2150000.00,198.00\n");
        return sb.toString();
    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return "unknown";
    }

    private void validateCsvUpload(Part filePart, String label) {
        String fileName = getFileName(filePart);
        if (filePart.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("The " + label + " file exceeds the 10 MB upload limit.");
        }
        if (fileName == null || !fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Upload a CSV file for " + label + ".");
        }
        String contentType = filePart.getContentType();
        if (contentType != null && !contentType.trim().isEmpty()) {
            String normalized = contentType.toLowerCase(java.util.Locale.ROOT).split(";", 2)[0].trim();
            if (!"text/csv".equals(normalized) && !"application/csv".equals(normalized)
                    && !"application/vnd.ms-excel".equals(normalized)
                    && !"application/octet-stream".equals(normalized)) {
                throw new IllegalArgumentException("The selected " + label + " file is not a CSV upload.");
            }
        }
    }

    public static final class MonthlyImportPreview {
        private static final long EXPIRY_MILLIS = 30L * 60L * 1000L;
        private final int userId;
        private final long createdAt;
        private final String fileName;
        private final boolean replaceExisting;
        private final List<MonthlySeasonData> importedData;
        private final SeasonThreshold threshold;
        private final boolean gaUsed;
        private final int rejectedRows;
        private final List<String> warnings;

        private MonthlyImportPreview(int userId, String fileName, boolean replaceExisting, List<MonthlySeasonData> importedData,
                                     SeasonThreshold threshold, boolean gaUsed, int rejectedRows,
                                     List<String> warnings) {
            this.userId = userId;
            this.createdAt = System.currentTimeMillis();
            this.fileName = fileName;
            this.replaceExisting = replaceExisting;
            this.importedData = new ArrayList<>(importedData);
            this.threshold = threshold;
            this.gaUsed = gaUsed;
            this.rejectedRows = rejectedRows;
            this.warnings = new ArrayList<>(warnings);
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isReplaceExisting() {
            return replaceExisting;
        }

        public List<MonthlySeasonData> getImportedData() {
            return importedData;
        }

        public SeasonThreshold getThreshold() {
            return threshold;
        }

        public boolean isGaUsed() {
            return gaUsed;
        }

        public int getAcceptedRows() {
            return importedData.size();
        }

        public int getRejectedRows() {
            return rejectedRows;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean isValidFor(int currentUserId) {
            return userId == currentUserId && System.currentTimeMillis() - createdAt <= EXPIRY_MILLIS;
        }
    }

    public static final class RoomImportPreview {
        private static final long EXPIRY_MILLIS = 30L * 60L * 1000L;
        private final int userId;
        private final long createdAt;
        private final String fileName;
        private final boolean replaceExisting;
        private final RoomDataImportUtil.ParseResult parseResult;

        private RoomImportPreview(int userId, String fileName, boolean replaceExisting, RoomDataImportUtil.ParseResult parseResult) {
            this.userId = userId;
            this.createdAt = System.currentTimeMillis();
            this.fileName = fileName;
            this.replaceExisting = replaceExisting;
            this.parseResult = parseResult;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isReplaceExisting() {
            return replaceExisting;
        }

        public RoomDataImportUtil.ParseResult getParseResult() {
            return parseResult;
        }

        public boolean isValidFor(int currentUserId) {
            return userId == currentUserId && System.currentTimeMillis() - createdAt <= EXPIRY_MILLIS;
        }
    }
}
