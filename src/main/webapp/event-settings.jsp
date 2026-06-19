<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Event Settings - EzBoost</title>
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/settings.css">
</head>
<body class="event-settings">
    <%@ include file="nav.jsp" %>

    <div class="main-container">
        <!-- Page Header -->
        <div class="page-header">
            <div class="page-badge">
                <i class="bi bi-calendar-event"></i>
                <span>Future Events & Holidays</span>
            </div>
            <h1 class="page-title"><i class="bi bi-calendar2-check"></i> Event Settings</h1>
            <p class="page-subtitle">Manage Malaysian holidays and events that impact seasonal pricing</p>
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

        <section class="workspace-hero event-hero">
            <div class="workspace-hero-copy">
                <span class="workspace-kicker">Forecast Overrides</span>
                <h2 class="workspace-title">Build the calendar layer that can push future months above their historical season.</h2>
                <p class="workspace-text">This page keeps fetched holidays, preset school breaks, and manual events coherent before the forecast maps monthly prices. Events only move a month upward in the season ladder.</p>
            </div>
            <div class="workspace-hero-metrics">
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Replace Rule</span>
                    <strong class="workspace-metric-value">By Source + Year</strong>
                    <span class="workspace-metric-note">fetched rows replace their own source only</span>
                </div>
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Manual Events</span>
                    <strong class="workspace-metric-value">Preserved</strong>
                    <span class="workspace-metric-note">until you edit or remove them</span>
                </div>
                <div class="workspace-metric">
                    <span class="workspace-metric-label">Season Rule</span>
                    <strong class="workspace-metric-value">Upward Only</strong>
                    <span class="workspace-metric-note">LOW &lt; NORMAL &lt; PEAK &lt; SUPER PEAK</span>
                </div>
            </div>
        </section>

        <section class="event-control-deck">
            <div class="api-section">
                <div class="api-header">
                    <i class="bi bi-key"></i> Calendarific API Key
                </div>
                <div class="api-body">
                    <form action="EventSettings" method="post">
                        <input type="hidden" name="action" value="saveApiKey">
                        <div class="api-row">
                            <div class="form-group" style="margin-bottom: 0;">
                                <label>API Key (get free key at calendarific.com)</label>
                                <input type="password" name="apiKey" class="form-control" value="" autocomplete="new-password"
                                       placeholder="${apiKeyConfigured ? 'A key is configured. Enter a new key to replace it.' : 'Enter your Calendarific API key...'}">
                            </div>
                            <button type="submit" class="btn-primary-modern" style="height: fit-content;">
                                <i class="bi bi-save"></i> Save Key
                            </button>
                            <c:if test="${apiKeyConfigured}">
                                <button type="submit" class="btn-secondary-modern" style="height: fit-content;"
                                        onclick="this.form.querySelector('input[name=action]').value='removeApiKey';">
                                    <i class="bi bi-trash"></i> Remove Key
                                </button>
                            </c:if>
                        </div>
                    </form>
                </div>
            </div>

            <div class="fetch-section">
                <div class="fetch-header">
                    <i class="bi bi-cloud-download"></i> Fetch Events
                </div>
                <div class="fetch-body">
                    <div class="threshold-explanation" style="margin-bottom: 1rem;">
                        Fetched API and preset events replace earlier rows for the same source and year. Manual events stay untouched.
                    </div>
                    <div class="fetch-row">
                        <div class="form-group" style="margin-bottom: 0; min-width: 120px;">
                            <label>Year</label>
                            <select id="fetchYear" class="form-control">
                                <c:forEach var="y" begin="2024" end="2030">
                                    <option value="${y}">${y}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <form action="EventSettings" method="post" class="fetch-action-form" data-loading-message="Fetching API holidays for the selected year..." style="display: inline-flex; align-items: flex-end;">
                            <input type="hidden" name="action" value="fetchHolidays">
                            <input type="hidden" name="year" id="fetchHolidaysYear">
                            <button type="submit" class="btn-primary-modern" onclick="document.getElementById('fetchHolidaysYear').value=document.getElementById('fetchYear').value;">
                                <i class="bi bi-cloud-download"></i> Fetch Holidays
                            </button>
                        </form>
                        <form action="EventSettings" method="post" class="fetch-action-form" data-loading-message="Loading preset school holidays for the selected year..." style="display: inline-flex; align-items: flex-end;">
                            <input type="hidden" name="action" value="loadSchoolHolidays">
                            <input type="hidden" name="year" id="loadSchoolYear">
                            <button type="submit" class="btn-accent-modern" onclick="document.getElementById('loadSchoolYear').value=document.getElementById('fetchYear').value;">
                                <i class="bi bi-backpack"></i> Load School Holidays
                            </button>
                        </form>
                    </div>
                    <div id="fetchStatus" class="threshold-explanation" style="margin-top: 1rem; display: none;"></div>
                </div>
            </div>
        </section>

        <c:if test="${onboardingActive}">
            <section class="onboarding-next-panel">
                <div>
                    <strong>Events are optional for first setup.</strong>
                    <span>Add or fetch events now, or continue with no events for the first optimization.</span>
                </div>
                <form action="EventSettings" method="post">
                    <input type="hidden" name="action" value="continueOnboardingEvents">
                    <button type="submit" class="btn-primary-modern">
                        <i class="bi bi-arrow-right-circle"></i> Continue to Segments
                    </button>
                </form>
            </section>
        </c:if>

        <section class="event-snapshot">
            <div class="event-snapshot-copy">
                <span class="event-snapshot-kicker">Event Snapshot</span>
                <h2 class="event-snapshot-title">Keep your calendar coherent before you tune the overrides.</h2>
                <p class="event-snapshot-text">Fetched sources, manual additions, and active rows are separated here so you can spot duplicate-heavy calendars before the forecast relies on them.</p>
            </div>
            <div class="event-snapshot-grid">
                <div class="event-snapshot-card active">
                    <div class="event-snapshot-icon"><i class="bi bi-check2-circle"></i></div>
                    <div class="event-snapshot-value">${activeEventCount}</div>
                    <div class="event-snapshot-label">Active Events</div>
                    <div class="event-snapshot-note">currently affecting future months</div>
                </div>
                <div class="event-snapshot-card api">
                    <div class="event-snapshot-icon"><i class="bi bi-cloud-download"></i></div>
                    <div class="event-snapshot-value">${apiEventCount}</div>
                    <div class="event-snapshot-label">API Imports</div>
                    <div class="event-snapshot-note">Calendarific rows for configured years</div>
                </div>
                <div class="event-snapshot-card preset">
                    <div class="event-snapshot-icon"><i class="bi bi-backpack"></i></div>
                    <div class="event-snapshot-value">${presetEventCount}</div>
                    <div class="event-snapshot-label">Preset Imports</div>
                    <div class="event-snapshot-note">school-break presets in the calendar</div>
                </div>
                <div class="event-snapshot-card manual">
                    <div class="event-snapshot-icon"><i class="bi bi-pencil-square"></i></div>
                    <div class="event-snapshot-value">${manualEventCount}</div>
                    <div class="event-snapshot-label">Manual Events</div>
                    <div class="event-snapshot-note">custom overrides you manage directly</div>
                </div>
            </div>
        </section>

        <div class="content-grid">
            <!-- Main Events Table -->
            <div class="main-card event-workspace-card">
                <div class="card-header">
                    <h5><i class="bi bi-table"></i> Configured Events</h5>
                    <span style="font-size: 0.85rem; color: var(--text-secondary);">
                        ${events.size()} events
                    </span>
                </div>
                <div class="workspace-toolbar">
                    <div class="workspace-toolbar-copy">
                        <strong>Working rule:</strong> fetched sources can refresh themselves, but manual events stay durable so your forecast logic is still under your control.
                    </div>
                </div>
                <div id="eventInlineStatus" class="threshold-explanation" style="margin: 1rem; display: none;"></div>

                <div class="card-body" style="padding: 0; overflow-x: auto;">
                    <c:choose>
                        <c:when test="${not empty events}">
                            <table class="segment-table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Event Name</th>
                                        <th>Type</th>
                                        <th>Season Override</th>
                                        <th>Source</th>
                                        <th>Active</th>
                                        <th></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="event" items="${events}">
                                        <tr>
                                            <td style="white-space: nowrap;">
                                                <fmt:formatDate value="${event.eventDate}" pattern="dd MMM yyyy"/>
                                                <c:if test="${event.eventEndDate != null}">
                                                    <br><small style="color: var(--text-secondary);">to <fmt:formatDate value="${event.eventEndDate}" pattern="dd MMM yyyy"/></small>
                                                </c:if>
                                            </td>
                                            <td><strong>${event.eventName}</strong></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${event.eventType == 'PUBLIC_HOLIDAY'}">
                                                        <span class="type-badge holiday">Holiday</span>
                                                    </c:when>
                                                    <c:when test="${event.eventType == 'SCHOOL_BREAK'}">
                                                        <span class="type-badge school">School</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="type-badge custom">Custom</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <select class="season-select" data-event-id="${event.eventId}" data-active="${event.active}"
                                                        onchange="updateEvent(this)">
                                                    <option value="PEAK" ${event.seasonOverride == 'PEAK' ? 'selected' : ''}>PEAK</option>
                                                    <option value="SUPER_PEAK" ${event.seasonOverride == 'SUPER_PEAK' ? 'selected' : ''}>SUPER_PEAK</option>
                                                </select>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${event.source == 'CALENDARIFIC'}">
                                                        <span class="source-badge api">API import</span>
                                                    </c:when>
                                                    <c:when test="${event.source == 'PRESET'}">
                                                        <span class="source-badge preset">Preset</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="source-badge manual">Manual</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <label class="toggle-switch" data-event-id="${event.eventId}" data-season="${event.seasonOverride}">
                                                    <input type="checkbox" ${event.active ? 'checked' : ''}
                                                           onchange="toggleActive(this)">
                                                    <span class="toggle-slider"></span>
                                                </label>
                                            </td>
                                            <td>
                                                <form action="EventSettings" method="post" style="display: inline;">
                                                    <input type="hidden" name="action" value="deleteEvent">
                                                    <input type="hidden" name="eventId" value="${event.eventId}">
                                                    <button type="submit" class="btn-danger-small" onclick="return confirm('Delete this event?')">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:when>
                        <c:otherwise>
                            <div style="padding: 3rem; text-align: center; color: var(--text-secondary);">
                                <i class="bi bi-calendar-x" style="font-size: 3rem; opacity: 0.3;"></i>
                                <p style="margin-top: 1rem;">No events configured yet.</p>
                                <p style="font-size: 0.85rem;">Use the controls above to fetch Malaysian holidays or load school holiday presets.</p>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <c:if test="${not empty events}">
                    <div class="action-bar">
                        <form action="EventSettings" method="post" style="flex: 1;">
                            <input type="hidden" name="action" value="deleteAll">
                            <button type="submit" class="btn-secondary-modern" style="width: 100%;" onclick="return confirm('Delete ALL events? This cannot be undone.')">
                                <i class="bi bi-trash"></i> Delete All Events
                            </button>
                        </form>
                    </div>
                </c:if>
            </div>

            <!-- Sidebar -->
            <div class="sidebar">
                <!-- Info Card -->
                <div class="info-card">
                    <h5><i class="bi bi-info-circle"></i> How It Works</h5>
                    <hr>
                    <p style="font-size: 0.9rem; margin-bottom: 1rem;">
                        Events <strong>override</strong> the historical season classification for affected months:
                    </p>
                    <ul>
                        <li><strong style="color: #fca5a5;">SUPER_PEAK</strong> = Major holidays (Hari Raya, CNY)</li>
                        <li><strong style="color: #fbbf24;">PEAK</strong> = Regular holidays & school breaks</li>
                        <li>Events can only bump a season <strong>up</strong>, never down</li>
                        <li>Season hierarchy: LOW &lt; NORMAL &lt; PEAK &lt; SUPER_PEAK</li>
                    </ul>
                    <hr>
                    <p style="font-size: 0.85rem; opacity: 0.9;">
                        <strong>Example:</strong> If April is historically LOW but Hari Raya falls in April, the forecast shows PEAK rates for April.
                    </p>
                </div>

                <div class="info-card">
                    <h5><i class="bi bi-shuffle"></i> Replace Policy</h5>
                    <hr>
                    <ul>
                        <li><strong>API import:</strong> replaces earlier Calendarific rows for the same year.</li>
                        <li><strong>Preset import:</strong> replaces earlier preset school-break rows for the same year.</li>
                        <li><strong>Manual events:</strong> stay untouched until you edit or delete them.</li>
                    </ul>
                    <hr>
                    <p style="font-size: 0.85rem; opacity: 0.9;">
                        This keeps fetches predictable and avoids stacking duplicate rows across repeated imports.
                    </p>
                </div>

                <!-- Add Custom Event -->
                <div class="add-card">
                    <div class="add-header">
                        <i class="bi bi-plus-circle"></i> Add Custom Event
                    </div>
                    <div class="add-body">
                        <form action="EventSettings" method="post">
                            <input type="hidden" name="action" value="addEvent">

                            <div class="form-group">
                                <label>Event Name</label>
                                <input type="text" name="eventName" class="form-control" placeholder="e.g. Hotel Anniversary" required>
                            </div>

                            <div class="form-row">
                                <div class="form-group">
                                    <label>Start Date</label>
                                    <input type="date" name="eventDate" class="form-control" required>
                                </div>
                                <div class="form-group">
                                    <label>End Date (optional)</label>
                                    <input type="date" name="eventEndDate" class="form-control">
                                </div>
                            </div>

                            <div class="form-row">
                                <div class="form-group">
                                    <label>Event Type</label>
                                    <select name="eventType" class="form-control">
                                        <option value="CUSTOM">Custom</option>
                                        <option value="PUBLIC_HOLIDAY">Public Holiday</option>
                                        <option value="SCHOOL_BREAK">School Break</option>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label>Season Override</label>
                                    <select name="seasonOverride" class="form-control">
                                        <option value="PEAK">PEAK</option>
                                        <option value="SUPER_PEAK">SUPER_PEAK</option>
                                    </select>
                                </div>
                            </div>

                            <button type="submit" class="btn-success-modern" style="width: 100%;">
                                <i class="bi bi-plus-lg"></i> Add Event
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        // Set default year to current year + 1
        var currentYear = new Date().getFullYear();
        var yearSelect = document.getElementById('fetchYear');
        if (yearSelect) {
            for (var i = 0; i < yearSelect.options.length; i++) {
                if (parseInt(yearSelect.options[i].value) === currentYear + 1) {
                    yearSelect.selectedIndex = i;
                    break;
                }
            }
        }

        // AJAX toggle active status (no page refresh)
        function toggleActive(checkbox) {
            var label = checkbox.closest('.toggle-switch');
            var eventId = label.dataset.eventId;
            var seasonOverride = label.dataset.season;
            var newActive = checkbox.checked;

            var params = new URLSearchParams();
            params.append('action', 'updateEvent');
            params.append('eventId', eventId);
            params.append('seasonOverride', seasonOverride);
            params.append('active', newActive);

            fetch('EventSettings', {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest', 'X-CSRF-Token': window.ezBoostCsrfToken },
                body: params
            })
                .then(function(resp) {
                    return resp.json().then(function(data) {
                        if (!resp.ok || !data.success) {
                            throw new Error(data.message || 'Unable to update event.');
                        }
                        var seasonSelect = document.querySelector('.season-select[data-event-id="' + eventId + '"]');
                        if (seasonSelect) seasonSelect.dataset.active = String(newActive);
                        showEventStatus(data.message, false);
                    });
                })
                .catch(function(err) {
                    checkbox.checked = !newActive;
                    showEventStatus(err.message || 'Unable to update event.', true);
                });
        }

        // AJAX update season override (no page refresh)
        function updateEvent(selectEl) {
            var eventId = selectEl.dataset.eventId;
            var seasonOverride = selectEl.value;
            var active = selectEl.dataset.active;
            var previousValue = selectEl.dataset.previousValue || seasonOverride;
            var params = new URLSearchParams();
            params.append('action', 'updateEvent');
            params.append('eventId', eventId);
            params.append('seasonOverride', seasonOverride);
            params.append('active', active);

            fetch('EventSettings', {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest', 'X-CSRF-Token': window.ezBoostCsrfToken },
                body: params
            })
                .then(function(resp) {
                    return resp.json().then(function(data) {
                        if (!resp.ok || !data.success) {
                            throw new Error(data.message || 'Unable to update event.');
                        }
                        var toggle = document.querySelector('.toggle-switch[data-event-id="' + eventId + '"]');
                        if (toggle) toggle.dataset.season = seasonOverride;
                        selectEl.dataset.previousValue = seasonOverride;
                        showEventStatus(data.message, false);
                    });
                })
                .catch(function(err) {
                    selectEl.value = previousValue;
                    showEventStatus(err.message || 'Unable to update event.', true);
                });
        }

        function showEventStatus(message, isError) {
            var box = document.getElementById('eventInlineStatus');
            box.style.display = 'block';
            box.style.borderLeft = isError ? '4px solid #dc2626' : '4px solid #16a34a';
            box.textContent = message;
        }

        document.querySelectorAll('.fetch-action-form').forEach(function(form) {
            form.addEventListener('submit', function() {
                var box = document.getElementById('fetchStatus');
                var button = form.querySelector('button[type="submit"]');
                if (box) {
                    box.style.display = 'block';
                    box.style.borderLeft = '4px solid #1d4ed8';
                    box.textContent = form.dataset.loadingMessage || 'Loading events...';
                }
                if (button) {
                    button.disabled = true;
                }
            });
        });

        document.querySelectorAll('.season-select').forEach(function(select) {
            select.dataset.previousValue = select.value;
        });
    </script>
</body>
</html>
