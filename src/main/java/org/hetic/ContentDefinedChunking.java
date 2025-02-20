package org.hetic;

import org.rabinfingerprint.fingerprint.RabinFingerprintLong;
import org.rabinfingerprint.polynomial.Polynomial;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ContentDefinedChunking {
    private final Polynomial POLYNOMIAL = Polynomial.createFromLong(0x3DA3358B4DC173L); // Polynome optimisé
    private static final Logger logger = Logger.getLogger(ContentDefinedChunking.class.getName());

    public List<byte[]> chunkFile(String filePath) throws IOException {
        long startTime = System.nanoTime();

        File file = new File(filePath);
        long fileSize = file.length();

        ChunkParams params = adjustChunkParams(fileSize);

        List<byte[]> chunks = new ArrayList<>();
        try (InputStream inputStream = new FileInputStream(file)) {
            RabinFingerprintLong rabin = new RabinFingerprintLong(POLYNOMIAL);
            ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    chunkBuffer.write(buffer[i]);
                    rabin.pushByte(buffer[i]);

                    if ((rabin.getFingerprintLong() & params.mask) == 0 || chunkBuffer.size() >= params.maxChunkSize) {
                        if (chunkBuffer.size() >= params.minChunkSize) {
                            chunks.add(chunkBuffer.toByteArray());
                            chunkBuffer.reset();
                            rabin = new RabinFingerprintLong(POLYNOMIAL);
                        }
                    }
                }
            }

            // Ajouter le dernier chunk s'il reste des données
            if (chunkBuffer.size() > 0) {
                chunks.add(chunkBuffer.toByteArray());
            }
        }

        long endTime = System.nanoTime();
        logger.info("File split execution time: " + (endTime - startTime) / 1_000_000 + " ms");

        return chunks;
    }


    private ChunkParams adjustChunkParams(long fileSize) {
        final long BASE_MASK = (1 << 12) - 1; // Masque de base 4 KB

        long mask;
        int minChunkSize;
        int maxChunkSize;

        if (fileSize < 10 * 1024) { // < 10 KB
            mask = BASE_MASK >> 2; // (1 KB)
            minChunkSize = 512;
            maxChunkSize = 2048;
        } else if (fileSize < 100 * 1024) { // < 100 KB
            mask = BASE_MASK; // (4 KB)
            minChunkSize = 1024;
            maxChunkSize = 4096;
        } else if (fileSize < 1024 * 1024) { // < 1 MB
            mask = BASE_MASK << 1; // (8 KB)
            minChunkSize = 2048;
            maxChunkSize = 8192;
        } else if (fileSize < 10 * 1024 * 1024) { // < 10 MB
            mask = BASE_MASK << 2; // (16 KB)
            minChunkSize = 4096;
            maxChunkSize = 16384;
        } else if (fileSize < 100 * 1024 * 1024) { // < 100 MB
            mask = BASE_MASK << 3; // (32 KB)
            minChunkSize = 8192;
            maxChunkSize = 32768;
        } else { // > 100 MB
            mask = BASE_MASK << 4; // (64 KB)
            minChunkSize = 16384;
            maxChunkSize = 65536;
        }

        return new ChunkParams(mask, minChunkSize, maxChunkSize);
    }

    private static class ChunkParams {
        long mask;
        int minChunkSize;
        int maxChunkSize;

        ChunkParams(long mask, int minChunkSize, int maxChunkSize) {
            this.mask = mask;
            this.minChunkSize = minChunkSize;
            this.maxChunkSize = maxChunkSize;
        }
    }
}
