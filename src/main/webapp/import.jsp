<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Import Data - EzBoost</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <link href="css/styles.css" rel="stylesheet">
    <style>
        .upload-zone { border: 2px dashed #6c5ce7; border-radius: 15px; padding: 40px; text-align: center; background: linear-gradient(135deg, #f8f9ff 0%, #e8e4ff 100%); transition: all 0.3s ease; cursor: pointer; }
        .upload-zone:hover { border-color: #5b4cdb; background: linear-gradient(135deg, #f0edff 0%, #ddd7ff 100%); }
        .upload-zone.dragover { border-color: #00b894; background: linear-gradient(135deg, #e8fff5 0%, #d4ffe8 100%); }
        .season-badge { padding: 5px 15px; border-radius: 20px; font-weight: 600; font-size: 0.85em; }
        .season-LOW { background: #74b9ff; color: #0056b3; }
        .season-NORMAL { background: #00b894; color: white; }
        .season-PEAK { background: #fdcb6e; color: #856404; }
        .season-SUPER_PEAK { background: #e17055; color: white; }
        .metric-card { border-radius: 15px; padding: 15px; text-align: center; transition: transform 0.3s ease; }
        .metric-card:hover { transform: translateY(-3px); }
        .threshold-display { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 15px; padding: 20px; }
        .threshold-value { font-size: 1.4rem; font-weight: bold; }
        .delete-btn { opacity: 0.6; transition: opacity 0.3s; }
        .delete-btn:hover { opacity: 1; }
        .data-row:hover { background-color: #f8f9fa; }
    </style>
</head>
<body>
    <%@ include file="nav.jsp" %>

    <div class="container mt-4">
        <div class="row">
            <div class="col-12">
                <h2 class="mb-2"><i class="bi bi-cloud-upload text-primary"></i> Import Hotel Data</h2>
                <p class="text-muted">Upload monthly performance data for AI-powered season classification.</p>
            </div>
        </div>

        <!-- Alert Messages -->
        <c:if test="${not empty success}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <i class="bi bi-check-circle"></i> ${success}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="bi bi-exclamation-triangle"></i> ${error}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <div class="row">
            <!-- Left Column: Upload & Thresholds -->
            <div class="col-lg-5 mb-4">
                <!-- Upload Card -->
                <div class="card shadow-sm mb-4">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0"><i class="bi bi-file-earmark-arrow-up"></i> Upload Data File</h5>
                    </div>
                    <div class="card-body">
                        <form action="ImportData" method="post" enctype="multipart/form-data" id="uploadForm">
                            <div class="upload-zone" id="dropZone">
                                <i class="bi bi-cloud-arrow-up display-1 text-primary"></i>
                                <h5 class="mt-3">Drag & Drop or Click</h5>
                                <p class="text-muted small">Supports .csv and .xlsx files</p>
                                <input type="file" name="dataFile" id="fileInput" accept=".csv,.xlsx,.xls" class="d-none">
                                <div id="fileInfo" class="mt-3 d-none">
                                    <span class="badge bg-success fs-6"><i class="bi bi-file-earmark-check"></i> <span id="fileName"></span></span>
                                </div>
                            </div>

                            <div class="mt-3">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="replaceExisting" value="true" id="replaceCheck">
                                    <label class="form-check-label" for="replaceCheck">Replace existing data</label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" name="useGAClassification" value="true" id="gaCheck" checked>
                                    <label class="form-check-label" for="gaCheck">
                                        <i class="bi bi-cpu text-primary"></i> Use AI (GA) for season classification
                                    </label>
                                </div>
                            </div>

                            <div class="d-grid gap-2 mt-3">
                                <button type="submit" class="btn btn-primary" id="uploadBtn" disabled>
                                    <i class="bi bi-upload"></i> Import Data
                                </button>
                            </div>
                        </form>
                        
                        <hr>
                        <div class="d-flex gap-2">
                            <a href="ImportData?action=downloadTemplate" class="btn btn-outline-secondary btn-sm flex-fill">
                                <i class="bi bi-file-earmark-text"></i> Template
                            </a>
                            <a href="ImportData?action=downloadSample" class="btn btn-outline-info btn-sm flex-fill">
                                <i class="bi bi-file-earmark-spreadsheet"></i> Sample Data
                            </a>
                        </div>
                    </div>
                </div>

                <!-- Current Thresholds Card -->
                <c:if test="${not empty thresholds}">
                    <div class="card shadow-sm mb-4">
                        <div class="card-header">
                            <h6 class="mb-0">
                                <i class="bi bi-sliders"></i> Season Thresholds
                                <c:if test="${thresholds.autoGenerated}">
                                    <span class="badge bg-success ms-2"><i class="bi bi-cpu"></i> AI Generated</span>
                                </c:if>
                                <c:if test="${!thresholds.autoGenerated}">
                                    <span class="badge bg-secondary ms-2">Manual</span>
                                </c:if>
                            </h6>
                        </div>
                        <div class="card-body">
                            <div class="threshold-display mb-3">
                                <div class="row text-center">
                                    <div class="col-4">
                                        <small>LOW / NORMAL</small>
                                        <div class="threshold-value"><fmt:formatNumber value="${thresholds.thresholdLowNormal}" maxFractionDigits="1"/>%</div>
                                    </div>
                                    <div class="col-4">
                                        <small>NORMAL / PEAK</small>
                                        <div class="threshold-value"><fmt:formatNumber value="${thresholds.thresholdNormalPeak}" maxFractionDigits="1"/>%</div>
                                    </div>
                                    <div class="col-4">
                                        <small>PEAK / SUPER</small>
                                        <div class="threshold-value"><fmt:formatNumber value="${thresholds.thresholdPeakSuperPeak}" maxFractionDigits="1"/>%</div>
                                    </div>
                                </div>
                            </div>
                            
                            <form action="ImportData" method="post" class="mb-2">
                                <input type="hidden" name="action" value="reclassify">
                                <button type="submit" class="btn btn-outline-primary btn-sm w-100">
                                    <i class="bi bi-arrow-repeat"></i> Re-run AI Classification
                                </button>
                            </form>
                            
                            <button class="btn btn-outline-secondary btn-sm w-100" type="button" data-bs-toggle="collapse" data-bs-target="#manualThresholds">
                                <i class="bi bi-pencil"></i> Set Manual Thresholds
                            </button>
                            <div class="collapse mt-2" id="manualThresholds">
                                <form action="ImportData" method="post">
                                    <input type="hidden" name="action" value="manualThresholds">
                                    <div class="row g-2">
                                        <div class="col-4">
                                            <input type="number" name="thresholdLowNormal" class="form-control form-control-sm" 
                                                   value="${thresholds.thresholdLowNormal}" step="0.1" placeholder="T1">
                                        </div>
                                        <div class="col-4">
                                            <input type="number" name="thresholdNormalPeak" class="form-control form-control-sm" 
                                                   value="${thresholds.thresholdNormalPeak}" step="0.1" placeholder="T2">
                                        </div>
                                        <div class="col-4">
                                            <input type="number" name="thresholdPeakSuperPeak" class="form-control form-control-sm" 
                                                   value="${thresholds.thresholdPeakSuperPeak}" step="0.1" placeholder="T3">
                                        </div>
                                    </div>
                                    <button type="submit" class="btn btn-primary btn-sm w-100 mt-2">Apply</button>
                                </form>
                            </div>
                        </div>
                    </div>
                </c:if>

                <!-- Season Distribution Card -->
                <c:if test="${not empty seasonDistribution}">
                    <div class="card shadow-sm">
                        <div class="card-header bg-success text-white">
                            <h6 class="mb-0"><i class="bi bi-pie-chart"></i> Season Distribution</h6>
                        </div>
                        <div class="card-body">
                            <div class="row text-center g-2">
                                <div class="col-3">
                                    <div class="metric-card bg-info bg-opacity-25">
                                        <h4 class="mb-0">${seasonDistribution['LOW']}</h4>
                                        <span class="season-badge season-LOW" style="font-size:0.7em;">LOW</span>
                                    </div>
                                </div>
                                <div class="col-3">
                                    <div class="metric-card bg-success bg-opacity-25">
                                        <h4 class="mb-0">${seasonDistribution['NORMAL']}</h4>
                                        <span class="season-badge season-NORMAL" style="font-size:0.7em;">NORMAL</span>
                                    </div>
                                </div>
                                <div class="col-3">
                                    <div class="metric-card bg-warning bg-opacity-25">
                                        <h4 class="mb-0">${seasonDistribution['PEAK']}</h4>
                                        <span class="season-badge season-PEAK" style="font-size:0.7em;">PEAK</span>
                                    </div>
                                </div>
                                <div class="col-3">
                                    <div class="metric-card bg-danger bg-opacity-25">
                                        <h4 class="mb-0">${seasonDistribution['SUPER_PEAK']}</h4>
                                        <span class="season-badge season-SUPER_PEAK" style="font-size:0.7em;">SUPER</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:if>
            </div>

            <!-- Right Column: Data Table -->
            <div class="col-lg-7 mb-4">
                <div class="card shadow-sm">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="bi bi-table"></i> Monthly Data</h5>
                        <c:if test="${not empty existingData}">
                            <div class="d-flex gap-2">
                                <c:if test="${not empty availableYears}">
                                    <select class="form-select form-select-sm" style="width: auto;" id="yearFilter">
                                        <option value="">All Years</option>
                                        <c:forEach var="year" items="${availableYears}">
                                            <option value="${year}">${year}</option>
                                        </c:forEach>
                                    </select>
                                </c:if>
                                <form action="ImportData" method="post" class="d-inline" 
                                      onsubmit="return confirm('Delete ALL data? This cannot be undone.');">
                                    <input type="hidden" name="action" value="deleteAll">
                                    <button type="submit" class="btn btn-outline-danger btn-sm">
                                        <i class="bi bi-trash"></i> Delete All
                                    </button>
                                </form>
                            </div>
                        </c:if>
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <c:when test="${not empty existingData}">
                                <div class="table-responsive">
                                    <table class="table table-sm table-hover" id="dataTable">
                                        <thead class="table-light">
                                            <tr>
                                                <th>Month</th>
                                                <th>Occupancy</th>
                                                <th>Revenue</th>
                                                <th>ADR</th>
                                                <th>Season</th>
                                                <th></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="data" items="${existingData}">
                                                <tr class="data-row" data-year="${data.monthYear.substring(0, 4)}">
                                                    <td>
                                                        <strong>${data.monthName}</strong>
                                                        <br><small class="text-muted">${data.monthYear}</small>
                                                    </td>
                                                    <td>
                                                        <div class="progress" style="height: 20px; min-width: 80px;">
                                                            <div class="progress-bar 
                                                                <c:choose>
                                                                    <c:when test="${data.occupancyRate >= 85}">bg-danger</c:when>
                                                                    <c:when test="${data.occupancyRate >= 75}">bg-warning</c:when>
                                                                    <c:when test="${data.occupancyRate >= 65}">bg-success</c:when>
                                                                    <c:otherwise>bg-info</c:otherwise>
                                                                </c:choose>" 
                                                                style="width: ${data.occupancyRate}%">
                                                                <fmt:formatNumber value="${data.occupancyRate}" maxFractionDigits="1"/>%
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td><fmt:formatNumber value="${data.totalRevenue}" type="number" groupingUsed="true" maxFractionDigits="0"/></td>
                                                    <td><fmt:formatNumber value="${data.avgRoomRate}" maxFractionDigits="0"/></td>
                                                    <td><span class="season-badge season-${data.classifiedSeason}">${data.classifiedSeason}</span></td>
                                                    <td>
                                                        <form action="ImportData" method="post" class="d-inline"
                                                              onsubmit="return confirm('Delete ${data.monthName}?');">
                                                            <input type="hidden" name="action" value="delete">
                                                            <input type="hidden" name="dataId" value="${data.dataId}">
                                                            <button type="submit" class="btn btn-link btn-sm text-danger delete-btn p-0">
                                                                <i class="bi bi-trash"></i>
                                                            </button>
                                                        </form>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                                
                                <div class="mt-3 p-3 bg-light rounded">
                                    <div class="row text-center">
                                        <div class="col-4">
                                            <small class="text-muted">Total Months</small>
                                            <h5>${existingData.size()}</h5>
                                        </div>
                                        <div class="col-4">
                                            <small class="text-muted">Avg Occupancy</small>
                                            <h5>
                                                <c:set var="totalOcc" value="0"/>
                                                <c:forEach var="d" items="${existingData}">
                                                    <c:set var="totalOcc" value="${totalOcc + d.occupancyRate}"/>
                                                </c:forEach>
                                                <fmt:formatNumber value="${totalOcc / existingData.size()}" maxFractionDigits="1"/>%
                                            </h5>
                                        </div>
                                        <div class="col-4">
                                            <small class="text-muted">Total Revenue</small>
                                            <h5>
                                                <c:set var="totalRev" value="0"/>
                                                <c:forEach var="d" items="${existingData}">
                                                    <c:set var="totalRev" value="${totalRev + d.totalRevenue}"/>
                                                </c:forEach>
                                                <fmt:formatNumber value="${totalRev / 1000000}" maxFractionDigits="1"/>M
                                            </h5>
                                        </div>
                                    </div>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="text-center py-5">
                                    <i class="bi bi-inbox display-1 text-muted"></i>
                                    <p class="text-muted mt-3">No data imported yet.</p>
                                    <p class="text-muted">Upload a CSV/Excel file or download the sample data to get started.</p>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        const dropZone = document.getElementById('dropZone');
        const fileInput = document.getElementById('fileInput');
        const uploadBtn = document.getElementById('uploadBtn');
        const fileInfo = document.getElementById('fileInfo');
        const fileName = document.getElementById('fileName');

        dropZone.addEventListener('click', () => fileInput.click());
        dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });
        dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.classList.remove('dragover');
            if (e.dataTransfer.files.length > 0) {
                fileInput.files = e.dataTransfer.files;
                showFileInfo(e.dataTransfer.files[0]);
            }
        });
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) showFileInfo(e.target.files[0]);
        });
        function showFileInfo(file) {
            fileName.textContent = file.name;
            fileInfo.classList.remove('d-none');
            uploadBtn.disabled = false;
        }

        const yearFilter = document.getElementById('yearFilter');
        if (yearFilter) {
            yearFilter.addEventListener('change', function() {
                const selectedYear = this.value;
                document.querySelectorAll('.data-row').forEach(row => {
                    row.style.display = (!selectedYear || row.dataset.year === selectedYear) ? '' : 'none';
                });
            });
        }
    </script>
</body>
</html>
