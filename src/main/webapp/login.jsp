<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Login - EzBoost</title>
    <link rel="stylesheet" href="css/login.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>

<div class="auth-container">
    <div class="auth-card">
        <div class="logo-section">
            <h1 class="brand-name">EzBoost</h1>
            <p class="brand-tagline">Revenue Management System</p>
        </div>

        <h2>Welcome Back</h2>
        <p class="auth-subtitle">Login to continue managing your hotel</p>

        <c:if test="${not empty error}">
            <div class="error-message">${error}</div>
        </c:if>

        <c:if test="${param.registered == '1'}">
            <div class="success-message">Account created successfully. Please login.</div>
        </c:if>

        <form action="LoginServlet" method="post">
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" placeholder="Enter your email" required>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" placeholder="Enter your password" required>
            </div>

            <button type="submit" class="btn-submit">Login</button>
        </form>

        <p class="alt-action">Don't have an account? <a href="register.jsp">Register now</a></p>
    </div>
</div>

</body>
</html>
