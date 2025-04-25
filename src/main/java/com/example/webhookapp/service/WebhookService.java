package com.example.webhookapp.service;

import com.example.webhookapp.model.GenerateWebhookRequest;
import com.example.webhookapp.model.GenerateWebhookResponse;
import com.example.webhookapp.model.GenerateWebhookResponse.User;
import com.example.webhookapp.model.GenerateWebhookResponse.UsersData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebClient webClient;

    public void processWebhook() {
        generateWebhookToken()
            .flatMap(response -> {
                String accessToken = response.getAccessToken();
                log.info("Generated access token: {}", accessToken);
     
                // Step 2: Get users data
                return getUsersData(accessToken)
                    .flatMap(usersData -> {
                        // Step 3: Solve the problem
                        List<List<Integer>> solution = solveProblem(usersData);
                        log.info("Solution: {}", solution);
                        
                        // Step 4: Send solution to webhook
                        return sendSolutionToWebhook(accessToken, solution);
                    });
            })
            .block();
    }

    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Mono<GenerateWebhookResponse> generateWebhookToken() {
        GenerateWebhookRequest request = GenerateWebhookRequest.builder()
            .name("Ayush Tiwari")
            .regNo("RA2211003010305")
            .email("at7257@srmist.edu.in")
            .build();

        return webClient.post()
            .uri("/hiring/generateWebhook")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(GenerateWebhookResponse.class)
            .doOnError(error -> log.error("Error generating webhook token: {}", error.getMessage()))
            .doOnSuccess(response -> log.info("Successfully generated webhook token"));
    }

    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Mono<UsersData> getUsersData(String accessToken) {
        return webClient.get()
            .uri("/users")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .retrieve()
            .bodyToMono(UsersData.class)
            .doOnError(error -> log.error("Error getting users data: {}", error.getMessage()))
            .doOnSuccess(response -> log.info("Successfully got users data"));
    }

    public List<List<Integer>> solveProblem(UsersData usersData) {
        List<User> users = usersData.getUsers();
        int findId = usersData.getFindId();
        int n = usersData.getN();

        // Create a map for quick user lookup
        Map<Integer, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }

        // Find all users reachable within n steps from findId using BFS
        Set<Integer> reachableUsers = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> distance = new HashMap<>();

        queue.add(findId);
        distance.put(findId, 0);
        reachableUsers.add(findId);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentDistance = distance.get(current);

            if (currentDistance >= n) {
                continue;
            }

            User currentUser = userMap.get(current);
            if (currentUser == null) {
                continue;
            }

            for (int followId : currentUser.getFollows()) {
                if (!distance.containsKey(followId)) {
                    distance.put(followId, currentDistance + 1);
                    reachableUsers.add(followId);
                    queue.add(followId);
                }
            }
        }

        // Find all pairs of users who follow each other
        List<List<Integer>> result = new ArrayList<>();
        for (int userId : reachableUsers) {
            User user = userMap.get(userId);
            if (user == null) {
                continue;
            }

            for (int followId : user.getFollows()) {
                if (reachableUsers.contains(followId)) {
                    User followedUser = userMap.get(followId);
                    if (followedUser != null && followedUser.getFollows().contains(userId)) {
                        // Found a mutual follow
                        List<Integer> pair = new ArrayList<>();
                        pair.add(Math.min(userId, followId));
                        pair.add(Math.max(userId, followId));
                        if (!result.contains(pair)) {
                            result.add(pair);
                        }
                    }
                }
            }
        }

        // Sort the result
        result.sort((a, b) -> {
            if (a.get(0) != b.get(0)) {
                return a.get(0) - b.get(0);
            }
            return a.get(1) - b.get(1);
        });

        return result;
    }

    @Retryable(
        maxAttempts = 4,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private Mono<Void> sendSolutionToWebhook(String accessToken, List<List<Integer>> solution) {
        WebhookSolutionRequest request = WebhookSolutionRequest.builder()
            .regNo("RA2211003010305")
            .outcome(solution)
            .build();

        return webClient.post()
            .uri("/hiring/testWebhook")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnError(error -> log.error("Error sending solution to webhook: {}", error.getMessage()))
            .doOnSuccess(v -> log.info("Successfully sent solution to webhook"));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class WebhookSolutionRequest {
        private String regNo;
        private List<List<Integer>> outcome;
    }
} 