package bg.edu.utp.aitravelbenchmark.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Service
public class GeminiClient implements AiProviderClient {

    private final RestClient restClient;
    private final String apiKey;
    @Value("${ai.gemini.model}")
    private final String model;

    public GeminiClient(
            RestClient.Builder builder,
            @Value("${ai.gemini.base-url}") String baseUrl,
            @Value("${ai.gemini.api-key}") String apiKey,
            @Value("${ai.gemini.model}") String model
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }

    @Override
    public String generate(String instruction) {
        Map<String, Object> textPart = Map.of(
                "text", instruction
        );

        Map<String, Object> content = Map.of(
                "role", "user",
                "parts", List.of(textPart)
        );

        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json"
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );

        JsonNode response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Gemini returned an empty response."
            );
        }

        return extractText(response);
    }

    private String extractText(JsonNode response) {
        JsonNode candidates = response.get("candidates");

        if (candidates == null ||
                !candidates.isArray() ||
                candidates.isEmpty()) {

            throw new IllegalStateException(
                    "Gemini response does not contain candidates."
            );
        }

        JsonNode parts = candidates.get(0)
                .path("content")
                .path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini response does not contain content parts."
            );
        }

        JsonNode text = parts.get(0).get("text");

        if (text == null || !text.isTextual()) {
            throw new IllegalStateException(
                    "Gemini response does not contain text."
            );
        }

        return text.asText();
    }

    @Override
    public String getModelName() {
        return model;
    }
}