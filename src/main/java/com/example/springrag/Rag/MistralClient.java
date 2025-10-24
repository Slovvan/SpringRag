package com.example.springrag.Rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
@Component
public class MistralClient {
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;
    public MistralClient(
            @Value("${mistral.api.key:}") String apiKey,
            @Value("${mistral.chat.model:mistral-small-latest}") String
                    chatModel,
            @Value("${mistral.embedding.model:mistral-embed}") String
                    embeddingModel
    ) {
        this.http =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }
    public float[] embed(String text) {
        try {
            Map<String, Object> body = Map.of(
                    "model", embeddingModel,
                    "input", text
            );
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mistral.ai/v1/embeddings")
                    )
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json,
                            StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 300) throw new
                    RuntimeException("Embedding HTTP " + resp.statusCode() + ": " +
                    resp.body());
            Map<?,?> response = mapper.readValue(resp.body(), Map.class);
            List<?> data = (List<?>) response.get("data");
            if (data == null || data.isEmpty()) throw new
                    RuntimeException("No embedding data returned");
            Map<?,?> first = (Map<?,?>) data.get(0);
            List<?> vector = (List<?>) first.get("embedding");
            float[] arr = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                arr[i] = ((Number) vector.get(i)).floatValue();
            }
            return arr;
        } catch (Exception e) {
            throw new RuntimeException("Mistral embedding error: " + e.getMessage(), e);
        }
    }
    public String chat(String system, String user) {
        try {
            return doChatWithRetries(chatModel, system, user);
        } catch (Exception e) {
            throw new RuntimeException("Mistral chat error: " + e.getMessage(), e);
        }
    }
    private String doChatWithRetries(String model, String system, String
            user) throws Exception {
        final int maxAttempts = 3;
        long backoffMs = 1000;
        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return sendChat(model, system, user);
            } catch (RuntimeException ex) {
                String msg = ex.getMessage() == null ? "" :
                        ex.getMessage();
                // 429 or 5xx-> retry with backoff
                if (msg.contains(" 429:") || msg.matches(".*Chat HTTP 5\\d{2}.*")) {
                last = ex;
                if (attempt < maxAttempts) {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
            }
            throw ex;
        }
    }
    // If still failing after retries and not already on small model,try one fallback attempt
 if (!"mistral-small-latest".equalsIgnoreCase(model)) {
        try {
            return sendChat("mistral-small-latest", system, user);
        } catch (RuntimeException ignored) {
            // fall through to throw last
        }
    }
 if (last != null) throw last;
 throw new RuntimeException("Unknown chat failure");
}
private String sendChat(String model, String system, String user)
        throws Exception {
    Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)
            )
    );
    String json = mapper.writeValueAsString(body);
    HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create("https://api.mistral.ai/v1/chat/completions"))
                            .timeout(Duration.ofSeconds(120))
                            .header("Authorization", "Bearer " + apiKey)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json,
                                    StandardCharsets.UTF_8))
                            .build();
    HttpResponse<String> resp = http.send(req,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() >= 300) throw new RuntimeException("Chat HTTP " + resp.statusCode() + ": " + resp.body());
    Map<?,?> response = mapper.readValue(resp.body(), Map.class);
    List<?> choices = (List<?>) response.get("choices");
    if (choices == null || choices.isEmpty()) throw new
            RuntimeException("No chat choices returned");
    Map<?,?> choice0 = (Map<?,?>) choices.get(0);
    Map<?,?> message = (Map<?,?>) choice0.get("message");
    return (String) message.get("content");
}
public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
}
 }