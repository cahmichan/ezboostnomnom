@echo off
setlocal

set "DERBY_BIN=C:\Derby\bin"
set "GF_BIN=C:\Seksyen7\glassfish\bin"
set "DOMAIN=domain1"

echo Stopping EzBoost services...
echo.

echo [1/2] Stopping GlassFish domain "%DOMAIN%"...
call "%GF_BIN%\asadmin.bat" stop-domain %DOMAIN%

echo [2/2] Stopping Derby Network Server...
call "%DERBY_BIN%\NetworkServerControl.bat" shutdown

echo.
echo EzBoost services stopped.

endlocal
