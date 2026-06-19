@echo off
setlocal

if "%DERBY_BIN%"=="" (
    echo Error: DERBY_BIN is not configured. Set it to your Derby bin directory.
    exit /b 1
)
if "%GF_BIN%"=="" (
    echo Error: GF_BIN is not configured. Set it to your GlassFish bin directory.
    exit /b 1
)
if "%DOMAIN%"=="" set "DOMAIN=domain1"

echo Stopping EzBoost services...
echo.

echo [1/2] Stopping GlassFish domain "%DOMAIN%"...
call "%GF_BIN%\asadmin.bat" stop-domain %DOMAIN%

echo [2/2] Stopping Derby Network Server...
call "%DERBY_BIN%\NetworkServerControl.bat" shutdown

echo.
echo EzBoost services stopped.

endlocal
