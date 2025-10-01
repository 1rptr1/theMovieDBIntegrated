# 🚀 Single Command Startup Guide

## Option 1: Windows Batch Script (Easiest)

```bash
# Simply double-click this file:
start-all.bat
```

This script:
- ✅ Checks all prerequisites
- ✅ Starts PostgreSQL database
- ✅ Starts Spring Boot API
- ✅ Installs frontend dependencies (if needed)
- ✅ Starts React frontend
- ✅ Waits for services to be ready
- ✅ Shows status and URLs

## Option 2: PowerShell Script (Most Control)

```bash
# Run with full control:
powershell -ExecutionPolicy Bypass -File start-all.ps1

# Run without frontend:
powershell -ExecutionPolicy Bypass -File start-all.ps1 -SkipFrontend
```

## Option 3: Manual Step-by-Step

### Terminal 1 - Database
```bash
docker-compose up -d postgres
```

### Terminal 2 - Backend
```bash
mvn spring-boot:run
```

### Terminal 3 - Frontend
```bash
cd frontend
npm install  # first time only
npm run dev
```

## Option 4: Node.js Script (Cross-platform)

```bash
node start-all.js
```

## Access Points

After successful startup, you'll see:

```
🎉 SUCCESS! All services started!
===============================================
🎬 Frontend: http://localhost:3000
🔧 Backend API: http://localhost:8080
📚 API Docs: http://localhost:8080/swagger-ui.html
🗃️  Database: postgresql://localhost:5432/imdb
===============================================
```

## Prerequisites Check

The scripts automatically check for:
- ✅ Docker & Docker Compose
- ✅ Java 21+
- ✅ Maven 3.8+
- ✅ Node.js 18+

## What Happens During Startup

### 1. Database (PostgreSQL)
- Starts container on port 5432
- Creates `imdb` database
- Runs schema migrations
- Loads sample movie data

### 2. Backend (Spring Boot)
- Starts on port 8080
- Initializes database connection
- Loads sample data (if local)
- Exposes REST API endpoints
- Health check at `/api/movies/health`

### 3. Frontend (React + Vite)
- Starts dev server on port 3000
- Proxies API calls to backend
- Hot reload for development
- Beautiful movie browsing UI

## Troubleshooting

### If startup fails:

1. **Check if ports are free:**
   ```bash
   netstat -ano | findstr :8080
   netstat -ano | findstr :3000
   ```

2. **Kill conflicting processes:**
   ```bash
   # Find and kill process on port 8080
   for /f "tokens=5" %a in ('netstat -ano ^| findstr :8080') do taskkill /f /pid %a

   # Kill process on port 3000
   npx kill-port 3000
   ```

3. **Restart Docker Desktop** if database won't start

4. **Clear Maven cache** if backend fails:
   ```bash
   mvn clean
   ```

5. **Reinstall frontend dependencies:**
   ```bash
   cd frontend
   rm -rf node_modules package-lock.json
   npm install
   ```

## Quick Test

Once everything is running:

1. Open http://localhost:3000
2. Click "Top Rated" button
3. Search for "Inception"
4. Click a movie card to see details

## Stop All Services

### Windows
- Close all console windows, OR
- Press Ctrl+C in the main startup script

### Manual
```bash
# Stop containers
docker-compose stop

# Kill processes (Windows)
taskkill /f /im java.exe
taskkill /f /im node.exe
```

## Next Steps

- ✅ All services start automatically
- ✅ Database has sample movie data
- ✅ API endpoints are ready
- ✅ Frontend UI is beautiful and responsive

**Ready to explore movies! 🎬**
