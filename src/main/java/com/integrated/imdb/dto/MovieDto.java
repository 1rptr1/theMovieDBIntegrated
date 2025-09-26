package com.integrated.imdb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {
    private String tconst;
    private String primaryTitle;
    private String startYear;
    private String genres;
    private String actorName;
    private Double averageRating;
    private Integer numVotes;
    private String plot;
    private String poster;
    private String runtime;
    private String director;
    private String cast;
}
