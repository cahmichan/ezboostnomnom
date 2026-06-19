<!-- nav.jsp -->
<%@ page import="com.ezboost.dao.RoomDataDAO" %>
<%@ page import="com.ezboost.dao.SeasonalityDAO" %>
<%@ page import="com.ezboost.model.User" %>
<%@ page import="com.ezboost.util.OnboardingUtil" %>
<link rel="stylesheet" href="css/theme.css">
<link rel="stylesheet" href="css/styles.css">
<link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">

<%
    User navUser = (User) session.getAttribute("user");
    String boostLink = "DataImport";
    String navHomeLink = "homepage.jsp";
    boolean navOnboarding = OnboardingUtil.isOnboardingActive(navUser);
    String navCurrentStep = OnboardingUtil.getCurrentStep(navUser);
    String navCurrentUrl = OnboardingUtil.getStepUrl(navCurrentStep);
    if (navUser != null) {
        boolean navReady = RoomDataDAO.hasRoomData(navUser.getUserId())
                && !SeasonalityDAO.getMonthlyDataByUser(navUser.getUserId()).isEmpty()
                && SeasonalityDAO.getThresholdsByUser(navUser.getUserId()) != null;
        boostLink = navReady ? "BoostMe.jsp" : "DataImport";
    }
    if (navOnboarding) {
        navHomeLink = navCurrentUrl;
        boostLink = navCurrentUrl;
    }

    String importHref = navOnboarding && !OnboardingUtil.STEP_IMPORT.equals(navCurrentStep) ? navCurrentUrl : "DataImport";
    String multiplierHref = navOnboarding && !OnboardingUtil.STEP_MULTIPLIERS.equals(navCurrentStep) ? navCurrentUrl : "MultiplierSettings";
    String eventsHref = navOnboarding && !OnboardingUtil.STEP_EVENTS.equals(navCurrentStep) ? navCurrentUrl : "EventSettings";
    String segmentsHref = navOnboarding && !OnboardingUtil.STEP_SEGMENTS.equals(navCurrentStep) ? navCurrentUrl : "SegmentSettings";
    String profileHref = navOnboarding ? navCurrentUrl : "Profile";

    String lockedClass = " nav-link-locked";
    String importClass = "nav-link" + (navOnboarding && !OnboardingUtil.STEP_IMPORT.equals(navCurrentStep) ? lockedClass : "");
    String multiplierClass = "nav-link" + (navOnboarding && !OnboardingUtil.STEP_MULTIPLIERS.equals(navCurrentStep) ? lockedClass : "");
    String eventsClass = "nav-link" + (navOnboarding && !OnboardingUtil.STEP_EVENTS.equals(navCurrentStep) ? lockedClass : "");
    String segmentsClass = "nav-link" + (navOnboarding && !OnboardingUtil.STEP_SEGMENTS.equals(navCurrentStep) ? lockedClass : "");
    String boostClass = "nav-link" + (navOnboarding ? lockedClass : "");
    String profileClass = "nav-link" + (navOnboarding ? lockedClass : "");
%>

<nav class="navbar">
    <div class="nav-left">
        <a href="<%= navHomeLink %>" class="nav-logo">
            <span class="logo-text">EzBoost</span>
        </a>
    </div>
    <div class="nav-center">
        <div class="nav-links">
            <a href="about.jsp" class="nav-link">About</a>
            <a href="<%= importHref %>" class="<%= importClass %>">Import</a>
            <a href="<%= multiplierHref %>" class="<%= multiplierClass %>">Multipliers</a>
            <a href="<%= eventsHref %>" class="<%= eventsClass %>">Events</a>
            <a href="<%= segmentsHref %>" class="<%= segmentsClass %>">Segments</a>
            <a href="<%= boostLink %>" class="<%= boostClass %>">BoostMe</a>
            <a href="<%= profileHref %>" class="<%= profileClass %>">Profiles</a>
        </div>
    </div>
    <div class="nav-right">
        <form action="LogoutServlet" method="post" class="logout-form">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <button type="submit" class="btn-logout">Logout</button>
        </form>
    </div>
</nav>
<script>
    (function () {
        var csrfToken = '${sessionScope.csrfToken}';
        window.ezBoostCsrfToken = csrfToken;

        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (!form || String(form.method).toLowerCase() !== 'post' || form.querySelector('input[name="csrfToken"]')) {
                return;
            }
            var token = document.createElement('input');
            token.type = 'hidden';
            token.name = 'csrfToken';
            token.value = csrfToken;
            form.appendChild(token);
        }, true);
    }());
</script>
