package com.integrated.imdb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestResponse {
    private String userId;
    private List<MovieDto> recommendations;
    private String message;
    
    public SuggestResponse(String userId, List<MovieDto> recommendations) {
        this.userId = userId;
        this.recommendations = recommendations;
        this.message = "Recommendations generated successfully";
    }
}
