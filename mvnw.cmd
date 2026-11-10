@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "MAVEN_BIN=C:\Users\HP\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd"

where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

if exist "%MAVEN_BIN%" (
    "%MAVEN_BIN%" %*
    exit /b %ERRORLEVEL%
)

if defined MAVEN_HOME (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        "%MAVEN_HOME%\bin\mvn.cmd" %*
        exit /b %ERRORLEVEL%
    )
)

echo [ERROR] Maven not found. Please ensure Maven is installed.
exit /b 1
