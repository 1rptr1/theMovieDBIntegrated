-- =====================================
-- IMDB Tables
-- =====================================

CREATE TABLE IF NOT EXISTS name_basics (
    nconst TEXT PRIMARY KEY,
    primary_name TEXT,
    birth_year TEXT,
    death_year TEXT,
    primary_profession TEXT,
    known_for_titles TEXT
);

CREATE TABLE IF NOT EXISTS title_basics (
    tconst TEXT PRIMARY KEY,
    title_type TEXT,
    primary_title TEXT,
    original_title TEXT,
    is_adult BOOLEAN,
    start_year INTEGER,
    end_year INTEGER,
    runtime_minutes INTEGER,
    genres TEXT
);

CREATE TABLE IF NOT EXISTS title_principals (
    tconst TEXT,
    ordering INTEGER,
    nconst TEXT,
    category TEXT,
    job TEXT,
    characters TEXT,
    PRIMARY KEY (tconst, nconst, category)
);

CREATE TABLE IF NOT EXISTS title_ratings (
    tconst TEXT PRIMARY KEY,
    average_rating FLOAT,
    num_votes INTEGER
);

-- =====================================
-- Recommendation System Tables
-- =====================================

CREATE TABLE IF NOT EXISTS user_preferences (
    user_id VARCHAR(50) PRIMARY KEY,
    preferences JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_feedback (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    movie_id VARCHAR(20) NOT NULL,
    liked BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, movie_id)
);

-- =====================================
-- Indexes
-- =====================================

CREATE INDEX IF NOT EXISTS idx_name_basics_primary_name ON name_basics(primary_name);
CREATE INDEX IF NOT EXISTS idx_title_basics_primary_title ON title_basics(primary_title);
CREATE INDEX IF NOT EXISTS idx_title_principals_nconst ON title_principals(nconst);
CREATE INDEX IF NOT EXISTS idx_title_ratings_rating ON title_ratings(average_rating);
CREATE INDEX IF NOT EXISTS idx_user_feedback_user_id ON user_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_user_feedback_movie_id ON user_feedback(movie_id);

-- Enable pg_trgm for text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_title_basics_gin ON title_basics USING GIN (primary_title gin_trgm_ops);
-- Materialized View for Movie Search
-- =====================================

CREATE MATERIALIZED VIEW IF NOT EXISTS movie_search_view AS 
SELECT 
    tb.tconst, 
    tb.primary_title, 
    tb.original_title, 
    tb.start_year, 
    tb.genres, 
    tr.average_rating, 
    tr.num_votes, 
    string_agg(DISTINCT n.primary_name, ', ' ORDER BY n.primary_name) AS actors, 
    setweight(to_tsvector('english', tb.primary_title), 'A') || 
    setweight(to_tsvector('english', tb.original_title), 'B') || 
    setweight(to_tsvector('english', COALESCE(tb.genres, '')), 'C') || 
    setweight(to_tsvector('english', COALESCE(string_agg(n.primary_name, ' '), '')), 'D') AS search_vector 
FROM title_basics tb
    LEFT JOIN title_ratings tr ON tb.tconst = tr.tconst 
    LEFT JOIN title_principals tp ON tb.tconst = tp.tconst 
    LEFT JOIN name_basics n ON tp.nconst = n.nconst 
WHERE 
    tb.title_type = 'movie' 
GROUP BY tb.tconst, tb.primary_title, tb.original_title, tb.start_year, tb.genres, tr.average_rating, tr.num_votes;

CREATE INDEX IF NOT EXISTS idx_movie_search_vector ON movie_search_view USING GIN (search_vector);

-- Unique index required for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_movie_search_tconst 
ON movie_search_view (tconst);

-- =====================================
-- Functions & Triggers
-- Function and Trigger for refreshing materialized view

-- First, ensure the function exists
CREATE OR REPLACE FUNCTION refresh_movie_search()
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY movie_search_view;
END;
$$;

-- Drop trigger first (safe cleanup)
DROP TRIGGER IF EXISTS refresh_movie_search_after_update ON title_basics;

-- Drop trigger function if exists
DROP FUNCTION IF EXISTS trigger_refresh_movie_search() CASCADE;

-- Create trigger function
CREATE OR REPLACE FUNCTION trigger_refresh_movie_search()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM refresh_movie_search();
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER refresh_movie_search_after_update
AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE
ON title_basics
FOR EACH STATEMENT
EXECUTE FUNCTION trigger_refresh_movie_search();
-- =====================================
-- Full-text search function
-- =====================================

CREATE OR REPLACE FUNCTION search_movies(query TEXT)
RETURNS TABLE (
    tconst TEXT,
    primary_title TEXT,
    original_title TEXT,
    start_year INTEGER,
    genres TEXT,
    average_rating FLOAT,
    num_votes INTEGER,
    actors TEXT,
    rank FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        msv.tconst,
        msv.primary_title,
        msv.original_title,
        msv.start_year,
        msv.genres,
        msv.average_rating,
        msv.num_votes,
        msv.actors,
        ts_rank(msv.search_vector, websearch_to_tsquery('english', query)) AS rank
    FROM
        movie_search_view msv
    WHERE
        msv.search_vector @@ websearch_to_tsquery('english', query)
    ORDER BY
        rank DESC,
        msv.num_votes DESC
    LIMIT 100;
END;
$$ LANGUAGE plpgsql;
