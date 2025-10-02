@echo off
echo 🔍 IMDb Application Diagnostics
echo ===============================

echo.
echo 1. Checking if WAR file exists...
if exist "target\theMovieDBIntegrated-0.0.1-SNAPSHOT.war" (
    echo ✅ WAR file found
) else (
    echo ❌ WAR file not found - building now...
    mvn clean package
    if errorlevel 1 (
        echo ❌ Build failed
        pause
        exit /b 1
    )
    echo ✅ Build completed
)

echo.
echo 2. Checking database connectivity...
docker-compose exec db pg_isready -U imdb
if errorlevel 1 (
    echo ❌ Database not ready
) else (
    echo ✅ Database ready
)

echo.
echo 3. Checking Maven/Java setup...
mvn --version

echo.
echo 4. Starting application manually...
echo Starting: java -jar target/theMovieDBIntegrated-0.0.1-SNAPSHOT.war
echo.
echo The application will start in a new window.
echo Check the console output for any errors.
echo.

pause
start "IMDb Backend" cmd /k "java -jar target/theMovieDBIntegrated-0.0.1-SNAPSHOT.war"
