package bg.edu.utp.aitravelbenchmark.comparison;

import java.util.List;
import java.util.Map;

public record BenchmarkComparisonResponse(
        List<ComparisonTable> tables,
        Map<String, Double> totalScores,
        List<RankingEntry> ranking
) {
}