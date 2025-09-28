package com.integrated.imdb.dto;

import java.util.List;
import java.util.Objects;

public class SuggestResponse {
    private String userId;
    private List<MovieDto> recommendations;
    private String message;
    
    public SuggestResponse() {
    }
    
    public SuggestResponse(String userId, List<MovieDto> recommendations, String message) {
        this.userId = userId;
        this.recommendations = recommendations;
        this.message = message;
    }
    
    public SuggestResponse(String userId, List<MovieDto> recommendations) {
        this.userId = userId;
        this.recommendations = recommendations;
        this.message = "Recommendations generated successfully";
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<MovieDto> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<MovieDto> recommendations) {
        this.recommendations = recommendations;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuggestResponse that = (SuggestResponse) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(recommendations, that.recommendations) &&
               Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, recommendations, message);
    }

    @Override
    public String toString() {
        return "SuggestResponse{" +
               "userId='" + userId + '\'' +
               ", recommendations=" + recommendations +
               ", message='" + message + '\'' +
               '}';
    }
}
