@echo off
echo 🎬 IMDb Movie Explorer - Single WAR Deployment
echo ==============================================
echo.

REM Check prerequisites
echo 🔍 Checking prerequisites...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java is not installed
    echo    Please install Java 17 or higher
    pause
    exit /b 1
)

docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not installed
    echo    Please install Docker Desktop
    pause
    exit /b 1
)

echo ✅ All prerequisites found
echo.

REM Build the application with embedded frontend
echo 🔧 Building complete application (WAR with embedded frontend)...
mvn clean package
if errorlevel 1 (
    echo ❌ Build failed
    pause
    exit /b 1
)
echo ✅ Application built successfully
echo.

REM Start database
echo 📊 Starting PostgreSQL database...
docker-compose up -d db
if errorlevel 1 (
    echo ❌ Failed to start database
    pause
    exit /b 1
)
echo ✅ Database started
echo.

REM Wait for database
echo ⏳ Waiting for database...
timeout /t 15 /nobreak > nul
echo.

REM Initialize database if needed
echo 🔄 Checking database setup...
docker-compose exec db psql -h localhost -U imdb -d imdb -c "SELECT COUNT(*) FROM title_basics;" >nul 2>&1
if errorlevel 1 (
    echo 📥 Loading sample data...
    docker-compose exec db psql -h localhost -U imdb -d imdb -f /docker-entrypoint-initdb.d/V1__Initial_Schema.sql >nul 2>&1
    docker-compose exec db psql -h localhost -U imdb -d imdb -f /docker-entrypoint-initdb.d/data.sql >nul 2>&1
    echo ✅ Sample data loaded
) else (
    echo ✅ Database already has data
)
echo.

REM Start the complete application
echo 🚀 Starting IMDb Movie Explorer...
echo.
echo 📍 Frontend + Backend: http://localhost:8080
echo 📚 API Documentation: http://localhost:8080/swagger-ui.html
echo 🗃️  Database: postgresql://localhost:5432/imdb
echo.
echo ==============================================
echo.

java -jar target/theMovieDBIntegrated-0.0.1-SNAPSHOT.war

echo.
echo 🛑 Application stopped
echo.
echo To restart, run this script again
pause
