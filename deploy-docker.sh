#!/bin/bash

echo "🐳 IMDb Movie Explorer - Complete Docker Deployment"
echo "=================================================="

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo "🔍 Checking prerequisites..."

if ! command_exists docker; then
    echo "❌ Docker is not installed"
    echo "   Please install Docker and try again"
    exit 1
fi

if ! command_exists docker-compose; then
    echo "❌ Docker Compose is not installed"
    echo "   Please install Docker Compose and try again"
    exit 1
fi

echo "✅ All prerequisites found!"
echo

# Build the complete Docker image
echo "🔨 Building complete Docker image..."
echo "   This includes: Spring Boot + React + PostgreSQL + Sample Data"
echo

docker-compose -f docker-compose.full.yml build --no-cache

if [ $? -ne 0 ]; then
    echo "❌ Failed to build Docker image"
    exit 1
fi

echo "✅ Docker image built successfully!"
echo

# Run the complete application
echo "🚀 Starting IMDb Movie Explorer..."
echo
echo "📍 Application will be available at:"
echo "   🎬 Frontend + Backend: http://localhost:8080"
echo "   📚 API Documentation: http://localhost:8080/swagger-ui.html"
echo "   🗃️  Database: postgresql://localhost:5432/imdb (inside container)"
echo
echo "=================================================="
echo

# Start the container
docker-compose -f docker-compose.full.yml up -d

if [ $? -ne 0 ]; then
    echo "❌ Failed to start application"
    exit 1
fi

echo "✅ Application started successfully!"
echo
echo "📝 Container Status:"
docker-compose -f docker-compose.full.yml ps
echo
echo "🛑 To stop the application:"
echo "   docker-compose -f docker-compose.full.yml down"
echo
echo "🔄 To rebuild and restart:"
echo "   $0"
echo
echo "🎉 Your complete IMDb Movie Explorer is now running!"
echo "   Access it at: http://localhost:8080"

# Optional: Wait for health check
echo
echo "⏳ Waiting for application to be ready..."
sleep 30

# Check if application is responding
if curl -f http://localhost:8080/api/movies/health >/dev/null 2>&1; then
    echo "✅ Application is ready!"
    echo "🎬 You can now browse movies at: http://localhost:8080"
else
    echo "⚠️  Application is starting up... please wait a moment"
    echo "   Check status with: docker-compose -f docker-compose.full.yml logs"
fi
