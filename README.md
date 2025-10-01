# The Movie DB Integrated

A full-stack movie discovery application with a Spring Boot backend and modern React frontend. Explore movies from the IMDb dataset with intelligent recommendations and beautiful UI.

## 🎬 Features

- **Movie Search** - Search movies by title, actor, genre, or year
- **Top Rated Movies** - Browse the highest rated movies
- **Movie Details** - View comprehensive information including plot, cast, ratings, and more
- **Recommendations** - Get personalized movie recommendations based on your preferences
- **Modern UI** - Beautiful, responsive dark-themed interface
- **Real IMDb Data** - Powered by actual IMDb datasets in CI/CD

## Prerequisites

### Backend
- Java 21 or higher
- Maven 3.8+
- PostgreSQL 15+

### Frontend
- Node.js 18+
- npm or yarn

### Optional
- Docker and Docker Compose
- OMDb API key (get it from http://www.omdbapi.com/apikey.aspx) for enhanced movie details

## Quick Start

### 1. Start the Backend

```bash
# Start PostgreSQL (if using Docker)
docker-compose up -d postgres

# Build and run the Spring Boot application
mvn clean package
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### 2. Start the Frontend

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The UI will be available at `http://localhost:3000`

## Detailed Setup

### Backend Configuration

1. Clone the repository
2. Copy `.env.example` to `.env` and update the environment variables:
   ```bash
   cp .env.example .env
   ```
   Edit the `.env` file and set your OMDb API key and any other required configurations.

## Running with Docker Compose (Recommended)

1. Build and start the application:
   ```bash
   docker-compose up --build
   ```
   This will start both the PostgreSQL database and the Spring Boot application.

2. The application will be available at: http://localhost:8080

## Running Locally

1. Start PostgreSQL database:
   ```bash
   docker-compose up -d postgres
   ```

2. Build the application:
   ```bash
   mvn clean package
   ```

3. Run the application:
   ```bash
   java -jar target/*.jar
   ```

## API Documentation

Once the application is running, you can access:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI docs: http://localhost:8080/v3/api-docs

## Database Initialization

The application will automatically initialize the database schema and load initial data on startup using the scripts in `src/main/resources/db/migration/`.

## Environment Variables

- `SPRING_DATASOURCE_URL`: JDBC URL for the database (default: jdbc:postgresql://localhost:5432/imdb)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: postgres)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: postgres)
- `OMDB_API_KEY`: Your OMDb API key

## API Endpoints

### Movies
- `GET /api/movies` - Search movies with filters
- `GET /api/movies/{id}` - Get movie details by ID
- `GET /api/movies/top` - Get top-rated movies

### Recommendations
- `POST /api/suggest/start` - Start recommendation session
- `POST /api/suggest/feedback` - Submit feedback on recommendations
- `GET /api/suggest/{userId}` - Get recommendations for a user

## Development

### Building
```bash
mvn clean package
```

### Running Tests
```bash
mvn test
```

### Code Style
This project uses Google Java Format for code style. You can format the code using:
```bash
mvn spotless:apply
```

## Deployment

The application is configured to be deployed as a Docker container. You can build and push the Docker image using the provided GitHub Actions workflow.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
