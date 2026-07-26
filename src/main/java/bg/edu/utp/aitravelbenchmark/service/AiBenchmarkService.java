package bg.edu.utp.aitravelbenchmark.service;

import bg.edu.utp.aitravelbenchmark.ai.AiProviderClient;
import bg.edu.utp.aitravelbenchmark.dto.AiModelResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiBenchmarkService {

    private final List<AiProviderClient> clients;
    private final ObjectMapper objectMapper;

    public AiBenchmarkService(
            List<AiProviderClient> clients,
            ObjectMapper objectMapper
    ) {
        this.clients = clients;
        this.objectMapper = objectMapper;
    }

    public List<AiModelResult> runBenchmark(
            String combinedInstruction
    ) {
        List<AiModelResult> results = new ArrayList<>();

        for (AiProviderClient client : clients) {
            long startTime = System.nanoTime();

            try {
                String rawContent =
                        client.generate(combinedInstruction);

                JsonNode parsedContent =
                        objectMapper.readTree(rawContent);

                long elapsedMilliseconds =
                        (System.nanoTime() - startTime) / 1_000_000;

                results.add(new AiModelResult(
                        client.getProviderName(),
                        client.getModelName(),
                        true,
                        parsedContent,
                        rawContent,
                        null,
                        elapsedMilliseconds
                ));

            } catch (Exception exception) {
                long elapsedMilliseconds =
                        (System.nanoTime() - startTime) / 1_000_000;

                results.add(new AiModelResult(
                        client.getProviderName(),
                        client.getModelName(),
                        false,
                        null,
                        null,
                        exception.getMessage(),
                        elapsedMilliseconds
                ));
            }
        }

        return results;
    }
}