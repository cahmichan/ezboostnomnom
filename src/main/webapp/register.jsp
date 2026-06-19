<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Register - EzBoost</title>
    <link rel="stylesheet" href="css/register.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>

<div class="auth-container">
    <div class="auth-card">
        <div class="logo-section">
            <h1 class="brand-name">EzBoost</h1>
            <p class="brand-tagline">Revenue Management System</p>
        </div>

        <h2>Create Your Account</h2>
        <p class="auth-subtitle">Join EzBoost and start optimizing your hotel revenue</p>

        <c:if test="${not empty error}">
            <div class="error-message">${error}</div>
        </c:if>

        <form action="RegisterServlet" method="post" id="registerForm" novalidate>
            <div class="form-grid">
                <div class="form-group">
                    <label for="firstName">First Name</label>
                    <input type="text" id="firstName" name="firstName" placeholder="First Name"
                           value="${firstName}" required>
                    <span class="field-error" id="firstNameError"></span>
                </div>

                <div class="form-group">
                    <label for="lastName">Last Name</label>
                    <input type="text" id="lastName" name="lastName" placeholder="Last Name"
                           value="${lastName}" required>
                    <span class="field-error" id="lastNameError"></span>
                </div>
            </div>

            <div class="form-grid">
                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username" placeholder="Username"
                           value="${username}" required>
                    <span class="field-error" id="usernameError"></span>
                </div>

                <div class="form-group">
                    <label for="email">Email Address</label>
                    <input type="email" id="email" name="email" placeholder="Email Address"
                           value="${email}" required>
                    <span class="field-error" id="emailError"></span>
                </div>
            </div>

            <div class="form-grid">
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" placeholder="Min. 6 characters" required>
                    <span class="field-error" id="passwordError"></span>
                </div>

                <div class="form-group">
                    <label for="phoneNumber">Phone Number</label>
                    <input type="text" id="phoneNumber" name="phoneNumber" placeholder="Phone Number"
                           value="${phoneNumber}" required>
                    <span class="field-error" id="phoneNumberError"></span>
                </div>
            </div>

            <button type="submit" class="btn-submit">Create Account</button>

            <p class="alt-action">Already have an account? <a href="login.jsp">Login here</a></p>
        </form>
    </div>
</div>

<script>
    document.getElementById('registerForm').addEventListener('submit', function(e) {
        // Clear previous errors
        document.querySelectorAll('.field-error').forEach(function(el) { el.textContent = ''; });
        document.querySelectorAll('.form-group input').forEach(function(el) { el.classList.remove('input-error'); });

        var valid = true;

        var firstName = document.getElementById('firstName').value.trim();
        var lastName = document.getElementById('lastName').value.trim();
        var username = document.getElementById('username').value.trim();
        var email = document.getElementById('email').value.trim();
        var password = document.getElementById('password').value;
        var phone = document.getElementById('phoneNumber').value.trim();

        if (!firstName) {
            showFieldError('firstName', 'First name is required');
            valid = false;
        }
        if (!lastName) {
            showFieldError('lastName', 'Last name is required');
            valid = false;
        }
        if (!username) {
            showFieldError('username', 'Username is required');
            valid = false;
        } else if (username.length < 3) {
            showFieldError('username', 'At least 3 characters');
            valid = false;
        } else if (!/^[a-zA-Z0-9_]+$/.test(username)) {
            showFieldError('username', 'Letters, numbers, underscores only');
            valid = false;
        }
        if (!email) {
            showFieldError('email', 'Email is required');
            valid = false;
        } else if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
            showFieldError('email', 'Enter a valid email');
            valid = false;
        }
        if (!password) {
            showFieldError('password', 'Password is required');
            valid = false;
        } else if (password.length < 6) {
            showFieldError('password', 'At least 6 characters');
            valid = false;
        }
        if (!phone) {
            showFieldError('phoneNumber', 'Phone number is required');
            valid = false;
        } else if (phone.replace(/[^0-9]/g, '').length < 7) {
            showFieldError('phoneNumber', 'At least 7 digits');
            valid = false;
        }

        if (!valid) {
            e.preventDefault();
        }
    });

    function showFieldError(fieldId, message) {
        document.getElementById(fieldId).classList.add('input-error');
        document.getElementById(fieldId + 'Error').textContent = message;
    }
</script>

</body>
</html>
