# IMDb Movie Explorer - Full Stack Guide

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      USER BROWSER                           │
│                 http://localhost:3000                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ HTTP Requests
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   REACT FRONTEND                            │
│  • Vite Dev Server (Port 3000)                             │
│  • React 18 + TailwindCSS                                  │
│  • shadcn/ui Components                                    │
│  • Proxy to Backend API                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Proxied API Calls
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              SPRING BOOT BACKEND                            │
│                 http://localhost:8080                       │
│  • Java 21 + Spring Boot 3.x                               │
│  • REST API Endpoints                                      │
│  • JdbcTemplate for Database                               │
│  • OMDb API Integration                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ JDBC Connection
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              POSTGRESQL DATABASE                            │
│                 Port 5432                                   │
│  • IMDb Dataset Tables                                     │
│  • Materialized Views                                      │
│  • Full-Text Search Indexes                                │
│  • Recommendation System                                   │
└─────────────────────────────────────────────────────────────┘
```

## 📦 Technology Stack

### Frontend
- **Framework**: React 18 with Hooks
- **Build Tool**: Vite (fast HMR, optimized builds)
- **Styling**: TailwindCSS (utility-first CSS)
- **Components**: shadcn/ui (accessible, customizable)
- **Icons**: Lucide React (modern icon library)
- **HTTP Client**: Fetch API

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Database Access**: JdbcTemplate
- **API Docs**: Swagger/OpenAPI
- **External API**: OMDb API for enhanced metadata

### Database
- **DBMS**: PostgreSQL 15
- **Extensions**: pg_trgm (trigram search)
- **Features**: Materialized views, Full-text search, GIN indexes

### DevOps
- **Containerization**: Docker
- **CI/CD**: GitHub Actions
- **Version Control**: Git

## 🚀 Getting Started

### Prerequisites Checklist

```bash
# Check Java version (should be 21+)
java -version

# Check Maven version
mvn -version

# Check Node version (should be 18+)
node -version

# Check npm version
npm -version

# Check Docker
docker --version
docker-compose --version
```

### Step-by-Step Setup

#### 1. Clone and Setup Backend

```bash
# Clone repository
git clone <repository-url>
cd theMovieDBIntegrated

# Start PostgreSQL
docker-compose up -d postgres

# Wait for database to be ready (about 10 seconds)

# Build and run Spring Boot
mvn clean package
mvn spring-boot:run
```

**Expected Output:**
```
Started ImdbApplication in X.XXX seconds
Database initialized successfully
API running at http://localhost:8080
```

#### 2. Setup Frontend

```bash
# Open a new terminal
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

**Expected Output:**
```
  VITE v5.x.x  ready in XXX ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

#### 3. Access the Application

- **Frontend UI**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Swagger Docs**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/api/movies/health

## 📚 API Endpoints Reference

### Movie Endpoints

| Method | Endpoint | Description | Example |
|--------|----------|-------------|---------|
| GET | `/api/movies` | Search movies | `/api/movies?title=inception&page=0&size=20` |
| GET | `/api/movies/{id}` | Get movie details | `/api/movies/tt1375666` |
| GET | `/api/movies/top` | Top rated movies | `/api/movies/top?limit=20` |
| GET | `/api/movies/top-rated` | Top rated (alias) | `/api/movies/top-rated?limit=20` |
| GET | `/api/movies/search` | Search by query | `/api/movies/search?query=inception` |
| GET | `/api/movies/top-by-actor` | Actor's top movies | `/api/movies/top-by-actor?actor=DiCaprio` |
| GET | `/api/movies/filter` | Filter movies | `/api/movies/filter?genre=Action&fromYear=2010` |
| GET | `/api/movies/health` | Health check | `/api/movies/health` |

### Recommendation Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/movies/suggest/start` | Start recommendation session |
| POST | `/api/movies/suggest/feedback` | Submit user feedback |
| GET | `/api/movies/suggest/{userId}` | Get recommendations |

