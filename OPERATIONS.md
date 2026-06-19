# EzBoost deployment and recovery

EzBoost remains a Java 11 / Jakarta EE 10 WAR deployed through NetBeans 27 to the existing GlassFish server and Apache Derby network server.

## GlassFish configuration

Do not add credentials to `DBConnection.java`, batch files, or source control. Set the following Java system properties in the GlassFish domain before deploying. The `.env.example` file documents the required values.

```powershell
asadmin create-system-properties EZBOOST_ENV=production
asadmin create-system-properties EZBOOST_DB_URL=jdbc:derby://localhost:1527/ezboost_db
asadmin create-system-properties EZBOOST_DB_USER=app
asadmin create-system-properties EZBOOST_DB_PASSWORD=<database-password>
asadmin create-system-properties EZBOOST_SECURE_COOKIES=true
asadmin create-system-properties EZBOOST_API_KEY_ENCRYPTION_KEY=<base64-32-byte-key>
```

Restart the GlassFish domain after changing properties. `EZBOOST_API_KEY_ENCRYPTION_KEY` is required before saving a new Calendarific key. Keep the same key for the life of the database; losing it prevents decryption of encrypted API keys.

For local development, use `EZBOOST_ENV=development` and `EZBOOST_SECURE_COOKIES=false`. Never copy the development Derby password into a production deployment.

## Database migration and backup

EzBoost creates `EZBOOST_SCHEMA_HISTORY` and applies migrations when the database is first used after deployment. Migration history is additive: do not edit an applied migration.

Before each deployment:

1. Stop GlassFish so no request can write during the backup.
2. Run a Derby backup from an authenticated database administration session, or stop the Derby server and copy the complete database directory to a timestamped, access-controlled location.
3. Start Derby, deploy the WAR from NetBeans, and inspect the application log for `Applied EzBoost schema migration`.
4. Verify `/EzBoost-main/health` returns `{"status":"ok"}`.
5. Confirm existing user, room, monthly, event, and optimization record counts before and after deployment.

If a migration fails, restore the backup before retrying. Do not delete rows to make a migration pass; resolve duplicate or invalid legacy data first.

## Recovery and support

- Use the `X-Request-Id` response header to correlate a user error with server logs.
- Rotate a Calendarific API key by saving a replacement in Event Settings; the value is never displayed after saving.
- The GitHub baseline commit `f03655a` is the pre-hardening recovery point. Each later hardening change is a separate commit on `main`.
- Run `mvn clean verify` before every deployment. A release is valid only when the full suite passes and the WAR packages successfully.
