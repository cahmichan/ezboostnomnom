# EzBoost

EzBoost is a hotel revenue-management web application that helps a property team prepare seasonal room prices against a chosen revenue target. It combines imported room inventory and historical monthly demand with a **seasonal genetic algorithm (GA)**, then presents an explainable monthly forecast and an exportable report.

> Scope: EzBoost performs seasonal price optimisation with monthly season mapping and event overrides. It is not a real-time daily-pricing engine, inventory-allocation system, or market-segment-aware GA search.

## What it does

- Creates and authenticates independent user accounts.
- Imports and validates room inventory and monthly season/revenue data from CSV, with a preview before changes are committed.
- Optimises per-room seasonal prices within imported minimum and maximum ADR constraints.
- Maps forecast months to learned seasons, with optional event-driven season overrides.
- Applies market-segment price multipliers after optimisation for display and reporting.
- Manages occupancy/revenue thresholds, segment multipliers, future events, and optional Calendarific holiday settings.
- Generates authenticated Excel reports, including durable owner-scoped optimisation snapshots that remain exportable after a session expires.
- Provides a database-aware health endpoint for deployment checks.

## Technology

| Area | Technology |
| --- | --- |
| Language | Java 11 |
| Web platform | Jakarta EE 10, JSP, Servlets, JSTL |
| Build | Maven WAR |
| Application server | Payara 6 / GlassFish compatible Jakarta EE 10 server |
| Database | Apache Derby network server |
| Connection pool | HikariCP |
| Optimisation | Seeded seasonal genetic algorithm |
| Testing | JUnit 5, Mockito, temporary Derby integration tests |

## Quick start

### Prerequisites

- JDK 11 (the checked-in build targets Java 11)
- Maven 3.8 or later
- A Jakarta EE 10-compatible Payara/GlassFish server
- Apache Derby running in network-server mode

### 1. Configure the runtime

Copy the values in [`.env.example`](.env.example) into your server configuration. The application reads Java system properties first, then operating-system environment variables. The `.env.example` file is a template only; it is not loaded automatically.

Required database settings:

```text
EZBOOST_DB_URL=jdbc:derby://localhost:1527/ezboost_db
EZBOOST_DB_USER=app
EZBOOST_DB_PASSWORD=<your-database-password>
```

Useful local-development settings:

```text
EZBOOST_ENV=development
EZBOOST_SECURE_COOKIES=false
```

For production, configure these in the Payara/GlassFish domain rather than source code:

```text
EZBOOST_ENV=production
EZBOOST_SECURE_COOKIES=true
EZBOOST_API_KEY_ENCRYPTION_KEY=<base64-encoded-32-byte-key>
```

`EZBOOST_DB_USER` and `EZBOOST_DB_PASSWORD` are mandatory. The application deliberately fails at startup if either is absent. Keep the API-key encryption key safe and stable: replacing or losing it makes previously encrypted Calendarific keys unreadable.

Restart the application-server domain after changing any of these values. Configure the JDBC URL as a single system-property value; on Windows `asadmin` may require its colon characters to be escaped.

### 2. Build and test

```powershell
mvn clean verify
```

This runs the automated test suite, checks dependency convergence, produces JaCoCo coverage output, and builds:

```text
target/EzBoost-1.0-SNAPSHOT.war
```

### 3. Deploy

Deploy the WAR through NetBeans or your server administration tool with the context root `EzBoost-main`.

```powershell
asadmin deploy --force=true --name EzBoost --contextroot EzBoost-main target/EzBoost-1.0-SNAPSHOT.war
```

After deployment, verify that Derby is running and check:

```text
http://localhost:8080/EzBoost-main/health
```

Expected response:

```json
{"status":"ok"}
```

## Data and optimisation model

1. Import room data: room type, capacity, base ADR, minimum ADR, maximum ADR, and occupancy.
2. Import monthly data: historical month/season, occupancy, and revenue information.
3. Define revenue targets and optional multipliers/event overrides.
4. Run the seasonal GA. It uses a recorded random seed so a run can be reproduced.
5. Review the resulting seasonal prices, forecast mapping, demand-curve status, and segment-price display.
6. Export the report.

The GA preserves the project’s seasonal 200/600 population/generation settings, target-fit objective, demand-curve fallback behaviour, and monthly event-override model. Market segments are display/report price multipliers applied after optimisation; they do not allocate inventory or change the GA search space.

## Security and data integrity

- Passwords are stored using BCrypt hashes.
- Mutating requests, including logout, use CSRF protection and authenticated owner checks.
- Session cookies are HTTP-only; secure-cookie behaviour is configurable per environment.
- Import, price, revenue target, date, multiplier, and event inputs are centrally validated.
- Database migrations add user-scoped uniqueness and ownership foreign keys while refusing to silently discard invalid legacy data.
- Calendarific API keys are encrypted at rest and are never rendered back to users.
- Account, import, settings, event, and optimisation actions are recorded as audit events.
- Structured request IDs and the health endpoint support operational troubleshooting.

## Database migrations and backups

EzBoost maintains an `EZBOOST_SCHEMA_HISTORY` table and applies ordered migrations on startup. Do not alter an already-applied migration or delete data merely to bypass a migration failure.

Before deploying to a database with real data:

1. Stop application writes and take a complete Derby backup.
2. Deploy the WAR and inspect the server log for migration results.
3. Check `/EzBoost-main/health`.
4. Confirm existing user, room, monthly, event, and optimisation record counts.

Keep backups access-controlled and test the restore procedure before relying on it for a production deployment.

## Repository guide

| File or directory | Purpose |
| --- | --- |
| `src/main/java` | Servlets, services, DAOs, optimisation logic, migrations, and security utilities |
| `src/main/webapp` | JSP views, CSS, JavaScript, and `WEB-INF/web.xml` |
| `src/test` | Unit, servlet/service, migration, import rollback, and Derby integration tests |
| `sample_data` | Sanitised data suitable for local experimentation |

## Development notes

- Do not commit Derby databases, build output, local server artifacts, IDE files, `.env` files, or API keys.
- Run `mvn clean verify` before merging or deploying changes.
- The optional vulnerability scan is available with `mvn -Pdependency-check -DnvdApiKey=<key> clean verify`. It is intentionally separate because the NVD service rate-limits anonymous scans.
- The included `start-ezboost-stack.bat` and `stop-ezboost-stack.bat` scripts require local `DERBY_BIN` and `GF_BIN` paths. Use [`ezboost-stack.example.cmd`](ezboost-stack.example.cmd) as the configuration template; do not commit a machine-specific copy.

## Status

The active build has automated validation around migrations, data ownership, CSRF, imports, deterministic optimisation, reports, events, and demand-curve fallback. The current release gate is:

```powershell
mvn clean verify
```
