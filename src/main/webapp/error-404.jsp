<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Page Not Found - EzBoost</title>
    <link rel="stylesheet" href="css/theme.css">
    <style>
        body { background: var(--bg-cream, #f5f0e8); font-family: 'Inter', sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
        .error-card { background: white; border-radius: 12px; padding: 3rem; text-align: center; max-width: 480px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
        .error-code { font-family: 'DM Serif Display', serif; font-size: 5rem; color: var(--text-primary, #1a1a1a); margin: 0; line-height: 1; }
        .error-message { color: #666; margin: 1rem 0 2rem; }
        .back-link { display: inline-block; background: var(--text-primary, #1a1a1a); color: white; padding: 0.75rem 2rem; border-radius: 50px; text-decoration: none; font-weight: 500; }
        .back-link:hover { opacity: 0.85; }
    </style>
</head>
<body>
    <div class="error-card">
        <p class="error-code">404</p>
        <p class="error-message">The page you're looking for doesn't exist.</p>
        <a href="homepage.jsp" class="back-link">Back to Home</a>
    </div>
</body>
</html>
