<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="effectivePreviewBaseRate" value="${previewBaseRate > 0 ? previewBaseRate : 200}" />
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Multiplier Settings - EzBoost</title>
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/settings.css">
</head>
<body class="multiplier-settings">
    <%@ include file="nav.jsp" %>

    <div class="main-container">
        <!-- Page Header -->
        <div class="page-header">
            <div class="page-badge">
                <i class="bi bi-sliders"></i>
                <span>Pricing Configuration</span>
            </div>
            <h1 class="page-title"><i class="bi bi-gear-wide-connected"></i> Season Multiplier Settings</h1>
            <p class="page-subtitle">Adjust global season multipliers for your whole hotel. Room-specific and segment-specific optimizer overrides are intentionally out of scope.</p>
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

        <form action="MultiplierSettings" method="post" id="multiplierForm">
            <input type="hidden" name="action" value="updateMultipliers">

            <section class="workspace-hero pricing-hero">
                <div class="workspace-hero-copy">
                    <span class="workspace-kicker">Hotel-Wide Pricing Board</span>
                    <h2 class="workspace-title">Tune the seasonal rate posture before the optimizer starts searching.</h2>
                    <p class="workspace-text">These multipliers anchor the GA's starting point for every room in the hotel. EzBoost keeps the scope intentionally simple here: one multiplier per season, applied across the full inventory.</p>
                </div>
                <div class="workspace-hero-metrics">
                    <div class="workspace-metric">
                        <span class="workspace-metric-label">Weighted ADR</span>
                        <strong class="workspace-metric-value">RM <fmt:formatNumber value="${effectivePreviewBaseRate}" maxFractionDigits="0"/></strong>
                        <span class="workspace-metric-note">live hotel baseline</span>
                    </div>
                    <div class="workspace-metric">
                        <span class="workspace-metric-label">Average Multiplier</span>
                        <strong class="workspace-metric-value" id="heroAverageMultiplier"><fmt:formatNumber value="${avgMultiplier}" maxFractionDigits="2"/>x</strong>
                        <span class="workspace-metric-note">across all four seasons</span>
                    </div>
                    <div class="workspace-metric">
                        <span class="workspace-metric-label">Optimizer Scope</span>
                        <strong class="workspace-metric-value">Global Only</strong>
                        <span class="workspace-metric-note">room and segment overrides stay out</span>
                    </div>
                </div>
            </section>

            <div class="content-grid pricing-board-layout">
                <!-- Left Column: Season Cards -->
                <div class="left-column">
                    <section class="workspace-panel season-board-shell">
                        <div class="workspace-panel-header">
                            <div>
                                <span class="panel-kicker">Seasonal Progression</span>
                                <h3 class="workspace-panel-title">Move from low-demand protection to high-demand yield.</h3>
                            </div>
                            <p class="workspace-panel-text">Each season below updates the hotel-wide pricing posture and previews the expected ADR effect using your imported room mix.</p>
                        </div>
                        <div class="season-preview-strip">
                            <div class="season-preview-card low">
                                <span class="season-preview-label">LOW</span>
                                <strong class="season-preview-rate" id="heroPreview_LOW">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['LOW'].customMultiplier}" maxFractionDigits="0"/></strong>
                                <span class="season-preview-multiplier" id="heroSummary_LOW">${seasonMultipliers['LOW'].customMultiplier}x</span>
                            </div>
                            <div class="season-preview-card normal">
                                <span class="season-preview-label">NORMAL</span>
                                <strong class="season-preview-rate" id="heroPreview_NORMAL">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['NORMAL'].customMultiplier}" maxFractionDigits="0"/></strong>
                                <span class="season-preview-multiplier" id="heroSummary_NORMAL">${seasonMultipliers['NORMAL'].customMultiplier}x</span>
                            </div>
                            <div class="season-preview-card peak">
                                <span class="season-preview-label">PEAK</span>
                                <strong class="season-preview-rate" id="heroPreview_PEAK">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['PEAK'].customMultiplier}" maxFractionDigits="0"/></strong>
                                <span class="season-preview-multiplier" id="heroSummary_PEAK">${seasonMultipliers['PEAK'].customMultiplier}x</span>
                            </div>
                            <div class="season-preview-card super">
                                <span class="season-preview-label">SUPER PEAK</span>
                                <strong class="season-preview-rate" id="heroPreview_SUPER_PEAK">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['SUPER_PEAK'].customMultiplier}" maxFractionDigits="0"/></strong>
                                <span class="season-preview-multiplier" id="heroSummary_SUPER_PEAK">${seasonMultipliers['SUPER_PEAK'].customMultiplier}x</span>
                            </div>
                        </div>
                    </section>

                    <div class="season-grid">
                        <!-- LOW Season -->
                        <div class="season-card season-LOW">
                            <div class="season-header">
                                <i class="bi bi-cloud-drizzle"></i>
                                <h4>LOW Season</h4>
                                <small>Off-peak periods with lower demand</small>
                            </div>
                            <div class="season-body">
                                <label>Price Multiplier</label>
                                <input type="number" 
                                       name="multiplier_LOW" 
                                       class="multiplier-input" 
                                       value="${seasonMultipliers['LOW'].customMultiplier}" 
                                       min="0.5" max="2.0" step="0.05"
                                       onchange="updatePreview()">
                                <input type="range" 
                                       class="range-slider" 
                                       min="0.5" max="2.0" step="0.05"
                                       value="${seasonMultipliers['LOW'].customMultiplier}"
                                       oninput="syncInput(this, 'multiplier_LOW')">
                                <div class="range-labels">
                                    <span>0.5x</span>
                                    <span>2.0x</span>
                                </div>
                                <div class="preview-box">
                                    <small>Weighted hotel ADR: RM <fmt:formatNumber value="${effectivePreviewBaseRate}" maxFractionDigits="0"/> → </small>
                                    <strong id="preview_LOW">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['LOW'].customMultiplier}" maxFractionDigits="0"/></strong>
                                </div>
                            </div>
                        </div>

                        <!-- NORMAL Season -->
                        <div class="season-card season-NORMAL">
                            <div class="season-header">
                                <i class="bi bi-brightness-high"></i>
                                <h4>NORMAL Season</h4>
                                <small>Standard operating periods</small>
                            </div>
                            <div class="season-body">
                                <label>Price Multiplier</label>
                                <input type="number" 
                                       name="multiplier_NORMAL" 
                                       class="multiplier-input" 
                                       value="${seasonMultipliers['NORMAL'].customMultiplier}" 
                                       min="0.5" max="2.0" step="0.05"
                                       onchange="updatePreview()">
                                <input type="range" 
                                       class="range-slider" 
                                       min="0.5" max="2.0" step="0.05"
                                       value="${seasonMultipliers['NORMAL'].customMultiplier}"
                                       oninput="syncInput(this, 'multiplier_NORMAL')">
                                <div class="range-labels">
                                    <span>0.5x</span>
                                    <span>2.0x</span>
                                </div>
                                <div class="preview-box">
                                    <small>Weighted hotel ADR: RM <fmt:formatNumber value="${effectivePreviewBaseRate}" maxFractionDigits="0"/> → </small>
                                    <strong id="preview_NORMAL">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['NORMAL'].customMultiplier}" maxFractionDigits="0"/></strong>
                                </div>
                            </div>
                        </div>

                        <!-- PEAK Season -->
                        <div class="season-card season-PEAK">
                            <div class="season-header">
                                <i class="bi bi-graph-up-arrow"></i>
                                <h4>PEAK Season</h4>
                                <small>High demand periods</small>
                            </div>
                            <div class="season-body">
                                <label>Price Multiplier</label>
                                <input type="number" 
                                       name="multiplier_PEAK" 
                                       class="multiplier-input" 
                                       value="${seasonMultipliers['PEAK'].customMultiplier}" 
                                       min="0.5" max="2.0" step="0.05"
                                       onchange="updatePreview()">
                                <input type="range" 
                                       class="range-slider" 
                                       min="0.5" max="2.0" step="0.05"
                                       value="${seasonMultipliers['PEAK'].customMultiplier}"
                                       oninput="syncInput(this, 'multiplier_PEAK')">
                                <div class="range-labels">
                                    <span>0.5x</span>
                                    <span>2.0x</span>
                                </div>
                                <div class="preview-box">
                                    <small>Weighted hotel ADR: RM <fmt:formatNumber value="${effectivePreviewBaseRate}" maxFractionDigits="0"/> → </small>
                                    <strong id="preview_PEAK">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['PEAK'].customMultiplier}" maxFractionDigits="0"/></strong>
                                </div>
                            </div>
                        </div>

                        <!-- SUPER PEAK Season -->
                        <div class="season-card season-SUPER_PEAK">
                            <div class="season-header">
                                <i class="bi bi-fire"></i>
                                <h4>SUPER PEAK</h4>
                                <small>Maximum demand (holidays, events)</small>
                            </div>
                            <div class="season-body">
                                <label>Price Multiplier</label>
                                <input type="number" 
                                       name="multiplier_SUPER_PEAK" 
                                       class="multiplier-input" 
                                       value="${seasonMultipliers['SUPER_PEAK'].customMultiplier}" 
                                       min="0.5" max="2.0" step="0.05"
                                       onchange="updatePreview()">
                                <input type="range" 
                                       class="range-slider" 
                                       min="0.5" max="2.0" step="0.05"
                                       value="${seasonMultipliers['SUPER_PEAK'].customMultiplier}"
                                       oninput="syncInput(this, 'multiplier_SUPER_PEAK')">
                                <div class="range-labels">
                                    <span>0.5x</span>
                                    <span>2.0x</span>
                                </div>
                                <div class="preview-box">
                                    <small>Weighted hotel ADR: RM <fmt:formatNumber value="${effectivePreviewBaseRate}" maxFractionDigits="0"/> → </small>
                                    <strong id="preview_SUPER_PEAK">RM <fmt:formatNumber value="${effectivePreviewBaseRate * seasonMultipliers['SUPER_PEAK'].customMultiplier}" maxFractionDigits="0"/></strong>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Action Buttons -->
                    <div class="action-buttons board-actions">
                        <button type="submit" class="btn-primary-modern">
                            <i class="bi bi-save"></i> ${onboardingActive ? 'Save and Continue to Events' : 'Save Multipliers'}
                        </button>
                        <button type="button" class="btn-secondary-modern" onclick="showResetModal()">
                            <i class="bi bi-arrow-counterclockwise"></i> Reset
                        </button>
                    </div>
                </div>

                <!-- Right Column: Sidebar -->
                <div class="sidebar">
                    <!-- Info Card -->
                    <div class="info-card">
                        <h5><i class="bi bi-info-circle"></i> How Multipliers Work</h5>
                        <hr>
                        <p><strong>Formula:</strong></p>
                        <div class="formula">Seasonal Price = Base Rate × Multiplier</div>
                        <p style="margin-top: 1rem;"><strong>Scope:</strong> One multiplier per season for the whole hotel.</p>
                        <p><strong>Operational meaning:</strong></p>
                        <ul>
                            <li>Higher multipliers lift the seasonal starting point before room bounds are applied.</li>
                            <li>Lower multipliers protect occupancy in softer demand windows.</li>
                        </ul>
                    </div>

                    <!-- Summary Card -->
                    <div class="summary-card">
                        <div class="summary-header">
                            <i class="bi bi-calculator"></i> Current Summary
                        </div>
                        <div class="summary-body">
                            <table class="summary-table">
                                <tr>
                                    <td><span class="badge badge-low">LOW</span></td>
                                    <td id="summary_LOW">${seasonMultipliers['LOW'].customMultiplier}x</td>
                                </tr>
                                <tr>
                                    <td><span class="badge badge-normal">NORMAL</span></td>
                                    <td id="summary_NORMAL">${seasonMultipliers['NORMAL'].customMultiplier}x</td>
                                </tr>
                                <tr>
                                    <td><span class="badge badge-peak">PEAK</span></td>
                                    <td id="summary_PEAK">${seasonMultipliers['PEAK'].customMultiplier}x</td>
                                </tr>
                                <tr>
                                    <td><span class="badge badge-super">SUPER</span></td>
                                    <td id="summary_SUPER_PEAK">${seasonMultipliers['SUPER_PEAK'].customMultiplier}x</td>
                                </tr>
                                <tr class="summary-total">
                                    <td><strong>Average</strong></td>
                                    <td id="summary_AVG"><fmt:formatNumber value="${avgMultiplier}" maxFractionDigits="2"/>x</td>
                                </tr>
                            </table>
                        </div>
                    </div>

                    <!-- Recommendations Card -->
                    <div class="rec-card">
                        <div class="rec-header">
                            <i class="bi bi-lightbulb"></i> Real Data Preview
                        </div>
                        <div class="rec-body">
                            <p>Weighted average base ADR: <strong>RM <fmt:formatNumber value="${effectivePreviewBaseRate}" maxFractionDigits="2"/></strong></p>
                            <c:if test="${not empty previewRoomA}">
                                <p style="margin-bottom: 0.5rem;"><strong>${previewRoomA.name}</strong>: base RM <fmt:formatNumber value="${previewRoomA.baseAdr}" maxFractionDigits="2"/>, min RM <fmt:formatNumber value="${previewRoomA.minAdr}" maxFractionDigits="2"/>, max RM <fmt:formatNumber value="${previewRoomA.maxAdr}" maxFractionDigits="2"/></p>
                            </c:if>
                            <c:if test="${not empty previewRoomB}">
                                <p><strong>${previewRoomB.name}</strong>: base RM <fmt:formatNumber value="${previewRoomB.baseAdr}" maxFractionDigits="2"/>, min RM <fmt:formatNumber value="${previewRoomB.minAdr}" maxFractionDigits="2"/>, max RM <fmt:formatNumber value="${previewRoomB.maxAdr}" maxFractionDigits="2"/></p>
                            </c:if>
                            <c:if test="${empty previewRoomA}">
                                <p>Import room data to preview multiplier impact on your actual hotel inventory.</p>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>

    <!-- Reset Modal -->
    <div class="modal-overlay" id="resetModal">
        <div class="modal-content">
            <h5><i class="bi bi-exclamation-triangle"></i> Reset to Defaults?</h5>
            <p>This will reset all multipliers to their default values:</p>
            <ul>
                <li><strong>LOW:</strong> 0.85x</li>
                <li><strong>NORMAL:</strong> 1.0x</li>
                <li><strong>PEAK:</strong> 1.15x</li>
                <li><strong>SUPER_PEAK:</strong> 1.35x</li>
            </ul>
            <div class="modal-buttons">
                <button type="button" class="btn-cancel" onclick="hideResetModal()">Cancel</button>
                <form action="MultiplierSettings" method="post" style="flex: 1;">
                    <input type="hidden" name="action" value="resetDefaults">
                    <button type="submit" class="btn-danger" style="width: 100%;">Reset to Defaults</button>
                </form>
            </div>
        </div>
    </div>

    <script>
        // Sync slider with input
        function syncInput(slider, inputName) {
            const input = document.querySelector('input[name="' + inputName + '"]');
            input.value = slider.value;
            updatePreview();
        }

        // Update preview calculations
        function updatePreview() {
            const seasons = ['LOW', 'NORMAL', 'PEAK', 'SUPER_PEAK'];
            const basePrice = ${effectivePreviewBaseRate};
            let total = 0;

            seasons.forEach(season => {
                const input = document.querySelector('input[name="multiplier_' + season + '"]');
                const multiplier = parseFloat(input.value) || 1.0;
                const price = Math.round(basePrice * multiplier);
                
                document.getElementById('preview_' + season).textContent = 'RM ' + price;
                document.getElementById('summary_' + season).textContent = multiplier.toFixed(2) + 'x';
                document.getElementById('heroPreview_' + season).textContent = 'RM ' + price;
                document.getElementById('heroSummary_' + season).textContent = multiplier.toFixed(2) + 'x';
                
                // Sync slider
                const slider = input.nextElementSibling;
                if (slider && slider.type === 'range') {
                    slider.value = multiplier;
                }
                
                total += multiplier;
            });

            // Update average
            const avg = total / 4;
            document.getElementById('summary_AVG').textContent = avg.toFixed(2) + 'x';
            document.getElementById('heroAverageMultiplier').textContent = avg.toFixed(2) + 'x';
        }

        // Modal functions
        function showResetModal() {
            document.getElementById('resetModal').classList.add('active');
        }

        function hideResetModal() {
            document.getElementById('resetModal').classList.remove('active');
        }

        // Close modal on outside click
        document.getElementById('resetModal').addEventListener('click', function(e) {
            if (e.target === this) {
                hideResetModal();
            }
        });

        // Initialize on page load
        document.addEventListener('DOMContentLoaded', function() {
            updatePreview();
        });
    </script>
</body>
</html>
