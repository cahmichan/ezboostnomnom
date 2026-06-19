# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
mvn compile          # Compile only
mvn test             # Run JUnit 5 tests (25 tests across 3 files)
mvn clean package    # Full build → target/*.war
```

No linter configured. No single-test runner shortcut — use `mvn test -Dtest=ClassName#methodName` for individual tests.

## Tech Stack

- **Java 11**, Maven WAR, **Jakarta EE 10** (Servlets, JSP, JSTL 3.0)
- **Apache Derby 10.16** in client mode (`localhost:1527`, db=`ezboost_db`, user/pass=`app/app`)
- **HikariCP 5.1.0** connection pool (configured in `DBConnection.java`)
- **Apache POI 5.2.5** for Excel import/export
- **SLF4J 2.0.9** (api + slf4j-simple) for all logging
- **JUnit 5.10.1** + **Mockito 5.8.0** for tests
- Deployed on **Payara/GlassFish** application server

## Architecture

### Package Layout (`com.ezboost.*`)

| Package | Role |
|---------|------|
| `model` | POJOs + `Season` enum (LOW/NORMAL/PEAK/SUPER_PEAK) |
| `dao` | Static data access methods, all use `DBConnection.getConnection()` with try-with-resources |
| `servlet` | HTTP handlers — all mapped in `web.xml` (not `@WebServlet`), except `DownloadReportServlet` |
| `ga` | `GeneticAlgorithm`, `DemandCurve`, `SeasonClassifierGA` — pricing optimization core |
| `service` | `CalendarificService`, `EventSeasonService`, `ExcelExportService`, `SegmentPricingService` |
| `util` | `DBConnection`, `AuthenticationFilter`, CSV/Excel import utilities |

### Request Flow

1. **AuthenticationFilter** (`util/`) intercepts all requests (`/*`)
2. Public resources bypass: `login.jsp`, `register.jsp`, `LoginServlet`, `RegisterServlet`, `LogoutServlet`, `/css/*`, `/js/*`, `/images/*`
3. Authenticated requests proceed to servlet → servlet sets request attributes → forwards to JSP

### Session & Per-User Data

Session stores a `User` object. Every servlet extracts userId the same way:
```java
User user = (User) session.getAttribute("user");
int userId = user.getUserId();
```
All DAO methods that touch user-owned data accept `userId` and filter with `WHERE user_id = ?`.

### DAO Pattern

All DAOs use **static methods** with pooled connections:
```java
public static List<Thing> getThings(int userId) {
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, userId);
        // ...
    } catch (SQLException e) {
        logger.error("...", e.getMessage(), e);
    }
}
```
Batch operations use manual `Connection` management with explicit rollback/close.

### Servlet Pattern

Servlets check session, extract userId, delegate to DAOs, set request attributes, forward to JSP:
```java
protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    HttpSession session = req.getSession(false);
    if (session == null || session.getAttribute("user") == null) {
        resp.sendRedirect("login.jsp?error=Please login first");
        return;
    }
    User user = (User) session.getAttribute("user");
    // ... load data via DAO, set attributes, forward to JSP
}
```
After successful mutations, servlets should redirect to the **servlet URL** (e.g., `Profile`) not directly to the JSP, so the servlet can load required request attributes.

### GA Optimization Flow

`RunGA` servlet → loads rooms from `RoomDataDAO` → builds `DemandCurve` from `SeasonalityDAO` data → runs `GeneticAlgorithm` → stores results in session → `EventSeasonService` generates 12-month forecast with event-based season overrides → forward to `BoostMe.jsp`.

Revenue formula: `Price × f(Price) × Days × Rooms` where `f(Price)` is the demand curve (quadratic with genuine optimum).

### JSP Conventions

- Use **JSTL/EL** (`<c:if>`, `<c:forEach>`, `${expr}`), not scriptlets — except `BoostMe.jsp` which still has complex embedded Java
- Taglib URIs: `jakarta.tags.core`, `jakarta.tags.fmt`, `jakarta.tags.functions`
- All pages include `nav.jsp` via `<%@ include file="nav.jsp" %>`
- Alert pattern: `<c:if test="${not empty error}">` / `<c:if test="${not empty success}">`

### CSS Architecture

- `theme.css` — CSS custom properties, Google Fonts (DM Serif Display, Inter, JetBrains Mono), color palette (cream `#f5f0e8`, dark `#1a1a1a`, green `#22c55e`)
- `styles.css` — Base navbar, sticky header
- `settings.css` — Shared styles for all 4 settings pages (multiplier, segment, event, data-import)
- Page-specific: `login.css`, `register.css`, `homepage.css`, `profile.css`, `result.css`, `about.css`

### Derby Driver Note

Derby 10.16 uses `org.apache.derby.client.ClientAutoloadedDriver` (not the legacy `org.apache.derby.jdbc.ClientDriver`). HikariCP handles driver loading via `config.setDriverClassName()` — do not use `Class.forName()`.

## Key Files

- `web.xml` — All servlet/filter mappings, error pages (404/500), multipart config for DataImportServlet
- `DBConnection.java` — HikariCP pool config (10 max, 2 min idle, 30s timeout)
- `AuthenticationFilter.java` — Session-based auth filter
- `GeneticAlgorithm.java` — Core optimization (population 200, generations 600); constants defined as named `static final` fields
- `Season.java` — Enum with `scaleFactor`, `minMultiplier`, `maxMultiplier` per season
- `Room.java` — `seasonalPrices` and `seasonalOccupancies` as `EnumMap<Season, Double>`

## Logging

Every class uses SLF4J:
```java
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);
```
No `System.out.println` — all logging goes through SLF4J. Use `logger.debug()` for flow tracing, `logger.error()` for exceptions.
