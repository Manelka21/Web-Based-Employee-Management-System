@echo off
setlocal
title LankaTech EMS - Feature switch

rem ---------------------------------------------------------------------------
rem  Turns the frontend's non-core functions ON or OFF.
rem
rem  Always on (never switched):
rem    - Department management   (Organization: departments + positions)
rem    - Onboarding and recruitment (Recruitment: vacancies + applications)
rem    - Training
rem    - Sign in, Home, Profile and Notifications (needed to use the app at all)
rem
rem  Switched by this file:
rem    Employee directory, Attendance, Leave and holidays, Payroll and salaries,
rem    Performance, Reports, Users and access, Activity log, Email outbox
rem
rem  Usage:
rem    switch-features.bat           menu
rem    switch-features.bat on        turn the other functions on
rem    switch-features.bat off       turn the other functions off
rem    switch-features.bat status    show the current state
rem
rem  It only rewrites FRONTEND\js\features.js. The backend is never touched, so
rem  refresh the browser after switching - no restart needed.
rem ---------------------------------------------------------------------------

cd /d "%~dp0"
set "FILE=js\features.js"
set "INTERACTIVE="
set "RC=0"

if /i "%~1"=="on" goto turn_on
if /i "%~1"=="off" goto turn_off
if /i "%~1"=="status" goto status_only
if not "%~1"=="" goto usage

set "INTERACTIVE=1"
call :show_status
echo.
echo   [1] Turn ON  all other functions
echo   [2] Turn OFF all other functions  (core functions stay on)
echo   [3] Exit without changes
echo.
choice /c 123 /n /m "  Choose 1, 2 or 3: "
if errorlevel 3 goto end
if errorlevel 2 goto turn_off
goto turn_on

:turn_on
set "VAL=true"
set "STATE=ON"
goto apply

:turn_off
set "VAL=false"
set "STATE=OFF"
goto apply

:apply
call :write
if errorlevel 1 (
  echo.
  echo  Could not write %FILE%. Is the folder read-only?
  set "RC=1"
  goto end
)
echo.
echo  Other functions are now %STATE%. Refresh the browser to apply.
call :show_status
set "RC=0"
goto end

:status_only
call :show_status
set "RC=0"
goto end

:usage
echo.
echo  Unknown option "%~1". Use: switch-features.bat [on ^| off ^| status]
set "RC=1"
goto end

rem ---------------------------------------------------------------- write
:write
> "%FILE%" (
  echo // LankaTech EMS feature switches - written by switch-features.bat.
  echo // Run switch-features.bat to change them, then refresh the browser.
  echo // STATE: %STATE%
  echo //
  echo // Always on and not listed: Department management, Onboarding and recruitment,
  echo // Training, plus Sign in, Home, Profile and Notifications.
  echo // A missing file or key counts as true, so deleting this file turns everything on.
  echo window.EMS_FEATURES = {
  echo   employees: %VAL%,
  echo   attendance: %VAL%,
  echo   leave: %VAL%,
  echo   payroll: %VAL%,
  echo   performance: %VAL%,
  echo   reports: %VAL%,
  echo   userAdmin: %VAL%,
  echo   activityLog: %VAL%,
  echo   emailOutbox: %VAL%,
  echo };
)
exit /b %errorlevel%

rem ---------------------------------------------------------------- status
:show_status
set "CURRENT=ON"
if exist "%FILE%" (
  findstr /c:"STATE: OFF" "%FILE%" >nul && set "CURRENT=OFF"
)
echo.
echo  LankaTech EMS - feature switch
echo  ------------------------------
echo  Always on : Department management, Onboarding and recruitment, Training
echo  Switchable: Directory, Attendance, Leave, Payroll, Performance, Reports,
echo              Users and access, Activity log, Email outbox
echo.
echo  Switchable functions are currently: %CURRENT%
exit /b 0

:end
if defined INTERACTIVE (
  echo.
  pause
)
exit /b %RC%
