@echo off
setlocal

set SCRIPT_DIR=%~dp0
set GRADLE_BIN=C:\Users\sinmingyu\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat

if not exist "%GRADLE_BIN%" (
  echo 로컬 Gradle 실행 파일을 찾을 수 없습니다: %GRADLE_BIN%
  exit /b 1
)

if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%SCRIPT_DIR%.gradle-local
if "%GRADLE_OPTS%"=="" (
  set GRADLE_OPTS=-Dorg.gradle.native=false
) else (
  set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.native=false
)

"%GRADLE_BIN%" %*
