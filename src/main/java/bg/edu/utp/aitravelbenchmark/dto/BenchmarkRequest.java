package bg.edu.utp.aitravelbenchmark.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public class BenchmarkRequest {
    @NotBlank(message = "Prompt cannot be empty.")
    @Size(max = 5000, message = "Prompt cannot exceed 5000 characters.")
    private String prompt;

    @NotNull(message = "Output schema is required.")
    private JsonNode outputSchema;

    public BenchmarkRequest() {
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public JsonNode getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(JsonNode outputSchema) {
        this.outputSchema = outputSchema;
    }
}
