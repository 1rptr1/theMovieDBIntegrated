package com.integrated.imdb.dto;

import java.util.Objects;

public class StartRequest {
    private String query;
    private String userId;

    public StartRequest() {
    }

    public StartRequest(String query, String userId) {
        this.query = query;
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StartRequest that = (StartRequest) o;
        return Objects.equals(query, that.query) &&
               Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, userId);
    }

    @Override
    public String toString() {
        return "StartRequest{" +
               "query='" + query + '\'' +
               ", userId='" + userId + '\'' +
               '}';
    }
}
