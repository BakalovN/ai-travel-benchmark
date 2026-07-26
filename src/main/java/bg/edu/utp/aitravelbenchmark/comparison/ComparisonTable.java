package bg.edu.utp.aitravelbenchmark.comparison;

import java.util.List;
import java.util.Map;

public record ComparisonTable(
        String title,
        List<ComparisonRow> rows,
        Map<String, Double> scores,
        String winner
) {
}