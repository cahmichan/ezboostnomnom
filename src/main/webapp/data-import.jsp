<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Data Import - EzBoost</title>
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/settings.css">
</head>
<body class="data-import">
    <%@ include file="nav.jsp" %>

    <div class="main-container">
        <!-- Page Header -->
        <div class="page-header">
            <div class="page-badge">
                <i class="bi bi-database"></i>
                <span>Data Management</span>
            </div>
            <h1 class="page-title"><i class="bi bi-cloud-upload"></i> Data Import Center</h1>
            <p class="page-subtitle">Bring in your demand history and room inventory, review the parsing carefully, and only then commit the data that drives the optimizer.</p>
        </div>

        <%@ include file="onboarding-progress.jsp" %>

        <!-- Alert Messages -->
        <c:if test="${not empty success}">
            <div class="alert-modern alert-success">
                <i class="bi bi-check-circle-fill"></i>
                <span>${success}</span>
                <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        </c:if>
        <c:if test="${not empty monthlySuccess}">
            <div class="alert-modern alert-success">
                <i class="bi bi-calendar-check"></i>
                <span>${monthlySuccess}</span>
                <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        </c:if>
        <c:if test="${not empty monthlyWarnings}">
            <div class="alert-modern alert-warning">
                <i class="bi bi-exclamation-circle-fill"></i>
                <div>
                    <strong>Monthly import warnings:</strong>
                    <c:forEach var="warning" items="${monthlyWarnings}">
                        <div>${warning}</div>
                    </c:forEach>
                </div>
                <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        </c:if>
        <c:if test="${not empty roomSuccess}">
            <div class="alert-modern alert-success">
                <i class="bi bi-building-check"></i>
                <span>${roomSuccess}</span>
                <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert-modern alert-danger">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <span>${error}</span>
                <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        </c:if>
        <c:if test="${not empty roomImportWarnings}">
            <div class="alert-modern alert-warning">
                <i class="bi bi-exclamation-circle-fill"></i>
                <div>
                    <strong>Room import warnings:</strong>
                    <c:forEach var="warning" items="${roomImportWarnings}">
                        <div>${warning}</div>
                    </c:forEach>
                </div>
                <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        </c:if>

        <section class="workspace-hero import-hero">
            <div class="workspace-hero-copy">
                <span class="workspace-kicker">Guided Setup</span>
                <h2 class="workspace-title">Move from raw CSV files to a reviewable, optimizer-ready hotel dataset.</h2>
                <p class="workspace-text">This page is the setup source of truth. Load your monthly history, inspect the generated thresholds, preview room constraints, and confirm the import only when the digest looks trustworthy.</p>
            </div>
            <div class="workspace-hero-metrics">
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Readiness</span>
                    <strong class="workspace-metric-value">${readyForOptimization ? 'Ready' : 'In Progress'}</strong>
                    <span class="workspace-metric-note">history + rooms + thresholds</span>
                </div>
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Monthly History</span>
                    <strong class="workspace-metric-value">${hasMonthlyData ? monthlyData.size() : 0}</strong>
                    <span class="workspace-metric-note">month rows imported</span>
                </div>
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Room Inventory</span>
                    <strong class="workspace-metric-value">${hasRoomData ? roomTypeCount : 0}</strong>
                    <span class="workspace-metric-note">room types loaded</span>
                </div>
            </div>
        </section>

        <section class="utility-band import-journey-band">
            <div class="utility-band-copy">
                <span class="utility-band-kicker">Workflow</span>
                <h3 class="utility-band-title">History -> Inventory -> Threshold Review -> BoostMe</h3>
                <p class="utility-band-text">Replace and append modes stay visible so every import behaves like an explicit decision, not a hidden overwrite.</p>
            </div>
        </section>

        <!-- Status Overview -->
        <div class="status-grid">
            <div class="status-card ${hasMonthlyData ? 'status-success' : 'status-danger'}">
                <div class="status-icon">
                    <i class="bi ${hasMonthlyData ? 'bi-calendar-check' : 'bi-calendar-x'}"></i>
                </div>
                <div class="status-content">
                    <h6>Monthly Data</h6>
                    <small>${hasMonthlyData ? monthlyData.size() : 0} months imported</small>
                </div>
            </div>
            <div class="status-card ${hasRoomData ? 'status-success' : 'status-danger'}">
                <div class="status-icon">
                    <i class="bi ${hasRoomData ? 'bi-building-check' : 'bi-building-x'}"></i>
                </div>
                <div class="status-content">
                    <h6>Room Data</h6>
                    <small>${hasRoomData ? roomTypeCount : 0} types (${totalRoomCount} rooms)</small>
                </div>
            </div>
            <div class="status-card ${readyForOptimization ? 'status-success' : 'status-warning'}">
                <div class="status-icon">
                    <i class="bi ${readyForOptimization ? 'bi-rocket-takeoff' : 'bi-hourglass'}"></i>
                </div>
                <div class="status-content">
                    <h6>Optimization Status</h6>
                    <c:choose>
                        <c:when test="${readyForOptimization}">
                            <span class="ready-badge">Ready to Optimize!</span>
                        </c:when>
                        <c:otherwise>
                            <small>Import both datasets first</small>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <div class="content-grid import-layout">
            <!-- Left Column: Upload Forms -->
            <div class="upload-section">
                <!-- Monthly Data Upload -->
                <div class="upload-card import-station">
                    <div class="upload-header primary">
                        <i class="bi bi-calendar3"></i>
                        <h5>Step 1: Monthly Data</h5>
                    </div>
                    <div class="upload-body">
                        <p>Preview your demand history first, then confirm the import once the thresholds and rows look right.</p>
                        <form action="DataImport" method="post" enctype="multipart/form-data">
                            <input type="hidden" name="action" value="previewMonthly">
                            <div class="upload-zone" onclick="document.getElementById('monthlyFile').click()">
                                <i class="bi bi-file-earmark-bar-graph"></i>
                                <p>Click to upload monthly_data.csv</p>
                                <input type="file" name="monthlyFile" id="monthlyFile" accept=".csv" style="display:none;"
                                       onchange="showFileName(this, 'monthlyFileName')">
                                <small id="monthlyFileName" class="file-name"></small>
                            </div>
                            <div class="threshold-explanation" style="margin-bottom: 1rem;">
                                <strong>Import mode:</strong>
                                <label style="display:block; margin-top: 0.5rem;">
                                    <input type="radio" name="monthlyImportMode" value="replace" checked>
                                    Replace my existing monthly history for this account
                                </label>
                                <label style="display:block;">
                                    <input type="radio" name="monthlyImportMode" value="append">
                                    Append new month rows to my existing history
                                </label>
                            </div>
                            <div class="btn-row">
                                <button type="submit" class="btn-primary-modern">
                                    <i class="bi bi-search"></i> Preview Monthly Data
                                </button>
                                <a href="DataImport?action=downloadMonthlyTemplate" class="btn-outline">
                                    <i class="bi bi-file-earmark-arrow-down"></i> Template
                                </a>
                                <a href="DataImport?action=downloadMonthlySample" class="btn-outline">
                                    <i class="bi bi-download"></i> Sample
                                </a>
                            </div>
                            <c:if test="${not empty monthlyPreview}">
                                <div class="threshold-explanation" style="margin-top: 1rem;">
                                    <strong>Preview ready:</strong>
                                    <br>${monthlyPreview.acceptedRows} valid month rows from ${monthlyPreview.fileName}
                                    <br>Rejected rows: ${monthlyPreview.rejectedRows}
                                    <br>Import mode: ${monthlyPreview.replaceExisting ? 'replace' : 'append'}
                                </div>
                            </c:if>
                        </form>
                    </div>
                </div>

                <!-- Room Data Upload -->
                <div class="upload-card import-station">
                    <div class="upload-header success">
                        <i class="bi bi-building"></i>
                        <h5>Step 2: Room Data</h5>
                    </div>
                    <div class="upload-body">
                        <p>Preview room constraints before saving so floors, ceilings, and legacy backfills are obvious.</p>
                        <form action="DataImport" method="post" enctype="multipart/form-data">
                            <input type="hidden" name="action" value="previewRooms">
                            <div class="upload-zone" onclick="document.getElementById('roomFile').click()">
                                <i class="bi bi-building-add"></i>
                                <p>Click to upload room_data.csv</p>
                                <input type="file" name="roomFile" id="roomFile" accept=".csv" style="display:none;"
                                       onchange="showFileName(this, 'roomFileName')">
                                <small id="roomFileName" class="file-name"></small>
                            </div>
                            <div class="threshold-explanation" style="margin-bottom: 1rem;">
                                <strong>Import mode:</strong>
                                <label style="display:block; margin-top: 0.5rem;">
                                    <input type="radio" name="roomImportMode" value="replace" checked>
                                    Replace my current room inventory for this account
                                </label>
                                <label style="display:block;">
                                    <input type="radio" name="roomImportMode" value="append">
                                    Append additional room rows to my current inventory
                                </label>
                            </div>
                            <div class="btn-row">
                                <button type="submit" class="btn-primary-modern">
                                    <i class="bi bi-search"></i> Preview Room Data
                                </button>
                                <a href="DataImport?action=downloadRoomTemplate" class="btn-outline">
                                    <i class="bi bi-file-earmark-arrow-down"></i> Template
                                </a>
                                <a href="DataImport?action=downloadRoomSample" class="btn-outline">
                                    <i class="bi bi-download"></i> Sample
                                </a>
                            </div>
                            <c:if test="${not empty roomPreview}">
                                <div class="threshold-explanation" style="margin-top: 1rem;">
                                    <strong>Preview ready:</strong>
                                    <br>${roomPreview.parseResult.acceptedRows} valid room rows from ${roomPreview.fileName}
                                    <br>Rejected rows: ${roomPreview.parseResult.rejectedRows}
                                    <br>Import mode: ${roomPreview.replaceExisting ? 'replace' : 'append'}
                                </div>
                            </c:if>
                        </form>
                    </div>
                </div>

                <!-- Threshold Settings - FIXED VERSION -->
                <c:if test="${hasMonthlyData}">
                    <div class="threshold-card">
                        <div class="threshold-header">
                            <h5><i class="bi bi-sliders"></i> Season Thresholds (AI-Generated)</h5>

                            <!-- Visual representation of thresholds -->
                            <div class="threshold-visual">
                                <div class="threshold-season low">LOW<br><small>< T1</small></div>
                                <div class="threshold-divider">T1: <fmt:formatNumber value="${thresholds.thresholdLowNormal}" maxFractionDigits="1"/>%</div>
                                <div class="threshold-season normal">NORMAL<br><small>T1-T2</small></div>
                                <div class="threshold-divider">T2: <fmt:formatNumber value="${thresholds.thresholdNormalPeak}" maxFractionDigits="1"/>%</div>
                                <div class="threshold-season peak">PEAK<br><small>T2-T3</small></div>
                                <div class="threshold-divider">T3: <fmt:formatNumber value="${thresholds.thresholdPeakSuperPeak}" maxFractionDigits="1"/>%</div>
                                <div class="threshold-season super">SUPER<br><small>> T3</small></div>
                            </div>
                        </div>
                        <div class="threshold-body">
                            <div class="threshold-explanation">
                                <strong>How thresholds work:</strong> Occupancy rates are classified into 4 seasons based on 3 threshold values.
                                <br>• <strong>LOW:</strong> Occupancy &lt; T1
                                <br>• <strong>NORMAL:</strong> T1 ≤ Occupancy &lt; T2
                                <br>• <strong>PEAK:</strong> T2 ≤ Occupancy &lt; T3
                                <br>• <strong>SUPER_PEAK:</strong> Occupancy ≥ T3
                            </div>

                            <form action="DataImport" method="post">
                                <input type="hidden" name="action" value="updateThresholds">
                                <div class="threshold-form-grid">
                                    <div class="threshold-item">
                                        <label>T1: LOW → NORMAL</label>
                                        <input type="number" name="t1" class="threshold-input"
                                               value="<fmt:formatNumber value="${thresholds.thresholdLowNormal}" maxFractionDigits="2"/>"
                                               step="0.1" placeholder="e.g. 65">
                                    </div>
                                    <div class="threshold-item">
                                        <label>T2: NORMAL → PEAK</label>
                                        <input type="number" name="t2" class="threshold-input"
                                               value="<fmt:formatNumber value="${thresholds.thresholdNormalPeak}" maxFractionDigits="2"/>"
                                               step="0.1" placeholder="e.g. 75">
                                    </div>
                                    <div class="threshold-item">
                                        <label>T3: PEAK → SUPER</label>
                                        <input type="number" name="t3" class="threshold-input"
                                               value="<fmt:formatNumber value="${thresholds.thresholdPeakSuperPeak}" maxFractionDigits="2"/>"
                                               step="0.1" placeholder="e.g. 85">
                                    </div>
                                </div>
                                <button type="submit" class="btn-primary-modern" style="width: 100%;">
                                    <i class="bi bi-check-lg"></i> Apply Thresholds
                                </button>
                            </form>
                        </div>
                    </div>
                </c:if>

                <!-- Ready to Optimize -->
                <c:if test="${readyForOptimization}">
                    <div class="ready-card">
                        <h5><i class="bi bi-rocket-takeoff"></i> Ready to Optimize!</h5>
                        <p>Your monthly demand history, thresholds, and room inventory are loaded for this account.</p>
                        <c:choose>
                            <c:when test="${onboardingActive}">
                                <form action="DataImport" method="post">
                                    <input type="hidden" name="action" value="continueOnboardingImport">
                                    <button type="submit" class="btn-light">
                                        <i class="bi bi-arrow-right-circle"></i> Continue to Multipliers
                                    </button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <a href="${nextStepUrl}" class="btn-light">
                                    <i class="bi bi-magic"></i> Go to BoostMe
                                </a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:if>
            </div>

            <!-- Right Column: Data Display -->
            <div class="data-section">
                <c:if test="${not empty monthlyPreview}">
                    <div class="data-card preview-card">
                        <div class="data-header preview-header">
                            <div>
                                <h6><i class="bi bi-eye"></i> Monthly Import Preview</h6>
                                <span class="preview-subtitle">${monthlyPreview.fileName}</span>
                            </div>
                            <span class="preview-badge ${monthlyPreview.gaUsed ? 'preview-badge-ai' : 'preview-badge-default'}">
                                ${monthlyPreview.gaUsed ? 'AI threshold preview' : 'Default threshold fallback'}
                            </span>
                        </div>
                        <div class="data-body">
                            <div class="preview-intro-shell">
                                <div class="preview-intro-copy">
                                    <span class="preview-kicker">Import Digest</span>
                                    <h3 class="preview-intro-title">${monthlyPreview.acceptedRows} month rows are ready to import</h3>
                                    <p class="preview-intro-text">
                                        ${monthlyPreview.gaUsed ? 'The uploaded history produced an AI threshold preview based on your hotel’s own occupancy patterns.' : 'The uploaded history is still previewable, but the threshold map is using the default fallback because there are fewer than 4 valid rows.'}
                                    </p>
                                </div>
                                <div class="preview-summary-grid">
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Accepted Rows</span>
                                        <strong>${monthlyPreview.acceptedRows}</strong>
                                    </div>
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Rejected Rows</span>
                                        <strong>${monthlyPreview.rejectedRows}</strong>
                                    </div>
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Import Mode</span>
                                        <strong>${monthlyPreview.replaceExisting ? 'Replace existing history' : 'Append to existing history'}</strong>
                                    </div>
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Threshold Source</span>
                                        <strong>${monthlyPreview.gaUsed ? 'GA-generated from uploaded history' : 'Default thresholds'}</strong>
                                    </div>
                                </div>
                            </div>

                            <div class="preview-threshold-board">
                                <div class="preview-threshold-board-header">
                                    <span class="preview-kicker">Season Map</span>
                                    <span class="preview-threshold-caption">LOW &lt; NORMAL &lt; PEAK &lt; SUPER_PEAK</span>
                                </div>
                                <div class="preview-thresholds">
                                    <div class="preview-threshold-chip low">LOW &lt; <fmt:formatNumber value="${monthlyPreview.threshold.thresholdLowNormal}" maxFractionDigits="1"/>%</div>
                                    <div class="preview-threshold-chip normal">NORMAL &lt; <fmt:formatNumber value="${monthlyPreview.threshold.thresholdNormalPeak}" maxFractionDigits="1"/>%</div>
                                    <div class="preview-threshold-chip peak">PEAK &lt; <fmt:formatNumber value="${monthlyPreview.threshold.thresholdPeakSuperPeak}" maxFractionDigits="1"/>%</div>
                                    <div class="preview-threshold-chip super">SUPER ≥ <fmt:formatNumber value="${monthlyPreview.threshold.thresholdPeakSuperPeak}" maxFractionDigits="1"/>%</div>
                                </div>
                            </div>

                            <c:if test="${not empty monthlyPreview.warnings}">
                                <div class="preview-note warning">
                                    <strong>Check before import:</strong>
                                    <c:forEach var="warning" items="${monthlyPreview.warnings}">
                                        <div>${warning}</div>
                                    </c:forEach>
                                </div>
                            </c:if>

                            <div class="table-wrapper">
                                <table class="data-table preview-table">
                                    <thead>
                                        <tr>
                                            <th>Month</th>
                                            <th>Occupancy</th>
                                            <th>Revenue</th>
                                            <th>ADR</th>
                                            <th>Preview Season</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="data" items="${monthlyPreview.importedData}">
                                            <tr>
                                                <td>
                                                    <strong>${data.monthName}</strong><br>
                                                    <small style="color: var(--text-secondary);">${data.monthYear}</small>
                                                </td>
                                                <td><fmt:formatNumber value="${data.occupancyRate}" maxFractionDigits="1"/>%</td>
                                                <td>RM <fmt:formatNumber value="${data.totalRevenue}" type="number" groupingUsed="true" maxFractionDigits="0"/></td>
                                                <td>RM <fmt:formatNumber value="${data.avgRoomRate}" maxFractionDigits="2"/></td>
                                                <td>
                                                    <span class="season-badge ${data.classifiedSeason == 'LOW' ? 'badge-low' : data.classifiedSeason == 'NORMAL' ? 'badge-normal' : data.classifiedSeason == 'PEAK' ? 'badge-peak' : 'badge-super'}">
                                                        ${data.classifiedSeason}
                                                    </span>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>

                            <div class="preview-actions">
                                <form action="DataImport" method="post">
                                    <input type="hidden" name="action" value="commitMonthlyImport">
                                    <button type="submit" class="btn-primary-modern">
                                        <i class="bi bi-check2-circle"></i> Confirm Monthly Import
                                    </button>
                                </form>
                                <form action="DataImport" method="post">
                                    <input type="hidden" name="action" value="discardMonthlyPreview">
                                    <button type="submit" class="btn-outline">
                                        <i class="bi bi-x-circle"></i> Discard Preview
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </c:if>

                <c:if test="${not empty roomPreview}">
                    <div class="data-card preview-card">
                        <div class="data-header preview-header">
                            <div>
                                <h6><i class="bi bi-eye"></i> Room Import Preview</h6>
                                <span class="preview-subtitle">${roomPreview.fileName}</span>
                            </div>
                            <span class="preview-badge preview-badge-rooms">
                                ${roomPreview.replaceExisting ? 'Replace inventory' : 'Append inventory'}
                            </span>
                        </div>
                        <div class="data-body">
                            <div class="preview-intro-shell">
                                <div class="preview-intro-copy">
                                    <span class="preview-kicker">Constraint Digest</span>
                                    <h3 class="preview-intro-title">${roomPreview.parseResult.acceptedRows} room rows are ready to become optimizer bounds</h3>
                                    <p class="preview-intro-text">
                                        This preview highlights backfilled base rates and floor-anchored rooms before those constraints shape the GA search space.
                                    </p>
                                </div>
                                <div class="preview-summary-grid">
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Accepted Rows</span>
                                        <strong>${roomPreview.parseResult.acceptedRows}</strong>
                                    </div>
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Rejected Rows</span>
                                        <strong>${roomPreview.parseResult.rejectedRows}</strong>
                                    </div>
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Base Backfills</span>
                                        <strong>${roomPreview.parseResult.backfilledBaseRateCount}</strong>
                                    </div>
                                    <div class="preview-summary-item">
                                        <span class="preview-summary-label">Base = Min Floor Locks</span>
                                        <strong>${roomPreview.parseResult.floorAnchoredCount}</strong>
                                    </div>
                                </div>
                            </div>

                            <c:if test="${not empty roomPreview.parseResult.warnings}">
                                <div class="preview-note warning">
                                    <strong>Check before import:</strong>
                                    <c:forEach var="warning" items="${roomPreview.parseResult.warnings}">
                                        <div>${warning}</div>
                                    </c:forEach>
                                </div>
                            </c:if>

                            <div class="table-wrapper">
                                <table class="data-table preview-table">
                                    <thead>
                                        <tr>
                                            <th>CSV Line</th>
                                            <th>Room Type</th>
                                            <th>Qty</th>
                                            <th>Base ADR</th>
                                            <th>Min ADR</th>
                                            <th>Max ADR</th>
                                            <th>Occupancy</th>
                                            <th>Flags</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="row" items="${roomPreview.parseResult.previewRows}">
                                            <tr>
                                                <td>${row.lineNumber}</td>
                                                <td><strong>${row.room.name}</strong></td>
                                                <td>${row.room.totalRooms}</td>
                                                <td>RM <fmt:formatNumber value="${row.room.baseAdr}" maxFractionDigits="2"/></td>
                                                <td>RM <fmt:formatNumber value="${row.room.minAdr}" maxFractionDigits="2"/></td>
                                                <td>RM <fmt:formatNumber value="${row.room.maxAdr}" maxFractionDigits="2"/></td>
                                                <td><fmt:formatNumber value="${row.room.occupancy}" maxFractionDigits="1"/>%</td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${empty row.flags}">
                                                            <span class="preview-flag neutral">Clean</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <c:forEach var="flag" items="${row.flags}">
                                                                <span class="preview-flag ${flag == 'Base ADR backfilled' ? 'warning' : flag == 'Base = floor' ? 'floor' : 'attention'}">${flag}</span>
                                                            </c:forEach>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>

                            <div class="preview-actions">
                                <form action="DataImport" method="post">
                                    <input type="hidden" name="action" value="commitRoomImport">
                                    <button type="submit" class="btn-primary-modern">
                                        <i class="bi bi-check2-circle"></i> Confirm Room Import
                                    </button>
                                </form>
                                <form action="DataImport" method="post">
                                    <input type="hidden" name="action" value="discardRoomPreview">
                                    <button type="submit" class="btn-outline">
                                        <i class="bi bi-x-circle"></i> Discard Preview
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </c:if>

                <!-- Season Distribution -->
                <c:if test="${hasMonthlyData}">
                    <div class="data-card">
                        <div class="data-header">
                            <h6><i class="bi bi-pie-chart"></i> Season Distribution</h6>
                            <form action="DataImport" method="post" onsubmit="return confirm('Delete all monthly data?');">
                                <input type="hidden" name="action" value="deleteMonthly">
                                <button type="submit" class="btn-delete">
                                    <i class="bi bi-trash"></i> Delete
                                </button>
                            </form>
                        </div>
                        <div class="data-body">
                            <div class="season-dist">
                                <div class="season-box low" data-season="LOW" onclick="toggleSeasonDetail('LOW', this)">
                                    <h4>${seasonDistribution['LOW']}</h4>
                                    <span class="season-badge badge-low">LOW</span>
                                    <span class="click-hint">Click to view months</span>
                                </div>
                                <div class="season-box normal" data-season="NORMAL" onclick="toggleSeasonDetail('NORMAL', this)">
                                    <h4>${seasonDistribution['NORMAL']}</h4>
                                    <span class="season-badge badge-normal">NORMAL</span>
                                    <span class="click-hint">Click to view months</span>
                                </div>
                                <div class="season-box peak" data-season="PEAK" onclick="toggleSeasonDetail('PEAK', this)">
                                    <h4>${seasonDistribution['PEAK']}</h4>
                                    <span class="season-badge badge-peak">PEAK</span>
                                    <span class="click-hint">Click to view months</span>
                                </div>
                                <div class="season-box super" data-season="SUPER_PEAK" onclick="toggleSeasonDetail('SUPER_PEAK', this)">
                                    <h4>${seasonDistribution['SUPER_PEAK']}</h4>
                                    <span class="season-badge badge-super">SUPER</span>
                                    <span class="click-hint">Click to view months</span>
                                </div>
                            </div>

                            <!-- Season detail panel (populated by JS) -->
                            <div id="seasonDetailPanel" class="season-detail-panel">
                                <div id="seasonDetailHeader" class="season-detail-header">
                                    <span id="seasonDetailTitle" class="season-detail-title"></span>
                                    <button class="season-detail-close" onclick="closeSeasonDetail()">&times;</button>
                                </div>
                                <div class="season-detail-body">
                                    <div id="seasonMonthList" class="season-month-list"></div>
                                </div>
                            </div>
                            <div class="table-wrapper">
                                <table class="data-table">
                                    <thead>
                                        <tr>
                                            <th>Month</th>
                                            <th>Occupancy</th>
                                            <th>Revenue</th>
                                            <th>Season</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="data" items="${monthlyData}">
                                            <tr>
                                                <td>
                                                    <strong>${data.monthName}</strong><br>
                                                    <small style="color: var(--text-secondary);">${data.monthYear}</small>
                                                </td>
                                                <td><fmt:formatNumber value="${data.occupancyRate}" maxFractionDigits="1"/>%</td>
                                                <td>RM <fmt:formatNumber value="${data.totalRevenue}" type="number" groupingUsed="true" maxFractionDigits="0"/></td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${data.classifiedSeason == 'LOW'}">
                                                            <span class="season-badge badge-low">LOW</span>
                                                        </c:when>
                                                        <c:when test="${data.classifiedSeason == 'NORMAL'}">
                                                            <span class="season-badge badge-normal">NORMAL</span>
                                                        </c:when>
                                                        <c:when test="${data.classifiedSeason == 'PEAK'}">
                                                            <span class="season-badge badge-peak">PEAK</span>
                                                        </c:when>
                                                        <c:when test="${data.classifiedSeason == 'SUPER_PEAK'}">
                                                            <span class="season-badge badge-super">SUPER</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="season-badge">${data.classifiedSeason}</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </c:if>

                <!-- Room Inventory -->
                <c:if test="${hasRoomData}">
                    <div class="data-card">
                        <div class="data-header">
                            <h6><i class="bi bi-building"></i> Room Inventory (${roomTypeCount} types, ${totalRoomCount} rooms)</h6>
                            <form action="DataImport" method="post" onsubmit="return confirm('Delete all room data?');">
                                <input type="hidden" name="action" value="deleteRooms">
                                <button type="submit" class="btn-delete">
                                    <i class="bi bi-trash"></i> Delete
                                </button>
                            </form>
                        </div>
                        <div class="data-body">
                            <div class="threshold-explanation" style="margin-bottom: 1rem;">
                                Weighted average base ADR: RM <fmt:formatNumber value="${weightedAverageBaseRate}" maxFractionDigits="2"/>
                            </div>
                            <div class="table-wrapper">
                                <table class="data-table">
                                    <thead>
                                        <tr>
                                            <th>Room Type</th>
                                            <th>Qty</th>
                                            <th>Base Rate</th>
                                            <th>Min</th>
                                            <th>Max</th>
                                            <th>Occupancy</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="room" items="${rooms}">
                                            <tr>
                                                <td>
                                                    <i class="bi bi-door-open" style="color: var(--text-secondary);"></i>
                                                    ${room.name}
                                                </td>
                                                <td><span class="season-badge" style="background: var(--bg-dark); color: var(--text-inverse);">${room.totalRooms}</span></td>
                                                <td><strong>RM <fmt:formatNumber value="${room.baseAdr}" maxFractionDigits="0"/></strong></td>
                                                <td>RM <fmt:formatNumber value="${room.minAdr}" maxFractionDigits="0"/></td>
                                                <td>RM <fmt:formatNumber value="${room.maxAdr}" maxFractionDigits="0"/></td>
                                                <td><fmt:formatNumber value="${room.occupancy}" maxFractionDigits="1"/>%</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </c:if>

                <!-- Empty State -->
                <c:if test="${!hasMonthlyData && !hasRoomData}">
                    <div class="data-card">
                        <div class="data-body">
                            <div class="empty-state">
                                <i class="bi bi-inbox"></i>
                                <h5>No Data Imported Yet</h5>
                                <p>Upload your monthly data and room data using the forms on the left.</p>
                                <p>You can download sample files to see the expected format.</p>
                            </div>
                        </div>
                    </div>
                </c:if>
            </div>
        </div>
    </div>

    <script>
        function showFileName(input, labelId) {
            const label = document.getElementById(labelId);
            if (input.files.length > 0) {
                label.textContent = '\u2713 ' + input.files[0].name;
                label.classList.add('show');
            }
        }

        // Drag and drop support
        document.querySelectorAll('.upload-zone').forEach(zone => {
            zone.addEventListener('dragover', (e) => {
                e.preventDefault();
                zone.classList.add('dragover');
            });

            zone.addEventListener('dragleave', () => {
                zone.classList.remove('dragover');
            });

            zone.addEventListener('drop', (e) => {
                e.preventDefault();
                zone.classList.remove('dragover');
                const input = zone.querySelector('input[type="file"]');
                if (e.dataTransfer.files.length > 0) {
                    input.files = e.dataTransfer.files;
                    const labelId = input.id === 'monthlyFile' ? 'monthlyFileName' : 'roomFileName';
                    showFileName(input, labelId);
                }
            });
        });

        // === Season Detail Panel ===
        // Build monthly data grouped by season from the server-side data
        var monthsBySeason = {};
        <c:if test="${hasMonthlyData}">
            <c:forEach var="data" items="${monthlyData}">
                (function() {
                    var season = '${data.classifiedSeason}';
                    if (!monthsBySeason[season]) monthsBySeason[season] = [];
                    monthsBySeason[season].push({
                        name: '${data.monthName}',
                        year: '${data.monthYear}',
                        occupancy: '<fmt:formatNumber value="${data.occupancyRate}" maxFractionDigits="1"/>',
                        revenue: '<fmt:formatNumber value="${data.totalRevenue}" type="number" groupingUsed="true" maxFractionDigits="0"/>'
                    });
                })();
            </c:forEach>
        </c:if>

        var activeSeason = null;

        var seasonLabels = {
            'LOW': 'Low Season',
            'NORMAL': 'Normal Season',
            'PEAK': 'Peak Season',
            'SUPER_PEAK': 'Super Peak Season'
        };

        var seasonHeaderClass = {
            'LOW': 'low',
            'NORMAL': 'normal',
            'PEAK': 'peak',
            'SUPER_PEAK': 'super'
        };

        var seasonBadgeClass = {
            'LOW': 'badge-low',
            'NORMAL': 'badge-normal',
            'PEAK': 'badge-peak',
            'SUPER_PEAK': 'badge-super'
        };

        function toggleSeasonDetail(season, boxEl) {
            var panel = document.getElementById('seasonDetailPanel');
            var header = document.getElementById('seasonDetailHeader');
            var title = document.getElementById('seasonDetailTitle');
            var list = document.getElementById('seasonMonthList');

            // Remove active from all boxes
            document.querySelectorAll('.season-box').forEach(function(b) { b.classList.remove('active'); });

            // If clicking the same season, close the panel
            if (activeSeason === season && panel.classList.contains('show')) {
                panel.classList.remove('show');
                activeSeason = null;
                return;
            }

            activeSeason = season;
            boxEl.classList.add('active');

            // Set header style
            header.className = 'season-detail-header ' + seasonHeaderClass[season];
            title.textContent = seasonLabels[season] + ' \u2014 Months';

            // Populate month chips
            var months = monthsBySeason[season] || [];
            list.innerHTML = '';

            if (months.length === 0) {
                list.innerHTML = '<span class="season-detail-empty">No months classified in this season.</span>';
            } else {
                months.forEach(function(m) {
                    var chip = document.createElement('div');
                    chip.className = 'season-month-chip';
                    chip.innerHTML =
                        '<span class="month-name">' + m.name + ' ' + m.year + '</span>' +
                        '<span class="month-occ">' + m.occupancy + '% occ</span>';
                    list.appendChild(chip);
                });
            }

            panel.classList.add('show');
            panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        function closeSeasonDetail() {
            document.getElementById('seasonDetailPanel').classList.remove('show');
            document.querySelectorAll('.season-box').forEach(function(b) { b.classList.remove('active'); });
            activeSeason = null;
        }
    </script>
</body>
</html>
