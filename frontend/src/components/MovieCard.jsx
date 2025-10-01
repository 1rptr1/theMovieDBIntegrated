import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Star, Calendar, Clock } from "lucide-react"

export function MovieCard({ movie, onClick }) {
  return (
    <Card 
      className="hover:shadow-lg transition-shadow cursor-pointer group overflow-hidden"
      onClick={() => onClick?.(movie)}
    >
      <div className="relative">
        {movie.poster && movie.poster !== 'N/A' ? (
          <img
            src={movie.poster}
            alt={movie.primaryTitle}
            className="w-full h-64 object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="w-full h-64 bg-gradient-to-br from-purple-900 to-blue-900 flex items-center justify-center">
            <span className="text-6xl">🎬</span>
          </div>
        )}
        {movie.averageRating && (
          <div className="absolute top-2 right-2 bg-black/80 backdrop-blur-sm px-2 py-1 rounded-md flex items-center gap-1">
            <Star className="w-4 h-4 fill-yellow-400 text-yellow-400" />
            <span className="font-semibold">{movie.averageRating.toFixed(1)}</span>
          </div>
        )}
      </div>
      
      <CardHeader>
        <CardTitle className="line-clamp-2 group-hover:text-primary transition-colors">
          {movie.primaryTitle}
        </CardTitle>
      </CardHeader>
      
      <CardContent>
        <div className="space-y-2 text-sm text-muted-foreground">
          {movie.startYear && (
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4" />
              <span>{movie.startYear}</span>
            </div>
          )}
          {movie.runtime && (
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4" />
              <span>{movie.runtime}</span>
            </div>
          )}
          {movie.genres && (
            <div className="flex flex-wrap gap-1 mt-2">
              {movie.genres.split(',').slice(0, 3).map((genre, idx) => (
                <span
                  key={idx}
                  className="px-2 py-1 bg-secondary text-secondary-foreground rounded-full text-xs"
                >
                  {genre.trim()}
                </span>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
