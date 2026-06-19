<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page session="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="loggedUser" value="${sessionScope.user}" />
<c:if test="${empty loggedUser}">
    <c:redirect url="login.jsp" />
</c:if>
<c:if test="${empty daysActive}"><c:set var="daysActive" value="0" /></c:if>
<c:if test="${empty optimizationCount}"><c:set var="optimizationCount" value="0" /></c:if>
<c:if test="${empty bestRevenue}"><c:set var="bestRevenue" value="0.00" /></c:if>
<c:if test="${empty bestRevenueRaw}"><c:set var="bestRevenueRaw" value="0.0" /></c:if>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Profile - EzBoost</title>
    <link rel="stylesheet" href="css/profile.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
    <%@ include file="nav.jsp" %>

    <c:if test="${not empty profileSuccess}">
        <div class="alert-modern alert-success" style="max-width: 1200px; margin: 1rem auto 0;">
            <i class="bi bi-check-circle-fill"></i>
            <span>${profileSuccess}</span>
        </div>
    </c:if>
    <c:if test="${not empty profileError}">
        <div class="alert-modern alert-danger" style="max-width: 1200px; margin: 1rem auto 0;">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <span>${profileError}</span>
        </div>
    </c:if>

    <!-- Profile Header -->
    <section class="profile-hero">
        <div class="hero-container">
            <div class="profile-hero-content">
                <div class="profile-avatar-section">
                    <div class="avatar-circle">
                        <span class="avatar-initials">${fn:toUpperCase(fn:substring(loggedUser.firstName, 0, 1))}${fn:toUpperCase(fn:substring(loggedUser.lastName, 0, 1))}</span>
                    </div>
                    <div class="profile-info">
                        <h1 class="profile-name">
                            ${loggedUser.firstName} ${loggedUser.lastName}
                        </h1>
                        <p class="profile-username">@${loggedUser.username}</p>
                    </div>
                </div>

                <div class="profile-stats">
                    <div class="stat-card">
                        <div class="stat-info">
                            <span class="stat-number">${daysActive}</span>
                            <span class="stat-label">Days Active</span>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-info">
                            <span class="stat-number">${optimizationCount}</span>
                            <span class="stat-label">Optimizations</span>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-info">
                            <span class="stat-number"><c:choose><c:when test="${bestRevenueRaw > 0}">RM ${bestRevenue}</c:when><c:otherwise>-</c:otherwise></c:choose></span>
                            <span class="stat-label">Best Result</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Profile Content -->
    <section class="profile-content">
        <div class="content-container">
            <div class="profile-grid">

                <!-- Personal Information Card -->
                <div class="profile-card personal-info">
                    <div class="card-header">
                        <div class="card-icon-wrapper">
                            <div class="card-icon">P</div>
                        </div>
                        <div class="card-title">
                            <h2>Personal Information</h2>
                            <p>Manage your account details</p>
                        </div>
                        <button class="btn-edit" onclick="openEditModal()">
                            <span>Edit</span>
                        </button>
                    </div>
                    <div class="card-content">
                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-details">
                                    <label>First Name</label>
                                    <span class="info-value">${loggedUser.firstName}</span>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="info-details">
                                    <label>Last Name</label>
                                    <span class="info-value">${loggedUser.lastName}</span>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="info-details">
                                    <label>Username</label>
                                    <span class="info-value">${loggedUser.username}</span>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="info-details">
                                    <label>Email Address</label>
                                    <span class="info-value">${loggedUser.email}</span>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="info-details">
                                    <label>Phone Number</label>
                                    <span class="info-value">${loggedUser.phoneNumber}</span>
                                </div>
                            </div>
                            <div class="info-item">
                                <div class="info-details">
                                    <label>Password</label>
                                    <span class="info-value password-hidden">????????</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Activity Dashboard Card -->
                <div class="profile-card activity wide-card">
                    <div class="card-header">
                        <div class="card-icon-wrapper">
                            <div class="card-icon">A</div>
                        </div>
                        <div class="card-title">
                            <h2>Activity Dashboard</h2>
                            <p>Your optimization history</p>
                        </div>
                    </div>
                    <div class="card-content">
                        <div class="activity-stats">
                            <div class="activity-item">
                                <div class="activity-details">
                                    <div class="activity-number">${optimizationCount}</div>
                                    <div class="activity-label">Total Optimizations</div>
                                </div>
                            </div>
                            <c:if test="${bestRevenueRaw > 0}">
                            <div class="activity-item">
                                <div class="activity-details">
                                    <div class="activity-number">RM ${bestRevenue}</div>
                                    <div class="activity-label">Best Estimated Revenue</div>
                                </div>
                            </div>
                            </c:if>
                        </div>

                        <div class="recent-activity">
                            <h3>Recent Optimizations</h3>
                            <c:choose>
                            <c:when test="${not empty recentOptimizations}">
                            <div class="activity-timeline">
                                <c:forEach var="opt" items="${recentOptimizations}">
                                <div class="timeline-item">
                                    <div class="timeline-dot success"></div>
                                    <div class="timeline-content">
                                        <div class="timeline-header">
                                            <span class="timeline-title">Optimization completed</span>
                                            <span class="timeline-time"><c:choose><c:when test="${not empty opt.date}"><fmt:formatDate value="${opt.date}" pattern="dd MMM yyyy, HH:mm"/></c:when><c:otherwise>Unknown date</c:otherwise></c:choose></span>
                                        </div>
                                        <p class="timeline-description">Total estimated revenue: RM <fmt:formatNumber value="${opt.totalRevenue}" pattern="#,##0.00"/></p>
                                    </div>
                                </div>
                                </c:forEach>
                            </div>
                            </c:when>
                            <c:otherwise>
                            <div class="empty-state">
                                <p>No optimizations yet.</p>
                                <a href="BoostMe">Run your first optimization</a>
                            </div>
                            </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </section>

    <!-- Edit Profile Modal -->
    <div class="modal" id="editModal">
        <div class="modal-backdrop" onclick="closeEditModal()"></div>
        <div class="modal-content">
            <div class="modal-header">
                <div class="modal-title">
                    <h2>Edit Profile</h2>
                    <p>Update your personal information</p>
                </div>
                <button class="modal-close" onclick="closeEditModal()">&times;</button>
            </div>
            <form action="UpdateProfileServlet" method="post" class="edit-form">
                <div class="form-row">
                    <div class="form-group">
                        <label for="firstName">First Name</label>
                        <input type="text" id="firstName" name="firstName"
                               value="${not empty profileFormData ? profileFormData['firstName'] : loggedUser.firstName}" required>
                    </div>

                    <div class="form-group">
                        <label for="lastName">Last Name</label>
                        <input type="text" id="lastName" name="lastName"
                               value="${not empty profileFormData ? profileFormData['lastName'] : loggedUser.lastName}" required>
                    </div>
                </div>

                <div class="form-group">
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username"
                           value="${not empty profileFormData ? profileFormData['username'] : loggedUser.username}" required>
                </div>

                <div class="form-group">
                    <label for="email">Email Address</label>
                    <input type="email" id="email" name="email"
                           value="${not empty profileFormData ? profileFormData['email'] : loggedUser.email}" required>
                </div>

                <div class="form-group">
                    <label for="phoneNumber">Phone Number</label>
                    <input type="tel" id="phoneNumber" name="phoneNumber"
                           value="${not empty profileFormData ? profileFormData['phoneNumber'] : loggedUser.phoneNumber}" required>
                </div>

                <div class="form-group">
                    <label for="newPassword">New Password</label>
                    <input type="password" id="newPassword" name="newPassword"
                           placeholder="Leave blank to keep current password">
                    <small>Leave blank to keep your current password</small>
                </div>

                <div class="form-actions">
                    <button type="button" class="btn-cancel" onclick="closeEditModal()">Cancel</button>
                    <button type="submit" class="btn-save">Save Changes</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Simple Footer -->
    <footer class="simple-footer">
        <p>&copy; 2026 EzBoost. All Rights Reserved.</p>
    </footer>

    <script>
        // Modal Functions
        function openEditModal() {
            document.getElementById('editModal').classList.add('active');
            document.body.style.overflow = 'hidden';
        }

        function closeEditModal() {
            document.getElementById('editModal').classList.remove('active');
            document.body.style.overflow = 'auto';
        }

        // Form validation
        document.querySelector('.edit-form').addEventListener('submit', function(e) {
            const inputs = this.querySelectorAll('input[required]');
            let valid = true;

            inputs.forEach(input => {
                if (!input.value.trim()) {
                    valid = false;
                    input.classList.add('error');
                } else {
                    input.classList.remove('error');
                }
            });

            if (!valid) {
                e.preventDefault();
                alert('Please fill in all required fields.');
            }
        });

        // Close modal on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeEditModal();
            }
        });

        <% if (Boolean.TRUE.equals(request.getAttribute("openEditModal"))) { %>
        openEditModal();
        <% } %>

        // Animate cards on scroll
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('animate-in');
                }
            });
        }, observerOptions);

        document.querySelectorAll('.profile-card').forEach(card => {
            observer.observe(card);
        });
    </script>
</body>
</html>
