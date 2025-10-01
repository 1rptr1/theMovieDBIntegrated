@echo off
echo 🚀 IMDb Movie Explorer - Single Command Startup
echo ================================================
echo.

REM Check prerequisites
echo 🔍 Checking prerequisites...
echo.

REM Check if Docker is running
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not installed or not running
    echo    Please install Docker Desktop and make sure it's running
    echo.
    pause
    exit /b 1
)

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java is not installed
    echo    Please install Java 21 or higher
    echo.
    pause
    exit /b 1
)

REM Check if Maven is available
mvn --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven is not installed
    echo    Please install Maven 3.8 or higher
    echo.
    pause
)

echo ✅ All prerequisites found!
echo.

REM Build frontend for production
echo 🎨 Building React frontend...
cd frontend
npm run build
if errorlevel 1 (
    echo ❌ Failed to build frontend
    cd ..
    pause
    exit /b 1
)
cd ..
echo ✅ Frontend built successfully
echo.

REM Build the application as WAR
echo 🔧 Building Spring Boot application (WAR)...
mvn clean package
if errorlevel 1 (
    echo ❌ Failed to build application
    pause
    exit /b 1
)
echo ✅ Application built successfully
echo.

REM Wait for backend to be ready
echo ⏳ Waiting for API to be ready...
set /a attempts=0
:wait_backend
set /a attempts+=1
curl -s http://localhost:8080/api/movies/health >nul 2>&1
if errorlevel 1 (
    if %attempts% lss 30 (
        timeout /t 2 /nobreak > nul
        goto wait_backend
    ) else (
        echo ❌ Backend failed to start within 60 seconds
        echo    Check the backend console window for errors
        echo.
        pause
        exit /b 1
    )
)
echo ✅ API is ready!
echo.

REM Check if frontend dependencies are installed
echo 🎨 Checking frontend setup...
if not exist "frontend\node_modules" (
    echo 📦 Installing frontend dependencies...
    cd frontend
    npm install
    if errorlevel 1 (
        echo ❌ Failed to install frontend dependencies
        cd ..
        pause
        exit /b 1
    )
    cd ..
    echo ✅ Frontend dependencies installed
) else (
    echo ✅ Frontend dependencies already installed
)

REM Start frontend in background
echo 🎨 Starting React frontend...
start "IMDb Frontend" cmd /k "cd frontend && npm run dev"
echo ✅ Frontend starting in new window
echo.

REM Final status
echo.
echo 🎉 SUCCESS! All services started!
echo ================================================
echo 🎬 Frontend: http://localhost:3000
echo 🔧 Backend API: http://localhost:8080
echo 📚 API Docs: http://localhost:8080/swagger-ui.html
echo 🗃️  Database: postgresql://localhost:5432/imdb
echo ================================================
echo.
echo 📝 Quick test:
echo    1. Open http://localhost:3000
echo    2. Click 'Top Rated' button
echo    3. Search for 'Inception'
echo    4. Click a movie card to see details
echo.
echo 🛑 To stop all services, close all console windows
echo.
REM Display Docker deployment option
echo.
echo 🐳 DOCKER DEPLOYMENT AVAILABLE:
echo    Run 'docker-compose up --build' for containerized deployment
echo    This includes: PostgreSQL + Spring Boot WAR + React Frontend
echo.