## 🎨 Frontend Features

### 1. Movie Search
- Real-time search by movie title
- Displays results in responsive grid
- Shows ratings, year, genres

### 2. Top Rated Movies
- Browse highest rated movies from IMDb
- Minimum vote threshold filtering
- Sorted by rating and popularity

### 3. Movie Details Modal
- Click any movie card to see full details
- Displays:
  - Movie poster
  - Plot summary
  - Director and cast
  - Runtime and release year
  - User ratings and vote counts
  - Genre tags

### 4. Responsive Design
- Mobile-first approach
- Adaptive grid layout:
  - Mobile: 1 column
  - Tablet: 2 columns
  - Desktop: 3-4 columns

## 🔧 Configuration

### Backend Configuration (`application.yml`)

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/imdb}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
```

### Frontend Configuration (`vite.config.js`)

```javascript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

## 🧪 Testing

### Backend Tests
```bash
mvn test
```

### Frontend Manual Testing
1. Search for "Inception"
2. Click on a movie card
3. Verify modal shows details
4. Close modal
5. Click "Top Rated" button
6. Verify top movies load

## 📊 Database Schema

### Key Tables
- `title_basics` - Movie information
- `title_ratings` - Ratings and votes
- `name_basics` - Actors and directors
- `title_principals` - Cast/crew relationships
- `user_preferences` - User settings
- `user_feedback` - User ratings

### Materialized View
- `movie_search_view` - Optimized for fast searches

## 🚢 Deployment

### Production Build

#### Frontend
```bash
cd frontend
npm run build
```
Outputs to `frontend/dist/`

#### Backend
```bash
mvn clean package
```
Creates JAR in `target/`

### Docker Deployment
```bash
docker-compose up --build
```

## 🐛 Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Find and kill process on port 8080 (backend)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Kill process on port 3000 (frontend)
npx kill-port 3000
```

#### Database Connection Failed
```bash
# Check if PostgreSQL is running
docker ps

# Restart PostgreSQL
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

#### Frontend Can't Connect to Backend
1. Verify backend is running: `curl http://localhost:8080/api/movies/health`
2. Check CORS configuration in `CorsConfig.java`
3. Verify proxy settings in `vite.config.js`

#### No Movies Showing
1. Check if database has data: Database initializes with sample data automatically
2. Verify API response: `curl http://localhost:8080/api/movies/top`
3. Check browser console for errors

## 📈 Performance Optimization

### Backend
- Database connection pooling (HikariCP)
- Materialized views for fast queries
- GIN indexes for full-text search
- Query result caching

### Frontend
- Code splitting with Vite
- Lazy loading of images
- React memo for expensive components
- Debounced search input

## 🔐 Security Considerations

### Current Setup (Development)
- CORS enabled for localhost
- No authentication required
- Direct database access

### Production Recommendations
- Add JWT authentication
- Implement rate limiting
- Use environment variables for secrets
- Enable HTTPS
- Add input validation
- SQL injection prevention (using prepared statements)

## 📝 Next Steps

### Immediate Improvements
1. Add loading skeletons
2. Implement error boundaries
3. Add toast notifications
4. Improve accessibility (ARIA labels)

### Future Features
1. User authentication
2. Favorites/Watchlist
3. Movie trailers integration
4. Advanced filtering
5. Social features (reviews, ratings)
6. Recommendation engine improvements
7. Mobile app (React Native)

## 🤝 Contributing

### Code Style
- **Backend**: Google Java Format
- **Frontend**: ESLint + Prettier

### Git Workflow
1. Create feature branch
2. Make changes
3. Run tests
4. Submit PR
5. CI/CD pipeline validates

## 📞 Support

- Check documentation in `/frontend/SETUP.md`
- Review API docs at `/swagger-ui.html`
- Check GitHub issues

---

**Happy Coding! 🎬**
