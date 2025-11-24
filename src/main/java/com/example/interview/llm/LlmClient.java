package com.example.interview.llm;

import com.example.interview.llm.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LlmClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey;
    private final String provider;
    private final String model;

    public LlmClient(
            WebClient.Builder webClientBuilder,
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.provider}") String provider,
            @Value("${llm.model}") String model
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.provider = provider;
        this.model = model;
    }

    public String chat(List<ChatMessage> messages) {
        try {
            if ("GEMINI".equalsIgnoreCase(provider)) {
                return callGemini(messages);
            } else {
                return callOpenAiCompatible(messages);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "I'm sorry, I couldn't generate a response: " + e.getMessage();
        }
    }


    private String callOpenAiCompatible(List<ChatMessage> messages) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);

        String rawJson = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // If the chat size is large, return raw JSON for structured feedback (like in finishInterview)
        if (messages.size() > 2) { 
            return rawJson;
        }
        
        JsonNode root = objectMapper.readTree(rawJson);
        JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
        return contentNode.asText();
    }

    private String callGemini(List<ChatMessage> messages) throws Exception {
        List<Map<String, Object>> contents = messages.stream()
                .map(msg -> {
                    Map<String, Object> part = new HashMap<>();
                    part.put("text", msg.getContent());

                    Map<String, Object> content = new HashMap<>();
                    // Gemini roles: 'user' for input, 'model' for previous model output
                    content.put("role", "assistant".equalsIgnoreCase(msg.getRole()) ? "model" : "user");
                    content.put("parts", List.of(part));
                    return content;
                })
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);

        // FIX: Use direct string concatenation to ensure the model name is correctly embedded in the URI.
        String uri = "/v1/models/" + model + ":generateContent?key=" + apiKey;

        String rawJson = webClient.post()
                .uri(uri)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        // If the message history is long, return raw JSON for structured feedback
        if (messages.size() > 2) {
             return rawJson; 
        }

        // For regular question generation (1 or 2 messages), extract the text.
        JsonNode root = objectMapper.readTree(rawJson);
        JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");

        return textNode.asText();
    }
}