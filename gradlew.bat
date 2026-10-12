@echo off
setlocal
set GRADLE_VERSION=9.5.1
set GRADLE_HOME=%USERPROFILE%\.gradle\vmtrade-wrapper\gradle-%GRADLE_VERSION%
if exist "%GRADLE_HOME%\bin\gradle.bat" goto run
set ZIP=%TEMP%\gradle-%GRADLE_VERSION%.zip
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
if errorlevel 1 exit /b 1
if exist "%USERPROFILE%\.gradle\vmtrade-wrapper" rmdir /s /q "%USERPROFILE%\.gradle\vmtrade-wrapper"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%USERPROFILE%\.gradle\vmtrade-wrapper'"
del /q "%ZIP%" >nul 2>&1
:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
