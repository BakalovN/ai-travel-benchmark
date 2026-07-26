package bg.edu.utp.aitravelbenchmark.comparison;

import java.util.Map;

public record ComparisonRow(
        String label,
        Map<String, String> values
) {
}