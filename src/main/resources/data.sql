-- Insert some sample user preferences (for development only)
INSERT INTO user_preferences (user_id, preferences)
VALUES 
    ('test_user_1', '{"preferredGenres": ["Action", "Drama", "Sci-Fi"], "preferredActors": ["Tom Hanks", "Meryl Streep"], "lastUpdated": "2025-09-27T00:00:00Z"}')
ON CONFLICT (user_id) DO NOTHING;

-- Insert some sample feedback (for development only)
INSERT INTO user_feedback (user_id, movie_id, liked)
VALUES 
    ('test_user_1', 'tt0111161', true),  -- The Shawshank Redemption
    ('test_user_1', 'tt0068646', true),  -- The Godfather
    ('test_user_1', 'tt0468569', true),  -- The Dark Knight
    ('test_user_1', 'tt0108052', false), -- Schindler's List (disliked)
    ('test_user_1', 'tt0167260', true)   -- The Lord of the Rings: The Return of the King
ON CONFLICT (user_id, movie_id) DO NOTHING;

-- Create a view for movie recommendations
CREATE OR REPLACE VIEW movie_recommendations AS
WITH user_prefs AS (
    SELECT 
        user_id,
        jsonb_array_elements_text(preferences->'preferredGenres') AS genre,
        jsonb_array_elements_text(preferences->'preferredActors') AS actor
    FROM user_preferences
),
user_likes AS (
    SELECT user_id, movie_id
    FROM user_feedback
    WHERE liked = true
),
user_dislikes AS (
    SELECT user_id, movie_id
    FROM user_feedback
    WHERE liked = false
)
SELECT 
    up.user_id,
    tb.tconst,
    tb.primary_title,
    tb.genres,
    tb.start_year,
    tr.average_rating,
    tr.num_votes,
    -- Calculate a recommendation score
    (
        -- Base score from rating and number of votes
        (COALESCE(tr.average_rating, 0) * 0.7) + 
        (LOG(COALESCE(tr.num_votes, 1)) * 0.3) +
        -- Bonus for matching preferred genres
        (SELECT COUNT(*) * 0.5 
         FROM user_prefs upg 
         WHERE upg.user_id = up.user_id 
         AND tb.genres ILIKE '%' || upg.genre || '%') +
        -- Bonus for matching preferred actors
        (SELECT COUNT(*) * 0.3 
         FROM user_prefs upa
         JOIN title_principals tp ON tp.tconst = tb.tconst
         JOIN name_basics n ON tp.nconst = n.nconst
         WHERE upa.user_id = up.user_id 
         AND n.primary_name = upa.actor)
    ) AS recommendation_score
FROM 
    user_preferences up
CROSS JOIN 
    title_basics tb
LEFT JOIN 
    title_ratings tr ON tb.tconst = tr.tconst
LEFT JOIN
    user_likes ul ON up.user_id = ul.user_id AND tb.tconst = ul.movie_id
LEFT JOIN
    user_dislikes ud ON up.user_id = ud.user_id AND tb.tconst = ud.movie_id
WHERE
    tb.title_type = 'movie'
    AND ul.movie_id IS NULL  -- Not already liked
    AND ud.movie_id IS NULL  -- Not already disliked
    AND tr.num_votes > 1000  -- Minimum votes threshold
ORDER BY
    up.user_id,
    recommendation_score DESC;

-- Create a function to get personalized recommendations
CREATE OR REPLACE FUNCTION get_personalized_recommendations(
    p_user_id TEXT,
    p_limit INTEGER DEFAULT 20
)
RETURNS TABLE (
    tconst TEXT,
    primary_title TEXT,
    genres TEXT,
    start_year INTEGER,
    average_rating FLOAT,
    num_votes INTEGER,
    recommendation_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        mr.tconst,
        mr.primary_title,
        mr.genres,
        mr.start_year,
        mr.average_rating,
        mr.num_votes,
        mr.recommendation_score
    FROM 
        movie_recommendations mr
    WHERE 
        mr.user_id = p_user_id
    ORDER BY 
        mr.recommendation_score DESC
    LIMIT 
        p_limit;
END;
$$ LANGUAGE plpgsql;
