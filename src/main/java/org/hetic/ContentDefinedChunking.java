package org.hetic;

import org.rabinfingerprint.fingerprint.RabinFingerprintLong;
import org.rabinfingerprint.polynomial.Polynomial;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ContentDefinedChunking {
    private final Polynomial POLYNOMIAL = Polynomial.createFromLong(0x3DA3358B4DC173L); // Polynome optimisé

    public List<byte[]> chunkFile(String filePath) throws IOException {
        final long MASK = 4095; // 4095 = 2^12 - 1
        final int MAX_CHUNK_SIZE = 20480; // 20 Ko

        List<byte[]> chunks = new ArrayList<>();
        File file = new File(filePath);
        try (InputStream inputStream = new FileInputStream(file)) {
            RabinFingerprintLong rabin = new RabinFingerprintLong(POLYNOMIAL);
            ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1];  // On lit 1 octet à la fois

            while (inputStream.read(buffer) != -1) {
                chunkBuffer.write(buffer);  // Ajouter l’octet au chunk en cours
                rabin.pushByte(buffer[0]);  // Mettre à jour l’empreinte

                // Détection d’un point de coupure si l'empreinte respecte le masque
                if ((rabin.getFingerprintLong() & MASK) == 0) {
                    chunks.add(chunkBuffer.toByteArray());
                    chunkBuffer.reset();
                    rabin = new RabinFingerprintLong(POLYNOMIAL);  // Réinitialiser l’empreinte
                } else if (chunkBuffer.size() >= MAX_CHUNK_SIZE) {
                    chunks.add(chunkBuffer.toByteArray());
                    chunkBuffer.reset();
                    rabin = new RabinFingerprintLong(POLYNOMIAL);
                }
            }

            // Ajouter le dernier chunk s'il reste des données
            if (chunkBuffer.size() > 0) {
                chunks.add(chunkBuffer.toByteArray());
            }
        }
        return chunks;
    }
}