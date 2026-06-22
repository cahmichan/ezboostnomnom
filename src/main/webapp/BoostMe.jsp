<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
                <div class="boost-summary-card"><span class="boost-note-label">Optimizer</span><strong class="boost-note-value">Seasonal GA</strong><span class="boost-note-text">four seasonal prices per room type</span></div>
                <div class="boost-summary-card"><span class="boost-note-label">Forecast</span><strong class="boost-note-value">Historical Mapping</strong><span class="boost-note-text">future months inherit seasonal results, then apply event bumps</span></div>
                <div class="boost-summary-card"><span class="boost-note-label">Guardrails</span><strong class="boost-note-value">Room Bounds + Demand</strong><span class="boost-note-text">Base ADR, min/max ADR, and demand-curve mode shape the run</span></div>
            </div>
        </section>

        <c:if test="${not empty boostView.error}"><div class="alert-modern alert-danger" role="alert" style="margin-bottom: 1.5rem;"><span><c:out value="${boostView.error}" /></span></div></c:if>
        <c:if test="${not empty boostView.optimizationWarning}"><div class="alert-modern alert-warning" role="status" aria-live="polite" style="margin-bottom: 1.5rem;"><span><c:out value="${boostView.optimizationWarning}" /></span></div></c:if>

        <section class="status-panel"><div class="status-grid">
            <div class="status-card"><span class="status-dot ${boostView.readyForOptimization ? 'active' : ''}"></span><div class="status-info"><span class="status-label">Setup</span><span class="status-value">${boostView.readyForOptimization ? 'Ready' : 'Import required'}</span></div></div>
            <div class="status-card"><span class="status-dot ${boostView.hasResults ? 'active' : ''}"></span><div class="status-info"><span class="status-label">Demand Model</span><span class="status-value"><c:out value="${boostView.demandCurveMode}" /></span></div></div>
            <div class="status-card"><span class="status-dot ${boostView.monthlyCount gt 0 ? 'active' : ''}"></span><div class="status-info"><span class="status-label">Season History</span><span class="status-value"><c:out value="${boostView.monthlyCount}" /> months</span></div></div>
            <div class="status-card"><span class="status-dot ${boostView.hasResults ? 'active' : ''}"></span><div class="status-info"><span class="status-label">Event Adjustments</span><span class="status-value"><c:choose><c:when test="${boostView.hasResults}"><c:out value="${boostView.eventAdjustedMonthCount}" /> months</c:when><c:otherwise>Not evaluated</c:otherwise></c:choose></span></div></div>
        </div></section>

        <section class="input-section"><div class="ai-prompt-box">
            <div class="prompt-header"><span class="prompt-label">REVENUE TARGET</span><span class="prompt-status"><span class="status-dot ${boostView.readyForOptimization ? 'active' : ''}"></span>${boostView.readyForOptimization ? 'Ready to optimize' : 'Finish setup first'}</span></div>
            <form action="RunGA" method="post" id="optimizeForm" class="prompt-form">
                <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>">
                <div class="prompt-input-row"><span class="input-prefix">RM</span><label class="visually-hidden" for="expectedRevenue">Target revenue in Malaysian ringgit</label><input type="number" id="expectedRevenue" name="expectedRevenue" class="prompt-input" placeholder="0.00" value="<c:out value='${boostView.expectedRevenueInput}'/>" min="0.01" step="0.01" inputmode="decimal" autocomplete="off" aria-describedby="revenueTargetHelp" required></div>
                <div class="prompt-footer"><div class="prompt-pills"><span class="prompt-pill"><c:out value="${boostView.roomTypeCount}" /> room types</span><span class="prompt-pill"><c:out value="${boostView.monthlyCount}" /> demand months</span><span class="prompt-pill" id="revenueTargetHelp">Global season multipliers</span></div><button type="submit" class="run-button" <c:if test="${not boostView.readyForOptimization}">disabled aria-disabled="true"</c:if>>Run Optimization</button></div>
            </form>
        </div></section>

        <c:choose>
        <c:when test="${not boostView.hasResults}">
            <section class="results-section"><div class="empty-state"><c:choose><c:when test="${boostView.readyForOptimization}"><p>Your setup is complete. Enter a revenue target above to generate an optimization.</p></c:when><c:otherwise><p>BoostMe needs both monthly demand history and room inventory for your account before it can generate a reliable recommendation.</p><a href="DataImport" class="empty-link">Open Data Import</a></c:otherwise></c:choose></div></section>
        </c:when>
        <c:otherwise>
        <section class="results-section" id="resultsSection">
            <div class="result-header"><div class="result-label">OPTIMIZATION OUTCOME</div><h2 class="result-title">Optimization Complete</h2><p class="result-subtitle">Read this page from top to bottom: target intake, demand assumptions, pricing constraints, seasonal room outputs, then the month-by-month forecast.</p></div>
            <div class="metrics-grid">
                <div class="metric-card"><div class="metric-label">Target Revenue</div><div class="metric-value">RM <c:out value="${boostView.expectedRevenue}" /></div><div class="metric-tag">Requested</div></div>
                <div class="metric-card"><div class="metric-label">Estimated Revenue</div><div class="metric-value">RM <c:out value="${boostView.estimatedRevenue}" /></div><div class="metric-tag success">Optimized</div></div>
                <div class="metric-card"><div class="metric-label">Achievable Range</div><div class="metric-value">RM <c:out value="${boostView.achievableMinimum}" /> - <br>RM <c:out value="${boostView.achievableMaximum}" /></div><div class="metric-tag"><c:out value="${boostView.targetDifficulty}" /></div></div>
                <div class="metric-card"><div class="metric-label">Accuracy</div><div class="metric-value"><c:out value="${boostView.accuracy}" />%</div><div class="metric-tag">Target fit</div></div>
            </div>
            <div class="table-section"><div class="table-header"><h3>Optimization Context</h3><p>These are the assumptions and guardrails the optimizer used for this run.</p></div><div class="segment-explain-grid"><div class="explain-card"><h4>Demand Curve</h4><p><strong><c:out value="${boostView.demandCurveMode}" /></strong></p><p><c:out value="${boostView.demandCurveSummary}" /></p></div><div class="explain-card"><h4>Constraint Highlights</h4><ul class="explain-list"><c:forEach items="${boostView.constraintHighlights}" var="highlight"><li><c:out value="${highlight}" /></li></c:forEach></ul></div></div></div>
            <div class="table-section"><div class="table-header"><h3>Optimized Pricing Strategy</h3><p>Base ADR, rate bounds, and season-by-season outputs. Floor and ceiling chips mark prices constrained by your current setup.</p></div><div class="table-wrapper"><table class="data-table" aria-label="Seasonal room price recommendations"><caption class="visually-hidden">Seasonal room price recommendations and estimated revenue by room type.</caption><thead><tr><th scope="col">Room Type</th><th scope="col">Rooms</th><th scope="col">Base ADR</th><th scope="col">Min ADR</th><th scope="col">Max ADR</th><th scope="col">Low Season</th><th scope="col">Normal Season</th><th scope="col">Peak Season</th><th scope="col">Super Peak Season</th><th scope="col">Estimated Revenue</th></tr></thead><tbody>
                <c:forEach items="${boostView.roomRows}" var="room"><tr><td><span class="room-name"><c:out value="${room.name}" /></span></td><td><span class="count-badge"><c:out value="${room.roomCount}" /></span></td><td>RM <c:out value="${room.baseAdr}" /></td><td>RM <c:out value="${room.minAdr}" /></td><td>RM <c:out value="${room.maxAdr}" /></td>
                    <td><div class="price-tag low-season">RM <c:out value="${room.low.price}" /><c:if test="${room.low.floor}"><span class="bound-chip floor">Floor</span></c:if><c:if test="${room.low.ceiling}"><span class="bound-chip ceiling">Ceiling</span></c:if><div class="occ-sub"><c:out value="${room.low.occupancy}" />% occ</div></div></td>
                    <td><div class="price-tag normal-season">RM <c:out value="${room.normal.price}" /><c:if test="${room.normal.floor}"><span class="bound-chip floor">Floor</span></c:if><c:if test="${room.normal.ceiling}"><span class="bound-chip ceiling">Ceiling</span></c:if><div class="occ-sub"><c:out value="${room.normal.occupancy}" />% occ</div></div></td>
                    <td><div class="price-tag peak-season">RM <c:out value="${room.peak.price}" /><c:if test="${room.peak.floor}"><span class="bound-chip floor">Floor</span></c:if><c:if test="${room.peak.ceiling}"><span class="bound-chip ceiling">Ceiling</span></c:if><div class="occ-sub"><c:out value="${room.peak.occupancy}" />% occ</div></div></td>
                    <td><div class="price-tag super-season">RM <c:out value="${room.superPeak.price}" /><c:if test="${room.superPeak.floor}"><span class="bound-chip floor">Floor</span></c:if><c:if test="${room.superPeak.ceiling}"><span class="bound-chip ceiling">Ceiling</span></c:if><div class="occ-sub"><c:out value="${room.superPeak.occupancy}" />% occ</div></div></td>
                    <td><div class="revenue-value">RM <c:out value="${room.estimatedRevenue}" /></div></td></tr></c:forEach>
                </tbody><tfoot><tr class="total-row"><td colspan="9" class="total-label">Total Estimated Revenue</td><td><div class="total-value">RM <c:out value="${boostView.estimatedRevenue}" /></div></td></tr></tfoot></table></div></div>
            <c:if test="${not empty boostView.segments}"><div class="table-section"><div class="table-header"><h3>Market Segment Pricing</h3><p>Displayed for reference only. The supported optimizer scope remains global season multipliers.</p></div><div class="segment-legend"><div class="legend-title">Segment Rate Multipliers</div><div class="legend-pills"><c:forEach items="${boostView.segments}" var="segment"><span class="segment-badge ${segment.badgeClass}" title="<c:out value='${segment.description}' />"><c:out value="${segment.code}" />: <c:out value="${segment.multiplier}" />x</span></c:forEach></div></div><div class="table-wrapper"><table class="data-table" aria-label="Market segment price display"><caption class="visually-hidden">Post-optimization price display by room type, season, and market segment.</caption><thead><tr><th scope="col">Room Type</th><th scope="col">Season</th><th scope="col">Base Price</th><c:forEach items="${boostView.segments}" var="segment"><th scope="col"><c:out value="${segment.code}" /></th></c:forEach></tr></thead><tbody><c:forEach items="${boostView.segmentPriceRows}" var="row"><tr><c:if test="${row.firstSeason}"><td rowspan="4" style="vertical-align: middle;"><span class="room-name"><c:out value="${row.roomName}" /></span></td></c:if><td><c:out value="${row.season}" /></td><td>RM <c:out value="${row.basePrice}" /></td><c:forEach items="${row.segmentPrices}" var="price"><td>RM <c:out value="${price}" /></td></c:forEach></tr></c:forEach></tbody></table></div></div></c:if>
            <div class="table-section"><div class="table-header"><h3>Monthly Pricing Forecast with Events</h3><p>Historical month classification with event overrides for <c:out value="${boostView.forecastYear}" />.</p></div><div class="forecast-overview"><div class="forecast-overview-card"><span class="forecast-overview-label">Base Mapping</span><strong class="forecast-overview-value">Historical month -&gt; season</strong></div><div class="forecast-overview-card"><span class="forecast-overview-label">Event Rule</span><strong class="forecast-overview-value">Upward override only</strong></div><div class="forecast-overview-card"><span class="forecast-overview-label">Changed Months</span><strong class="forecast-overview-value"><c:out value="${boostView.eventAdjustedMonthCount}" /></strong></div></div>
                <c:choose><c:when test="${not empty boostView.forecastRows}"><div class="table-wrapper forecast-table-wrapper"><table class="data-table" aria-label="Monthly forecast and event adjustments"><caption class="visually-hidden">Monthly forecast pricing with base seasons, event overrides, and room prices.</caption><thead><tr><th scope="col">Month</th><th scope="col">Events</th><th scope="col">Base Season</th><th scope="col">Adjusted Season</th><c:forEach items="${boostView.forecastRoomNames}" var="roomName"><th scope="col"><c:out value="${roomName}" /></th></c:forEach></tr></thead><tbody><c:forEach items="${boostView.forecastRows}" var="month"><tr class="${month.changed ? 'row-changed' : ''}"><td><strong><c:out value="${month.month}" /></strong></td><td><c:choose><c:when test="${not empty month.events}"><c:forEach items="${month.events}" var="event"><span class="event-tag"><c:out value="${event}" /></span></c:forEach></c:when><c:otherwise><span class="no-event">-</span></c:otherwise></c:choose></td><td><span class="price-tag"><c:out value="${month.baseSeason}" /></span></td><td><span class="price-tag"><c:out value="${month.adjustedSeason}" /><c:if test="${month.changed}"> ↑</c:if></span></td><c:forEach items="${month.prices}" var="price"><td>RM <c:out value="${price}" /></td></c:forEach></tr></c:forEach></tbody></table></div><c:if test="${boostView.anyForecastChanges}"><div class="info-note">Highlighted months were bumped upward by configured events. Months without events stay on their historical classification.</div></c:if></c:when><c:otherwise><div class="empty-state"><p>No forecast is available yet. Configure events after your first optimization if you want event-adjusted monthly pricing.</p><a href="EventSettings" class="empty-link">Configure Event Settings</a></div></c:otherwise></c:choose>
            </div>
            <div class="download-section"><div class="download-card"><div class="download-info"><h3>Export Your Results</h3><p>The Excel report includes revenue labels, achievable range, demand-curve mode, constraints, segment pricing, and monthly event overrides.</p></div><form action="DownloadReportServlet" method="post"><input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}'/>"><button type="submit" class="download-btn">Download Report</button></form></div></div>
        </section>
        </c:otherwise>
        </c:choose>
    </div>
    <div class="loading-overlay" id="loadingOverlay" role="status" aria-live="polite" aria-atomic="true"><div class="loading-card"><div class="loading-spinner"></div><h3 class="loading-title">Optimizing Revenue...</h3><p class="loading-subtitle">EzBoost is evaluating the target, bounds, and season-specific pricing tradeoffs.</p><div class="progress-bar-container"><div class="progress-bar"></div></div></div></div>
    <script>
        document.getElementById('optimizeForm').addEventListener('submit', function() { var loadingOverlay = document.getElementById('loadingOverlay'); if (loadingOverlay) { loadingOverlay.classList.add('active'); var progressBar = document.querySelector('.progress-bar'); if (progressBar) { setTimeout(function() { progressBar.style.width = '100%'; }, 100); } } });
        <c:if test="${boostView.hasResults}">document.addEventListener('DOMContentLoaded', function() { var resultsSection = document.getElementById('resultsSection'); if (resultsSection) { setTimeout(function() { resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' }); resultsSection.classList.add('animate-in'); }, 300); } });</c:if>
    </script>
</body>
</html>
