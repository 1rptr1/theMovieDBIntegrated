import { useState, useEffect } from 'react'
import { Search, TrendingUp, Film, Loader2 } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { MovieCard } from '@/components/MovieCard'
import { MovieModal } from '@/components/MovieModal'

function App() {
  const [searchQuery, setSearchQuery] = useState('')
  const [movies, setMovies] = useState([])
  const [selectedMovie, setSelectedMovie] = useState(null)
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('top')

  useEffect(() => {
    loadTopMovies()
  }, [])

  const loadTopMovies = async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/movies/top-rated?limit=20')
      const data = await response.json()
      setMovies(data)
      setActiveTab('top')
    } catch (error) {
      console.error('Error loading top movies:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!searchQuery.trim()) return

    setLoading(true)
    try {
      const response = await fetch(
        `/api/movies?title=${encodeURIComponent(searchQuery)}&page=0&size=20`
      )
      const data = await response.json()
      setMovies(data)
      setActiveTab('search')
    } catch (error) {
      console.error('Error searching movies:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleMovieClick = async (movie) => {
    try {
      const response = await fetch(`/api/movies/${movie.tconst}`)
      const detailedMovie = await response.json()
      setSelectedMovie(detailedMovie)
    } catch (error) {
      console.error('Error loading movie details:', error)
      setSelectedMovie(movie)
    }
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between px-4 mx-auto">
          <div className="flex items-center gap-2">
            <Film className="w-8 h-8 text-primary" />
            <h1 className="text-2xl font-bold">IMDb Explorer</h1>
          </div>
          
          <nav className="flex gap-2">
            <Button
              variant={activeTab === 'top' ? 'default' : 'ghost'}
              onClick={loadTopMovies}
              className="gap-2"
            >
              <TrendingUp className="w-4 h-4" />
              Top Rated
            </Button>
          </nav>
        </div>
      </header>

      {/* Search Bar */}
      <div className="container mx-auto px-4 py-8">
        <form onSubmit={handleSearch} className="max-w-2xl mx-auto">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search for movies..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
            <Button type="submit" disabled={loading}>
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Search'}
            </Button>
          </div>
        </form>
      </div>

      {/* Movies Grid */}
      <div className="container mx-auto px-4 pb-8">
        {loading ? (
          <div className="flex justify-center items-center py-20">
            <Loader2 className="w-12 h-12 animate-spin text-primary" />
          </div>
        ) : movies.length > 0 ? (
          <>
            <div className="mb-6">
              <h2 className="text-2xl font-bold">
                {activeTab === 'top' ? 'Top Rated Movies' : `Search Results (${movies.length})`}
              </h2>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {movies.map((movie) => (
                <MovieCard
                  key={movie.tconst}
                  movie={movie}
                  onClick={handleMovieClick}
                />
              ))}
            </div>
          </>
        ) : (
          <div className="text-center py-20">
            <Film className="w-20 h-20 mx-auto text-muted-foreground mb-4" />
            <h3 className="text-xl font-semibold mb-2">No movies found</h3>
            <p className="text-muted-foreground">
              Try searching for a different movie or explore top rated movies
            </p>
          </div>
        )}
      </div>

      {/* Movie Modal */}
      {selectedMovie && (
        <MovieModal
          movie={selectedMovie}
          onClose={() => setSelectedMovie(null)}
        />
      )}
    </div>
  )
}

export default App
