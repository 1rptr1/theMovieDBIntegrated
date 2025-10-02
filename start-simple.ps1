Write-Host "Starting IMDb Movie Explorer..." -ForegroundColor Green
Write-Host ""

# Check prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Cyan

$prerequisites = @("docker", "docker-compose", "java", "mvn")
$missing = @()

foreach ($prereq in $prerequisites) {
    try {
        Get-Command $prereq -ErrorAction Stop | Out-Null
    }
    catch {
        $missing += $prereq
    }
}

if ($missing.Count -gt 0) {
    Write-Host "Missing prerequisites:" -ForegroundColor Red
    foreach ($item in $missing) {
        Write-Host "   - $item" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Please install missing prerequisites and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host "All prerequisites found!" -ForegroundColor Green

# Start PostgreSQL
Write-Host "Starting PostgreSQL database..." -ForegroundColor Cyan
try {
    docker-compose up -d db
    if ($LASTEXITCODE -eq 0) {
        Write-Host "PostgreSQL started" -ForegroundColor Green
    } else {
        throw "Failed to start PostgreSQL"
    }
} catch {
    Write-Host "Failed to start PostgreSQL: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Wait for database
Write-Host "Waiting for database to be ready..." -ForegroundColor Cyan
Start-Sleep -Seconds 10

# Check if WAR file exists, build if needed
Write-Host "Checking application build..." -ForegroundColor Cyan
if (-not (Test-Path "target\theMovieDBIntegrated-0.0.1-SNAPSHOT.war")) {
    Write-Host "Building Spring Boot application..." -ForegroundColor Cyan
    mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to build application" -ForegroundColor Red
        exit 1
    }
    Write-Host "Application built successfully" -ForegroundColor Green
} else {
    Write-Host "Application already built" -ForegroundColor Green
}

# Check if imdb user exists
try {
    $userExists = docker-compose exec -T db psql -h localhost -U postgres -d postgres -c "SELECT 1 FROM pg_user WHERE usename = 'imdb';" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Creating database user..." -ForegroundColor Cyan
        docker-compose exec -T db psql -h localhost -U postgres -d postgres -c "CREATE USER imdb WITH PASSWORD 'imdb';" 2>$null
        Write-Host "Database user created" -ForegroundColor Green
    } else {
        Write-Host "Database user already exists" -ForegroundColor Green
    }
} catch {
    Write-Host "Creating database user..." -ForegroundColor Cyan
    docker-compose exec -T db psql -h localhost -U postgres -d postgres -c "CREATE USER imdb WITH PASSWORD 'imdb';" 2>$null
    Write-Host "Database user created" -ForegroundColor Green
}

# Check if tables exist and have sufficient data
try {
    $result = docker-compose exec -T db psql -h localhost -U imdb -d imdb -c "SELECT COUNT(*) FROM title_basics;" 2>$null
    $count = $result | Select-String -Pattern '^\s*(\d+)' | ForEach-Object { $_.Matches[0].Groups[1].Value }

    if ([int]$count -lt 100) {
        Write-Host "Loading database schema..." -ForegroundColor Cyan
        docker-compose exec -T db psql -h localhost -U imdb -d imdb -f /docker-entrypoint-initdb.d/V1__Initial_Schema.sql 2>$null
        Write-Host "Schema loaded" -ForegroundColor Green

        Write-Host "Loading sample data ($count movies found, loading development dataset)..." -ForegroundColor Cyan
        docker-compose exec -T db psql -h localhost -U imdb -d imdb -f /docker-entrypoint-initdb.d/data.sql 2>$null
        Write-Host "Sample data loaded (10 movies for development)" -ForegroundColor Green

        Write-Host "Note: For production data (140K+ movies), run in CI/CD environment" -ForegroundColor Yellow
    } else {
        Write-Host "Database has sufficient data ($count movies)" -ForegroundColor Green
    }
} catch {
    Write-Host "Loading database schema..." -ForegroundColor Cyan
    docker-compose exec -T db psql -h localhost -U imdb -d imdb -f /docker-entrypoint-initdb.d/V1__Initial_Schema.sql 2>$null
    Write-Host "Schema loaded" -ForegroundColor Green

    Write-Host "Loading sample data..." -ForegroundColor Cyan
    docker-compose exec -T db psql -h localhost -U imdb -d imdb -f /docker-entrypoint-initdb.d/data.sql 2>$null
    Write-Host "Sample data loaded (10 movies for development)" -ForegroundColor Green

    Write-Host "Note: For production data (140K+ movies), run in CI/CD environment" -ForegroundColor Yellow
}

# Start Spring Boot
Write-Host "Starting Spring Boot API..." -ForegroundColor Cyan
try {
    $backendJob = Start-Job -ScriptBlock {
        Set-Location $using:PWD
        java -jar "target/theMovieDBIntegrated-0.0.1-SNAPSHOT.war"
    }

    # Wait for backend to be ready
    Write-Host "Waiting for API to be ready..." -ForegroundColor Cyan
    $maxWait = 60
    $waited = 0

    while ($waited -lt $maxWait) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/movies/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Host "API is ready!" -ForegroundColor Green
                break
            }
        } catch {
            # API not ready yet
        }

        Start-Sleep -Seconds 2
        $waited += 2

        if ($waited % 10 -eq 0) {
            Write-Host "Still waiting for API... ($waited seconds)" -ForegroundColor Cyan
        }
    }

    if ($waited -ge $maxWait) {
        Write-Host "API failed to start within $maxWait seconds" -ForegroundColor Red
        Write-Host "Check the backend logs for errors" -ForegroundColor Yellow
        Stop-Job $backendJob
        exit 1
    }

} catch {
    Write-Host "Failed to start Spring Boot API: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Start frontend
Write-Host "Starting React frontend..." -ForegroundColor Cyan

# Check if frontend dependencies are installed
if (-not (Test-Path "frontend\node_modules")) {
    Write-Host "Installing frontend dependencies..." -ForegroundColor Cyan
    Set-Location "frontend"
    npm install
    Set-Location ".."
}

try {
    $frontendJob = Start-Job -ScriptBlock {
        Set-Location $using:PWD
        Set-Location "frontend"
        npm run dev
    }

    Write-Host "Frontend starting in background" -ForegroundColor Green
} catch {
    Write-Host "Failed to start frontend: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "You can start frontend manually later with: cd frontend && npm run dev" -ForegroundColor Yellow
}

# Display status
Write-Host ""
Write-Host "SUCCESS!" -ForegroundColor Green
Write-Host "=".PadRight(50, "=") -ForegroundColor Green
Write-Host "Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "Backend API: http://localhost:8080" -ForegroundColor Cyan
Write-Host "API Docs: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "Database: postgresql://localhost:5432/imdb" -ForegroundColor Cyan
Write-Host "=".PadRight(50, "=") -ForegroundColor Green
Write-Host ""
Write-Host "Quick test:" -ForegroundColor Yellow
Write-Host "   1. Open http://localhost:3000" -ForegroundColor White
Write-Host "   2. Click 'Top Rated' button" -ForegroundColor White
Write-Host "   3. Search for 'Inception'" -ForegroundColor White
Write-Host "   4. Click a movie card to see details" -ForegroundColor White
Write-Host ""
Write-Host "To stop all services, press Ctrl+C" -ForegroundColor Yellow

# Wait for user input
try {
    while ($true) {
        Start-Sleep -Seconds 1

        # Check if jobs are still running
        if ($backendJob) {
            $backendState = Get-Job -Id $backendJob.Id -ErrorAction SilentlyContinue
            if ($backendState.State -ne "Running") {
                Write-Host "Backend stopped unexpectedly" -ForegroundColor Red
                break
            }
        }

        if ($frontendJob) {
            $frontendState = Get-Job -Id $frontendJob.Id -ErrorAction SilentlyContinue
            if ($frontendState.State -ne "Running") {
                Write-Host "Frontend stopped unexpectedly" -ForegroundColor Red
                break
            }
        }
    }
} finally {
    Write-Host ""
    Write-Host "Shutting down services..." -ForegroundColor Yellow

    if ($backendJob) {
        Stop-Job $backendJob -ErrorAction SilentlyContinue
        Remove-Job $backendJob -ErrorAction SilentlyContinue
    }

    if ($frontendJob) {
        Stop-Job $frontendJob -ErrorAction SilentlyContinue
        Remove-Job $frontendJob -ErrorAction SilentlyContinue
    }

    Write-Host "All services stopped" -ForegroundColor Green
}
