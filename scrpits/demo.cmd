@echo off
setlocal

set BASE=http://localhost:8080/api/users

echo === Register ===
curl -i -X POST "%BASE%/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"alice\",\"password\":\"secret\"}"

echo.
echo === Login ===
curl -s -X POST "%BASE%/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"alice\",\"password\":\"secret\"}"

echo.
echo Done.
endlocal