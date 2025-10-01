import { X, Star, Calendar, Clock, Users, User } from "lucide-react"
import { Button } from "@/components/ui/button"

export function MovieModal({ movie, onClose }) {
  if (!movie) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-card rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto shadow-2xl">
        <div className="sticky top-0 bg-card/95 backdrop-blur-sm z-10 p-4 border-b flex justify-between items-center">
          <h2 className="text-2xl font-bold">{movie.primaryTitle}</h2>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="w-5 h-5" />
          </Button>
        </div>

        <div className="p-6">
          <div className="grid md:grid-cols-3 gap-6">
            {/* Poster */}
            <div className="md:col-span-1">
              {movie.poster && movie.poster !== 'N/A' ? (
                <img
                  src={movie.poster}
                  alt={movie.primaryTitle}
                  className="w-full rounded-lg shadow-lg"
                />
              ) : (
                <div className="w-full aspect-[2/3] bg-gradient-to-br from-purple-900 to-blue-900 rounded-lg flex items-center justify-center">
                  <span className="text-8xl">🎬</span>
                </div>
              )}
            </div>

            {/* Details */}
            <div className="md:col-span-2 space-y-6">
              {/* Rating & Meta */}
              <div className="flex flex-wrap gap-4">
                {movie.averageRating && (
                  <div className="flex items-center gap-2 bg-secondary px-4 py-2 rounded-lg">
                    <Star className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                    <span className="font-bold text-lg">{movie.averageRating.toFixed(1)}</span>
                    {movie.numVotes && (
                      <span className="text-sm text-muted-foreground">
                        ({movie.numVotes.toLocaleString()} votes)
                      </span>
                    )}
                  </div>
                )}
                
                {movie.startYear && (
                  <div className="flex items-center gap-2 bg-secondary px-4 py-2 rounded-lg">
                    <Calendar className="w-5 h-5" />
                    <span>{movie.startYear}</span>
                  </div>
                )}
                
                {movie.runtime && (
                  <div className="flex items-center gap-2 bg-secondary px-4 py-2 rounded-lg">
                    <Clock className="w-5 h-5" />
                    <span>{movie.runtime}</span>
                  </div>
                )}
              </div>

              {/* Genres */}
              {movie.genres && (
                <div>
                  <h3 className="text-sm font-semibold text-muted-foreground mb-2">GENRES</h3>
                  <div className="flex flex-wrap gap-2">
                    {movie.genres.split(',').map((genre, idx) => (
                      <span
                        key={idx}
                        className="px-3 py-1 bg-primary/10 text-primary rounded-full text-sm font-medium"
                      >
                        {genre.trim()}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Plot */}
              {movie.plot && movie.plot !== 'Plot not available' && (
                <div>
                  <h3 className="text-sm font-semibold text-muted-foreground mb-2">PLOT</h3>
                  <p className="text-foreground leading-relaxed">{movie.plot}</p>
                </div>
              )}

              {/* Director */}
              {movie.director && (
                <div>
                  <h3 className="text-sm font-semibold text-muted-foreground mb-2 flex items-center gap-2">
                    <User className="w-4 h-4" />
                    DIRECTOR
                  </h3>
                  <p className="text-foreground">{movie.director}</p>
                </div>
              )}

              {/* Cast */}
              {movie.cast && (
                <div>
                  <h3 className="text-sm font-semibold text-muted-foreground mb-2 flex items-center gap-2">
                    <Users className="w-4 h-4" />
                    CAST
                  </h3>
                  <p className="text-foreground">{movie.cast}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
