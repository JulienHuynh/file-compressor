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

        final long MASK = (1 << 12) - 1;  // Seuil de coupure (4 Ko)
        final int MAX_CHUNK_SIZE = 20480; // 20 KB max

        List<byte[]> chunks = new ArrayList<>();
        File file = new File(filePath);
        try (InputStream inputStream = new FileInputStream(file)) {
            RabinFingerprintLong rabin = new RabinFingerprintLong(POLYNOMIAL);
            ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    chunkBuffer.write(buffer[i]);
                    rabin.pushByte(buffer[i]);

                    if ((rabin.getFingerprintLong() & MASK) == 0 || chunkBuffer.size() >= MAX_CHUNK_SIZE) {
                        chunks.add(chunkBuffer.toByteArray());
                        chunkBuffer.reset();
                        rabin = new RabinFingerprintLong(POLYNOMIAL);
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
}
