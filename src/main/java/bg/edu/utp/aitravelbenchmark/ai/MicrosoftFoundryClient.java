package bg.edu.utp.aitravelbenchmark.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Service
public class MicrosoftFoundryClient implements AiProviderClient {

    private final RestClient restClient;
    private final String model;

    public MicrosoftFoundryClient(
            RestClient.Builder builder,
            @Value("${ai.microsoft.api-key}") String apiKey,
            @Value("${ai.microsoft.endpoint}") String endpoint,
            @Value("${ai.microsoft.model}") String model
    ) {
        this.restClient = builder
                .baseUrl(removeTrailingSlash(endpoint))
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.model = model;
    }

    @Override
    public String getProviderName() {
        return "Microsoft Foundry";
    }

    @Override
    public String generate(String instruction) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content",
                                "Return valid JSON only. "
                                        + "Do not use Markdown code fences. "
                                        + "Do not include text before or after the JSON."
                        ),
                        Map.of(
                                "role", "user",
                                "content", instruction
                        )
                ),
                "temperature", 0.1,
                "max_tokens", 4096
        );

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Microsoft Foundry returned an empty response."
            );
        }

        String result = extractText(response);

        System.out.println("Raw Microsoft response:");
        System.out.println(result);

        return cleanJsonResponse(result);
    }

    private String extractText(JsonNode response) {
        JsonNode content = response.path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (!content.isTextual()) {
            throw new IllegalStateException(
                    "No text was found in the Microsoft Foundry response: "
                            + response
            );
        }

        return content.asText();
    }

    private static String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Microsoft Foundry endpoint must not be empty."
            );
        }

        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String cleanJsonResponse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException(
                    "Microsoft Foundry returned empty textual content."
            );
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring("```json".length()).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring("```".length()).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(
                    0,
                    cleaned.length() - "```".length()
            ).trim();
        }

        return cleaned;
    }
}