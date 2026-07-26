package bg.edu.utp.aitravelbenchmark.comparison;

public record RankingEntry(
        int position,
        String model,
        double score
) {
}