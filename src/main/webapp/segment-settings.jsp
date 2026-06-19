<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Market Segment Settings - EzBoost</title>
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/settings.css">
</head>
<body class="segment-settings">
    <%@ include file="nav.jsp" %>

    <div class="main-container">
        <!-- Page Header -->
        <div class="page-header">
            <div class="page-badge">
                <i class="bi bi-people"></i>
                <span>Customer Segmentation</span>
            </div>
            <h1 class="page-title"><i class="bi bi-diagram-3"></i> Market Segment Settings</h1>
            <p class="page-subtitle">Base seasonal price × segment multiplier. Segment rows adjust the GA result after the optimizer sets the hotel-wide price.</p>
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
        <c:if test="${not empty error}">
            <div class="alert-modern alert-danger">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <span>${error}</span>
                <button class="alert-close" onclick="this.parentElement.remove()">&times;</button>
            </div>
        </c:if>

        <section class="workspace-hero segment-hero">
            <div class="workspace-hero-copy">
                <span class="workspace-kicker">Segment Pricing Matrix</span>
                <h2 class="workspace-title">Shape how each customer channel sits around the seasonal base price.</h2>
                <p class="workspace-text">EzBoost applies these multipliers after the GA sets the hotel-wide seasonal rate. Use this page to keep your segment posture clear, consistent, and easy to explain.</p>
            </div>
            <div class="workspace-hero-metrics">
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Configured Segments</span>
                    <strong class="workspace-metric-value">${segments.size()}</strong>
                    <span class="workspace-metric-note">active rate profiles</span>
                </div>
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Pricing Logic</span>
                    <strong class="workspace-metric-value">After GA</strong>
                    <span class="workspace-metric-note">base seasonal rate × segment multiplier</span>
                </div>
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Example</span>
                    <strong class="workspace-metric-value">
                        <c:choose>
                            <c:when test="${not empty segments}">
                                RM 240 → RM <fmt:formatNumber value="${240 * segments[0].rateMultiplier}" maxFractionDigits="0"/>
                            </c:when>
                            <c:otherwise>
                                RM 240 → RM 240
                            </c:otherwise>
                        </c:choose>
                    </strong>
                    <span class="workspace-metric-note">
                        <c:choose>
                            <c:when test="${not empty segments}">
                                sample using ${segments[0].segmentCode}
                            </c:when>
                            <c:otherwise>
                                sample seasonal price conversion
                            </c:otherwise>
                        </c:choose>
                    </span>
                </div>
            </div>
        </section>

        <section class="utility-band segment-example-band">
            <div class="utility-band-copy">
                <span class="utility-band-kicker">Live Reading</span>
                <h3 class="utility-band-title">Think in translated selling rates, not abstract decimals.</h3>
                <p class="utility-band-text">A multiplier of 1.20x means the segment displays 20% above the GA base price. A multiplier of 0.85x means the segment intentionally sits below the hotel-wide seasonal rate.</p>
            </div>
        </section>

        <div class="content-grid">
            <!-- Main Table Card -->
            <form action="SegmentSettings" method="post" id="segmentForm">
                <input type="hidden" name="action" value="updateSegments">
                <div class="main-card segment-workspace-card">
                    <div class="card-header">
                        <h5><i class="bi bi-table"></i> Market Segments</h5>
                        <span style="font-size: 0.85rem; color: var(--text-secondary);">${segments.size()} segments configured</span>
                    </div>

                    <div class="workspace-toolbar">
                        <div class="workspace-toolbar-copy">
                            <strong>Editing mode:</strong> update the multiplier inline, refine the description, and keep categories balanced for clearer downstream pricing.
                        </div>
                    </div>
                    
                    <div class="card-body" style="padding: 0; overflow-x: auto;">
                        <table class="segment-table">
                            <thead>
                                <tr>
                                    <th>Segment</th>
                                    <th>Category</th>
                                    <th>Rate Multiplier</th>
                                    <th>Description</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="segment" items="${segments}">
                                    <tr>
                                        <td>
                                            <div class="segment-name">
                                                <div class="segment-icon ${segment.rateMultiplier > 1.0 ? 'premium' : segment.rateMultiplier < 1.0 ? 'discount' : 'standard'}">
                                                    <i class="bi bi-person-badge"></i>
                                                </div>
                                                <div class="segment-details">
                                                    <h6>${segment.segmentName}</h6>
                                                    <small>${segment.segmentCode}</small>
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <span class="category-badge ${segment.category == 'FIT' ? 'fit' : 'git'}">
                                                ${segment.category}
                                            </span>
                                        </td>
                                        <td>
                                            <div class="multiplier-indicator">
                                                <input type="number" 
                                                       name="multiplier_${segment.segmentCode}" 
                                                       class="input-small" 
                                                       value="${segment.rateMultiplier}" 
                                                       min="0.5" max="2.0" step="0.05"
                                                       onchange="updateMultiplierBar(this)">
                                                <div class="multiplier-bar">
                                                    <div class="multiplier-fill ${segment.rateMultiplier > 1.0 ? 'premium' : segment.rateMultiplier < 1.0 ? 'discount' : 'standard'}" 
                                                         style="width: ${(segment.rateMultiplier / 2.0) * 100}%"></div>
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <input type="text" 
                                                   name="desc_${segment.segmentCode}" 
                                                   class="input-desc" 
                                                   value="${segment.description}" 
                                                   placeholder="Description...">
                                        </td>
                                        <td>
                                            <button type="button" class="btn-delete-small" onclick="confirmDelete('${segment.segmentCode}', '${segment.segmentName}')">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                    
                    <div class="action-bar">
                        <button type="submit" class="btn-primary-modern">
                            <i class="bi bi-save"></i> ${onboardingActive ? 'Save and Continue to BoostMe' : 'Save Changes'}
                        </button>
                        <button type="button" class="btn-secondary-modern" onclick="showResetModal()">
                            <i class="bi bi-arrow-counterclockwise"></i> Reset Defaults
                        </button>
                    </div>
                </div>
            </form>

                <!-- Sidebar -->
                <div class="sidebar">
                    <!-- Info Card -->
                    <div class="info-card">
                        <h5><i class="bi bi-info-circle"></i> How It Works</h5>
                        <hr>
                        <p style="font-size: 0.9rem; margin-bottom: 1rem;">
                            The <strong>Rate Multiplier</strong> adjusts the GA-optimized base price for each customer segment:
                        </p>
                        <ul>
                            <li><strong style="color: #fca5a5;">&gt; 1.0</strong> = Premium (higher price)</li>
                            <li><strong style="color: #fbbf24;">= 1.0</strong> = Standard (base price)</li>
                            <li><strong style="color: #86efac;">&lt; 1.0</strong> = Discount (lower price)</li>
                        </ul>
                        <hr>
                        <p style="font-size: 0.85rem; opacity: 0.9;">
                            <strong>Example:</strong> If GA optimizes a room to RM 200 and OTA multiplier is 1.20, OTA customers pay RM 240.
                        </p>
                        <p style="font-size: 0.85rem; opacity: 0.9; margin-top: 0.75rem;">
                            The optimizer does <strong>not</strong> search separate prices per segment. EzBoost first finds one seasonal base price, then applies these segment multipliers consistently.
                        </p>
                    </div>

                    <!-- Summary Card -->
                    <div class="summary-card">
                        <div class="summary-header">
                            <i class="bi bi-calculator"></i> Summary
                        </div>
                        <div class="summary-body">
                            <div class="summary-item">
                                <span class="summary-label">Total Segments</span>
                                <span class="summary-value">${segments.size()}</span>
                            </div>
                            <div class="summary-item">
                                <span class="summary-label">FIT Segments</span>
                                <span class="summary-value">
                                    <c:set var="fitCount" value="0"/>
                                    <c:forEach var="seg" items="${segments}">
                                        <c:if test="${seg.category == 'FIT'}">
                                            <c:set var="fitCount" value="${fitCount + 1}"/>
                                        </c:if>
                                    </c:forEach>
                                    ${fitCount}
                                </span>
                            </div>
                            <div class="summary-item">
                                <span class="summary-label">GIT Segments</span>
                                <span class="summary-value">
                                    <c:set var="gitCount" value="0"/>
                                    <c:forEach var="seg" items="${segments}">
                                        <c:if test="${seg.category == 'GIT'}">
                                            <c:set var="gitCount" value="${gitCount + 1}"/>
                                        </c:if>
                                    </c:forEach>
                                    ${gitCount}
                                </span>
                            </div>
                            <div class="summary-item">
                                <span class="summary-label">Avg Multiplier</span>
                                <span class="summary-value">
                                    <c:set var="avgMult" value="0"/>
                                    <c:forEach var="seg" items="${segments}">
                                        <c:set var="avgMult" value="${avgMult + seg.rateMultiplier}"/>
                                    </c:forEach>
                                    <fmt:formatNumber value="${avgMult / segments.size()}" maxFractionDigits="2"/>x
                                </span>
                            </div>
                        </div>
                    </div>

                    <!-- Add Segment Card -->
                    <div class="add-card">
                        <div class="add-header">
                            <i class="bi bi-plus-circle"></i> Add New Segment
                        </div>
                        <div class="add-body">
                            <form action="SegmentSettings" method="post">
                                <input type="hidden" name="action" value="addSegment">
                                
                                <div class="form-group">
                                    <label>Segment Name</label>
                                    <input type="text" name="newName" class="form-control" placeholder="e.g. Airline Crew" required>
                                </div>
                                
                                <div class="form-row">
                                    <div class="form-group">
                                        <label>Code</label>
                                        <input type="text" name="newCode" class="form-control" placeholder="e.g. CREW" maxlength="10" required>
                                    </div>
                                    <div class="form-group">
                                        <label>Category</label>
                                        <select name="newCategory" class="form-control" required>
                                            <option value="FIT">FIT (Individual)</option>
                                            <option value="GIT">GIT (Group)</option>
                                        </select>
                                    </div>
                                </div>
                                
                                <div class="form-group">
                                    <label>Rate Multiplier</label>
                                    <input type="number" name="newMultiplier" class="form-control" value="1.0" min="0.5" max="2.0" step="0.05" required>
                                </div>
                                
                                <div class="form-group">
                                    <label>Description</label>
                                    <input type="text" name="newDescription" class="form-control" placeholder="Brief description...">
                                </div>
                                
                                <button type="submit" class="btn-success-modern">
                                    <i class="bi bi-plus-lg"></i> Add Segment
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    <!-- Delete Confirmation Modal -->
    <div class="modal-overlay" id="deleteModal">
        <div class="modal-content">
            <h5><i class="bi bi-exclamation-triangle" style="color: #ef4444;"></i> Delete Segment?</h5>
            <p>Are you sure you want to delete <strong id="deleteSegmentName"></strong>?</p>
            <p style="font-size: 0.9rem; color: var(--text-secondary);">This action cannot be undone.</p>
            <div class="modal-buttons">
                <button type="button" class="btn-cancel" onclick="hideDeleteModal()">Cancel</button>
                <form action="SegmentSettings" method="post" id="deleteForm" style="flex: 1;">
                    <input type="hidden" name="action" value="deleteSegment">
                    <input type="hidden" name="segmentCode" id="deleteSegmentCode">
                    <button type="submit" class="btn-danger" style="width: 100%;">Delete</button>
                </form>
            </div>
        </div>
    </div>

    <!-- Reset Confirmation Modal -->
    <div class="modal-overlay" id="resetModal">
        <div class="modal-content">
            <h5><i class="bi bi-arrow-counterclockwise" style="color: #f59e0b;"></i> Reset to Defaults?</h5>
            <p>This will reset all segments to the default configuration:</p>
            <ul style="font-size: 0.9rem; margin: 1rem 0; padding-left: 1.5rem;">
                <li>Travel Agent: 0.90x (discount)</li>
                <li>OTA: 1.20x (premium)</li>
                <li>Website Direct: 1.00x (standard)</li>
                <li>Walk-in: 1.15x (premium)</li>
                <li>Long Stay: 0.75x (discount)</li>
                <li>Corporate Group: 1.05x</li>
                <li>Government: 0.85x (discount)</li>
                <li>Tour Group: 0.80x (discount)</li>
            </ul>
            <div class="modal-buttons">
                <button type="button" class="btn-cancel" onclick="hideResetModal()">Cancel</button>
                <form action="SegmentSettings" method="post" style="flex: 1;">
                    <input type="hidden" name="action" value="resetDefaults">
                    <button type="submit" class="btn-danger" style="width: 100%;">Reset All</button>
                </form>
            </div>
        </div>
    </div>

    <script>
        // Update multiplier visual bar
        function updateMultiplierBar(input) {
            const bar = input.parentElement.querySelector('.multiplier-fill');
            const value = parseFloat(input.value) || 1.0;
            const percentage = (value / 2.0) * 100;
            bar.style.width = percentage + '%';
            
            // Update color class
            bar.classList.remove('premium', 'standard', 'discount');
            if (value > 1.0) {
                bar.classList.add('premium');
            } else if (value < 1.0) {
                bar.classList.add('discount');
            } else {
                bar.classList.add('standard');
            }
        }

        // Delete confirmation
        function confirmDelete(code, name) {
            document.getElementById('deleteSegmentCode').value = code;
            document.getElementById('deleteSegmentName').textContent = name;
            document.getElementById('deleteModal').classList.add('active');
        }

        function hideDeleteModal() {
            document.getElementById('deleteModal').classList.remove('active');
        }

        // Reset confirmation
        function showResetModal() {
            document.getElementById('resetModal').classList.add('active');
        }

        function hideResetModal() {
            document.getElementById('resetModal').classList.remove('active');
        }

        // Close modals on outside click
        document.querySelectorAll('.modal-overlay').forEach(modal => {
            modal.addEventListener('click', function(e) {
                if (e.target === this) {
                    this.classList.remove('active');
                }
            });
        });
    </script>
</body>
</html>
