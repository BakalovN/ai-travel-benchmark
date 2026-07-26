package bg.edu.utp.aitravelbenchmark.dto;
import tools.jackson.databind.JsonNode;
public class BenchmarkResponse {
    private String prompt;
    private JsonNode outputSchema;
    private String combinedInstruction;

    public BenchmarkResponse(String prompt, JsonNode outputSchema, String combinedInstruction) {
        this.prompt = prompt;
        this.outputSchema = outputSchema;
        this.combinedInstruction = combinedInstruction;
    }

    public String getPrompt() {
        return prompt;
    }

    public JsonNode getOutputSchema() {
        return outputSchema;
    }

    public String getCombinedInstruction() {
        return combinedInstruction;
    }
}
