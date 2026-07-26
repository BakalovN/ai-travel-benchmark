package bg.edu.utp.aitravelbenchmark.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class OpenAiClient implements AiProviderClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    @Value("${ai.openai.model}")
    private final String model;


    public OpenAiClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${ai.openai.base-url}") String baseUrl,
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model}") String model
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public String generate(String instruction) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", instruction
        );

        JsonNode response = restClient.post()
                .uri("/v1/responses")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException(
                    "OpenAI returned an empty response."
            );
        }

        return extractText(response);
    }

    private String extractText(JsonNode response) {
        JsonNode output = response.get("output");

        if (output == null || !output.isArray()) {
            throw new IllegalStateException(
                    "OpenAI response does not contain an output array."
            );
        }

        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.get("content");

            if (content == null || !content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {
                JsonNode textNode = contentItem.get("text");

                if (textNode != null && textNode.isTextual()) {
                    return textNode.asText();
                }
            }
        }

        throw new IllegalStateException(
                "No text was found in the OpenAI response."
        );
    }

    @Override
    public String getModelName() {
        return model;
    }
}