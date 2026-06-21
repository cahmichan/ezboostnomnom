<!-- nav.jsp -->
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<link rel="stylesheet" href="css/theme.css">
<link rel="stylesheet" href="css/styles.css">
<link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">

<nav class="navbar">
    <div class="nav-left">
        <a href="${navHomeLink}" class="nav-logo">
            <span class="logo-text">EzBoost</span>
        </a>
    </div>
    <div class="nav-center">
        <div class="nav-links">
            <a href="about.jsp" class="nav-link">About</a>
            <a href="${navImportHref}" class="${navImportClass}">Import</a>
            <a href="${navMultiplierHref}" class="${navMultiplierClass}">Multipliers</a>
            <a href="${navEventsHref}" class="${navEventsClass}">Events</a>
            <a href="${navSegmentsHref}" class="${navSegmentsClass}">Segments</a>
            <a href="${navBoostLink}" class="${navBoostClass}">BoostMe</a>
            <a href="${navProfileHref}" class="${navProfileClass}">Profiles</a>
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
