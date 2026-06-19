@echo off
rem Copy these values into your user environment or run them in the terminal before start-ezboost-stack.bat.
set "DERBY_BIN=C:\path\to\Derby\bin"
set "GF_BIN=C:\path\to\glassfish\bin"
set "DOMAIN=domain1"
set "EZBOOST_DB_URL=jdbc:derby://localhost:1527/ezboost_db"
set "EZBOOST_DB_USER=app"
set "EZBOOST_DB_PASSWORD=replace-with-your-database-password"
set "EZBOOST_ENV=development"
set "EZBOOST_SECURE_COOKIES=false"
rem Generate a 32-byte Base64 key for production Calendarific API-key encryption.
set "EZBOOST_API_KEY_ENCRYPTION_KEY=replace-with-base64-32-byte-key"
