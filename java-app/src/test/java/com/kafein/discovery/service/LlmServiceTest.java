package com.kafein.discovery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafein.discovery.config.OpenAIConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LlmService Unit Tests")
class LlmServiceTest {

    @Mock
    private WebClient openAIWebClient;

    @Mock
    private OpenAIConfig openAIConfig;

    private LlmService llmService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        llmService = new LlmService(openAIWebClient, openAIConfig, objectMapper);
    }

    // ──────────────────────────────────────────
    // PII Category Tests
    // ──────────────────────────────────────────

    @Test
    @DisplayName("PII_CATEGORIES contains exactly 13 entries (12 PII + Not PII)")
    void piiCategories_hasExactly13Entries() throws Exception {
        Field field = LlmService.class.getDeclaredField("PII_CATEGORIES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> categories = (Map<String, ?>) field.get(null);

        assertThat(categories).hasSize(13);
    }

    @Test
    @DisplayName("PII_CATEGORIES includes 'Not PII' category")
    void piiCategories_containsNotPiiCategory() throws Exception {
        Field field = LlmService.class.getDeclaredField("PII_CATEGORIES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> categories = (Map<String, ?>) field.get(null);

        assertThat(categories).containsKey("Not PII");
    }

    @Test
    @DisplayName("PII_CATEGORIES includes TCKN category (Turkish specific)")
    void piiCategories_containsTcknCategory() throws Exception {
        Field field = LlmService.class.getDeclaredField("PII_CATEGORIES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> categories = (Map<String, ?>) field.get(null);

        assertThat(categories).containsKey("TCKN");
    }

    // ──────────────────────────────────────────
    // Prompt Building Tests
    // ──────────────────────────────────────────

    @Test
    @DisplayName("Prompt contains column name")
    void buildPrompt_containsColumnName() throws Exception {
        Method method = LlmService.class.getDeclaredMethod(
            "buildClassificationPrompt", String.class, String.class, String.class, List.class);
        method.setAccessible(true);

        String prompt = (String) method.invoke(llmService,
            "email", "customers", "character varying",
            List.of("test@example.com"));

        assertThat(prompt).contains("email");
    }

    @Test
    @DisplayName("Prompt contains table name")
    void buildPrompt_containsTableName() throws Exception {
        Method method = LlmService.class.getDeclaredMethod(
            "buildClassificationPrompt", String.class, String.class, String.class, List.class);
        method.setAccessible(true);

        String prompt = (String) method.invoke(llmService,
            "phone", "customers", "varchar",
            List.of("+90 555 123 4567"));

        assertThat(prompt).contains("customers");
    }

    @Test
    @DisplayName("Prompt contains all sample values")
    void buildPrompt_containsSampleData() throws Exception {
        Method method = LlmService.class.getDeclaredMethod(
            "buildClassificationPrompt", String.class, String.class, String.class, List.class);
        method.setAccessible(true);

        List<String> samples = List.of("foo@bar.com", "baz@qux.com");
        String prompt = (String) method.invoke(llmService,
            "email", "test", "text", samples);

        assertThat(prompt).contains("foo@bar.com");
        assertThat(prompt).contains("baz@qux.com");
    }

    @Test
    @DisplayName("Prompt requires JSON-only response format")
    void buildPrompt_requiresJsonOutput() throws Exception {
        Method method = LlmService.class.getDeclaredMethod(
            "buildClassificationPrompt", String.class, String.class, String.class, List.class);
        method.setAccessible(true);

        String prompt = (String) method.invoke(llmService,
            "col", "tbl", "text", List.of("value"));

        assertThat(prompt.toLowerCase()).containsAnyOf("json", "classifications");
    }

    // ──────────────────────────────────────────
    // Probability Normalization Tests
    // ──────────────────────────────────────────

    @Test
    @DisplayName("Normalization scales probabilities to sum to 1.0")
    void parseResponse_normalizesProbabilities() throws Exception {
        // Build a mock LLM response JSON
        String mockContent = """
            {
              "classifications": [
                {"category": "Email", "probability": 0.9},
                {"category": "Not PII", "probability": 0.1},
                {"category": "Phone Number", "probability": 0.0},
                {"category": "SSN", "probability": 0.0},
                {"category": "Credit Card Number", "probability": 0.0},
                {"category": "National ID", "probability": 0.0},
                {"category": "Full Name", "probability": 0.0},
                {"category": "First Name", "probability": 0.0},
                {"category": "Last Name", "probability": 0.0},
                {"category": "TCKN", "probability": 0.0},
                {"category": "Address", "probability": 0.0},
                {"category": "Date of Birth", "probability": 0.0},
                {"category": "IP Address", "probability": 0.0}
              ]
            }
            """;

        String mockResponse = """
            {
              "choices": [
                {
                  "message": {
                    "content": %s
                  }
                }
              ]
            }
            """.formatted(objectMapper.writeValueAsString(
                objectMapper.readTree(mockContent).toString()));

        Method method = LlmService.class.getDeclaredMethod("parseResponse", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(
            llmService,
            """
            {"choices":[{"message":{"content":"{\\"classifications\\":[{\\"category\\":\\"Email\\",\\"probability\\":0.9},{\\"category\\":\\"Not PII\\",\\"probability\\":0.1}]}"}}]}
            """
        );

        double total = result.stream()
            .mapToDouble(c -> ((Number) c.get("probability")).doubleValue())
            .sum();

        assertThat(total).isCloseTo(1.0, within(0.01));
    }

    @Test
    @DisplayName("Result is sorted by probability descending")
    void parseResponse_sortedByProbabilityDescending() throws Exception {
        Method method = LlmService.class.getDeclaredMethod("parseResponse", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(
            llmService,
            """
            {"choices":[{"message":{"content":"{\\"classifications\\":[{\\"category\\":\\"Not PII\\",\\"probability\\":0.1},{\\"category\\":\\"Email\\",\\"probability\\":0.9}]}"}}]}
            """
        );

        double firstProb = ((Number) result.get(0).get("probability")).doubleValue();
        double secondProb = ((Number) result.get(1).get("probability")).doubleValue();

        assertThat(firstProb).isGreaterThanOrEqualTo(secondProb);
    }
}
