package com.kafein.discovery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafein.discovery.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private final WebClient openAIWebClient;
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper;

    // 12 PII categories + Not PII
    private static final Map<String, Map<String, Object>> PII_CATEGORIES = new LinkedHashMap<>();

    static {
        addCategory("Email", "Email addresses", List.of("john@example.com", "user@company.co.uk"));
        addCategory("Phone Number", "Phone numbers in any format", List.of("+90 555 123 4567", "(212) 555-0100"));
        addCategory("SSN", "Social Security numbers", List.of("123-45-6789", "987654321"));
        addCategory("Credit Card Number", "Credit/debit card numbers", List.of("4532015112830366", "5425233430109903"));
        addCategory("National ID", "National identity numbers", List.of("AB123456", "X1234567"));
        addCategory("Full Name", "Complete person names", List.of("John Smith", "Ayşe Yılmaz"));
        addCategory("First Name", "Given/first names only", List.of("John", "Mehmet", "Sarah"));
        addCategory("Last Name", "Family/surnames only", List.of("Smith", "Yılmaz", "Johnson"));
        addCategory("TCKN", "Turkish Citizenship ID (11-digit)", List.of("12345678901", "98765432109"));
        addCategory("Address", "Physical/mailing addresses", List.of("123 Main St, City", "Atatürk Cad. No:5"));
        addCategory("Date of Birth", "Birth dates in any format", List.of("1990-01-15", "15/01/1990"));
        addCategory("IP Address", "IPv4 or IPv6 addresses", List.of("192.168.1.1", "2001:0db8::1"));
        addCategory("Not PII", "Non-personally identifiable data", List.of("product_name", "status", "amount"));
    }

    private static void addCategory(String name, String description, List<String> examples) {
        PII_CATEGORIES.put(name, Map.of("description", description, "examples", examples));
    }

    public List<Map<String, Object>> classifyWithLlm(
            String columnName, String tableName, String dataType, List<String> samples) {

        log.info("Building LLM classification prompt for {}.{}", tableName, columnName);

        String prompt = buildClassificationPrompt(columnName, tableName, dataType, samples);

        // Build request body
        Map<String, Object> requestBody = Map.of(
            "model", openAIConfig.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", "You are an expert data privacy analyst. Always respond with valid JSON only."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.1,
            "response_format", Map.of("type", "json_object")
        );

        log.info("Sending classification request to LLM (model: {})", openAIConfig.getModel());

        // Call OpenAI API
        String responseBody = openAIWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> {
                log.error("LLM API call failed: {}", e.getMessage());
                return Mono.error(new RuntimeException("LLM classification failed: " + e.getMessage()));
            })
            .block();

        return parseResponse(responseBody);
    }

    private String buildClassificationPrompt(String columnName, String tableName, String dataType, List<String> samples) {
        StringBuilder categoriesSection = new StringBuilder();
        int i = 1;
        for (var entry : PII_CATEGORIES.entrySet()) {
            @SuppressWarnings("unchecked")
            List<String> examples = (List<String>) entry.getValue().get("examples");
            String examplesStr = examples.stream().map(e -> "\"" + e + "\"").collect(Collectors.joining(", "));
            categoriesSection.append(String.format("  %d. **%s**\n     Description: %s\n     Examples: %s\n\n",
                i++, entry.getKey(), entry.getValue().get("description"), examplesStr));
        }

        StringBuilder samplesText = new StringBuilder();
        for (int j = 0; j < samples.size(); j++) {
            samplesText.append(String.format("  %d. %s\n", j + 1, samples.get(j)));
        }

        return String.format("""
            You are an expert data privacy analyst specializing in PII detection and classification. \
            Analyze the following database column and determine what type of data it contains.

            ## Context
            | Field        | Value        |
            |-------------|--------------|
            | Column Name  | `%s`  |
            | Table Name   | `%s`  |
            | Data Type    | `%s`  |

            ## Sample Data (%d samples)
            %s

            ## PII Categories
            Classify the data into one or more of the following categories:
            %s

            ## Classification Rules
            1. Analyze holistically: Consider column name, table name, SQL data type, AND sample values.
            2. Primary classification: Assign highest probability to the most likely category (>= 0.80 if confident).
            3. Secondary signals: Distribute probability if data could belong to multiple categories.
            4. Always include all 13 categories in your response.
            5. All probabilities must sum to exactly 1.0.
            6. Be aware of Turkish data formats (TCKN: 11-digit number, Turkish names, addresses).

            ## Required Output Format
            Return ONLY valid JSON:
            ```json
            {
              "classifications": [
                {"category": "Category Name", "probability": 0.95},
                {"category": "Not PII", "probability": 0.05}
              ]
            }
            ```
            Sort by probability descending. Include ALL 13 categories.
            """, columnName, tableName, dataType, samples.size(),
            samplesText.toString(), categoriesSection.toString());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in LLM response");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<>() {});
            List<Map<String, Object>> classifications = (List<Map<String, Object>>) parsed.get("classifications");

            if (classifications == null) {
                throw new RuntimeException("No 'classifications' key in LLM response");
            }

            // Normalize probabilities
            double total = classifications.stream()
                .mapToDouble(c -> ((Number) c.get("probability")).doubleValue())
                .sum();

            List<Map<String, Object>> normalized = new ArrayList<>();
            for (var c : classifications) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("category", c.get("category"));
                entry.put("probability", Math.round(((Number) c.get("probability")).doubleValue() / total * 10000.0) / 10000.0);
                normalized.add(entry);
            }

            // Ensure all categories are present
            Set<String> present = normalized.stream()
                .map(c -> (String) c.get("category"))
                .collect(Collectors.toSet());
            for (String category : PII_CATEGORIES.keySet()) {
                if (!present.contains(category)) {
                    normalized.add(Map.of("category", category, "probability", 0.0));
                }
            }

            // Sort by probability descending
            normalized.sort((a, b) -> Double.compare(
                ((Number) b.get("probability")).doubleValue(),
                ((Number) a.get("probability")).doubleValue()
            ));

            String topCategory = (String) normalized.get(0).get("category");
            double topProb = ((Number) normalized.get(0).get("probability")).doubleValue();
            log.info("Classification complete: top category = '{}' (probability: {})", topCategory, topProb);

            return normalized;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response as JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to parse LLM response: " + e.getMessage());
        }
    }
}
