package com.example.webhookapp;

import com.example.webhookapp.service.WebhookService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@RequiredArgsConstructor
public class WebhookAppApplication {

    private final WebhookService webhookService;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("https://bfhldevapigw.healthrx.co.in")
            .build();
    }

    @PostConstruct
    public void init() {
        webhookService.processWebhook();
    }

    public static void main(String[] args) {
        SpringApplication.run(WebhookAppApplication.class, args);
    }
} 