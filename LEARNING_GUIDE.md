# EzBoost Codebase - Complete Learning Guide

> **Goal**: Understand the entire EzBoost codebase within one month.
> **How to use this guide**: Read Part 1-3 in Week 1, Part 4-6 in Week 2, Part 7-9 in Week 3, and Part 10-12 in Week 4. Each part builds on the previous one.

---

## Table of Contents

- **Part 1** - [What is EzBoost?](#part-1---what-is-ezboost) *(Day 1)*
- **Part 2** - [Tech Stack Explained](#part-2---tech-stack-explained) *(Day 2)*
- **Part 3** - [Project Structure & File Map](#part-3---project-structure--file-map) *(Day 3-4)*
- **Part 4** - [The Model Layer - Your Data Shapes](#part-4---the-model-layer) *(Day 5-6)*
- **Part 5** - [The Database & DAO Layer - How Data is Stored and Retrieved](#part-5---the-database--dao-layer) *(Day 7-9)*
- **Part 6** - [Authentication & Security - How Users Log In](#part-6---authentication--security) *(Day 10)*
- **Part 7** - [Servlets - How HTTP Requests Are Handled](#part-7---servlets) *(Day 11-13)*
- **Part 8** - [The Genetic Algorithm - The Brain of EzBoost](#part-8---the-genetic-algorithm) *(Day 14-17)*
- **Part 9** - [Demand Curve & Season Classification](#part-9---demand-curve--season-classification) *(Day 18-19)*
- **Part 10** - [Services - Business Logic Layer](#part-10---services) *(Day 20-21)*
- **Part 11** - [The Frontend - JSPs and CSS](#part-11---the-frontend) *(Day 22-24)*
- **Part 12** - [Putting It All Together - Complete Request Flows](#part-12---putting-it-all-together) *(Day 25-28)*
- **Appendix A** - [Configuration Reference](#appendix-a---configuration-reference)
- **Appendix B** - [Database Table Reference](#appendix-b---database-table-reference)
- **Appendix C** - [Build & Test Commands](#appendix-c---build--test-commands)
- **Appendix D** - [Glossary](#appendix-d---glossary)

---

# Part 1 - What is EzBoost?

## The Problem

Imagine you run a hotel with 100 rooms of different types (Deluxe, Standard, Suite). You need to figure out the best price for each room type during different times of year. During school holidays, you can charge more. During slow months, you need to lower prices to attract guests. Getting this wrong means either:

- **Pricing too high** → empty rooms → lost revenue
- **Pricing too low** → full rooms but leaving money on the table

Doing this manually is extremely hard because:
- You have multiple room types
- Each room type needs 4 different prices (one per season)
- You also have different customer types (travel agents, walk-ins, online bookings) who each pay different rates
- Upcoming events (holidays, festivals) temporarily change demand

## The Solution

EzBoost is a **hotel revenue optimization platform** that uses a **Genetic Algorithm (GA)** to automatically find the best room prices. You tell it "I want to earn RM 100,000 this year" and it figures out what to charge for each room type in each season to hit that target.

## Core Features

1. **Price Optimization** - GA finds optimal prices for each room type across 4 seasons
2. **Demand Curve Modeling** - Learns from your historical data how price affects occupancy
3. **Season Classification** - Automatically figures out which months are low/normal/peak/super-peak based on your data
4. **Market Segments** - Different prices for different customer channels (OTA, walk-in, corporate, etc.)
5. **Event Integration** - Fetches public holidays and lets you add custom events that boost pricing
6. **Data Import/Export** - CSV import for historical data, Excel export for reports
7. **Per-User Isolation** - Each user sees only their own data

## How It Works (High Level)

```
User imports historical data (CSV)
          ↓
System classifies months into seasons (LOW/NORMAL/PEAK/SUPER_PEAK)
          ↓
System fits a demand curve (price vs. occupancy relationship)
          ↓
User sets target revenue (e.g., RM 100,000)
          ↓
Genetic Algorithm runs (200 candidate solutions × 600 generations)
          ↓
Output: Optimal price per room type per season
          ↓
Market segment multipliers applied (OTA gets 1.2x, walk-in gets 1.15x, etc.)
          ↓
Future events adjust monthly forecast (holidays bump seasons up)
          ↓
User sees results + downloads Excel report
```

---

# Part 2 - Tech Stack Explained

## What Each Technology Does

### Java 11 (The Language)
Everything is written in Java 11. Java was chosen because it runs on the JVM and is the standard for Jakarta EE web applications.

### Maven (The Build Tool)
Maven manages dependencies and builds the project. The `pom.xml` file at the project root defines:
- All libraries the project uses
- How to compile the code
- How to package it into a `.war` file

**Key commands:**
```bash
mvn compile          # Just compile the Java files
mvn test             # Run the 25 JUnit tests
mvn clean package    # Compile + test + package into target/EzBoost-1.0-SNAPSHOT.war
```

### Jakarta EE 10 (The Web Framework)
Jakarta EE (formerly Java EE) provides the web server APIs. EzBoost uses three parts:

| Component | What It Does | Where You See It |
|-----------|-------------|------------------|
| **Servlets** | Handle HTTP requests (GET/POST) | `src/main/java/com/ezboost/servlet/*.java` |
| **JSP** | HTML templates with embedded Java/JSTL | `src/main/webapp/*.jsp` |
| **Filters** | Intercept requests before they reach servlets | `AuthenticationFilter.java` |

**How a request flows through Jakarta EE:**
```
Browser → HTTP Request → Filter → Servlet → [business logic] → JSP → HTML Response → Browser
```

### Apache Derby 10.16 (The Database)
Derby is a lightweight relational database written in Java. EzBoost uses it in **client mode**, meaning:
- Derby runs as a separate server process on `localhost:1527`
- The app connects to it over the network using JDBC
- Database name: `ezboost_db`
- Login: username `app`, password `app`

**Important**: Derby uses a special driver class: `org.apache.derby.client.ClientAutoloadedDriver` (not the legacy `org.apache.derby.jdbc.ClientDriver`).

### HikariCP 5.1.0 (Connection Pool)
Opening a database connection is expensive (takes time). HikariCP keeps a pool of pre-opened connections ready to use. When code needs a connection, it borrows one from the pool and returns it when done.

**Pool settings** (from `DBConnection.java`):
```
Max connections:    10 (at most 10 simultaneous database connections)
Min idle:           2  (always keep at least 2 connections ready)
Connection timeout: 30 seconds (give up if can't get a connection in 30s)
Idle timeout:       10 minutes (close unused connections after 10 min)
Max lifetime:       30 minutes (recycle connections every 30 min)
```

### Apache POI 5.2.5 (Excel Library)
Used to create `.xlsx` Excel files for exporting optimization results. Also used to parse uploaded Excel files during room data import.

### SLF4J 2.0.9 (Logging)
Every class logs through SLF4J instead of `System.out.println`. The `slf4j-simple` binding sends all log output to `System.err`.

**Usage pattern (you'll see this in every class):**
```java
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

// Then throughout the class:
logger.debug("Processing room: {}", roomName);  // Debug-level (development)
logger.error("Failed to save: {}", e.getMessage(), e);  // Error-level (problems)
```

### Payara/GlassFish (Application Server)
The `.war` file gets deployed to Payara (or GlassFish), which provides the Jakarta EE runtime. The server handles HTTP, sessions, and the servlet lifecycle.

### JUnit 5 + Mockito (Testing)
- **JUnit 5**: Framework for writing and running tests
- **Mockito**: Library for creating mock/fake objects in tests
- Currently 25 tests across 3 test files: `RoomTest`, `SeasonTest`, `DemandCurveTest`

---

# Part 3 - Project Structure & File Map

## Directory Layout

```
EzBoost-main/
├── pom.xml                          ← Maven build config (dependencies, plugins)
├── CLAUDE.md                        ← Developer guidelines for the codebase
│
├── src/
│   ├── main/
│   │   ├── java/com/ezboost/       ← All Java source code
│   │   │   ├── model/              ← Data classes (POJOs) + Season enum
│   │   │   │   ├── User.java
│   │   │   │   ├── Room.java
│   │   │   │   ├── Season.java
│   │   │   │   ├── MarketSegment.java
│   │   │   │   ├── FutureEvent.java
│   │   │   │   ├── MonthlySeasonData.java
│   │   │   │   ├── SeasonThreshold.java
│   │   │   │   ├── UserMultiplierSettings.java
│   │   │   │   └── SegmentPricingResult.java
│   │   │   │
│   │   │   ├── dao/                ← Database access (static methods, SQL queries)
│   │   │   │   ├── UserDAO.java
│   │   │   │   ├── RoomDataDAO.java
│   │   │   │   ├── MarketSegmentDAO.java
│   │   │   │   ├── SeasonalityDAO.java
│   │   │   │   ├── FutureEventDAO.java
│   │   │   │   ├── UserSettingsDAO.java
│   │   │   │   ├── OptimizationRequestDAO.java
│   │   │   │   └── OptimizationResultDAO.java
│   │   │   │
│   │   │   ├── servlet/            ← HTTP request handlers
│   │   │   │   ├── LoginServlet.java
│   │   │   │   ├── RegisterServlet.java
│   │   │   │   ├── LogoutServlet.java
│   │   │   │   ├── RunGA.java              ← The main optimization servlet
│   │   │   │   ├── ProfileServlet.java
│   │   │   │   ├── UpdateProfileServlet.java
│   │   │   │   ├── DataImportServlet.java
│   │   │   │   ├── MarketSegmentSettingsServlet.java
│   │   │   │   ├── MultiplierSettingsServlet.java
│   │   │   │   ├── EventSettingsServlet.java
│   │   │   │   └── DownloadReportServlet.java
│   │   │   │
│   │   │   ├── ga/                 ← Genetic Algorithm + optimization math
│   │   │   │   ├── GeneticAlgorithm.java   ← THE core optimization engine
│   │   │   │   ├── DemandCurve.java        ← Price-to-occupancy model
│   │   │   │   └── SeasonClassifierGA.java ← Auto-classify seasons
│   │   │   │
│   │   │   ├── service/            ← Business logic layer
│   │   │   │   ├── SegmentPricingService.java
│   │   │   │   ├── EventSeasonService.java
│   │   │   │   ├── ExcelExportService.java
│   │   │   │   └── CalendarificService.java
│   │   │   │
│   │   │   └── util/               ← Shared utilities
│   │   │       ├── DBConnection.java       ← HikariCP connection pool
│   │   │       ├── AuthenticationFilter.java
│   │   │       ├── CSVImportUtil.java
│   │   │       └── RoomDataImportUtil.java
│   │   │
│   │   └── webapp/                 ← Web resources (JSPs, CSS, JS, images)
│   │       ├── WEB-INF/
│   │       │   └── web.xml         ← Servlet mappings, filter config, error pages
│   │       │
│   │       ├── css/                ← Stylesheets
│   │       │   ├── theme.css       ← Global design tokens (colors, fonts, variables)
│   │       │   ├── styles.css      ← Base layout (navbar, header)
│   │       │   ├── settings.css    ← Shared styles for all 4 settings pages
│   │       │   ├── homepage.css
│   │       │   ├── login.css
│   │       │   ├── register.css
│   │       │   ├── profile.css
│   │       │   ├── result.css
│   │       │   └── about.css
│   │       │
│   │       ├── nav.jsp             ← Shared navigation bar (included by all pages)
│   │       ├── login.jsp           ← Login form
│   │       ├── register.jsp        ← Registration form
│   │       ├── homepage.jsp        ← Main dashboard after login
│   │       ├── profile.jsp         ← User profile + stats
│   │       ├── about.jsp           ← About page
│   │       ├── BoostMe.jsp         ← Optimization results display
│   │       ├── data-import.jsp     ← Data import management
│   │       ├── multiplier-settings.jsp
│   │       ├── segment-settings.jsp
│   │       ├── event-settings.jsp
│   │       ├── segment-pricing-component.jsp
│   │       ├── import.jsp
│   │       ├── error-404.jsp       ← Custom 404 error page
│   │       └── error-500.jsp       ← Custom 500 error page
│   │
│   └── test/java/com/ezboost/     ← Test files
│       ├── model/
│       │   ├── RoomTest.java       ← 11 tests for Room model
│       │   └── SeasonTest.java     ← 5 tests for Season enum
│       └── ga/
│           └── DemandCurveTest.java ← 9 tests for DemandCurve
│
└── target/                          ← Build output (auto-generated, don't edit)
    └── EzBoost-1.0-SNAPSHOT.war    ← Deployable web archive
```

## Architecture Layers (How They Connect)

```
┌─────────────────────────────────────────────────────────────────┐
│                        BROWSER (User)                           │
│                    HTML / CSS / JavaScript                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP Request
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   AuthenticationFilter                           │
│                (checks: is user logged in?)                      │
│         Public? → pass through    Private? → check session      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVLET LAYER                              │
│   LoginServlet, RunGA, ProfileServlet, DataImportServlet, ...   │
│   (Extracts userId from session, validates input, delegates)    │
└───────────┬──────────────────────────┬──────────────────────────┘
            │                          │
            ▼                          ▼
┌───────────────────────┐  ┌──────────────────────────────────────┐
│    SERVICE LAYER      │  │        GA LAYER                      │
│ SegmentPricingService │  │ GeneticAlgorithm (price optimization)│
│ EventSeasonService    │  │ DemandCurve (price→occupancy model)  │
│ CalendarificService   │  │ SeasonClassifierGA (auto thresholds) │
│ ExcelExportService    │  │                                      │
└───────────┬───────────┘  └──────────────┬───────────────────────┘
            │                             │
            ▼                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DAO LAYER                                │
│   UserDAO, RoomDataDAO, SeasonalityDAO, MarketSegmentDAO, ...   │
│   (Static methods, SQL queries, try-with-resources)             │
└──────────────────────────┬──────────────────────────────────────┘
                           │ JDBC (via HikariCP pool)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Apache Derby Database                         │
│              jdbc:derby://localhost:1527/ezboost_db              │
└─────────────────────────────────────────────────────────────────┘
```

**Read this diagram bottom-up to understand the dependency direction:**
- JSPs depend on Servlets (servlets set request attributes that JSPs display)
- Servlets depend on DAOs and Services
- Services depend on DAOs
- DAOs depend on DBConnection (utility)
- Everything depends on Models (data shapes)

---

# Part 4 - The Model Layer

Models are simple Java classes (POJOs - Plain Old Java Objects) that represent the shapes of data in the system. They have fields, getters, setters, and sometimes a bit of logic.

**Location**: `src/main/java/com/ezboost/model/`

## Season.java - The Foundation Enum

This is the most important model to understand first because almost everything references it.

```java
public enum Season {
    LOW(-0.1, 0.2, 0.9),
    NORMAL(0.0, 0.5, 1.1),
    PEAK(0.1, 0.7, 1.4),
    SUPER_PEAK(0.2, 0.9, 1.8);

    private final double scaleFactor;
    private final double minMultiplier;
    private final double maxMultiplier;
}
```

**What do these numbers mean?**

Each season has three numbers that control price boundaries:

| Season | scaleFactor | minMultiplier | maxMultiplier | Meaning |
|--------|------------|---------------|---------------|---------|
| LOW | -0.1 | 0.2 | 0.9 | Prices can be 20%-90% of base |
| NORMAL | 0.0 | 0.5 | 1.1 | Prices can be 50%-110% of base |
| PEAK | 0.1 | 0.7 | 1.4 | Prices can be 70%-140% of base |
| SUPER_PEAK | 0.2 | 0.9 | 1.8 | Prices can be 90%-180% of base |

**Example**: If a room's base ADR (Average Daily Rate) is RM 200:
- During LOW season, the GA can set the price between RM 40 (200 × 0.2) and RM 180 (200 × 0.9)
- During SUPER_PEAK, the GA can set the price between RM 180 (200 × 0.9) and RM 360 (200 × 1.8)

The `scaleFactor` is used during initial price generation to bias prices:
- LOW (-0.1): nudges initial prices slightly below base
- SUPER_PEAK (+0.2): nudges initial prices above base

## Room.java - The Core Business Object

A Room represents a type of hotel room (not an individual room, but a category like "Deluxe" or "Standard").

```java
public class Room {
    private String name;           // "Deluxe King", "Standard Twin", etc.
    private double minADR;         // Minimum Average Daily Rate (e.g., RM 150)
    private double maxADR;         // Maximum Average Daily Rate (e.g., RM 400)
    private double occupancy;      // Default occupancy percentage (e.g., 75.0 = 75%)
    private int totalRooms;        // How many physical rooms of this type (e.g., 30)

    // Seasonal prices: one price per season, computed by GA
    private EnumMap<Season, Double> seasonalPrices;

    // Optional per-season occupancies (set when using demand curve)
    private EnumMap<Season, Double> seasonalOccupancies;
}
```

**Key methods you need to know:**

```java
// The "base" price used as a starting point
getBaseAdr() = (minADR + maxADR) / 2.0
// Example: min=150, max=400 → base = 275

// Revenue estimation (used by GA fitness function)
getEstimatedRevenue() {
    for each season:
        revenue += price × (occupancy/100) × totalRooms × 91.25 days
    return total
}
// Example: RM 300 × 0.75 × 30 rooms × 91.25 = RM 615,937.50 per season

// Occupancy falls back to flat rate if no per-season override
getOccupancyForSeason(Season s) {
    if (seasonalOccupancies has entry for s) return it
    else return flat occupancy
}
```

**Constructor behavior**: When you create a `new Room(...)`, it automatically generates initial seasonal prices by averaging each season's min/max multipliers:

```java
generateSeasonalPrices() {
    for each Season:
        multiplier = (season.minMultiplier + season.maxMultiplier) / 2
        seasonalPrices[season] = baseAdr × multiplier
}
// Example: Deluxe (base RM 275)
//   LOW:        275 × (0.2+0.9)/2 = 275 × 0.55 = RM 151.25
//   NORMAL:     275 × (0.5+1.1)/2 = 275 × 0.80 = RM 220.00
//   PEAK:       275 × (0.7+1.4)/2 = 275 × 1.05 = RM 288.75
//   SUPER_PEAK: 275 × (0.9+1.8)/2 = 275 × 1.35 = RM 371.25
```

## User.java - User Accounts

Simple user model with standard fields:

```java
public class User {
    private int userId;            // Auto-generated primary key
    private String firstName;
    private String lastName;
    private String username;       // Unique, 3+ chars, alphanumeric + underscore
    private String email;          // Unique
    private String password;       // Stored as plain text (no hashing)
    private String phoneNumber;    // 7+ digits
    private Timestamp createdAt;   // When the account was created
}
```

## MarketSegment.java - Customer Types

Hotels sell rooms through different channels, each with a different price multiplier:

```java
public class MarketSegment {
    private int id;
    private int userId;            // Per-user segments
    private String segmentName;    // "Online Travel Agent"
    private String segmentCode;    // "OTA"
    private String category;       // "FIT" (individual) or "GIT" (group)
    private double rateMultiplier; // 0.5 to 2.0 — multiplied against base price
    private String description;
    private boolean active;
}
```

**Default segments** (created automatically on first visit):

| Category | Segment | Code | Multiplier | Meaning |
|----------|---------|------|-----------|---------|
| FIT | Travel Agent | TA | 0.90x | 10% discount |
| FIT | Online Travel Agent | OTA | 1.20x | 20% premium (they bring volume) |
| FIT | Direct Website | WEB | 1.00x | Standard rate |
| FIT | Walk-in | WALK | 1.15x | 15% premium (convenience) |
| FIT | Long Stay | LONG | 0.75x | 25% discount for extended stays |
| GIT | Corporate | CORP | 1.05x | 5% premium |
| GIT | Government | GOV | 0.85x | 15% discount (contracted rate) |
| GIT | Tour Group | TOUR | 0.80x | 20% discount (volume) |

**Example**: If the GA optimized Deluxe PEAK to RM 300:
- OTA channel: RM 300 × 1.20 = RM 360
- Walk-in: RM 300 × 1.15 = RM 345
- Government: RM 300 × 0.85 = RM 255

## FutureEvent.java - Upcoming Events

Events (holidays, festivals) that temporarily boost pricing:

```java
public class FutureEvent {
    private int eventId;
    private int userId;
    private String eventName;      // "Chinese New Year"
    private Date eventDate;        // Start date
    private Date eventEndDate;     // End date (can be null for single-day)
    private String eventType;      // "PUBLIC_HOLIDAY", "SCHOOL_BREAK", "CUSTOM"
    private String seasonOverride; // "PEAK" or "SUPER_PEAK" — the season to boost to
    private String source;         // "CALENDARIFIC" (API), "PRESET", or "MANUAL"
    private boolean active;        // Can be deactivated without deleting
}
```

**Key rule**: Events can only bump seasons **UP**, never down. If a month is already PEAK and an event says PEAK, nothing changes. If an event says SUPER_PEAK, it bumps up.

## MonthlySeasonData.java - Historical Data

Each row represents one month of historical hotel performance:

```java
public class MonthlySeasonData {
    private int dataId;
    private int userId;
    private String monthYear;       // "2024-01" format
    private String monthName;       // "January"
    private double occupancyRate;   // e.g., 82.5 (percent)
    private double totalRevenue;    // e.g., 150000.00
    private double avgRoomRate;     // e.g., 285.50
    private String classifiedSeason; // "LOW", "NORMAL", "PEAK", or "SUPER_PEAK"
    private Timestamp importDate;
}
```

**Season classification** (using default thresholds):
- occupancy >= 85% → SUPER_PEAK
- occupancy >= 75% → PEAK
- occupancy >= 65% → NORMAL
- occupancy < 65% → LOW

This data is used by both the DemandCurve (to learn price→occupancy) and EventSeasonService (to determine base seasons for each calendar month).

## Other Models (Quick Reference)

**SeasonThreshold.java** - User-specific thresholds for season classification:
```java
double thresholdLowNormal;      // Default: 65.0
double thresholdNormalPeak;     // Default: 75.0
double thresholdPeakSuperPeak;  // Default: 85.0
boolean isAutoGenerated;        // true if set by SeasonClassifierGA
```

**UserMultiplierSettings.java** - Per-user price multiplier overrides:
```java
String roomType;          // null = applies to all rooms
String seasonName;        // "LOW", "NORMAL", "PEAK", "SUPER_PEAK"
String segmentName;       // null = applies to all segments
double customMultiplier;  // e.g., 1.15
double minBound, maxBound; // 0.5 to 2.0
boolean isLocked;         // If true, GA cannot modify this price
```

**SegmentPricingResult.java** - Display object for results:
```java
String roomType;
String segmentName;
double basePrice;      // From GA optimization
double multiplier;     // From MarketSegment
double segmentPrice;   // = basePrice × multiplier
```

---

# Part 5 - The Database & DAO Layer

## How Database Access Works

Every DAO follows the same pattern. Here's the template:

```java
public class SomeDAO {
    private static final Logger logger = LoggerFactory.getLogger(SomeDAO.class);

    public static SomeResult doSomething(int userId) {
        String sql = "SELECT * FROM SomeTable WHERE user_id = ?";

        // try-with-resources: connection auto-closes when block exits
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);           // Set parameter (prevents SQL injection)
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Build result objects from database rows
            }
        } catch (SQLException e) {
            logger.error("Failed to do something: {}", e.getMessage(), e);
        }

        return result;
    }
}
```

**Critical pattern**: `DBConnection.getConnection()` borrows a connection from the HikariCP pool. The `try-with-resources` automatically returns it when done. This is used in **every single DAO method**.

## Per-User Data Isolation

Most tables include a `user_id` column, and every query filters by it:

```sql
SELECT * FROM MonthlySeasonData WHERE user_id = ?
SELECT * FROM MarketSegment WHERE user_id = ? AND active = true
```

**Exception**: `RoomDataDAO` does NOT filter by user — room inventory is currently global.

## DAO Reference

### UserDAO.java
Handles user accounts. Key methods:

| Method | What It Does |
|--------|-------------|
| `registerUser(User)` | INSERT new user, returns true/false |
| `loginUser(email, password)` | SELECT user by email+password, returns User or null |
| `getUserById(userId)` | SELECT user by ID |
| `updateUser(User)` | UPDATE user's name, email, phone |
| `isUsernameTaken(username)` | Check if username exists |
| `isEmailTaken(email)` | Check if email exists |

### RoomDataDAO.java
Manages hotel room types. **Not per-user** (global inventory).

| Method | What It Does |
|--------|-------------|
| `getAllRooms()` | Get all room types as `List<Room>` |
| `saveRoomData(List<Room>)` | **Clears all existing rooms** then inserts new ones (batch) |
| `addRoom(Room)` | Insert single room, returns generated ID |
| `updateRoom(id, Room)` | Update room by ID |
| `deleteRoom(id)` | Delete room by ID |
| `clearAllRoomData()` | Delete all rooms |
| `hasRoomData()` | Check if any rooms exist |

**Important**: `saveRoomData()` is destructive — it deletes all existing rooms before inserting. This is used during Excel import (replace all room data).

**Batch operation pattern** (used by saveRoomData):
```java
Connection conn = null;
try {
    conn = DBConnection.getConnection();
    conn.setAutoCommit(false);     // Start transaction

    // DELETE existing data
    // INSERT new data (loop)

    conn.commit();                  // All succeeded → commit
} catch (Exception e) {
    if (conn != null) conn.rollback();  // Something failed → undo everything
} finally {
    if (conn != null) conn.close();     // Always close
}
```

### SeasonalityDAO.java
The largest DAO. Manages monthly historical data AND season thresholds.

**Monthly data operations:**

| Method | What It Does |
|--------|-------------|
| `saveMonthlyData(data)` | Insert single month |
| `batchSaveMonthlyData(list)` | Insert many months (transactional) |
| `getMonthlyDataByUser(userId)` | Get all historical months |
| `getSeasonDistribution(userId)` | Count of months per season |
| `getAverageMetricsBySeason(userId)` | Average occupancy/revenue/ADR per season |
| `reclassifyAllSeasons(userId, thresholds)` | Re-classify all months using new thresholds |

**Threshold operations:**

| Method | What It Does |
|--------|-------------|
| `saveThresholds(threshold)` | Create or update thresholds |
| `getThresholdsByUser(userId)` | Get user's thresholds |
| `getOrCreateThresholds(userId)` | Lazy init: returns existing or creates defaults (65/75/85) |

### MarketSegmentDAO.java
Per-user market segments.

| Method | What It Does |
|--------|-------------|
| `getAllSegments(userId)` | Get active segments for user |
| `saveSegment(segment)` | Insert or update segment |
| `deleteSegment(code, userId)` | Soft delete (sets active=false) |
| `initializeDefaultSegments(userId)` | Creates the 8 default segments |

### FutureEventDAO.java
Per-user events. Also stores Calendarific API keys.

| Method | What It Does |
|--------|-------------|
| `getAllEvents(userId)` | Active events only |
| `getEventsByMonth(userId, year, month)` | Events overlapping a specific month |
| `batchSaveEvents(list)` | Insert many events (transactional) |
| `getApiKey(userId)` | Get stored Calendarific API key |
| `saveApiKey(userId, key)` | Store API key |
| `deleteEventsBySource(userId, source)` | Delete all "CALENDARIFIC" events, etc. |

**Auto-creates tables**: This DAO calls `initializeTables()` which creates the `FutureEvent` and `UserApiSettings` tables if they don't exist (using `CREATE TABLE IF NOT EXISTS`-style logic with Derby).

### UserSettingsDAO.java
Per-user multiplier settings.

| Method | What It Does |
|--------|-------------|
| `getUserSettings(userId)` | Get all settings |
| `getApplicableMultiplier(userId, room, season, segment)` | Find most specific matching multiplier |
| `toggleLock(settingId, locked)` | Lock/unlock a setting |
| `initializeDefaultSettings(userId)` | Create defaults: LOW=0.85, NORMAL=1.0, PEAK=1.15, SUPER_PEAK=1.35 |

**Specificity scoring** (when multiple settings could match):
```
Room type specified:    +4 points
Season specified:       +2 points
Segment specified:      +1 point

Higher score wins. Most specific setting takes priority.
```

Example: If user has settings for:
- Season=PEAK (score: 2)
- Season=PEAK + Room=Deluxe (score: 6)

When looking up Deluxe/PEAK, the second setting wins because it's more specific.

### OptimizationRequestDAO.java + OptimizationResultDAO.java
Track GA runs and their results.

```java
// Create a run record
int requestId = OptimizationRequestDAO.createRequest(userId);

// Save the results (one row per room type)
OptimizationResultDAO.saveResult(requestId, rooms, totalProfit);

// Stats for profile page
int count = OptimizationRequestDAO.getOptimizationCount(userId);
double best = OptimizationResultDAO.getBestTotalProfit(userId);
```

---

# Part 6 - Authentication & Security

## How Login Works

### The Flow

```
1. User visits any page (e.g., /homepage.jsp)
         ↓
2. AuthenticationFilter intercepts the request
         ↓
3. Filter checks: is this a public resource?
   - /login.jsp → YES → pass through
   - /homepage.jsp → NO → check session
         ↓
4. Filter checks: does session have a "user" object?
   - No → redirect to /login.jsp
   - Yes → pass through to the servlet/JSP
```

### AuthenticationFilter.java

```java
public class AuthenticationFilter implements Filter {
    public void doFilter(ServletRequest req, ...) {
        String path = getPath(request);

        // Public resources skip authentication
        if (isPublicResource(path)) {
            chain.doFilter(req, res);   // Let it through
            return;
        }

        // Check for valid session
        HttpSession session = request.getSession(false);  // false = don't create new
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("/login.jsp");  // Not logged in → go to login
            return;
        }

        chain.doFilter(req, res);  // Logged in → proceed
    }
}
```

**Public resources** (no login required):
- `login.jsp`, `register.jsp`
- `LoginServlet`, `RegisterServlet`, `LogoutServlet`
- `/css/*`, `/js/*`, `/images/*`

### LoginServlet.java

```java
protected void doPost(request, response) {
    String email = request.getParameter("email");
    String password = request.getParameter("password");

    User user = UserDAO.loginUser(email, password);  // Check credentials

    if (user != null) {
        // Session fixation protection:
        request.getSession(false)?.invalidate();  // Kill old session
        HttpSession session = request.getSession(true);  // Create new session
        session.setAttribute("user", user);  // Store user object
        response.sendRedirect("homepage.jsp");
    } else {
        response.sendRedirect("login.jsp?error=Invalid credentials");
    }
}
```

### How Servlets Extract the User

Every protected servlet starts with this pattern:

```java
HttpSession session = request.getSession(false);
if (session == null || session.getAttribute("user") == null) {
    response.sendRedirect("login.jsp?error=Please login first");
    return;
}
User user = (User) session.getAttribute("user");
int userId = user.getUserId();

// Now use userId for all data access
List<Room> rooms = RoomDataDAO.getAllRooms();
List<MonthlySeasonData> data = SeasonalityDAO.getMonthlyDataByUser(userId);
```

### Registration Validation

`RegisterServlet.java` validates:
- Username: 3+ characters, only letters/numbers/underscore
- Email: standard format (regex)
- Password: 6+ characters
- Phone: 7+ digits
- Username not taken
- Email not taken

---

# Part 7 - Servlets

Servlets are Java classes that handle HTTP requests. Each servlet has `doGet()` (for page loads) and/or `doPost()` (for form submissions).

**All servlet URL mappings are in `web.xml`** (not annotations).

## URL Mapping Reference

| URL Pattern | Servlet Class | Purpose |
|-------------|--------------|---------|
| `/LoginServlet` | LoginServlet | Process login form |
| `/RegisterServlet` | RegisterServlet | Process registration form |
| `/LogoutServlet` | LogoutServlet | End user session |
| `/RunGA` | RunGA | Run price optimization |
| `/Profile` | ProfileServlet | Load profile page data |
| `/UpdateProfileServlet` | UpdateProfileServlet | Update user info |
| `/DataImport` | DataImportServlet | Import/manage data |
| `/SegmentSettings` | MarketSegmentSettingsServlet | Manage market segments |
| `/MultiplierSettings` | MultiplierSettingsServlet | Manage price multipliers |
| `/EventSettings` | EventSettingsServlet | Manage future events |
| `/DownloadReportServlet` | DownloadReportServlet | Download Excel report |

## The Servlet Pattern

Every servlet follows this structure:

```java
public class SomeServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SomeServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.jsp?error=Please login first");
            return;
        }
        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        // 2. Load data from DAOs
        List<Something> data = SomeDAO.getData(userId);

        // 3. Set request attributes (for JSP to display)
        request.setAttribute("data", data);

        // 4. Forward to JSP
        request.getRequestDispatcher("some-page.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Same auth check
        // 2. Get form parameters
        String value = request.getParameter("fieldName");

        // 3. Validate and process
        SomeDAO.saveData(value);

        // 4. Redirect to GET (Post-Redirect-Get pattern)
        response.sendRedirect("SomeServlet?success=Saved");
    }
}
```

**Key principle**: After a POST (form submission), always **redirect** to the servlet URL, not directly to the JSP. This way the servlet's doGet runs and loads the data the JSP needs.

## RunGA.java - The Main Optimization Servlet

This is the most important servlet. Here's what happens step-by-step when a user clicks "Optimize":

```java
protected void doPost(request, response) {
    // 1. Auth check + get userId
    User user = (User) session.getAttribute("user");
    int userId = user.getUserId();

    // 2. Parse the target revenue from the form
    double expectedRevenue = Double.parseDouble(request.getParameter("expectedRevenue"));

    // 3. Load room data
    List<Room> rooms = RoomDataDAO.getAllRooms();

    // 4. Build demand curve from historical data
    List<MonthlySeasonData> monthlyData = SeasonalityDAO.getMonthlyDataByUser(userId);
    DemandCurve demandCurve = DemandCurve.fitFromData(monthlyData);

    // 5. Create optimization request record
    int requestId = OptimizationRequestDAO.createRequest(userId);

    // 6. RUN THE GENETIC ALGORITHM
    GeneticAlgorithm ga = new GeneticAlgorithm(expectedRevenue, rooms, userId, demandCurve);
    List<Room> optimizedRooms = ga.runGA();

    // 7. Calculate segment-specific prices
    SegmentPricingService sps = new SegmentPricingService();
    // ... generates pricing matrix with segment multipliers

    // 8. Generate monthly forecast with events
    EventSeasonService ess = new EventSeasonService();
    List<Map<String, Object>> forecast = ess.generateMonthlyForecast(
        optimizedRooms, userId, currentYear, demandCurve
    );

    // 9. Save results to database
    OptimizationResultDAO.saveResult(requestId, optimizedRooms, totalProfit);

    // 10. Set request attributes for JSP
    request.setAttribute("rooms", optimizedRooms);
    request.setAttribute("monthlyForecast", forecast);
    request.setAttribute("targetRevenue", expectedRevenue);
    request.setAttribute("actualRevenue", totalRevenue);

    // 11. Forward to results page
    request.getRequestDispatcher("BoostMe.jsp").forward(request, response);
}
```

## DataImportServlet.java - Multi-Action Servlet

This servlet handles multiple actions via an `action` parameter:

```java
protected void doPost(request, response) {
    String action = request.getParameter("action");

    switch (action) {
        case "importMonthly":
            // Parse CSV file → create MonthlySeasonData objects
            // Auto-classify seasons using thresholds
            // Optionally run SeasonClassifierGA for optimal thresholds
            // Batch save to SeasonalityDAO
            break;

        case "importRooms":
            // Parse Excel file → create Room objects
            // Save via RoomDataDAO.saveRoomData() (replaces all)
            break;

        case "deleteMonthly":
            // Delete selected monthly data
            break;

        case "deleteRooms":
            // Clear all room data
            break;

        case "reclassifySeasons":
            // Re-run SeasonClassifierGA on existing data
            // Update all classifications
            break;

        case "updateThresholds":
            // User manually sets season thresholds
            break;
    }
}
```

**File upload**: The `DataImportServlet` is configured in `web.xml` with `<multipart-config>` allowing file uploads up to 10MB.

## ProfileServlet.java - User Stats

Loads statistics for the profile page:

```java
protected void doGet(request, response) {
    // Calculate days active
    long daysActive = ChronoUnit.DAYS.between(user.getCreatedAt(), now);

    // Get optimization stats
    int optimizationCount = OptimizationRequestDAO.getOptimizationCount(userId);
    double bestProfit = OptimizationResultDAO.getBestTotalProfit(userId);
    Timestamp lastOptDate = OptimizationRequestDAO.getLastOptimizationDate(userId);
    List<Map<String, Object>> recentOpts = OptimizationRequestDAO.getRecentOptimizations(userId, 5);

    // Room and data stats
    int roomCount = RoomDataDAO.getTotalRoomCount();
    int roomTypeCount = RoomDataDAO.getRoomTypeCount();

    // Set all as request attributes → forward to profile.jsp
}
```

---

# Part 8 - The Genetic Algorithm

This is the heart of EzBoost. Understanding this is crucial.

## What is a Genetic Algorithm?

A GA is an optimization technique inspired by biological evolution. Instead of trying every possible solution (which would take forever), it:

1. Creates a **population** of random solutions
2. Evaluates how good each solution is (**fitness**)
3. Lets the best solutions **reproduce** (crossover)
4. Introduces random changes (**mutation**)
5. Repeats for many **generations**

Over time, solutions evolve toward optimal ones.

## How EzBoost's GA Works

### The "DNA" (Chromosome)

Each "chromosome" in the population is a **complete pricing solution**: a price for every room type in every season.

```
Chromosome example (2 room types, 4 seasons):
┌─────────────────────────────────────────────┐
│ Deluxe-LOW:   RM 175                        │
│ Deluxe-NORM:  RM 210                        │
│ Deluxe-PEAK:  RM 280                        │
│ Deluxe-SUPER: RM 350                        │
│ Standard-LOW: RM  88                        │
│ Standard-NORM:RM 105                        │
│ Standard-PEAK:RM 140                        │
│ Standard-SUPER:RM 175                       │
└─────────────────────────────────────────────┘
```

If you have N room types, each chromosome has N × 4 price values.

### Step-by-Step Walkthrough

#### Step 1: Setup

```java
// Constants
POPULATION_SIZE = 200     // 200 candidate solutions
GENERATIONS = 600         // 600 rounds of evolution
CROSSOVER_RATE = 0.85     // 85% chance parents combine
BASE_MUTATION_RATE = 0.15 // 15% base chance of random change
ELITISM_COUNT = 10        // Top 10 survive unchanged
DAYS_PER_SEASON = 91.25   // 365 ÷ 4
```

#### Step 2: Load User Multipliers

The GA first checks if the user has custom multiplier settings:

```java
// If user has custom settings:
userMultipliers = loadUserMultipliers(userId);
// e.g., {LOW: 0.85, NORMAL: 1.0, PEAK: 1.2, SUPER_PEAK: 1.4}

// If not, use defaults:
// LOW=0.85, NORMAL=1.0, PEAK=1.15, SUPER_PEAK=1.35
```

These multipliers influence where initial prices start and the price boundaries.

#### Step 3: Calculate Target Scale Ratio

The GA needs to know how "ambitious" the user's target is relative to what the hotel naturally earns:

```java
naturalRevenue = sum of (room.minADR × multiplier × occupancy × rooms × daysPerSeason)
targetScaleRatio = expectedRevenue / naturalRevenue

// Example:
// Natural revenue at base prices: RM 80,000
// Target: RM 100,000
// Ratio: 100,000/80,000 = 1.25 (need 25% more than natural)
```

This ratio dynamically adjusts the price bounds so the GA searches in the right price range.

#### Step 4: Initialize Population (200 chromosomes)

```java
for each of 200 chromosomes:
    for each room type:
        for each season:
            targetPrice = room.minADR × userMultiplier[season]
            clamp to [minPrice, maxPrice]
            add ±10% randomness
            set as this chromosome's price for this room/season
        enforce LOW < NORMAL < PEAK < SUPER_PEAK ordering
```

#### Step 5: Target-Aware Scaling

After initialization, the GA estimates revenue from the first chromosome and scales all chromosomes proportionally:

```java
sampleRevenue = calculateEstimatedRevenue(population[0])
scaleFactor = expectedRevenue / sampleRevenue

// Scale all prices: price = price × scaleFactor (clamped to bounds)
```

This ensures the population starts "in the right ballpark" rather than randomly far from the target.

#### Step 6: Evolution Loop (600 generations)

```java
for generation = 0 to 599:

    // Adaptive mutation: more exploration early, more exploitation late
    mutationRate = 0.15 + (1.0 - generation/600) × 0.25
    // Gen 0:   0.15 + 1.0 × 0.25 = 0.40 (40% — lots of exploration)
    // Gen 300: 0.15 + 0.5 × 0.25 = 0.275 (27.5%)
    // Gen 599: 0.15 + 0.001 × 0.25 ≈ 0.15 (15% — fine-tuning)

    // Sort population by fitness (best first)
    sort(population by fitness descending)

    // ELITISM: Top 10 go directly to next generation (unchanged)
    newPopulation = population[0..9]

    // Fill remaining 190 slots:
    while (newPopulation.size < 200):
        parent1 = tournamentSelection(population, size=5)
        parent2 = tournamentSelection(population, size=5)

        if (random < 0.85):
            child = arithmeticCrossover(parent1, parent2)
        else:
            child = copy of parent1

        if (random < mutationRate):
            scrambleMutation(child)

        newPopulation.add(child)

    // Track the best solution ever seen
    if (best in newPopulation > bestEverSeen):
        bestEverSeen = that chromosome

    population = newPopulation
```

#### Step 7: Selection - Tournament Selection

To pick a parent for reproduction:

```java
tournamentSelection(population, size=5):
    Pick 5 random chromosomes from population
    Return the one with the best fitness
```

This is like holding a small tournament: 5 random contestants compete, the winner gets to be a parent. This balances selecting good solutions while maintaining diversity.

#### Step 8: Crossover - Arithmetic Crossover

When two parents combine to create a child:

```java
arithmeticCrossover(parent1, parent2):
    for each room type:
        for each season:
            childPrice = (parent1.price + parent2.price) / 2.0
            clamp to [min, max]
        enforce seasonal price ordering
    return child
```

**Example:**
```
Parent 1: Deluxe-PEAK = RM 280
Parent 2: Deluxe-PEAK = RM 320
Child:    Deluxe-PEAK = (280+320)/2 = RM 300
```

This creates children that are "between" their parents, gradually converging on good solutions.

#### Step 9: Mutation - Scramble Mutation

Introduces randomness to prevent getting stuck:

```java
scrambleMutation(chromosome):
    Pick one random room type
    For each season of that room:
        price = random value between [minPrice, maxPrice]
    enforce seasonal price ordering
```

Mutation completely randomizes one room's prices, helping the GA explore new areas of the solution space.

#### Step 10: Fitness Function

```java
calculateFitness(chromosome):
    estimatedRevenue = calculateEstimatedRevenue(chromosome)
    return -abs(estimatedRevenue - expectedRevenue)
```

- Perfect fitness = 0 (revenue exactly matches target)
- Worse fitness = more negative (further from target)

**Revenue calculation (without demand curve):**
```java
for each room:
    for each season:
        revenue += price × (occupancy/100) × totalRooms × 91.25
```

**Revenue calculation (with demand curve):**
```java
for each room:
    for each season:
        occupancy = demandCurve.getOccupancy(price)  // Price affects demand!
        revenue += price × (occupancy/100) × totalRooms × 91.25
```

The demand curve version is more realistic because raising price lowers occupancy.

#### Step 11: Local Search (forceExactTarget)

After 600 generations, the GA's best solution is *close* to the target but not exact. `forceExactTarget()` fine-tunes it:

**Without demand curve (simple case):**
```java
// Direct proportional scaling
requiredScale = targetRevenue / currentRevenue
for each room, season:
    price = price × requiredScale
```

**With demand curve (complex case — iterative):**
```java
for up to 5000 iterations:
    error = targetRevenue - currentRevenue
    if (error < 0.1%): stop  // Close enough

    // Find the single room/season adjustment with biggest impact
    for each room, season:
        test adjusting price up/down by a small step
        calculate the revenue impact (considering demand curve)
        pick the adjustment with highest impact toward target

    apply that best single adjustment
```

The iterative version is needed because with a demand curve, raising price doesn't always increase revenue (it could lower occupancy so much that revenue drops).

#### Step 12: Enforce Seasonal Price Ordering

After every operation, the GA ensures:

```
LOW price < NORMAL price < PEAK price < SUPER_PEAK price
```

With at least RM 1 gap between each. This is a business rule: it wouldn't make sense to charge more during low season than peak season.

### Achievable Range Check

Before running, the GA calculates the minimum and maximum revenue achievable:

```
Minimum: every room at minimum price in every season
Maximum: every room at maximum price (or optimal demand curve price)
```

If the user's target is outside this range, the GA logs a warning but still tries its best.

## Numerical Example

```
Setup:
  Room types: Deluxe (min RM200, max RM400, 75% occupancy, 30 rooms)
              Standard (min RM100, max RM250, 80% occupancy, 50 rooms)
  Target revenue: RM 100,000
  User multipliers: defaults (LOW=0.85, NORMAL=1.0, PEAK=1.15, SUPER_PEAK=1.35)

Natural Revenue Calculation:
  Deluxe:   200 × 0.85 × 0.75 × 30 × 91.25 = RM 349,256 (LOW)
            200 × 1.00 × 0.75 × 30 × 91.25 = RM 410,625 (NORMAL)
            200 × 1.15 × 0.75 × 30 × 91.25 = RM 472,219 (PEAK)
            200 × 1.35 × 0.75 × 30 × 91.25 = RM 554,344 (SUPER_PEAK)
  Standard: 100 × 0.85 × 0.80 × 50 × 91.25 = RM 310,250 (LOW)
            ... and so on

Population: 200 random pricing solutions
Evolution: 600 generations of selection + crossover + mutation

Result:
  Deluxe-LOW:    RM 210    Standard-LOW:    RM 105
  Deluxe-NORMAL: RM 248    Standard-NORMAL: RM 124
  Deluxe-PEAK:   RM 285    Standard-PEAK:   RM 143
  Deluxe-SUPER:  RM 334    Standard-SUPER:  RM 168
  Total estimated revenue: RM 100,012 (99.99% accuracy)
```

---

# Part 9 - Demand Curve & Season Classification

## DemandCurve.java - How Price Affects Demand

### The Core Idea

In reality, when you raise room prices, fewer people book. The DemandCurve models this relationship:

```
occupancy = intercept + slope × price
```

Where:
- **intercept** = occupancy when price is 0 (theoretical maximum occupancy)
- **slope** = negative number (how much occupancy drops per RM increase)

### Example

```
Fitted curve: occupancy = 120 - 0.1 × price

At RM 200:  occupancy = 120 - 0.1 × 200 = 100% → clamped to 98%
At RM 400:  occupancy = 120 - 0.1 × 400 = 80%
At RM 600:  occupancy = 120 - 0.1 × 600 = 60%
At RM 1000: occupancy = 120 - 0.1 × 1000 = 20%
At RM 1200: occupancy = 120 - 0.1 × 1200 = 0% → clamped to 5%
```

Occupancy is always clamped to [5%, 98%] for realism.

### How It's Fitted

The curve is fitted from the user's historical data using **least-squares linear regression**:

```
Given N months of data, each with (avgRoomRate, occupancyRate):

sumX  = sum of all prices
sumY  = sum of all occupancies
sumXX = sum of (price × price)
sumXY = sum of (price × occupancy)

slope = (N × sumXY - sumX × sumY) / (N × sumXX - sumX²)
intercept = meanY - slope × meanX
```

**Validation rules:**
- Need at least 3 valid data points (positive price and occupancy)
- Slope must be negative (higher price → lower occupancy)
- If validation fails, use the **default curve**: intercept=100, slope=-0.05

**R-squared** (R²) measures how well the line fits the data:
- R² = 1.0 → perfect fit
- R² = 0.0 → no correlation
- R² > 0.7 → generally considered a good fit

### The Optimal Price

Revenue per room-night is:

```
Revenue = price × (occupancy / 100)
        = price × (intercept + slope × price) / 100
        = (intercept × price + slope × price²) / 100
```

This is a downward-opening parabola (since slope < 0). The maximum is at:

```
d(Revenue)/d(price) = 0
intercept + 2 × slope × price = 0
optimal_price = -intercept / (2 × slope)
```

**Example:**
```
intercept = 120, slope = -0.1
optimal_price = -120 / (2 × -0.1) = 120/0.2 = RM 600

At RM 600: occupancy = 60%, revenue = 600 × 0.60 = RM 360/room-night
At RM 500: occupancy = 70%, revenue = 500 × 0.70 = RM 350/room-night  (less!)
At RM 700: occupancy = 50%, revenue = 700 × 0.50 = RM 350/room-night  (less!)
```

The optimal price is where revenue is maximized — going higher or lower both reduce revenue.

### Default Curve

When there's not enough historical data:
```
intercept = 100, slope = -0.05
Meaning: At RM 400 → 80% occupancy, at RM 1000 → 50% occupancy
Optimal price: -100 / (2 × -0.05) = RM 1000
```

## SeasonClassifierGA.java - Auto-Classifying Seasons

### The Problem

The default thresholds (65%, 75%, 85%) might not fit every hotel. A boutique hotel in a tourist area might have overall high occupancy, making everything look like "PEAK" or "SUPER_PEAK". The SeasonClassifierGA finds the best thresholds for your specific data.

### How It Works

The SeasonClassifierGA is itself a genetic algorithm (a smaller one) that optimizes three threshold values [T1, T2, T3]:

```
occupancy < T1          → LOW
T1 ≤ occupancy < T2     → NORMAL
T2 ≤ occupancy < T3     → PEAK
occupancy ≥ T3          → SUPER_PEAK
```

**Parameters:**
```
Population: 50 (smaller than main GA)
Generations: 200 (fewer needed)
Crossover rate: 80%
Mutation rate: 30% (higher for exploration)
Constraint: T1 < T2 < T3, minimum 3% spacing
```

**Fitness function (what makes good thresholds):**

```
40% Distribution Balance:
  - Each season should have roughly equal months
  - Penalize if any season is empty (-100 per empty season)
  - Lower variance in counts = better

40% Natural Clustering:
  - Thresholds should fall at natural "gaps" in the data
  - If occupancy values jump from 72% to 78%, that's a natural boundary

20% Threshold Spacing:
  - T1-T2 and T2-T3 should be roughly equal (even spacing)
  - Minimum 5% spacing between thresholds
```

**Example:**
```
Historical data occupancy rates: [52, 55, 58, 61, 63, 72, 74, 78, 81, 83, 88, 91, 95]

Default thresholds: [65, 75, 85]
  LOW: [52, 55, 58, 61, 63]     → 5 months
  NORMAL: [72, 74]              → 2 months
  PEAK: [78, 81, 83]            → 3 months
  SUPER_PEAK: [88, 91, 95]      → 3 months

SeasonClassifierGA optimized: [64, 76, 87]
  LOW: [52, 55, 58, 61, 63]     → 5 months
  NORMAL: [72, 74]              → 2 months
  PEAK: [78, 81, 83]            → 3 months
  SUPER_PEAK: [88, 91, 95]      → 3 months
  (Similar but potentially better aligned to natural clusters)
```

---

# Part 10 - Services

Services contain business logic that doesn't belong in servlets or DAOs.

## SegmentPricingService.java - Segment Price Calculation

**What it does**: Takes the GA's optimized base prices and applies market segment multipliers.

```java
// Input: Rooms with optimized seasonal prices + userId
// Output: A pricing matrix showing each segment's price

calculateSegmentPrices(rooms, userId):
    segments = MarketSegmentDAO.getAllSegments(userId)

    for each room:
        for each season:
            basePrice = room.getSeasonalPrice(season)
            for each segment:
                segmentPrice = basePrice × segment.rateMultiplier
                create SegmentPricingResult(basePrice, multiplier, segmentPrice)

    return pricingMatrix
```

**Example output:**
```
Room: Deluxe, Season: PEAK, Base Price: RM 280

Segment         | Multiplier | Price
OTA             | 1.20x      | RM 336
Walk-in         | 1.15x      | RM 322
Direct Website  | 1.00x      | RM 280
Corporate       | 1.05x      | RM 294
Government      | 0.85x      | RM 238
Tour Group      | 0.80x      | RM 224
```

## EventSeasonService.java - Monthly Forecast Generation

**What it does**: Creates a 12-month forecast that combines:
1. Historical season patterns (which months are typically LOW/NORMAL/PEAK/SUPER_PEAK)
2. Future events (holidays that boost the season)
3. GA-optimized prices for each season

### The Process

```java
generateMonthlyForecast(rooms, userId, year, demandCurve):

    // Step 1: Determine base season for each calendar month
    baseSeason = getBaseSeasonByMonth(userId)
    // This groups all historical data by month number (1-12)
    // and picks the most frequent season classification
    //
    // Example:
    //   All January data: LOW(3), NORMAL(1) → January = LOW
    //   All July data: SUPER_PEAK(2), PEAK(2) → July = SUPER_PEAK (or PEAK, by most common)

    // Step 2: For each month of the target year
    for month = 1 to 12:

        // Get events that overlap this month
        events = FutureEventDAO.getEventsByMonth(userId, year, month)

        // Start with base season
        adjustedSeason = baseSeason[month]

        // Apply event overrides (only bump UP, never down)
        for each event:
            if event.seasonOverride ranks higher than adjustedSeason:
                adjustedSeason = event.seasonOverride

        // Get prices for the adjusted season
        roomPrices = {}
        for each room:
            roomPrices[room.name] = room.getSeasonalPrice(adjustedSeason)

        // Build forecast entry
        entry = {
            month: "January",
            baseSeason: "NORMAL",
            events: [...],
            adjustedSeason: "PEAK",      // Bumped by Chinese New Year
            seasonChanged: true,
            roomPrices: {Deluxe: 280, Standard: 143}
        }

    return 12 forecast entries
```

### Season Hierarchy (only bumps UP)

```
LOW (0) < NORMAL (1) < PEAK (2) < SUPER_PEAK (3)
```

If January is naturally NORMAL (rank 1) and Chinese New Year overrides to SUPER_PEAK (rank 3), the adjusted season is SUPER_PEAK.

If July is naturally SUPER_PEAK (rank 3) and an event overrides to PEAK (rank 2), the adjusted season stays SUPER_PEAK (no downgrade).

## CalendarificService.java - Holiday API

Fetches public holidays from the Calendarific API (external service):

```java
fetchHolidays(apiKey, countryCode, year):
    // Makes HTTP GET to Calendarific API
    // Parses JSON response
    // Converts to List<FutureEvent> objects
    // Sets source = "CALENDARIFIC"
    // Sets eventType based on holiday type
    return events
```

The API key is stored per-user in the database via `FutureEventDAO.saveApiKey()`.

## ExcelExportService.java - Report Generation

Creates multi-sheet XLSX workbooks using Apache POI:

```
Sheet 1: Summary (target revenue, actual revenue, accuracy)
Sheet 2: Room Prices (per room type, per season)
Sheet 3: Segment Pricing (per segment, per room, per season)
Sheet 4: Monthly Forecast (12-month view with events)
```

---

# Part 11 - The Frontend

## JSP Technology

JSPs (JavaServer Pages) are HTML templates with embedded Java/JSTL. The server processes them into pure HTML before sending to the browser.

### JSTL Tags Used

```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>      <!-- Core: if, forEach, set -->
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>       <!-- Format: numbers, dates -->
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>  <!-- Functions: length, contains -->
```

**Common patterns:**

```jsp
<!-- Conditional display -->
<c:if test="${not empty error}">
    <div class="alert-error">${error}</div>
</c:if>

<!-- Loop over a list -->
<c:forEach var="room" items="${rooms}">
    <tr>
        <td>${room.name}</td>
        <td><fmt:formatNumber value="${room.getBaseAdr()}" type="number" maxFractionDigits="2"/></td>
    </tr>
</c:forEach>

<!-- Access session objects -->
<span>Welcome, ${sessionScope.user.firstName}</span>
```

### Expression Language (EL)

`${expression}` accesses data set by servlets:
- `${rooms}` → `request.getAttribute("rooms")`
- `${sessionScope.user}` → `session.getAttribute("user")`
- `${param.error}` → `request.getParameter("error")`

## Page Reference

### nav.jsp (Shared Navigation)
Included by every page via `<%@ include file="nav.jsp" %>`. Contains:
- Logo link to homepage
- Navigation links: About, BoostMe, Profile, Multipliers, Segments, Events, Import
- Logout button

### login.jsp
Login form with email/password. Shows error/success messages via URL parameters.

### register.jsp
Registration form with all user fields. Client-side validation + server-side validation in RegisterServlet.

### homepage.jsp
Main dashboard after login. Entry point to the system.

### profile.jsp
User profile with stats:
- Days active, total optimizations, best profit
- Recent optimization history (last 5)
- Edit profile form

### BoostMe.jsp
**The most complex JSP.** Displays optimization results:
- Revenue target vs actual with accuracy percentage
- Per-room seasonal pricing table
- Segment pricing breakdown
- 12-month forecast with event indicators
- Download report button

**Note**: This JSP still contains some embedded Java (scriptlets) for complex data processing that hasn't been converted to JSTL yet.

### data-import.jsp
Data management page:
- Upload CSV for monthly data
- Upload Excel for room data
- View/delete existing data
- Reclassify seasons
- Manually update thresholds

### multiplier-settings.jsp
Configure per-season price multipliers:
- View current multipliers per season
- Adjust multiplier values
- Lock/unlock multipliers (locked = GA can't change)

### segment-settings.jsp
Manage market segments:
- View all segments with multipliers
- Edit segment multipliers
- Add/delete segments
- Reset to defaults

### event-settings.jsp
Manage future events:
- Fetch holidays from Calendarific API (requires API key)
- Manually add custom events
- View/delete existing events
- Set season override per event

## CSS Architecture

### theme.css - Design Tokens
Defines the visual language for the entire app:
```css
:root {
    --color-cream: #f5f0e8;      /* Background */
    --color-dark: #1a1a1a;        /* Text */
    --color-green: #22c55e;       /* Accent/Success */
    /* ... many more variables */
}
```

Google Fonts:
- **DM Serif Display** - Headings (elegant serif)
- **Inter** - Body text (clean sans-serif)
- **JetBrains Mono** - Code/numbers (monospace)

### styles.css - Base Layout
Navbar styling, sticky header, base responsive layout.

### settings.css - Settings Pages
Shared styles for all four settings pages (multiplier, segment, event, data-import). Extracted from inline styles to reduce duplication.

### Page-Specific CSS
`login.css`, `register.css`, `homepage.css`, `profile.css`, `result.css`, `about.css` — each handles styling for its respective page.

---

# Part 12 - Putting It All Together

## Complete User Journey

### Scenario: New Hotel Manager Sets Up EzBoost

**Day 1: Account Setup**
```
1. Visit the app → redirected to login.jsp (welcome file)
2. Click "Register" → register.jsp
3. Fill form: name, email, password → POST /RegisterServlet
4. RegisterServlet validates, calls UserDAO.registerUser()
5. Redirect to login.jsp with success message
6. Login with email/password → POST /LoginServlet
7. LoginServlet creates session with User object
8. Redirect to homepage.jsp
```

**Day 1: Import Historical Data**
```
1. Navigate to "Import" → GET /DataImport → data-import.jsp
2. Upload monthly_data.csv with 24 months of history
3. POST /DataImport?action=importMonthly
4. DataImportServlet:
   a. CSVImportUtil.parseCSV() → List<MonthlySeasonData>
   b. Each row auto-classified (occupancy → LOW/NORMAL/PEAK/SUPER_PEAK)
   c. SeasonalityDAO.batchSaveMonthlyData() → saved to database
   d. SeasonClassifierGA runs → finds optimal thresholds [62, 74, 86]
   e. SeasonalityDAO.saveThresholds() → store optimized thresholds
   f. SeasonalityDAO.reclassifyAllSeasons() → re-classify with new thresholds
5. Upload room_data.xlsx with room types
6. POST /DataImport?action=importRooms
7. DataImportServlet:
   a. RoomDataImportUtil parses Excel → List<Room>
   b. RoomDataDAO.saveRoomData() → stored (replaces any existing)
8. Redirect to data-import.jsp with success message
```

**Day 2: Configure Pricing Settings**
```
1. Navigate to "Segments" → GET /SegmentSettings
2. MarketSegmentSettingsServlet:
   a. First visit: MarketSegmentDAO.initializeDefaultSegments(userId)
   b. Creates 8 default segments (OTA 1.20x, Walk-in 1.15x, etc.)
   c. Forward to segment-settings.jsp
3. Adjust OTA multiplier from 1.20 to 1.25
4. POST /SegmentSettings → MarketSegmentDAO.saveSegment()

5. Navigate to "Multipliers" → GET /MultiplierSettings
6. MultiplierSettingsServlet:
   a. First visit: UserSettingsDAO.initializeDefaultSettings(userId)
   b. Creates 4 defaults (LOW=0.85, NORMAL=1.0, PEAK=1.15, SUPER_PEAK=1.35)
   c. Forward to multiplier-settings.jsp
7. Set SUPER_PEAK to 1.45 (more aggressive peak pricing)
8. POST /MultiplierSettings → UserSettingsDAO.saveOrUpdateSetting()

9. Navigate to "Events" → GET /EventSettings
10. Enter Calendarific API key, fetch holidays for 2025
11. EventSettingsServlet:
    a. CalendarificService.fetchHolidays() → List<FutureEvent>
    b. FutureEventDAO.batchSaveEvents()
12. Add custom event: "Company Conference" July 15-18, PEAK override
```

**Day 3: Run Optimization**
```
1. Navigate to "BoostMe" → BoostMe.jsp
2. Enter target revenue: RM 500,000
3. Click "Optimize" → POST /RunGA

4. RunGA servlet orchestrates everything:

   a. Load rooms: RoomDataDAO.getAllRooms()
      → [Deluxe(200-400, 75%, 30), Standard(100-250, 80%, 50), Suite(350-700, 60%, 10)]

   b. Build demand curve: DemandCurve.fitFromData(monthlyData)
      → occupancy = 115 - 0.08 × price (R² = 0.82)
      → Optimal price: RM 718.75

   c. Create GA: new GeneticAlgorithm(500000, rooms, userId, demandCurve)
      → Loads user multipliers: {LOW:0.85, NORMAL:1.0, PEAK:1.15, SUPER_PEAK:1.45}

   d. Run GA evolution:
      → 200 chromosomes × 600 generations
      → Tournament selection → arithmetic crossover → scramble mutation
      → Adaptive mutation rate: 40% → 15%
      → Best solution tracked across all generations

   e. Local search (forceExactTarget):
      → Iteratively adjusts prices to hit RM 500,000 exactly
      → Respects demand curve (knows price↔occupancy relationship)

   f. Calculate segment prices:
      → For each room × season × segment: segmentPrice = basePrice × multiplier

   g. Generate monthly forecast:
      → Map months to base seasons (from historical data)
      → Apply event overrides (Chinese New Year → SUPER_PEAK in February)
      → Calculate per-month prices per room

   h. Save results:
      → OptimizationRequestDAO.createRequest(userId)
      → OptimizationResultDAO.saveResult(requestId, rooms, totalProfit)

   i. Forward to BoostMe.jsp with all data

5. User sees:
   ┌─────────────────────────────────────────────────┐
   │ Target Revenue: RM 500,000                      │
   │ Actual Revenue: RM 500,003  (99.999% accuracy)  │
   ├─────────────────────────────────────────────────┤
   │ Room Pricing Table:                             │
   │              LOW    NORMAL   PEAK   SUPER_PEAK  │
   │ Deluxe      RM 225  RM 268  RM 312  RM 398     │
   │ Standard    RM 112  RM 134  RM 156  RM 199     │
   │ Suite       RM 410  RM 488  RM 568  RM 725     │
   ├─────────────────────────────────────────────────┤
   │ Segment Pricing (Deluxe, PEAK):                 │
   │ OTA: RM 390  Walk-in: RM 359  Web: RM 312     │
   │ Corp: RM 328  Gov: RM 265  Tour: RM 250       │
   ├─────────────────────────────────────────────────┤
   │ Monthly Forecast:                               │
   │ Jan: NORMAL (RM 268/268/488)                   │
   │ Feb: SUPER_PEAK ⚡ Chinese New Year             │
   │ Mar: PEAK                                       │
   │ ...                                             │
   └─────────────────────────────────────────────────┘

6. Click "Download Report" → GET /DownloadReportServlet → Excel file
```

## Key Data Flow Diagram

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│  CSV Upload  │────→│ CSVImportUtil│────→│  MonthlySeasonData│
│ (monthly     │     │ (parsing)    │     │  (model objects)  │
│  history)    │     └──────────────┘     └────────┬─────────┘
└──────────────┘                                   │
                                                   ▼
                                          ┌────────────────────┐
                                          │  SeasonalityDAO    │
                                          │  (save to database)│
                                          └────────┬───────────┘
                                                   │
                              ┌─────────────────────┼─────────────────────┐
                              ▼                     ▼                     ▼
                    ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐
                    │ SeasonClassifier  │  │  DemandCurve     │  │EventSeason   │
                    │ GA               │  │  .fitFromData()  │  │Service       │
                    │ (find thresholds)│  │ (price→occupancy)│  │(base seasons)│
                    └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘
                             │                     │                   │
                             ▼                     ▼                   │
                    ┌──────────────────┐  ┌──────────────────┐         │
                    │ Reclassify all   │  │  GeneticAlgorithm│         │
                    │ seasons with     │  │  .runGA()        │         │
                    │ new thresholds   │  │  (optimize prices)│        │
                    └──────────────────┘  └────────┬─────────┘         │
                                                   │                   │
                                                   ▼                   │
                                          ┌──────────────────┐         │
                                          │ Optimized Rooms  │         │
                                          │ (seasonal prices)│         │
                                          └────┬──────────┬──┘         │
                                               │          │            │
                                               ▼          ▼            ▼
                                    ┌─────────────┐ ┌────────────────────┐
                                    │ Segment     │ │ Monthly Forecast   │
                                    │ Pricing     │ │ (rooms + events +  │
                                    │ Service     │ │  adjusted seasons) │
                                    └──────┬──────┘ └─────────┬──────────┘
                                           │                  │
                                           ▼                  ▼
                                    ┌──────────────────────────────────┐
                                    │          BoostMe.jsp             │
                                    │  (displays all results to user) │
                                    └──────────────────────────────────┘
```

---

# Appendix A - Configuration Reference

## web.xml Mappings

```xml
<!-- Filter: AuthenticationFilter → /* (all URLs) -->

<!-- Servlets -->
/LoginServlet           → com.ezboost.servlet.LoginServlet
/RegisterServlet        → com.ezboost.servlet.RegisterServlet
/LogoutServlet          → com.ezboost.servlet.LogoutServlet
/RunGA                  → com.ezboost.servlet.RunGA
/Profile                → com.ezboost.servlet.ProfileServlet
/UpdateProfileServlet   → com.ezboost.servlet.UpdateProfileServlet
/DataImport             → com.ezboost.servlet.DataImportServlet
/SegmentSettings        → com.ezboost.servlet.MarketSegmentSettingsServlet
/MultiplierSettings     → com.ezboost.servlet.MultiplierSettingsServlet
/EventSettings          → com.ezboost.servlet.EventSettingsServlet
/DownloadReportServlet  → com.ezboost.servlet.DownloadReportServlet

<!-- Error Pages -->
404 → /error-404.jsp
500 → /error-500.jsp

<!-- Welcome File: login.jsp -->
```

## DBConnection Settings

| Setting | Value | Meaning |
|---------|-------|---------|
| JDBC URL | `jdbc:derby://localhost:1527/ezboost_db` | Derby server location |
| Username | `app` | Database user |
| Password | `app` | Database password |
| Driver | `org.apache.derby.client.ClientAutoloadedDriver` | JDBC driver class |
| Max Pool Size | 10 | Maximum simultaneous connections |
| Min Idle | 2 | Minimum connections kept ready |
| Connection Timeout | 30,000ms | Max wait for a connection |
| Idle Timeout | 600,000ms (10 min) | Close idle connections after |
| Max Lifetime | 1,800,000ms (30 min) | Recycle connections after |

## GA Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| POPULATION_SIZE | 200 | Number of candidate solutions |
| GENERATIONS | 600 | Evolution iterations |
| CROSSOVER_RATE | 0.85 | Probability of combining parents |
| BASE_MUTATION_RATE | 0.15 | Base probability of random change |
| ELITISM_COUNT | 10 | Top solutions preserved unchanged |
| DAYS_PER_SEASON | 91.25 | 365/4 days |
| MIN_PRICE_SCALE | 0.85 | Lower bound multiplier |
| MAX_PRICE_SCALE | 1.25 | Upper bound multiplier |
| DEFAULT_LOW_MULTIPLIER | 0.85 | Default LOW season multiplier |
| DEFAULT_NORMAL_MULTIPLIER | 1.0 | Default NORMAL season multiplier |
| DEFAULT_PEAK_MULTIPLIER | 1.15 | Default PEAK season multiplier |
| DEFAULT_SUPER_PEAK_MULTIPLIER | 1.35 | Default SUPER_PEAK season multiplier |

---

# Appendix B - Database Table Reference

## USER
| Column | Type | Notes |
|--------|------|-------|
| USERID | INT (PK) | Auto-generated |
| FIRSTNAME | VARCHAR | |
| LASTNAME | VARCHAR | |
| USERNAME | VARCHAR | Unique |
| EMAIL | VARCHAR | Unique |
| PASSWORD | VARCHAR | Plain text |
| PHONENUMBER | VARCHAR | |
| CREATED_AT | TIMESTAMP | Auto-set |

## ActualRoomData
| Column | Type | Notes |
|--------|------|-------|
| RoomID | INT (PK) | Auto-generated |
| RoomType | VARCHAR | e.g., "Deluxe King" |
| MinADR | DOUBLE | Minimum rate |
| MaxADR | DOUBLE | Maximum rate |
| Occupancy | DOUBLE | Default occupancy % |
| NumberOfRoom | INT | Physical room count |

## MonthlySeasonData
| Column | Type | Notes |
|--------|------|-------|
| DataID | INT (PK) | Auto-generated |
| UserID | INT (FK) | Per-user |
| MonthYear | VARCHAR | "2024-01" format |
| OccupancyRate | DOUBLE | 0-100 |
| TotalRevenue | DOUBLE | |
| AvgRoomRate | DOUBLE | |
| ClassifiedSeason | VARCHAR | LOW/NORMAL/PEAK/SUPER_PEAK |
| ImportDate | TIMESTAMP | |

## SeasonThreshold
| Column | Type | Notes |
|--------|------|-------|
| ThresholdID | INT (PK) | Auto-generated |
| UserID | INT (FK) | Per-user |
| ThresholdLowNormal | DOUBLE | Default: 65.0 |
| ThresholdNormalPeak | DOUBLE | Default: 75.0 |
| ThresholdPeakSuperPeak | DOUBLE | Default: 85.0 |
| IsAutoGenerated | BOOLEAN | true = from SeasonClassifierGA |

## MarketSegment
| Column | Type | Notes |
|--------|------|-------|
| id | INT (PK) | Auto-generated |
| user_id | INT (FK) | Per-user |
| segment_name | VARCHAR | "Online Travel Agent" |
| segment_code | VARCHAR | "OTA" |
| category | VARCHAR | "FIT" or "GIT" |
| rate_multiplier | DOUBLE | 0.5-2.0 |
| active | BOOLEAN | Soft delete flag |

## FutureEvent
| Column | Type | Notes |
|--------|------|-------|
| event_id | INT (PK) | Auto-generated |
| user_id | INT (FK) | Per-user |
| event_name | VARCHAR | |
| event_date | DATE | Start date |
| event_end_date | DATE | End date (nullable) |
| event_type | VARCHAR | PUBLIC_HOLIDAY/SCHOOL_BREAK/CUSTOM |
| season_override | VARCHAR | PEAK or SUPER_PEAK |
| source | VARCHAR | CALENDARIFIC/PRESET/MANUAL |
| active | BOOLEAN | |

## UserMultiplierSettings
| Column | Type | Notes |
|--------|------|-------|
| SettingID | INT (PK) | Auto-generated |
| UserID | INT (FK) | Per-user |
| RoomType | VARCHAR | Nullable (null = all rooms) |
| SeasonName | VARCHAR | LOW/NORMAL/PEAK/SUPER_PEAK |
| SegmentName | VARCHAR | Nullable (null = all segments) |
| CustomMultiplier | DOUBLE | The multiplier value |
| MinBound | DOUBLE | Default: 0.5 |
| MaxBound | DOUBLE | Default: 2.0 |
| IsLocked | BOOLEAN | If true, GA won't modify |

## UserApiSettings
| Column | Type | Notes |
|--------|------|-------|
| setting_id | INT (PK) | Auto-generated |
| user_id | INT (FK) | Per-user |
| setting_key | VARCHAR | e.g., "calendarific_api_key" |
| setting_value | VARCHAR | The API key value |

## OptimizationRequest
| Column | Type | Notes |
|--------|------|-------|
| RequestID | INT (PK) | Auto-generated |
| UserID | INT (FK) | Per-user |
| RequestDate | TIMESTAMP | When the GA was run |

## OptimizationResult
| Column | Type | Notes |
|--------|------|-------|
| ResultID | INT (PK) | Auto-generated |
| RequestID | INT (FK) | Links to OptimizationRequest |
| RoomType | VARCHAR | |
| LowSeasonPrice | DOUBLE | |
| NormalSeasonPrice | DOUBLE | |
| PeakSeasonPrice | DOUBLE | |
| SuperPeakSeasonPrice | DOUBLE | |
| TotalEstimatedProfit | DOUBLE | |

---

# Appendix C - Build & Test Commands

```bash
# Compile only (fast check for syntax errors)
mvn compile

# Run all 25 tests
mvn test

# Run a specific test class
mvn test -Dtest=RoomTest

# Run a specific test method
mvn test -Dtest=DemandCurveTest#testDefaultCurve

# Full build (compile + test + package WAR)
mvn clean package

# Output: target/EzBoost-1.0-SNAPSHOT.war
```

## Existing Tests

### RoomTest.java (11 tests)
- Constructor sets fields correctly
- `getBaseAdr()` calculates midpoint
- Seasonal prices are generated on construction
- Occupancy clamping (negative → 0, over 100 → 100)
- Per-season occupancies work with fallback

### SeasonTest.java (5 tests)
- All 4 enum values exist
- Correct ordering (LOW → NORMAL → PEAK → SUPER_PEAK)
- Each season's multipliers increase progressively

### DemandCurveTest.java (9 tests)
- Default curve creation
- `getOccupancy()` clamping to [5%, 98%]
- `getOptimalPrice()` calculation
- `fitFromData()` with valid data
- Fallback behavior with insufficient data
- Fallback behavior with positive slope

---

# Appendix D - Glossary

| Term | Definition |
|------|-----------|
| **ADR** | Average Daily Rate — the average price per room per night |
| **Base ADR** | Midpoint between min and max ADR: (min+max)/2 |
| **Chromosome** | In GA terminology, one candidate solution (a complete set of prices) |
| **Crossover** | GA operation: combining two parent solutions to create a child |
| **DAO** | Data Access Object — class that handles database queries |
| **Demand Curve** | Mathematical model: how occupancy changes with price |
| **Elitism** | GA technique: best solutions survive unchanged to next generation |
| **FIT** | Free Independent Traveler — individual guests (not groups) |
| **Fitness** | How good a solution is (closer to target revenue = better) |
| **GA** | Genetic Algorithm — optimization technique inspired by evolution |
| **Generation** | One iteration of the GA evolution loop |
| **GIT** | Group Inclusive Tour — group bookings (corporate, tour, government) |
| **HikariCP** | High-performance JDBC connection pool library |
| **JSP** | JavaServer Pages — HTML template technology for Java web apps |
| **JSTL** | JSP Standard Tag Library — standard tags for common JSP operations |
| **Mutation** | GA operation: random change to a solution for diversity |
| **OTA** | Online Travel Agent (e.g., Booking.com, Expedia) |
| **POJO** | Plain Old Java Object — simple data class with fields and getters |
| **Population** | In GA, the full set of candidate solutions (200 chromosomes) |
| **R-squared (R²)** | Statistical measure of how well the demand curve fits the data (0-1) |
| **Season Override** | An event that temporarily bumps a month to a higher season |
| **Segment** | A type of customer channel (OTA, walk-in, corporate, etc.) |
| **Selection** | GA operation: choosing parents for reproduction (tournament style) |
| **Servlet** | Java class that handles HTTP requests |
| **Session** | Server-side storage tied to a user's browser session |
| **Tournament Selection** | Pick N random candidates, keep the best one |
| **WAR** | Web Application Archive — deployable package for Java web apps |

---

*This document covers the complete EzBoost codebase as of February 2026. For the most up-to-date information, always refer to the source code itself.*
