package com.harsh.explainreport.dashboard.service;

import com.harsh.explainreport.dashboard.dto.GuidedQuery;
import com.harsh.explainreport.dashboard.util.TextParsing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<String> guidedInsights(String reportText, GuidedQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }

        String prompt = """
        You are a medical report analyzer.

        TASK:
        """ + query.getPrompt() + """

        RULES:
        - Provide 4 to 6 concise bullet points.
        - Each bullet must be under 18 words.
        - Do not add headings or extra text.
        - If information is missing, say "Not found in report."

        REPORT:
        """ + reportText;

        String response = callGroq(prompt);
        List<String> items = TextParsing.parseList(response);
        if (items.isEmpty() && response != null && !response.isBlank()) {
            items = List.of(response.trim());
        }

        return items.stream()
                .limit(6)
                .map(item -> TextParsing.limitWords(item, 18))
                .collect(Collectors.toList());
    }

    public String complete(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        return callGroq(messages);
    }

    private String callGroq(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        return callGroq(messages);
    }

    private String callGroq(List<Map<String, String>> messages) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile");

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Groq Error: Empty response");
            }

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) body.get("choices");

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            return message.get("content").toString();
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String message = body == null || body.isBlank() ? ex.getMessage() : body;
            throw new RuntimeException("Groq Error: " + message);
        }
    }

}

