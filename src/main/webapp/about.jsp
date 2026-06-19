<%@ page import="com.ezboost.model.User" %>
<%
    User loggedInUser = (User) session.getAttribute("user");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>About - EzBoost</title>
    <link rel="stylesheet" href="css/about.css">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
    <%@ include file="nav.jsp" %>

    <!-- Hero Section -->
    <section class="hero-section">
        <div class="hero-container">
            <h1 class="hero-title">About EzBoost</h1>
            <p class="hero-subtitle">Intelligent hotel revenue optimization through genetic algorithms</p>
        </div>
    </section>

    <!-- Main Content -->
    <div class="main-content">
        <!-- About Section -->
        <section class="content-section">
            <h2>About EzBoost</h2>
            <p>EzBoost is the intelligent core of our hotel revenue optimization platform. A smart pricing engine powered by Genetic Algorithm (GA). It's designed to automatically generate room prices that support stronger revenue outcomes based on seasonal trends, room types, and occupancy forecasts. With just a few clicks, EzBoost analyzes your hotel's unique conditions and simulates thousands of pricing possibilities. By applying evolutionary concepts like crossover and mutation, it continuously improves price recommendations while helping you avoid leaving revenue on the table. Whether it's low season or peak demand, EzBoost adapts instantly to help you capture every opportunity. No complicated setup, no data science degree required.</p>
        </section>

        <!-- Mission Section -->
        <section class="content-section">
            <h2>Our Mission</h2>
            <p>We aim to empower hotels of all sizes to make smarter, data-driven pricing decisions through intelligent technology. By combining real-time seasonal demand with machine learning principles, our system enables hoteliers to optimize revenue without relying on expensive commercial platforms.</p>
        </section>

        <!-- Why EzBoost Section -->
        <section class="content-section">
            <h2>Why EzBoost?</h2>
            <p>In today's digital economy, traditional pricing strategies no longer suffice. Hotels must respond rapidly to changes in booking trends, events, and competitor prices. Our system eliminates guesswork and empowers hoteliers to apply evolutionary algorithms that simulate real-world decision-making that achieve smarter pricing in just a few clicks.</p>

            <!-- Comparison -->
            <div class="comparison-grid">
                <div class="compare-card">
                    <h4>Traditional Pricing</h4>
                    <ul class="compare-list">
                        <li class="negative">Manual calculations</li>
                        <li class="negative">Slow response</li>
                        <li class="negative">Human error</li>
                    </ul>
                </div>
                <div class="compare-vs">VS</div>
                <div class="compare-card highlight">
                    <h4>EzBoost Way</h4>
                    <ul class="compare-list">
                        <li class="positive">AI-powered automation</li>
                        <li class="positive">Real-time optimization</li>
                        <li class="positive">Data-driven pricing</li>
                    </ul>
                </div>
            </div>
        </section>

        <!-- Technology Section -->
        <section class="content-section">
            <h2>Technology We Used</h2>
            <p>The system is built using Java EE for secure, scalable web deployment. The optimization engine leverages the power of Genetic Algorithm, using mechanisms like crossover and mutation to evolve the best pricing solutions over time.</p>

            <div class="tech-grid">
                <div class="tech-card">
                    <h4>Java EE Platform</h4>
                    <p>Robust, enterprise-grade framework ensuring security, scalability, and reliability for web deployment across all hotel sizes.</p>
                </div>
                <div class="tech-card">
                    <h4>Genetic Algorithm</h4>
                    <p>Advanced evolutionary computing that mimics natural selection to continuously evolve and optimize pricing strategies.</p>
                </div>
                <div class="tech-card">
                    <h4>Crossover & Mutation</h4>
                    <p>Intelligent mechanisms that combine successful pricing strategies and introduce variations to discover optimal solutions.</p>
                </div>
            </div>
        </section>

        <!-- Stats Section -->
        <section class="stats-section">
            <div class="stats-grid">
                <div class="stat-item">
                    <span class="stat-number">GA</span>
                    <div class="stat-label">Genetic Algorithm</div>
                    <div class="stat-desc">Evolutionary Optimization</div>
                </div>
                <div class="stat-item">
                    <span class="stat-number">4</span>
                    <div class="stat-label">Season Pricing Model</div>
                    <div class="stat-desc">Low, Normal, Peak, Super Peak</div>
                </div>
                <div class="stat-item">
                    <span class="stat-number">7+</span>
                    <div class="stat-label">Market Segments</div>
                    <div class="stat-desc">FIT & GIT Categories</div>
                </div>
                <div class="stat-item">
                    <span class="stat-number">12</span>
                    <div class="stat-label">Month Forecast</div>
                    <div class="stat-desc">Event-Based Pricing</div>
                </div>
            </div>
        </section>

        <!-- CTA Section -->
        <section class="cta-section">
            <h3>Ready to Optimize Your Revenue?</h3>
            <p>Experience the power of AI-driven pricing optimization for your hotel</p>
            <a href="BoostMe.jsp" class="cta-button">Start Your Journey</a>
        </section>
    </div>

    <footer class="simple-footer">
        <p>&copy; 2026 EzBoost. All Rights Reserved.</p>
    </footer>

    <script>
        function scrollReveal() {
            var elements = document.querySelectorAll('.content-section, .stat-item, .tech-card');
            var observer = new IntersectionObserver(function(entries) {
                entries.forEach(function(entry) {
                    if (entry.isIntersecting) {
                        entry.target.style.opacity = '1';
                        entry.target.style.transform = 'translateY(0)';
                    }
                });
            }, { threshold: 0.1 });

            elements.forEach(function(el) {
                el.style.opacity = '0';
                el.style.transform = 'translateY(20px)';
                el.style.transition = 'all 0.6s ease';
                observer.observe(el);
            });
        }

        document.addEventListener('DOMContentLoaded', scrollReveal);
    </script>
</body>
</html>
