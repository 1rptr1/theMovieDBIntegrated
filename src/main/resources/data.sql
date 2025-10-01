-- =====================================================
-- Sample Data for Local Development
-- (This won't run in CI - real IMDb data is loaded there)
-- =====================================================

-- Insert sample movie data for testing
INSERT INTO title_basics (tconst, title_type, primary_title, original_title, is_adult, start_year, end_year, runtime_minutes, genres)
VALUES 
    ('tt1375666', 'movie', 'Inception', 'Inception', false, 2010, NULL, 148, 'Action,Adventure,Sci-Fi'),
    ('tt0111161', 'movie', 'The Shawshank Redemption', 'The Shawshank Redemption', false, 1994, NULL, 142, 'Drama'),
    ('tt0068646', 'movie', 'The Godfather', 'The Godfather', false, 1972, NULL, 175, 'Crime,Drama'),
    ('tt0468569', 'movie', 'The Dark Knight', 'The Dark Knight', false, 2008, NULL, 152, 'Action,Crime,Drama'),
    ('tt0108052', 'movie', 'Schindler''s List', 'Schindler''s List', false, 1993, NULL, 195, 'Biography,Drama,History'),
    ('tt0167260', 'movie', 'The Lord of the Rings: The Return of the King', 'The Lord of the Rings: The Return of the King', false, 2003, NULL, 201, 'Action,Adventure,Drama'),
    ('tt0109830', 'movie', 'Forrest Gump', 'Forrest Gump', false, 1994, NULL, 142, 'Drama,Romance'),
    ('tt0137523', 'movie', 'Fight Club', 'Fight Club', false, 1999, NULL, 139, 'Drama'),
    ('tt0133093', 'movie', 'The Matrix', 'The Matrix', false, 1999, NULL, 136, 'Action,Sci-Fi'),
    ('tt0073486', 'movie', 'One Flew Over the Cuckoo''s Nest', 'One Flew Over the Cuckoo''s Nest', false, 1975, NULL, 133, 'Drama')
ON CONFLICT (tconst) DO NOTHING;

-- Insert sample ratings for the movies
INSERT INTO title_ratings (tconst, average_rating, num_votes)
VALUES 
    ('tt1375666', 8.8, 2500000),
    ('tt0111161', 9.3, 2800000),
    ('tt0068646', 9.2, 1900000),
    ('tt0468569', 9.0, 2700000),
    ('tt0108052', 9.0, 1400000),
    ('tt0167260', 9.0, 1900000),
    ('tt0109830', 8.8, 2100000),
    ('tt0137523', 8.8, 2200000),
    ('tt0133093', 8.7, 1900000),
    ('tt0073486', 8.7, 1000000)
ON CONFLICT (tconst) DO NOTHING;

-- Insert sample name_basics (actors/directors)
INSERT INTO name_basics (nconst, primary_name, birth_year, death_year, primary_profession, known_for_titles)
VALUES 
    ('nm0000138', 'Leonardo DiCaprio', '1974', NULL, 'actor,producer', 'tt1375666,tt0110413,tt0993846'),
    ('nm0000125', 'Christopher Nolan', '1970', NULL, 'director,writer,producer', 'tt1375666,tt0468569,tt6723592'),
    ('nm0000142', 'Tom Hanks', '1956', NULL, 'actor,producer', 'tt0109830,tt0120815,tt0245429'),
    ('nm0000134', 'Robert De Niro', '1943', NULL, 'actor,producer', 'tt0068646,tt0075314,tt0081398'),
    ('nm0000122', 'Christian Bale', '1974', NULL, 'actor,producer', 'tt0468569,tt0810913,tt1345836')
ON CONFLICT (nconst) DO NOTHING;

-- Insert sample title_principals (cast/crew relationships)
INSERT INTO title_principals (tconst, ordering, nconst, category, job, characters)
VALUES 
    ('tt1375666', 1, 'nm0000138', 'actor', NULL, '["Dom Cobb"]'),
    ('tt1375666', 2, 'nm0000125', 'director', NULL, NULL),
    ('tt0109830', 1, 'nm0000142', 'actor', NULL, '["Forrest Gump"]'),
    ('tt0068646', 1, 'nm0000134', 'actor', NULL, '["Vito Corleone"]'),
    ('tt0468569', 1, 'nm0000122', 'actor', NULL, '["Bruce Wayne","Batman"]'),
    ('tt0468569', 2, 'nm0000125', 'director', NULL, NULL)
ON CONFLICT (tconst, nconst, category) DO NOTHING;

-- Insert some sample user preferences (for development only)
INSERT INTO user_preferences (user_id, preferences)
VALUES 
    ('test_user_1', '{"preferredGenres": ["Action", "Drama", "Sci-Fi"], "preferredActors": ["Tom Hanks", "Meryl Streep"], "lastUpdated": "2025-09-27T00:00:00Z"}'),
    ('user123', '{"preferredGenres": ["Action", "Sci-Fi"], "preferredActors": ["Leonardo DiCaprio", "Christian Bale"], "lastUpdated": "2025-10-01T00:00:00Z"}')
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
