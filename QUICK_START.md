# 🚀 Quick Start - IMDb Movie Explorer

## One-Time Setup (5 minutes)

### 1. Install Prerequisites
- ✅ Java 21+
- ✅ Node.js 18+
- ✅ Docker Desktop

### 2. Clone & Setup
```bash
git clone <your-repo-url>
cd theMovieDBIntegrated
```

## Every Time You Develop

### Option A: Windows Quick Start
```bash
# Double-click this file:
start-dev.bat

# Then in a new terminal:
cd frontend
npm install  # first time only
npm run dev
```

### Option B: Manual Start

**Terminal 1 - Database:**
```bash
docker-compose up postgres
```

**Terminal 2 - Backend:**
```bash
mvn spring-boot:run
```

**Terminal 3 - Frontend:**
```bash
cd frontend
npm install  # first time only
npm run dev
```

## Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| 🎨 **Frontend UI** | http://localhost:3000 | Main application |
| 🔧 **Backend API** | http://localhost:8080 | REST API |
| 📚 **API Docs** | http://localhost:8080/swagger-ui.html | Swagger UI |
| ✅ **Health Check** | http://localhost:8080/api/movies/health | Status check |

## Quick Test

1. Open http://localhost:3000
2. Click "Top Rated" button
3. Search for "Inception"
4. Click on a movie card
5. See detailed information

## Common Commands

### Backend
```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Run tests
mvn test

# Format code
mvn spotless:apply
```

### Frontend
```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Database
```bash
# Start
docker-compose up -d postgres

# Stop
docker-compose stop postgres

# View logs
docker-compose logs postgres

# Reset (WARNING: Deletes all data)
docker-compose down -v
docker-compose up -d postgres
```

## Troubleshooting One-Liners

```bash
# Kill port 8080 (backend)
npx kill-port 8080

# Kill port 3000 (frontend)
npx kill-port 3000

# Restart everything
docker-compose restart
mvn spring-boot:run  # in one terminal
npm run dev          # in another terminal (from frontend/)

# Check if services are running
curl http://localhost:8080/api/movies/health  # Backend
curl http://localhost:3000                    # Frontend
```

## Project Structure

```
theMovieDBIntegrated/
├── src/                    # Backend source code
│   ├── main/java/         # Java files
│   ├── main/resources/    # Config & SQL files
│   └── test/              # Tests
├── frontend/              # Frontend application
│   ├── src/
│   │   ├── components/   # React components
│   │   ├── App.jsx       # Main app
│   │   └── main.jsx      # Entry point
│   ├── package.json      # Dependencies
│   └── vite.config.js    # Vite config
├── .github/workflows/     # CI/CD pipelines
├── docker-compose.yml     # Docker setup
└── pom.xml               # Maven config
```

## Default Sample Data

The application comes with 10 sample movies:
- Inception (2010)
- The Shawshank Redemption (1994)
- The Godfather (1972)
- The Dark Knight (2008)
- And 6 more classics...

## Environment Variables

Create `.env` file in root (optional):
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/imdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
OMDB_API_KEY=your-key-here
```

## What's Running?

After successful start, you should have:
- ✅ PostgreSQL container (Port 5432)
- ✅ Spring Boot API (Port 8080)
- ✅ React + Vite dev server (Port 3000)

## Need Help?

📖 **Full Documentation:**
- Backend: `README.md`
- Frontend: `frontend/README.md`
- Full Stack: `FULL_STACK_GUIDE.md`

🐛 **Issues?**
- Check `TROUBLESHOOTING.md` (coming soon)
- Review logs in terminal
- Verify all services are running

---

**Ready to explore movies! 🎬**
