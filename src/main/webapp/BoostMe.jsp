<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, java.text.DecimalFormat" %>
<%@ page import="com.ezboost.model.Room, com.ezboost.model.Season, com.ezboost.model.MarketSegment, com.ezboost.model.FutureEvent, com.ezboost.model.User" %>
<%@ page import="com.ezboost.util.HtmlEscaper" %>

<%
    DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    DecimalFormat numberFormat = new DecimalFormat("0.00");

    int roomTypeCount = request.getAttribute("roomTypeCount") instanceof Integer
            ? (Integer) request.getAttribute("roomTypeCount") : 0;
    int monthlyCount = request.getAttribute("monthlyCount") instanceof Integer
            ? (Integer) request.getAttribute("monthlyCount") : 0;
    boolean readyForOptimization = Boolean.TRUE.equals(request.getAttribute("readyForOptimization"));

    Double expectedRevenue = (Double) request.getAttribute("expectedRevenue");
    Double estimatedRevenue = (Double) request.getAttribute("estimatedRevenue");
    List<Room> rooms = (List<Room>) request.getAttribute("bestSolution");
    Double accuracy = (Double) request.getAttribute("accuracy");
    Double achievableMinRevenue = (Double) request.getAttribute("achievableMinRevenue");
    Double achievableMaxRevenue = (Double) request.getAttribute("achievableMaxRevenue");
    String targetDifficulty = (String) request.getAttribute("targetDifficulty");
    String demandCurveMode = (String) request.getAttribute("demandCurveMode");
    String demandCurveSummary = (String) request.getAttribute("demandCurveSummary");
    String error = (String) request.getAttribute("error");
    String optimizationWarning = (String) request.getAttribute("optimizationWarning");
    Integer eventAdjustedMonthCount = (Integer) request.getAttribute("eventAdjustedMonthCount");
    List<String> constraintHighlights = (List<String>) request.getAttribute("constraintHighlights");
    Map<String, String> priceConstraintStates = (Map<String, String>) request.getAttribute("priceConstraintStates");
    List<MarketSegment> marketSegments = (List<MarketSegment>) request.getAttribute("marketSegments");
    List<Map<String, Object>> monthlyForecast = (List<Map<String, Object>>) request.getAttribute("monthlyForecast");
    Integer forecastYear = (Integer) request.getAttribute("forecastYear");
    boolean hasResults = expectedRevenue != null && estimatedRevenue != null && rooms != null && !rooms.isEmpty();
    String csrfToken = session != null && session.getAttribute("csrfToken") instanceof String
            ? (String) session.getAttribute("csrfToken") : "";
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>EzBoost - BoostMe</title>
    <link rel="stylesheet" href="css/result.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
    <%@ include file="nav.jsp" %>

    <div class="main-container">
        <section class="boost-intro">
            <div class="boost-intro-copy">
                <span class="boost-kicker">Optimization Workspace</span>
                <h1 class="boost-title">BoostMe</h1>
                <p class="boost-subtitle">Run the seasonal pricing optimizer, review the demand and bound assumptions, and carry the resulting room prices into the month-by-month forecast.</p>
                <div class="boost-intro-tags">
                    <span class="boost-intro-tag">Seasonal GA</span>
                    <span class="boost-intro-tag">Monthly mapping</span>
                    <span class="boost-intro-tag">Event overrides</span>
                </div>
            </div>
            <div class="boost-intro-summary">
                <div class="boost-summary-card">
                    <span class="boost-note-label">Optimizer</span>
                    <strong class="boost-note-value">Seasonal GA</strong>
                    <span class="boost-note-text">four seasonal prices per room type</span>
                </div>
                <div class="boost-summary-card">
                    <span class="boost-note-label">Forecast</span>
                    <strong class="boost-note-value">Historical Mapping</strong>
                    <span class="boost-note-text">future months inherit seasonal results, then apply event bumps</span>
                </div>
                <div class="boost-summary-card">
                    <span class="boost-note-label">Guardrails</span>
                    <strong class="boost-note-value">Room Bounds + Demand</strong>
                    <span class="boost-note-text">Base ADR, min/max ADR, and demand-curve mode shape the run</span>
                </div>
            </div>
        </section>

        <% if (error != null && !error.isEmpty()) { %>
        <div class="alert-modern alert-danger" role="alert" style="margin-bottom: 1.5rem;">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <span><%= HtmlEscaper.escape(error) %></span>
        </div>
        <% } %>
        <% if (optimizationWarning != null && !optimizationWarning.isEmpty()) { %>
        <div class="alert-modern alert-warning" role="status" aria-live="polite" style="margin-bottom: 1.5rem;">
            <i class="bi bi-exclamation-circle-fill"></i>
            <span><%= HtmlEscaper.escape(optimizationWarning) %></span>
        </div>
        <% } %>

        <section class="status-panel">
            <div class="status-grid">
                <div class="status-card">
                    <span class="status-dot <%= readyForOptimization ? "active" : "" %>"></span>
                    <div class="status-info">
                        <span class="status-label">Setup</span>
                        <span class="status-value"><%= readyForOptimization ? "Ready" : "Import required" %></span>
                    </div>
                </div>
                <div class="status-card">
                    <span class="status-dot <%= hasResults ? "active" : "" %>"></span>
                    <div class="status-info">
                        <span class="status-label">Demand Model</span>
                        <span class="status-value"><%= HtmlEscaper.escape(demandCurveMode != null ? demandCurveMode : "Awaiting run") %></span>
                    </div>
                </div>
                <div class="status-card">
                    <span class="status-dot <%= monthlyCount > 0 ? "active" : "" %>"></span>
                    <div class="status-info">
                        <span class="status-label">Season History</span>
                        <span class="status-value"><%= monthlyCount %> months</span>
                    </div>
                </div>
                <div class="status-card">
                    <span class="status-dot <%= hasResults ? "active" : "" %>"></span>
                    <div class="status-info">
                        <span class="status-label">Event Adjustments</span>
                        <span class="status-value"><%= eventAdjustedMonthCount != null ? eventAdjustedMonthCount + " months" : "Not evaluated" %></span>
                    </div>
                </div>
            </div>
        </section>

        <section class="input-section">
            <div class="ai-prompt-box">
                <div class="prompt-header">
                    <span class="prompt-label">REVENUE TARGET</span>
                    <span class="prompt-status">
                        <span class="status-dot <%= readyForOptimization ? "active" : "" %>"></span>
                        <%= readyForOptimization ? "Ready to optimize" : "Finish setup first" %>
                    </span>
                </div>

                <form action="RunGA" method="post" id="optimizeForm" class="prompt-form">
                    <input type="hidden" name="csrfToken" value="<%= HtmlEscaper.escape(csrfToken) %>">
                    <div class="prompt-input-row">
                        <span class="input-prefix">RM</span>
                        <label class="visually-hidden" for="expectedRevenue">Target revenue in Malaysian ringgit</label>
                        <input type="number"
                               id="expectedRevenue"
                               name="expectedRevenue"
                               class="prompt-input"
                               placeholder="0.00"
                               value="<%= expectedRevenue != null ? String.format(Locale.US, "%.2f", expectedRevenue) : "" %>"
                               min="0.01"
                               step="0.01"
                               inputmode="decimal"
                               autocomplete="off"
                               aria-describedby="revenueTargetHelp"
                               required>
                    </div>

                    <div class="prompt-footer">
                        <div class="prompt-pills">
                            <span class="prompt-pill"><%= roomTypeCount %> room types</span>
                            <span class="prompt-pill"><%= monthlyCount %> demand months</span>
                            <span class="prompt-pill" id="revenueTargetHelp">Global season multipliers</span>
                        </div>
                        <button type="submit" class="run-button" <%= readyForOptimization ? "" : "disabled aria-disabled=\"true\"" %>>Run Optimization</button>
                    </div>
                </form>
            </div>
        </section>

        <% if (!hasResults) { %>
        <section class="results-section">
            <div class="empty-state">
                <% if (readyForOptimization) { %>
                <p>Your setup is complete. Enter a revenue target above to generate an optimization.</p>
                <% } else { %>
                <p>BoostMe needs both monthly demand history and room inventory for your account before it can generate a reliable recommendation.</p>
                <a href="DataImport" class="empty-link">Open Data Import</a>
                <% } %>
            </div>
        </section>
        <% } else { %>
        <section class="results-section" id="resultsSection">
            <div class="result-header">
                <div class="result-label">OPTIMIZATION OUTCOME</div>
                <h2 class="result-title">Optimization Complete</h2>
                <p class="result-subtitle">Read this page from top to bottom: target intake, demand assumptions, pricing constraints, seasonal room outputs, then the month-by-month forecast.</p>
            </div>

            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-label">Target Revenue</div>
                    <div class="metric-value">RM <%= moneyFormat.format(expectedRevenue) %></div>
                    <div class="metric-tag">Requested</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Estimated Revenue</div>
                    <div class="metric-value">RM <%= moneyFormat.format(estimatedRevenue) %></div>
                    <div class="metric-tag success">Optimized</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Achievable Range</div>
                    <div class="metric-value">RM <%= moneyFormat.format(achievableMinRevenue != null ? achievableMinRevenue : 0) %> - <br>RM <%= moneyFormat.format(achievableMaxRevenue != null ? achievableMaxRevenue : 0) %></div>
                    <div class="metric-tag"><%= HtmlEscaper.escape(targetDifficulty != null ? targetDifficulty : "n/a") %></div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Accuracy</div>
                    <div class="metric-value"><%= numberFormat.format(accuracy != null ? accuracy : 0) %>%</div>
                    <div class="metric-tag">Target fit</div>
                </div>
            </div>

            <div class="table-section">
                <div class="table-header">
                    <h3>Optimization Context</h3>
                    <p>These are the assumptions and guardrails the optimizer used for this run.</p>
                </div>
                <div class="segment-explain-grid">
                    <div class="explain-card">
                        <h4>Demand Curve</h4>
                        <p><strong><%= HtmlEscaper.escape(demandCurveMode) %></strong></p>
                        <p><%= HtmlEscaper.escape(demandCurveSummary) %></p>
                    </div>
                    <div class="explain-card">
                        <h4>Constraint Highlights</h4>
                        <ul class="explain-list">
                            <% if (constraintHighlights != null) {
                                for (String highlight : constraintHighlights) { %>
                            <li><%= HtmlEscaper.escape(highlight) %></li>
                            <%  }
                               } %>
                        </ul>
                    </div>
                </div>
            </div>

            <div class="table-section">
                <div class="table-header">
                    <h3>Optimized Pricing Strategy</h3>
                    <p>Base ADR, rate bounds, and season-by-season outputs. Floor and ceiling chips mark prices constrained by your current setup.</p>
                </div>

                <div class="table-wrapper">
                    <table class="data-table" aria-label="Seasonal room price recommendations">
                        <caption class="visually-hidden">Seasonal room price recommendations and estimated revenue by room type.</caption>
                        <thead>
                            <tr>
                                <th scope="col">Room Type</th>
                                <th scope="col">Rooms</th>
                                <th scope="col">Base ADR</th>
                                <th scope="col">Min ADR</th>
                                <th scope="col">Max ADR</th>
                                <th scope="col">Low Season</th>
                                <th scope="col">Normal Season</th>
                                <th scope="col">Peak Season</th>
                                <th scope="col">Super Peak Season</th>
                                <th scope="col">Estimated Revenue</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Room room : rooms) { %>
                            <tr>
                                <%
                                    String lowState = priceConstraintStates != null ? priceConstraintStates.get(room.getName() + "|" + Season.LOW.name()) : null;
                                    String normalState = priceConstraintStates != null ? priceConstraintStates.get(room.getName() + "|" + Season.NORMAL.name()) : null;
                                    String peakState = priceConstraintStates != null ? priceConstraintStates.get(room.getName() + "|" + Season.PEAK.name()) : null;
                                    String superState = priceConstraintStates != null ? priceConstraintStates.get(room.getName() + "|" + Season.SUPER_PEAK.name()) : null;
                                %>
                                <td><span class="room-name"><%= HtmlEscaper.escape(room.getName()) %></span></td>
                                <td><span class="count-badge"><%= room.getTotalRooms() %></span></td>
                                <td>RM <%= moneyFormat.format(room.getBaseAdr()) %></td>
                                <td>RM <%= moneyFormat.format(room.getMinAdr()) %></td>
                                <td>RM <%= moneyFormat.format(room.getMaxAdr()) %></td>
                                <td>
                                    <div class="price-tag low-season">
                                        RM <%= moneyFormat.format(room.getSeasonalPrices().get(Season.LOW)) %>
                                        <% if ("FLOOR".equals(lowState)) { %><span class="bound-chip floor">Floor</span><% } %>
                                        <% if ("CEILING".equals(lowState)) { %><span class="bound-chip ceiling">Ceiling</span><% } %>
                                        <div class="occ-sub"><%= numberFormat.format(room.getOccupancyForSeason(Season.LOW)) %>% occ</div>
                                    </div>
                                </td>
                                <td>
                                    <div class="price-tag normal-season">
                                        RM <%= moneyFormat.format(room.getSeasonalPrices().get(Season.NORMAL)) %>
                                        <% if ("FLOOR".equals(normalState)) { %><span class="bound-chip floor">Floor</span><% } %>
                                        <% if ("CEILING".equals(normalState)) { %><span class="bound-chip ceiling">Ceiling</span><% } %>
                                        <div class="occ-sub"><%= numberFormat.format(room.getOccupancyForSeason(Season.NORMAL)) %>% occ</div>
                                    </div>
                                </td>
                                <td>
                                    <div class="price-tag peak-season">
                                        RM <%= moneyFormat.format(room.getSeasonalPrices().get(Season.PEAK)) %>
                                        <% if ("FLOOR".equals(peakState)) { %><span class="bound-chip floor">Floor</span><% } %>
                                        <% if ("CEILING".equals(peakState)) { %><span class="bound-chip ceiling">Ceiling</span><% } %>
                                        <div class="occ-sub"><%= numberFormat.format(room.getOccupancyForSeason(Season.PEAK)) %>% occ</div>
                                    </div>
                                </td>
                                <td>
                                    <div class="price-tag super-season">
                                        RM <%= moneyFormat.format(room.getSeasonalPrices().get(Season.SUPER_PEAK)) %>
                                        <% if ("FLOOR".equals(superState)) { %><span class="bound-chip floor">Floor</span><% } %>
                                        <% if ("CEILING".equals(superState)) { %><span class="bound-chip ceiling">Ceiling</span><% } %>
                                        <div class="occ-sub"><%= numberFormat.format(room.getOccupancyForSeason(Season.SUPER_PEAK)) %>% occ</div>
                                    </div>
                                </td>
                                <td><div class="revenue-value">RM <%= moneyFormat.format(room.getEstimatedRevenue()) %></div></td>
                            </tr>
                            <% } %>
                        </tbody>
                        <tfoot>
                            <tr class="total-row">
                                <td colspan="9" class="total-label">Total Estimated Revenue</td>
                                <td><div class="total-value">RM <%= moneyFormat.format(estimatedRevenue) %></div></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>

            <% if (marketSegments != null && !marketSegments.isEmpty()) { %>
            <div class="table-section">
                <div class="table-header">
                    <h3>Market Segment Pricing</h3>
                    <p>Displayed for reference only. The supported optimizer scope remains global season multipliers.</p>
                </div>

                <div class="segment-legend">
                    <div class="legend-title">Segment Rate Multipliers</div>
                    <div class="legend-pills">
                        <% for (MarketSegment segment : marketSegments) {
                            String badgeClass = segment.getRateMultiplier() > 1.0 ? "premium" :
                                               segment.getRateMultiplier() < 1.0 ? "discount" : "neutral";
                        %>
                        <span class="segment-badge <%= badgeClass %>"
                              title="<%= HtmlEscaper.escape(segment.getDescription() != null ? segment.getDescription() : segment.getSegmentName()) %>">
                            <%= HtmlEscaper.escape(segment.getSegmentCode()) %>: <%= String.format("%.2f", segment.getRateMultiplier()) %>x
                        </span>
                        <% } %>
                    </div>
                </div>

                <div class="table-wrapper">
                    <table class="data-table" aria-label="Market segment price display">
                        <caption class="visually-hidden">Post-optimization price display by room type, season, and market segment.</caption>
                        <thead>
                            <tr>
                                <th scope="col">Room Type</th>
                                <th scope="col">Season</th>
                                <th scope="col">Base Price</th>
                                <% for (MarketSegment segment : marketSegments) { %>
                                <th scope="col"><%= HtmlEscaper.escape(segment.getSegmentCode()) %></th>
                                <% } %>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Room room : rooms) {
                                Season[] seasons = {Season.LOW, Season.NORMAL, Season.PEAK, Season.SUPER_PEAK};
                                boolean firstSeason = true;
                                for (Season season : seasons) {
                                    double basePrice = room.getSeasonalPrices().get(season);
                            %>
                            <tr>
                                <% if (firstSeason) { %>
                                <td rowspan="4" style="vertical-align: middle;"><span class="room-name"><%= HtmlEscaper.escape(room.getName()) %></span></td>
                                <% firstSeason = false; } %>
                                <td><%= season.name().replace("_", " ") %></td>
                                <td>RM <%= moneyFormat.format(basePrice) %></td>
                                <% for (MarketSegment segment : marketSegments) { %>
                                <td>RM <%= moneyFormat.format(basePrice * segment.getRateMultiplier()) %></td>
                                <% } %>
                            </tr>
                            <% } } %>
                        </tbody>
                    </table>
                </div>
            </div>
            <% } %>

            <div class="table-section">
                <div class="table-header">
                    <h3>Monthly Pricing Forecast with Events</h3>
                    <p>Historical month classification with event overrides for <%= forecastYear != null ? forecastYear : Calendar.getInstance().get(Calendar.YEAR) %>.</p>
                </div>
                <div class="forecast-overview">
                    <div class="forecast-overview-card">
                        <span class="forecast-overview-label">Base Mapping</span>
                        <strong class="forecast-overview-value">Historical month -> season</strong>
                    </div>
                    <div class="forecast-overview-card">
                        <span class="forecast-overview-label">Event Rule</span>
                        <strong class="forecast-overview-value">Upward override only</strong>
                    </div>
                    <div class="forecast-overview-card">
                        <span class="forecast-overview-label">Changed Months</span>
                        <strong class="forecast-overview-value"><%= eventAdjustedMonthCount != null ? eventAdjustedMonthCount : 0 %></strong>
                    </div>
                </div>

                <% if (monthlyForecast != null && !monthlyForecast.isEmpty()) {
                    Map<String, Double> firstRoomPrices = (Map<String, Double>) monthlyForecast.get(0).get("roomPrices");
                    boolean anyChanged = false;
                    for (Map<String, Object> month : monthlyForecast) {
                        if (Boolean.TRUE.equals(month.get("seasonChanged"))) {
                            anyChanged = true;
                            break;
                        }
                    }
                %>
                <div class="table-wrapper forecast-table-wrapper">
                    <table class="data-table" aria-label="Monthly forecast and event adjustments">
                        <caption class="visually-hidden">Monthly forecast pricing with base seasons, event overrides, and room prices.</caption>
                        <thead>
                            <tr>
                                <th scope="col">Month</th>
                                <th scope="col">Events</th>
                                <th scope="col">Base Season</th>
                                <th scope="col">Adjusted Season</th>
                                <% if (firstRoomPrices != null) {
                                    for (String roomName : firstRoomPrices.keySet()) { %>
                                <th scope="col"><%= HtmlEscaper.escape(roomName) %></th>
                                <% } } %>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (Map<String, Object> monthData : monthlyForecast) {
                                List<FutureEvent> events = (List<FutureEvent>) monthData.get("events");
                                Map<String, Double> roomPrices = (Map<String, Double>) monthData.get("roomPrices");
                                boolean seasonChanged = Boolean.TRUE.equals(monthData.get("seasonChanged"));
                            %>
                            <tr class="<%= seasonChanged ? "row-changed" : "" %>">
                                <td><strong><%= HtmlEscaper.escape(monthData.get("month")) %></strong></td>
                                <td>
                                    <% if (events != null && !events.isEmpty()) {
                                        for (FutureEvent event : events) { %>
                                    <span class="event-tag"><%= HtmlEscaper.escape(event.getEventName()) %></span>
                                    <% } } else { %>
                                    <span class="no-event">-</span>
                                    <% } %>
                                </td>
                                <td><span class="price-tag"><%= monthData.get("baseSeason").toString().replace("_", " ") %></span></td>
                                <td><span class="price-tag"><%= monthData.get("adjustedSeason").toString().replace("_", " ") %><%= seasonChanged ? " ↑" : "" %></span></td>
                                <% if (firstRoomPrices != null) {
                                    for (String roomName : firstRoomPrices.keySet()) { %>
                                <td>RM <%= moneyFormat.format(roomPrices.get(roomName)) %></td>
                                <% } } %>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
                <% if (anyChanged) { %>
                <div class="info-note">Highlighted months were bumped upward by configured events. Months without events stay on their historical classification.</div>
                <% } %>
                <% } else { %>
                <div class="empty-state">
                    <p>No forecast is available yet. Configure events after your first optimization if you want event-adjusted monthly pricing.</p>
                    <a href="EventSettings" class="empty-link">Configure Event Settings</a>
                </div>
                <% } %>
            </div>

            <div class="download-section">
                <div class="download-card">
                    <div class="download-info">
                        <h3>Export Your Results</h3>
                        <p>The Excel report includes revenue labels, achievable range, demand-curve mode, constraints, segment pricing, and monthly event overrides.</p>
                    </div>
                    <a href="DownloadReportServlet" class="download-btn">Download Report</a>
                </div>
            </div>
        </section>
        <% } %>
    </div>

    <div class="loading-overlay" id="loadingOverlay" role="status" aria-live="polite" aria-atomic="true">
        <div class="loading-card">
            <div class="loading-spinner"></div>
            <h3 class="loading-title">Optimizing Revenue...</h3>
            <p class="loading-subtitle">EzBoost is evaluating the target, bounds, and season-specific pricing tradeoffs.</p>
            <div class="progress-bar-container">
                <div class="progress-bar"></div>
            </div>
        </div>
    </div>

    <script>
        document.getElementById('optimizeForm').addEventListener('submit', function() {
            var loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) {
                loadingOverlay.classList.add('active');
                var progressBar = document.querySelector('.progress-bar');
                if (progressBar) {
                    setTimeout(function() { progressBar.style.width = '100%'; }, 100);
                }
            }
        });

        <% if (hasResults) { %>
        document.addEventListener('DOMContentLoaded', function() {
            var resultsSection = document.getElementById('resultsSection');
            if (resultsSection) {
                setTimeout(function() {
                    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    resultsSection.classList.add('animate-in');
                }, 300);
            }
        });
        <% } %>
    </script>
</body>
</html>
