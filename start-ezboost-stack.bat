@echo off
setlocal

set "DERBY_BIN=C:\Derby\bin"
set "GF_BIN=C:\Seksyen7\glassfish\bin"
set "DOMAIN=domain1"
set "DERBY_URL=jdbc:derby://localhost:1527/ezboost_db;user=app;password=app"
set "DERBY_DB_HOME=C:\Derby\bin\ezboost_db"

if not exist "%DERBY_BIN%\startNetworkServer.bat" (
    echo Error: Derby start script not found at "%DERBY_BIN%\startNetworkServer.bat"
    exit /b 1
)

if not exist "%DERBY_BIN%\NetworkServerControl.bat" (
    echo Error: Derby control script not found at "%DERBY_BIN%\NetworkServerControl.bat"
    exit /b 1
)

if not exist "%DERBY_BIN%\ij.bat" (
    echo Error: Derby ij client not found at "%DERBY_BIN%\ij.bat"
    exit /b 1
)

if not exist "%GF_BIN%\asadmin.bat" (
    echo Error: GlassFish asadmin not found at "%GF_BIN%\asadmin.bat"
    exit /b 1
)

echo Starting EzBoost services...
echo.

call :check_db >nul 2>&1
if not errorlevel 1 (
    echo [1/2] Derby Network Server is running and ezboost_db is reachable.
) else (
    call "%DERBY_BIN%\NetworkServerControl.bat" ping >nul 2>&1
    if errorlevel 1 (
    echo [1/2] Starting Derby Network Server...
    start "Derby Network Server" /min cmd /c ""%DERBY_BIN%\startNetworkServer.bat""
    timeout /t 5 /nobreak >nul
        call :check_db >nul 2>&1
        if errorlevel 1 (
            echo [1/2] Derby started, but ezboost_db is still unavailable.
            echo        Expected database home: "%DERBY_DB_HOME%"
            echo        JDBC URL: %DERBY_URL%
            exit /b 1
        )
        echo [1/2] Derby Network Server is running and ezboost_db is reachable.
    ) else (
        echo [1/2] Derby is already listening on port 1527, but ezboost_db is not reachable.
        echo        Another Derby instance may be using a different derby.system.home.
        echo        Expected database home: "%DERBY_DB_HOME%"
        echo        JDBC URL: %DERBY_URL%
        echo        Stop the other Derby server, then run this script again.
        exit /b 1
    )
)

echo [2/2] Starting GlassFish domain "%DOMAIN%"...
call "%GF_BIN%\asadmin.bat" start-domain %DOMAIN%

echo.
echo EzBoost startup sequence completed.
echo If you changed code, run the project from NetBeans to redeploy.
echo Otherwise browse to:
echo http://localhost:8080/EzBoost-main/login.jsp

endlocal
exit /b 0

:check_db
set "IJ_SCRIPT=%TEMP%\ezboost-derby-check-%RANDOM%.sql"
> "%IJ_SCRIPT%" echo connect '%DERBY_URL%';
>> "%IJ_SCRIPT%" echo exit;
call "%DERBY_BIN%\ij.bat" "%IJ_SCRIPT%" >nul 2>&1
set "CHECK_DB_RC=%ERRORLEVEL%"
del "%IJ_SCRIPT%" >nul 2>&1
exit /b %CHECK_DB_RC%
