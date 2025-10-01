@echo off
echo Starting IMDb Movie Explorer...
echo.

echo [1/2] Starting PostgreSQL database...
start "PostgreSQL" docker-compose up postgres

timeout /t 10 /nobreak > nul

echo [2/2] Starting Spring Boot API...
start "Spring Boot API" cmd /k "mvn spring-boot:run"

echo.
echo ========================================
echo Services are starting...
echo.
echo Backend API will be at: http://localhost:8080
echo Frontend dev instructions:
echo   1. Open a new terminal
echo   2. cd frontend
echo   3. npm install (first time only)
echo   4. npm run dev
echo   5. Open http://localhost:3000
echo ========================================
echo.
pause
