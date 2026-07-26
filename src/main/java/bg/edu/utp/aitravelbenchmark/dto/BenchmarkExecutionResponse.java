package bg.edu.utp.aitravelbenchmark.dto;

import bg.edu.utp.aitravelbenchmark.comparison.BenchmarkComparisonResponse;

import java.util.List;

public class BenchmarkExecutionResponse {

    private String prompt;
    private String combinedInstruction;
    private List<AiModelResult> results;
    private BenchmarkComparisonResponse comparison;

    public BenchmarkExecutionResponse() {
    }

    public BenchmarkExecutionResponse(
            String prompt,
            String combinedInstruction,
            List<AiModelResult> results,
            BenchmarkComparisonResponse comparison
    ) {
        this.prompt = prompt;
        this.combinedInstruction = combinedInstruction;
        this.results = results;
        this.comparison = comparison;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getCombinedInstruction() {
        return combinedInstruction;
    }

    public void setCombinedInstruction(
            String combinedInstruction
    ) {
        this.combinedInstruction = combinedInstruction;
    }

    public List<AiModelResult> getResults() {
        return results;
    }

    public void setResults(
            List<AiModelResult> results
    ) {
        this.results = results;
    }

    public BenchmarkComparisonResponse getComparison() {
        return comparison;
    }

    public void setComparison(
            BenchmarkComparisonResponse comparison
    ) {
        this.comparison = comparison;
    }
}