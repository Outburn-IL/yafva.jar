@echo off

if "%GITHUB_PACKAGES_TOKEN%"=="" (
	echo ERROR: GITHUB_PACKAGES_TOKEN is required for local builds because Maven resolves
	echo the private github-yafva package registry from GitHub Packages.
	echo.
	echo CI can use the workflow GITHUB_TOKEN with packages:read, but local builds need
	echo a PAT with read:packages scope unless the required artifacts are already cached.
	echo.
	echo PowerShell setup:
	echo   gh auth refresh -h github.com -s read:packages
	echo   $env:GITHUB_PACKAGES_TOKEN = gh auth token
	exit /b 1
)

choice /C YN /N /M "Do you want to skip tests? (Y/N): "
if %errorlevel% equ 1 set YAFVA_SKIP_TESTS=-DskipTests

set YAFVA_IGS=-D--validator.ig[0]=il.core.fhir.r4#0.17.5
set YAFVA_TX_SERVER=-D--validator.tx-server=

mvnw.cmd clean package %YAFVA_SKIP_TESTS% %YAFVA_IGS% %YAFVA_TX_SERVER%