# IMDb Movie Explorer - Frontend

A modern, beautiful UI for exploring IMDb movies built with React, Vite, TailwindCSS, and shadcn/ui.

## Features

- 🎬 **Movie Search** - Search for movies by title
- ⭐ **Top Rated** - Browse top rated movies
- 📱 **Responsive Design** - Works on all devices
- 🎨 **Modern UI** - Beautiful dark theme with smooth animations
- 🔍 **Movie Details** - Click any movie to see full details including plot, cast, and ratings

## Tech Stack

- **React 18** - Modern React with hooks
- **Vite** - Lightning fast build tool
- **TailwindCSS** - Utility-first CSS framework
- **shadcn/ui** - Beautiful, accessible components
- **Lucide React** - Modern icon library

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

1. Install dependencies:
```bash
cd frontend
npm install
```

2. Start the development server:
```bash
npm run dev
```

The app will be available at `http://localhost:3000`

### Build for Production

```bash
npm run build
```

The built files will be in the `dist` folder.

## API Integration

The frontend connects to the Spring Boot API running on `http://localhost:8080`. Make sure the backend is running before starting the frontend.

The Vite dev server is configured to proxy API requests:
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build locally
