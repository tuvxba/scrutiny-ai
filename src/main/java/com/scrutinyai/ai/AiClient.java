package com.scrutinyai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.scrutinyai.exception.GeminiUnavailableException;

@Slf4j
@Component
public class AiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AiClient(
            WebClient geminiWebClient,
            ObjectMapper objectMapper,
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.model}") String model) {
        this.webClient = geminiWebClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Retryable(retryFor = GeminiUnavailableException.class, maxAttempts = 3, backoff = @Backoff(delay = 1500, multiplier = 2))
    public String generateStructuredContent(String prompt, JsonNode responseSchema) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", prompt);

        ObjectNode generationConfig = objectMapper.createObjectNode();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", responseSchema);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.putArray("contents").add(content);
        requestBody.set("generationConfig", generationConfig);

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert Gemini request to JSON", e);
        }

        String responseJson = webClient.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Gemini API error [{}]: {}", clientResponse.statusCode(), body);

                            HttpStatusCode status = clientResponse.statusCode();
                            if (status.value() == 503 || status.value() == 429) {
                                return reactor.core.publisher.Mono.error(
                                        new GeminiUnavailableException("Gemini API unavailable: " + body));
                            }
                            return reactor.core.publisher.Mono.error(
                                    new IllegalStateException("Gemini API error: " + body));
                        }))
                .bodyToMono(String.class)
                .block();

        JsonNode response;
        try {
            response = objectMapper.readTree(responseJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Gemini response: " + responseJson, e);
        }

        return extractText(response);
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Failed to receive a valid response from Gemini");
        }

        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            log.error("Failed to find valid candidate in Gemini response: {}", response);
            throw new IllegalStateException("Failed to receive a valid response from Gemini");
        }

        return candidates.get(0).path("content").path("parts").get(0).path("text").asText();
    }
}