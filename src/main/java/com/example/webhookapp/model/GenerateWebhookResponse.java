package com.example.webhookapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateWebhookResponse {
    @JsonProperty("webhook")
    private String webhook;
    
    @JsonProperty("accessToken")
    private String accessToken;
    
    @JsonProperty("data")
    private Data data;

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        @JsonProperty("users")
        private Users users;
    }

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Users {
        @JsonProperty("n")
        private int n;
        
        @JsonProperty("findId")
        private int findId;
        
        @JsonProperty("users")
        private List<User> users;
    }

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        @JsonProperty("id")
        private int id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("follows")
        private List<Integer> follows;
    }
} 