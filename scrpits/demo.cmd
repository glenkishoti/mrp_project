@echo off
setlocal ENABLEDELAYEDEXPANSION

set BASE=http://localhost:8080
set USERS=%BASE%/api/users

echo === Register ===
curl -i -X POST "%USERS%/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"alice\",\"password\":\"secret\"}"

echo.
echo === Login ===
for /f "tokens=* usebackq" %%a in (`curl -s -X POST "%USERS%/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"Username\":\"alice\",\"Password\":\"secret\"}"`) do (
    set LOGIN_RESPONSE=%%a
)

echo Login Response: %LOGIN_RESPONSE%

REM Extract token from JSON
for /f "tokens=2 delims=:" %%a in ("%LOGIN_RESPONSE%") do (
  set PART=%%a
)
set TOKEN=%PART:"=%

echo.
echo Extracted Token: %TOKEN%

echo.
echo === Profile ===
curl -i -X GET "%USERS%/alice/profile" ^
  -H "Authorization: Bearer %TOKEN%"

echo.
echo Done.
endlocal
