package com.integrated.imdb.dto;

import java.util.List;
import java.util.Objects;

public class FeedbackRequest {
    private String userId;
    private List<String> likedMovieIds;
    private List<String> dislikedMovieIds;

    public FeedbackRequest() {
    }

    public FeedbackRequest(String userId, List<String> likedMovieIds, List<String> dislikedMovieIds) {
        this.userId = userId;
        this.likedMovieIds = likedMovieIds;
        this.dislikedMovieIds = dislikedMovieIds;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getLikedMovieIds() {
        return likedMovieIds;
    }

    public void setLikedMovieIds(List<String> likedMovieIds) {
        this.likedMovieIds = likedMovieIds;
    }

    public List<String> getDislikedMovieIds() {
        return dislikedMovieIds;
    }

    public void setDislikedMovieIds(List<String> dislikedMovieIds) {
        this.dislikedMovieIds = dislikedMovieIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeedbackRequest that = (FeedbackRequest) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(likedMovieIds, that.likedMovieIds) &&
               Objects.equals(dislikedMovieIds, that.dislikedMovieIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, likedMovieIds, dislikedMovieIds);
    }

    @Override
    public String toString() {
        return "FeedbackRequest{" +
               "userId='" + userId + '\'' +
               ", likedMovieIds=" + likedMovieIds +
               ", dislikedMovieIds=" + dislikedMovieIds +
               '}';
    }
}
