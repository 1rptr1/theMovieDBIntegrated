FROM postgres:15
  
# Install wget, gzip
RUN apt-get update && apt-get install -y wget gzip && rm -rf /var/lib/apt/lists/*
  
# Download IMDb datasets
RUN mkdir -p /imdb_data && cd /imdb_data && \
wget https://datasets.imdbws.com/name.basics.tsv.gz && \
wget https://datasets.imdbws.com/title.basics.tsv.gz && \
wget https://datasets.imdbws.com/title.akas.tsv.gz && \
wget https://datasets.imdbws.com/title.crew.tsv.gz && \
wget https://datasets.imdbws.com/title.episode.tsv.gz && \
wget https://datasets.imdbws.com/title.principals.tsv.gz && \
wget https://datasets.imdbws.com/title.ratings.tsv.gz && \
gunzip *.gz
  
# Copy schema & loader
COPY ./src/main/resources/db/migration/*.sql /docker-entrypoint-initdb.d/
