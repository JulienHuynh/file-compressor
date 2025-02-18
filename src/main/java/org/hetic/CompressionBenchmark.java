package org.hetic;

import com.github.luben.zstd.Zstd;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;

public class CompressionBenchmark {
    private static final int COMPRESSION_LEVEL = 22;
    private final ChunkCompressor chunkCompressor = new ChunkCompressor();
    private final ContentDefinedChunking chunker = new ContentDefinedChunking();

    public record CompressionResult(
        long originalSize,
        long compressedSize,
        long timeInMs,
        double compressionRatio,
        String method
    ) {
        @Override
        public String toString() {
            return String.format("""
                Méthode: %s
                Taille originale: %.2f KB
                Taille compressée: %.2f KB
                Réduction de taille: %.2f%%
                Temps d'exécution: %d ms
                """,
                method,
                originalSize / 1024.0,
                compressedSize / 1024.0,
                compressionRatio,
                timeInMs
            );
        }
    }

    public CompressionResult benchmarkGlobalCompression(String filePath) throws IOException {
        byte[] content = Files.readAllBytes(Paths.get(filePath));
        long startTime = System.currentTimeMillis();

        // Compression globale
        long maxCompressedSize = Zstd.compressBound(content.length);
        byte[] compressedData = new byte[(int) maxCompressedSize];
        long compressedSize = Zstd.compress(compressedData, content, COMPRESSION_LEVEL);

        long endTime = System.currentTimeMillis();
        double ratio = (1.0 - ((double) compressedSize / content.length)) * 100;

        return new CompressionResult(
            content.length,
            compressedSize,
            endTime - startTime,
            ratio,
            "Compression Globale"
        );
    }

    public CompressionResult benchmarkChunkCompression(String filePath) throws IOException {
        long startTime = System.currentTimeMillis();
        List<byte[]> chunks = chunker.chunkFile(filePath);
        
        long totalOriginalSize = 0;
        long totalCompressedSize = 0;

        List<byte[]> compressedChunks = new ArrayList<>();
        for (byte[] chunk : chunks) {
            totalOriginalSize += chunk.length;
            byte[] compressedChunk = chunkCompressor.compressChunk(chunk);
            totalCompressedSize += compressedChunk.length;
            compressedChunks.add(compressedChunk);
        }

        long endTime = System.currentTimeMillis();
        double ratio = (1.0 - ((double) totalCompressedSize / totalOriginalSize)) * 100;

        return new CompressionResult(
            totalOriginalSize,
            totalCompressedSize,
            endTime - startTime,
            ratio,
            "Compression par Chunk"
        );
    }

    public void runComparaisonComplete(String filePath) throws IOException {
        String fileName = Paths.get(filePath).getFileName().toString().toLowerCase();
        boolean isPreCompressed = fileName.endsWith(".jpg") || 
                                fileName.endsWith(".jpeg") || 
                                fileName.endsWith(".png") || 
                                fileName.endsWith(".zip") || 
                                fileName.endsWith(".gz") ||
                                fileName.endsWith(".mp3") ||
                                fileName.endsWith(".mp4");

        System.out.println("Comparaison des méthodes de compression pour: " + filePath);
        System.out.println("=================================================");
        if (isPreCompressed) {
            System.out.println("ATTENTION: Ce fichier est déjà dans un format compressé.");
            System.out.println("Les résultats de compression seront limités.\n");
        }

        // Test compression globale
        CompressionResult globalResult = benchmarkGlobalCompression(filePath);
        System.out.println("\nRésultats compression globale:");
        System.out.println(globalResult);

        // Test compression par chunk
        CompressionResult chunkResult = benchmarkChunkCompression(filePath);
        System.out.println("\nRésultats compression par chunk:");
        System.out.println(chunkResult);

        // Comparaison
        System.out.println("\nAnalyse comparative:");
        System.out.println("-------------------------------------------------");
        
        // Différence de ratio
        double ratioDiff = Math.abs(globalResult.compressionRatio() - chunkResult.compressionRatio());
        System.out.printf("Différence de réduction: %.2f points de pourcentage%n", ratioDiff);
        
        // Différence de temps
        long timeDiff = Math.abs(globalResult.timeInMs() - chunkResult.timeInMs());
        System.out.printf("Différence de temps: %d ms%n", timeDiff);
        
        // Recommandation
        System.out.println("\nRecommandation:");
        if (isPreCompressed) {
            System.out.println("Ce type de fichier étant déjà compressé, la compression supplémentaire");
            System.out.println("n'apporte pas de bénéfices significatifs. Considérez de compresser");
            System.out.println("les fichiers source avant leur compression dans ce format.");
        } else {
            if (chunkResult.compressionRatio() < globalResult.compressionRatio() - 1.0) {
                System.out.println("La compression globale offre une meilleure réduction de taille");
            } else if (chunkResult.compressionRatio() > globalResult.compressionRatio() + 1.0) {
                System.out.println("La compression par chunk offre une meilleure réduction de taille");
            } else {
                System.out.println("Les deux méthodes ont des performances similaires en termes de réduction");
            }
        }

        if (chunkResult.timeInMs() < globalResult.timeInMs() * 1.1) {
            System.out.println("La compression par chunk est efficace en termes de temps");
        } else {
            System.out.println("La compression globale est significativement plus rapide");
            System.out.printf("(%.1fx plus rapide)%n", 
                (double)chunkResult.timeInMs() / globalResult.timeInMs());
        }
    }
}