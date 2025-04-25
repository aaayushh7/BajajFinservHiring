package com.example.webhookapp.service;

import com.example.webhookapp.model.GenerateWebhookRequest;
import com.example.webhookapp.model.GenerateWebhookResponse;
import com.example.webhookapp.model.GenerateWebhookResponse.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private static final String BASE_URL = "https://bfhldevapigw.healthrx.co.in/hiring";
    private final WebClient webClient;

    @Retryable(
        value = {WebClientResponseException.TooManyRequests.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void processWebhook() {
        try {
            logger.info("Starting webhook processing...");
            
            // Generate webhook token
            GenerateWebhookResponse response = generateWebhookToken();
            logger.info("Successfully generated webhook token");
            
            // Get access token from response
            String accessToken = response.getAccessToken();
            logger.info("Generated access token: {}", accessToken);
            
            // Get users from response and solve the problem
            List<User> users = response.getData().getUsers().getUsers();
            List<List<Integer>> solution = solveProblem(users);
            logger.info("Solution: {}", solution);
            
            // Send solution to webhook
            sendSolutionToWebhook(accessToken, solution);
            
        } catch (WebClientResponseException.TooManyRequests e) {
            logger.error("Rate limit exceeded. Retrying with backoff...", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in webhook processing: {}", e.getMessage(), e);
            throw e;
        }
    }

    private GenerateWebhookResponse generateWebhookToken() {
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
            .block();
    }

    public List<List<Integer>> solveProblem(List<User> users) {
        logger.debug("Input users: {}", users);
        Map<Integer, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }
        logger.debug("User map: {}", userMap);

        List<List<Integer>> result = new ArrayList<>();
        for (User user : users) {
            logger.debug("Processing user: {}", user);
            for (int followId : user.getFollows()) {
                User followedUser = userMap.get(followId);
                logger.debug("Checking if user {} follows back user {}", followId, user.getId());
                if (followedUser != null && followedUser.getFollows().contains(user.getId())) {
                    List<Integer> pair = new ArrayList<>();
                    pair.add(Math.min(user.getId(), followId));
                    pair.add(Math.max(user.getId(), followId));
                    if (!result.contains(pair)) {
                        result.add(pair);
                        logger.debug("Found mutual follow pair: {}", pair);
                    }
                }
            }
        }

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