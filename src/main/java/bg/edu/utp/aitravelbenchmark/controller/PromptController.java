package bg.edu.utp.aitravelbenchmark.controller;

import bg.edu.utp.aitravelbenchmark.comparison.BenchmarkComparisonResponse;
import bg.edu.utp.aitravelbenchmark.comparison.ComparisonService;
import bg.edu.utp.aitravelbenchmark.dto.AiModelResult;
import bg.edu.utp.aitravelbenchmark.dto.BenchmarkExecutionResponse;
import bg.edu.utp.aitravelbenchmark.dto.BenchmarkRequest;
import bg.edu.utp.aitravelbenchmark.service.AiBenchmarkService;
import bg.edu.utp.aitravelbenchmark.service.PromptService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PromptController {

    private final PromptService promptService;
    private final AiBenchmarkService aiBenchmarkService;
    private final ComparisonService comparisonService;

    public PromptController(
            PromptService promptService,
            AiBenchmarkService aiBenchmarkService,
            ComparisonService comparisonService
    ) {
        this.promptService = promptService;
        this.aiBenchmarkService = aiBenchmarkService;
        this.comparisonService = comparisonService;
    }

    @PostMapping("/prompt")
    public ResponseEntity<BenchmarkExecutionResponse> receivePrompt(
            @Valid @RequestBody BenchmarkRequest request
    ) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("POST /api/prompt received.");
        System.out.println("========================================");

        String prompt = request.getPrompt().trim();

        System.out.println("User prompt:");
        System.out.println(prompt);
        System.out.println();

        String combinedInstruction =
                promptService.buildCombinedInstruction(
                        prompt,
                        request.getOutputSchema()
                );

        System.out.println("Combined instruction created.");
        System.out.println("Starting AI benchmark...");

        long benchmarkStartTime =
                System.currentTimeMillis();

        List<AiModelResult> results =
                aiBenchmarkService.runBenchmark(
                        combinedInstruction
                );

        long benchmarkDuration =
                System.currentTimeMillis()
                        - benchmarkStartTime;

        System.out.println(
                "AI benchmark completed in "
                        + benchmarkDuration
                        + " ms."
        );

        System.out.println();
        System.out.println("AI model results:");
        System.out.println("----------------------------------------");

        for (AiModelResult result : results) {
            System.out.println(
                    "Provider: "
                            + result.getProvider()
            );

            System.out.println(
                    "Successful: "
                            + result.isSuccessful()
            );

            System.out.println(
                    "Response time: "
                            + result.getResponseTimeMilliseconds()
                            + " ms"
            );

            System.out.println(
                    "Error: "
                            + result.getErrorMessage()
            );

            System.out.println("----------------------------------------");
        }

        JsonNode chatGptJson =
                findSuccessfulModelResponse(
                        results,
                        "OpenAI",
                        "ChatGPT"
                );

        JsonNode geminiJson =
                findSuccessfulModelResponse(
                        results,
                        "Google",
                        "Gemini"
                );

        JsonNode microsoftPhiJson =
                findSuccessfulModelResponse(
                        results,
                        "Microsoft",
                        "Phi"
                );

        BenchmarkComparisonResponse comparison = null;

        boolean allRequiredProvidersSuccessful =
                chatGptJson != null
                        && geminiJson != null
                        && microsoftPhiJson != null;

        if (allRequiredProvidersSuccessful) {
            System.out.println(
                    "All required AI responses were found."
            );

            System.out.println(
                    "Starting comparison..."
            );

            comparison =
                    comparisonService.compare(
                            chatGptJson,
                            geminiJson,
                            microsoftPhiJson
                    );

            System.out.println(
                    "Comparison completed successfully."
            );
        } else {
            System.err.println();
            System.err.println(
                    "Comparison skipped because one or more "
                            + "AI providers did not return "
                            + "a successful response."
            );

            printMissingProviders(
                    chatGptJson,
                    geminiJson,
                    microsoftPhiJson
            );
        }

        BenchmarkExecutionResponse response =
                new BenchmarkExecutionResponse(
                        prompt,
                        combinedInstruction,
                        results,
                        comparison
                );

        System.out.println(
                "Returning benchmark response to frontend."
        );

        System.out.println("========================================");
        System.out.println();

        return ResponseEntity.ok(response);
    }

    private JsonNode findSuccessfulModelResponse(
            List<AiModelResult> results,
            String... expectedProviderNames
    ) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        return results.stream()
                .filter(AiModelResult::isSuccessful)
                .filter(result ->
                        matchesProvider(
                                result.getProvider(),
                                expectedProviderNames
                        )
                )
                .map(AiModelResult::getContent)
                .filter(content -> content != null)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesProvider(
            String actualProvider,
            String... acceptedProviderNames
    ) {
        if (
                actualProvider == null
                        || actualProvider.isBlank()
                        || acceptedProviderNames == null
        ) {
            return false;
        }

        String normalizedActual =
                actualProvider
                        .trim()
                        .toLowerCase();

        return Arrays.stream(acceptedProviderNames)
                .filter(name ->
                        name != null
                                && !name.isBlank()
                )
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(acceptedName ->
                        normalizedActual.equals(acceptedName)
                                || normalizedActual.contains(
                                acceptedName
                        )
                                || acceptedName.contains(
                                normalizedActual
                        )
                );
    }

    private void printMissingProviders(
            JsonNode chatGptJson,
            JsonNode geminiJson,
            JsonNode microsoftPhiJson
    ) {
        if (chatGptJson == null) {
            System.err.println(
                    "Missing successful OpenAI/ChatGPT response."
            );
        }

        if (geminiJson == null) {
            System.err.println(
                    "Missing successful Google/Gemini response."
            );
        }

        if (microsoftPhiJson == null) {
            System.err.println(
                    "Missing successful Microsoft/Phi response."
            );
        }
    }
}