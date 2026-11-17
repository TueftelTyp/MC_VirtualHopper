@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
 echo Gradle was not found in PATH. Install Gradle and JDK 25.
 pause
 exit /b 1
)
call gradle --no-daemon clean build
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" echo Build failed with exit code %RESULT%.
pause
exit /b %RESULT%
