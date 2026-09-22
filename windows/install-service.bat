@echo off
REM YAFVA.JAR Windows Service Installation Script using NSSM
REM This script installs YAFVA.JAR as a Windows service

setlocal enabledelayedexpansion

REM Check for administrator privileges
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: This script requires administrator privileges.
    echo Please right-click and select "Run as administrator"
    pause
    exit /b 1
)

REM Get the directory where this script is located
set "INSTALL_DIR=%~dp0"
REM Remove trailing backslash
set "INSTALL_DIR=%INSTALL_DIR:~0,-1%"

REM Define paths
set "NSSM_EXE=%INSTALL_DIR%\bin\nssm.exe"
set "JAR_FILE=%INSTALL_DIR%\yafva.jar"
set "CONFIG_FILE=%INSTALL_DIR%\application.yaml"
set "LOG_DIR=%INSTALL_DIR%\logs"

REM Service configuration
set "SERVICE_NAME=yafvajar"
set "SERVICE_DISPLAY=YafvaJar - FHIR Validator"
set "SERVICE_DESC=YafvaJar - FHIR Validator"

REM Optional JVM options (memory, proxy, ...). Example: -Xmx2g -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080
set "JAVA_TOOL_OPTIONS="

echo ========================================
echo YAFVA.JAR Service Installation
echo ========================================
echo.
echo Installation directory: %INSTALL_DIR%
echo Service name: %SERVICE_NAME%
echo.

REM Verify NSSM exists
if not exist "%NSSM_EXE%" (
    echo ERROR: NSSM not found at: %NSSM_EXE%
    echo.
    echo Please download NSSM from https://nssm.cc/download
    echo and place nssm.exe in the bin\ folder.
    echo See bin\README.md for detailed instructions.
    pause
    exit /b 1
)

REM Verify JAR file exists
if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found at: %JAR_FILE%
    echo.
    echo Please ensure yafva.jar is in the installation directory.
    pause
    exit /b 1
)

REM Verify Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH.
    echo Please install Java 21 or later and ensure it is in your PATH.
    pause
    exit /b 1
)

REM Check if service already exists
sc query "%SERVICE_NAME%" >nul 2>&1
if %errorlevel% equ 0 (
    echo WARNING: Service '%SERVICE_NAME%' already exists.
    echo Please uninstall it first using uninstall-service.bat
    pause
    exit /b 1
)

REM Create logs directory if it doesn't exist
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo Installing service...
echo.

REM Install the service
"%NSSM_EXE%" install "%SERVICE_NAME%" "java" "-jar" "%JAR_FILE%" >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Failed to install service
    pause
    exit /b 1
)

REM Configure service display name and description
"%NSSM_EXE%" set "%SERVICE_NAME%" DisplayName "%SERVICE_DISPLAY%" >nul
"%NSSM_EXE%" set "%SERVICE_NAME%" Description "%SERVICE_DESC%" >nul

REM Set working directory
"%NSSM_EXE%" set "%SERVICE_NAME%" AppDirectory "%INSTALL_DIR%" >nul

REM Pass JVM options via JAVA_TOOL_OPTIONS
if defined JAVA_TOOL_OPTIONS "%NSSM_EXE%" set "%SERVICE_NAME%" AppEnvironmentExtra "JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS%" >nul

REM Configure startup type (automatic)
"%NSSM_EXE%" set "%SERVICE_NAME%" Start SERVICE_AUTO_START >nul

REM Configure restart behavior (restart on failure)
"%NSSM_EXE%" set "%SERVICE_NAME%" AppExit Default Restart >nul
"%NSSM_EXE%" set "%SERVICE_NAME%" AppRestartDelay 10000 >nul

REM Configure shutdown timeout (15 seconds)
"%NSSM_EXE%" set "%SERVICE_NAME%" AppStopMethodSkip 0 >nul
"%NSSM_EXE%" set "%SERVICE_NAME%" AppStopMethodConsole 15000 >nul
"%NSSM_EXE%" set "%SERVICE_NAME%" AppStopMethodWindow 15000 >nul
"%NSSM_EXE%" set "%SERVICE_NAME%" AppStopMethodThreads 15000 >nul

echo SUCCESS: Service '%SERVICE_NAME%' installed successfully!
echo.
echo To start the service, run:
echo   net start %SERVICE_NAME%
echo.
echo Or use Windows Services (services.msc) to start it.
echo.

pause
