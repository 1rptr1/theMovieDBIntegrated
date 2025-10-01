# Frontend Setup Guide

## First Time Setup

### 1. Install Dependencies

```bash
cd frontend
npm install
```

This will install:
- React 18
- Vite (build tool)
- TailwindCSS (styling)
- shadcn/ui components
- Lucide React (icons)

### 2. Start Development Server

```bash
npm run dev
```

The application will start at `http://localhost:3000`

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/              # shadcn/ui components
│   │   │   ├── button.jsx
│   │   │   ├── card.jsx
│   │   │   └── input.jsx
│   │   ├── MovieCard.jsx    # Movie card component
│   │   └── MovieModal.jsx   # Movie details modal
│   ├── lib/
│   │   └── utils.js         # Utility functions
│   ├── App.jsx              # Main application
│   ├── main.jsx             # React entry point
│   └── index.css            # Global styles
├── index.html               # HTML template
├── vite.config.js           # Vite configuration
├── tailwind.config.js       # Tailwind configuration
└── package.json             # Dependencies

```

## Features

### Movie Search
- Real-time search as you type
- Search by movie title
- Responsive grid layout

### Top Rated Movies
- Browse highest rated movies
- Sorted by rating and vote count
- Beautiful card-based UI

### Movie Details
- Click any movie card to see details
- View plot, cast, director, and ratings
- High-quality movie posters from OMDb API

## API Integration

The frontend connects to the Spring Boot backend via Vite's proxy configuration:

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

This means:
- Frontend makes requests to `/api/movies`
- Vite proxies to `http://localhost:8080/api/movies`
- No CORS issues during development

## Customization

### Changing Colors

Edit `src/index.css` to modify the color scheme:

```css
:root {
  --primary: 210 40% 98%;      /* Primary color */
  --background: 222.2 84% 4.9%; /* Background color */
  /* ... more colors */
}
```

### Adding New Features

1. Create component in `src/components/`
2. Import in `App.jsx`
3. Add API call if needed
4. Style with Tailwind classes

## Troubleshooting

### Port 3000 already in use
```bash
# Kill the process using port 3000
npx kill-port 3000
# Or specify a different port
npm run dev -- --port 3001
```

### Module not found errors
```bash
# Clear node_modules and reinstall
rm -rf node_modules
npm install
```

### API not connecting
1. Make sure Spring Boot is running on port 8080
2. Check `vite.config.js` proxy settings
3. Verify CORS is configured in backend

## Production Build

```bash
npm run build
```

This creates an optimized build in the `dist/` folder ready for deployment.

### Preview Production Build

```bash
npm run preview
```

## Environment Variables

Create a `.env` file in the frontend directory for environment-specific settings:

```env
VITE_API_URL=http://localhost:8080
```

Access in code:
```javascript
const apiUrl = import.meta.env.VITE_API_URL
```

## Next Steps

- Add user authentication
- Implement favorites/watchlist
- Add advanced filters (genre, year range, etc.)
- Implement infinite scroll
- Add movie trailers
- Social sharing features
