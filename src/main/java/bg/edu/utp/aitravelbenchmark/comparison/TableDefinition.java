package bg.edu.utp.aitravelbenchmark.comparison;

import java.util.List;

public record TableDefinition(
        String title,
        List<FieldDefinition> fields
) {
}