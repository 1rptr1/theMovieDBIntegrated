package com.integrated.imdb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    private String userId;
    private List<String> likedMovieIds;
    private List<String> dislikedMovieIds;
}
