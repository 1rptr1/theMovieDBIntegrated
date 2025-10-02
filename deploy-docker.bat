@echo off
echo 🐳 IMDb Movie Explorer - Complete Docker Deployment (Windows)
echo ==========================================================
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

REM Check if Docker Compose is available
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker Compose is not installed
    echo    Please install Docker Compose and try again
    echo.
    pause
    exit /b 1
)

echo ✅ All prerequisites found!
echo.

REM Build the complete Docker image
echo 🔨 Building complete Docker image...
echo    This includes: Spring Boot + React + PostgreSQL + Sample Data
echo.

docker-compose -f docker-compose.full.yml build --no-cache
if errorlevel 1 (
    echo ❌ Failed to build Docker image
    pause
    exit /b 1
)

echo ✅ Docker image built successfully!
echo.

REM Run the complete application
echo 🚀 Starting IMDb Movie Explorer...
echo.
echo 📍 Application will be available at:
echo    🎬 Frontend + Backend: http://localhost:8080
echo    📚 API Documentation: http://localhost:8080/swagger-ui.html
echo    🗃️  Database: postgresql://localhost:5432/imdb (inside container)
echo.
echo ==========================================================
echo.

REM Start the container
docker-compose -f docker-compose.full.yml up -d
if errorlevel 1 (
    echo ❌ Failed to start application
    pause
    exit /b 1
)

echo ✅ Application started successfully!
echo.
echo 📝 Container Status:
docker-compose -f docker-compose.full.yml ps
echo.
echo 🛑 To stop the application:
echo    docker-compose -f docker-compose.full.yml down
echo.
echo 🔄 To rebuild and restart:
echo    .\deploy-docker.bat
echo.
echo 🎉 Your complete IMDb Movie Explorer is now running!
echo    Access it at: http://localhost:8080
echo.

REM Optional: Wait for health check
echo.
echo ⏳ Waiting for application to be ready...
timeout /t 30 /nobreak > nul

REM Check if application is responding (requires curl)
curl -f http://localhost:8080/api/movies/health >nul 2>&1
if errorlevel 1 (
    echo ⚠️  Application is starting up... please wait a moment
    echo    Check status with: docker-compose -f docker-compose.full.yml logs
) else (
    echo ✅ Application is ready!
    echo 🎬 You can now browse movies at: http://localhost:8080
)

echo.
pause
