package bg.edu.utp.aitravelbenchmark.dto;

import tools.jackson.databind.JsonNode;

public class AiModelResult {

    private String provider;
    private String model;
    private boolean successful;
    private JsonNode content;
    private String rawContent;
    private String errorMessage;
    private long responseTimeMilliseconds;

    public AiModelResult(
            String provider,
            String model,
            boolean successful,
            JsonNode content,
            String rawContent,
            String errorMessage,
            long responseTimeMilliseconds
    ) {
        this.provider = provider;
        this.model = model;
        this.successful = successful;
        this.content = content;
        this.rawContent = rawContent;
        this.errorMessage = errorMessage;
        this.responseTimeMilliseconds = responseTimeMilliseconds;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public JsonNode getContent() {
        return content;
    }

    public String getRawContent() {
        return rawContent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getResponseTimeMilliseconds() {
        return responseTimeMilliseconds;
    }
}