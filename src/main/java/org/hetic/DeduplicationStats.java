package org.hetic;

public record DeduplicationStats(
    long totalChunks,
    long uniqueChunks,
    long duplicateChunks,
    long totalSize,
    long savedSize,
    double deduplicationRatio
) {
    @Override
    public String toString() {
        return String.format("""
            Statistiques de déduplication:
            - Nombre total de chunks: %d
            - Chunks uniques: %d
            - Chunks dupliqués: %d
            - Taille totale: %d octets
            - Espace économisé: %d octets
            - Taux de déduplication: %.2f%%
            """,
            totalChunks, uniqueChunks, duplicateChunks,
            totalSize, savedSize, deduplicationRatio
        );
    }
}