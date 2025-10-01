#!/usr/bin/env node

import { spawn } from 'child_process'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

const isWindows = process.platform === 'win32'

console.log('🚀 Starting IMDb Movie Explorer...\n')

// Start PostgreSQL
console.log('📊 Starting PostgreSQL database...')
const postgresProcess = spawn(isWindows ? 'docker-compose' : 'docker-compose', ['up', '-d', 'postgres'], {
  cwd: join(__dirname, '..'),
  stdio: 'pipe',
  shell: isWindows
})

// Wait for database to be ready
await new Promise(resolve => setTimeout(resolve, 10000))

// Start Spring Boot backend
console.log('🔧 Starting Spring Boot API...')
const backendProcess = spawn(isWindows ? 'mvn' : './mvnw', ['spring-boot:run'], {
  cwd: join(__dirname, '..'),
  stdio: 'pipe',
  shell: isWindows
})

// Wait for backend to be ready
await new Promise(resolve => setTimeout(resolve, 20000))

// Start frontend
console.log('🎨 Starting React frontend...')
const frontendProcess = spawn('npm', ['run', 'dev'], {
  cwd: join(__dirname, 'frontend'),
  stdio: 'pipe',
  shell: isWindows
})

// Handle process termination
process.on('SIGINT', () => {
  console.log('\n🛑 Shutting down services...')
  postgresProcess.kill()
  backendProcess.kill()
  frontendProcess.kill()
  process.exit(0)
})

process.on('SIGTERM', () => {
  console.log('\n🛑 Shutting down services...')
  postgresProcess.kill()
  backendProcess.kill()
  frontendProcess.kill()
  process.exit(0)
})

// Display status
console.log('\n' + '='.repeat(50))
console.log('✅ Services started successfully!')
console.log('🎬 Frontend: http://localhost:3000')
console.log('🔧 Backend API: http://localhost:8080')
console.log('📚 API Docs: http://localhost:8080/swagger-ui.html')
console.log('🗃️  Database: postgresql://localhost:5432/imdb')
console.log('='.repeat(50))
console.log('\nPress Ctrl+C to stop all services\n')

// Keep script running
await new Promise(() => {})
