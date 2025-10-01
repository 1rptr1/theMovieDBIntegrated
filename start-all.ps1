param(
    [switch]$SkipFrontend
)

Write-Host "🚀 Starting IMDb Movie Explorer..." -ForegroundColor Green
Write-Host ""

# Function to check if a command exists
function Test-Command($cmd) {
    try { Get-Command $cmd -ErrorAction Stop | Out-Null; return $true }
    catch { return $false }
}

# Check prerequisites
Write-Host "🔍 Checking prerequisites..." -ForegroundColor Cyan

$prerequisites = @(
    @{name="Docker"; cmd="docker"; desc="Container runtime"},
    @{name="Docker Compose"; cmd="docker-compose"; desc="Container orchestration"},
    @{name="Java"; cmd="java"; desc="Runtime environment"},
    @{name="Maven"; cmd="mvn"; desc="Build tool"}
)

$missing = @()
foreach ($prereq in $prerequisites) {
    if (-not (Test-Command $prereq.cmd)) {
        $missing += $prereq
    }
}

if ($missing.Count -gt 0) {
    Write-Host "❌ Missing prerequisites:" -ForegroundColor Red
    foreach ($item in $missing) {
        Write-Host "   - $($item.name) ($($item.desc))" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Please install missing prerequisites and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ All prerequisites found!" -ForegroundColor Green

# Start PostgreSQL
Write-Host "📊 Starting PostgreSQL database..." -ForegroundColor Cyan
try {
    docker-compose up -d postgres
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ PostgreSQL started" -ForegroundColor Green
    } else {
        throw "Failed to start PostgreSQL"
    }
} catch {
    Write-Host "❌ Failed to start PostgreSQL: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Wait for database
Write-Host "⏳ Waiting for database to be ready..." -ForegroundColor Cyan
Start-Sleep -Seconds 10

# Start Spring Boot
Write-Host "🔧 Starting Spring Boot API..." -ForegroundColor Cyan
try {
    $backendJob = Start-Job -ScriptBlock {
        Set-Location $using:PWD
        mvn spring-boot:run
    }

    # Wait for backend to start
    Write-Host "⏳ Waiting for API to be ready..." -ForegroundColor Cyan
    $maxWait = 60
    $waited = 0

    while ($waited -lt $maxWait) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/movies/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Host "✅ API is ready!" -ForegroundColor Green
                break
            }
        } catch {
            # API not ready yet
        }

        Start-Sleep -Seconds 2
        $waited += 2

        if ($waited -mod 10 -eq 0) {
            Write-Host "⏳ Still waiting for API... ($waited seconds)" -ForegroundColor Cyan
        }
    }

    if ($waited -ge $maxWait) {
        Write-Host "❌ API failed to start within $maxWait seconds" -ForegroundColor Red
        Write-Host "🔍 Check the backend logs for errors" -ForegroundColor Yellow
        Stop-Job $backendJob
        exit 1
    }

} catch {
    Write-Host "❌ Failed to start Spring Boot API: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Start frontend if not skipped
if (-not $SkipFrontend) {
    Write-Host "🎨 Starting React frontend..." -ForegroundColor Cyan

    # Check if frontend dependencies are installed
    if (-not (Test-Path "frontend\node_modules")) {
        Write-Host "📦 Installing frontend dependencies..." -ForegroundColor Cyan
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

        Write-Host "✅ Frontend starting in background" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to start frontend: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "ℹ️  You can start frontend manually later with: cd frontend && npm run dev" -ForegroundColor Yellow
    }
}

# Display status
Write-Host ""
Write-Host "🎉 SUCCESS!" -ForegroundColor Green
Write-Host "=".PadRight(50, "=") -ForegroundColor Green
Write-Host "🎬 Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "🔧 Backend API: http://localhost:8080" -ForegroundColor Cyan
Write-Host "📚 API Docs: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "🗃️  Database: postgresql://localhost:5432/imdb" -ForegroundColor Cyan
Write-Host "=".PadRight(50, "=") -ForegroundColor Green
Write-Host ""
Write-Host "📝 Quick test:" -ForegroundColor Yellow
Write-Host "   1. Open http://localhost:3000" -ForegroundColor White
Write-Host "   2. Click 'Top Rated' button" -ForegroundColor White
Write-Host "   3. Search for 'Inception'" -ForegroundColor White
Write-Host "   4. Click a movie card to see details" -ForegroundColor White
Write-Host ""
Write-Host "🛑 To stop all services, press Ctrl+C" -ForegroundColor Yellow

# Wait for user input
try {
    while ($true) {
        Start-Sleep -Seconds 1

        # Check if jobs are still running
        if ($backendJob) {
            $backendState = Get-Job -Id $backendJob.Id -ErrorAction SilentlyContinue
            if ($backendState.State -ne "Running") {
                Write-Host "❌ Backend stopped unexpectedly" -ForegroundColor Red
                break
            }
        }

        if ($frontendJob -and $SkipFrontend -eq $false) {
            $frontendState = Get-Job -Id $frontendJob.Id -ErrorAction SilentlyContinue
            if ($frontendState.State -ne "Running") {
                Write-Host "❌ Frontend stopped unexpectedly" -ForegroundColor Red
                break
            }
        }
    }
} finally {
    Write-Host ""
    Write-Host "🛑 Shutting down services..." -ForegroundColor Yellow

    if ($backendJob) {
        Stop-Job $backendJob -ErrorAction SilentlyContinue
        Remove-Job $backendJob -ErrorAction SilentlyContinue
    }

    if ($frontendJob) {
        Stop-Job $frontendJob -ErrorAction SilentlyContinue
        Remove-Job $frontendJob -ErrorAction SilentlyContinue
    }

    Write-Host "✅ All services stopped" -ForegroundColor Green
}
