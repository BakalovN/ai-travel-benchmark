package bg.edu.utp.aitravelbenchmark.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    private final ObjectMapper objectMapper;

    public PromptService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildCombinedInstruction(String userPrompt, JsonNode outputSchema) {
        String formattedSchema =
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(outputSchema);

        return """
                You are participating in a standardized travel-planning benchmark.

                USER REQUEST:
                %s

                RESPONSE REQUIREMENTS:
                1. Return valid JSON only.
                2. Do not include Markdown.
                3. Do not include explanatory text outside the JSON.
                4. Preserve all field names from the provided JSON structure.
                5. Do not remove fields.
                6. Use null when reliable information is unavailable.
                7. Use empty arrays when no list items are available.
                8. Use numeric values without currency symbols.
                9. Use ISO 8601 dates in YYYY-MM-DD format.
                10. Use 24-hour time in HH:mm format.
                11. Do not invent sources.
                12. Clearly record assumptions and uncertain information.
                13. All current field values are placeholders and must be replaced.

                REQUIRED JSON STRUCTURE:
                %s
                """.formatted(userPrompt, formattedSchema);

    }
}