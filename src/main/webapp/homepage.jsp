<%@ page import="com.ezboost.model.User" %>
<%@ page import="com.ezboost.dao.OptimizationRequestDAO" %>
<%@ page import="com.ezboost.dao.RoomDataDAO" %>
<%@ page import="com.ezboost.dao.SeasonalityDAO" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    User loggedInUser = (User) session.getAttribute("user");
    int homepageOptCount = 0;
    boolean homepageReady = false;
    String homepageNextLink = "DataImport";
    String homepageNextLabel = "Finish Setup";
    if (loggedInUser != null) {
        homepageOptCount = OptimizationRequestDAO.getOptimizationCount(loggedInUser.getUserId());
        homepageReady = RoomDataDAO.hasRoomData(loggedInUser.getUserId())
                && !SeasonalityDAO.getMonthlyDataByUser(loggedInUser.getUserId()).isEmpty()
                && SeasonalityDAO.getThresholdsByUser(loggedInUser.getUserId()) != null;
        homepageNextLink = homepageReady ? "BoostMe.jsp" : "DataImport";
        homepageNextLabel = homepageReady ? "Start Optimizing" : "Import Your Data";
    }
    request.setAttribute("loggedInUser", loggedInUser);
    request.setAttribute("homepageOptCount", homepageOptCount);
    request.setAttribute("homepageReady", homepageReady);
    request.setAttribute("homepageNextLink", homepageNextLink);
    request.setAttribute("homepageNextLabel", homepageNextLabel);
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Home - EzBoost</title>
    <link rel="stylesheet" href="css/homepage.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
    <!-- Navigation -->
    <%@ include file="nav.jsp" %>

    <!-- Welcome Banner -->
    <c:if test="${not empty loggedInUser}">
        <div class="welcome-banner">
            <span class="welcome-text">
                Welcome back, <strong>${loggedInUser.username}</strong>
                <c:if test="${homepageOptCount > 0}">
                    &mdash; ${homepageOptCount} optimization${homepageOptCount > 1 ? 's' : ''} run
                </c:if>
            </span>
        </div>
    </c:if>

    <!-- Hero Section -->
    <section class="hero-section">
        <div class="hero-container">
            <div class="hero-eyebrow">Revenue Management System</div>
            <h1 class="hero-title">Meet your hotel's<br>revenue optimizer.</h1>
            <p class="hero-description">
                EzBoost uses genetic algorithms to explore thousands of pricing combinations,
                finding the optimal room rates that maximize revenue across all four seasons.
            </p>

            <!-- AI Prompt Preview -->
            <div class="ai-prompt-preview">
                <div class="prompt-line">
                    <span class="prompt-prefix">></span>
                    <span class="prompt-text">Optimize room pricing across 4 seasons...</span>
                    <span class="prompt-cursor"></span>
                </div>
            </div>

            <!-- Capability Pills -->
            <div class="capability-pills">
                <span class="pill">Seasonal Demand</span>
                <span class="pill">Market Segments</span>
                <span class="pill">Event Calendar</span>
                <span class="pill">Demand Curves</span>
                <span class="pill">Genetic Algorithm</span>
            </div>

            <div class="hero-cta">
                <a href="${homepageNextLink}" class="btn-primary">${homepageNextLabel}</a>
            </div>
        </div>
    </section>

    <!-- Features Section -->
    <section class="features-section">
        <div class="features-container">
            <div class="section-header">
                <h2>What EzBoost Does</h2>
                <p>AI-driven tools to optimize your hotel's revenue strategy</p>
            </div>

            <div class="features-grid">
                <div class="feature-card">
                    <div class="feature-label">OPTIMIZATION</div>
                    <h3>Genetic Algorithm Pricing</h3>
                    <p>Our engine uses evolutionary algorithms to explore thousands of pricing combinations, finding the optimal room rates that maximize revenue across all four seasons.</p>
                </div>

                <div class="feature-card">
                    <div class="feature-label">ANALYSIS</div>
                    <h3>Seasonal Demand Analysis</h3>
                    <p>Analyze historical occupancy data across Low, Normal, Peak, and Super Peak seasons. Price each season independently for maximum yield.</p>
                </div>

                <div class="feature-card">
                    <div class="feature-label">SEGMENTATION</div>
                    <h3>Market Segment Pricing</h3>
                    <p>Apply differentiated rates for corporate, leisure, OTA, walk-in, and group bookings. Each segment gets prices adjusted to its booking behavior.</p>
                </div>
            </div>
        </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section">
        <div class="cta-container">
            <h2>${homepageReady ? "Ready to optimize your hotel's revenue?" : "Start by loading your hotel's setup data"}</h2>
            <p>${homepageReady ? "Run EzBoost on your imported data and review the full revenue strategy." : "Import monthly demand and room data first so EzBoost can produce a reliable recommendation."}</p>
            <a href="${homepageNextLink}" class="btn-primary">${homepageReady ? "BoostMe Now" : "Open Data Import"}</a>
        </div>
    </section>

    <!-- Footer -->
    <footer class="simple-footer">
        <p>&copy; 2026 EzBoost. All Rights Reserved.</p>
    </footer>
</body>
</html>
