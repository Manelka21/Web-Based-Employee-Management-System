@echo off
setlocal
title LankaTech EMS - Frontend

rem Serves this folder on http://localhost:5173 and opens it in the browser.
rem The backend only accepts this origin (CORS), so keep port 5173.
set "PORT=5173"
set "URL=http://localhost:%PORT%"

cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
  echo.
  echo  Node.js was not found. Install it from https://nodejs.org and try again.
  echo.
  pause
  exit /b 1
)

rem Already running? Just open the app.
netstat -ano | findstr /r /c:":%PORT% .*LISTENING" >nul
if not errorlevel 1 (
  echo  The frontend is already running at %URL%
  if not defined EMS_NO_BROWSER start "" "%URL%"
  ping -n 4 127.0.0.1 >nul
  exit /b 0
)

echo.
echo  LankaTech EMS frontend
echo  ----------------------
echo  App:  %URL%
echo  API:  http://localhost:8080/api  (start the backend separately)
echo.
echo  Close this window or press Ctrl+C to stop.
echo.

rem Open the browser a moment after the server starts
if not defined EMS_NO_BROWSER start "" /b cmd /c "ping -n 3 127.0.0.1 >nul & start "" %URL%"

node server.js
set "EXITCODE=%errorlevel%"

if not "%EXITCODE%"=="0" (
  echo.
  echo  The server stopped with an error ^(code %EXITCODE%^).
  pause
)
exit /b %EXITCODE%
