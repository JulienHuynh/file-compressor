package org.hetic.models;

public record CompressionStats(
    long totalOriginalSize,
    long totalCompressedSize,
    int chunkCount,
    double compressionRatio,
    double spaceSaved
) {
    @Override
    public String toString() {
        return String.format("""
            Statistiques de compression :
            ----------------------------------------
            Nombre de chunks : %d
            Taille originale : %.2f MB
            Taille compressée : %.2f MB
            Ratio de compression : %.2f%%
            Taille finale : %.2f MB
            ----------------------------------------
            """,
            chunkCount,
            totalOriginalSize / (1024.0 * 1024.0),
            totalCompressedSize / (1024.0 * 1024.0),
            compressionRatio,
            spaceSaved / (1024.0 * 1024.0)
        );
    }
}