package org.hetic.models;

public record DeduplicationStats(
    long totalChunks,
    long uniqueChunks,
    long duplicateChunks,
    double deduplicationRatio
) {
    @Override
    public String toString() {
        return String.format("""
            Statistiques de déduplication:
            - Nombre total de chunks: %d
            - Chunks uniques: %d
            - Chunks dupliqués: %d
            - Taux de déduplication: %.2f%%
            """,
            totalChunks, uniqueChunks, duplicateChunks,
            deduplicationRatio
        );
    }
}