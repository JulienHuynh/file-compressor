package org.hetic;

import org.rabinfingerprint.fingerprint.RabinFingerprintLong;
import org.rabinfingerprint.polynomial.Polynomial;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class CompressFile {
    private static final Polynomial POLYNOMIAL = Polynomial.createFromLong(0x3DA3358B4DC173L); // Polynome optimisé
    private static final long MASK = (1 << 12) - 1;  // Seuil de coupure (4 Ko)
    private static final int MAX_CHUNK_SIZE = 20480; // 20 KB max

    public static List<byte[]> chunkFile(String filePath) throws IOException {
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

    public static String hashChunk(byte[] chunk) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(chunk);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String filePath = args[0];

        List<byte[]> chunks = chunkFile(filePath);
        System.out.println("Nombre de chunks : " + chunks.size());

        int i = 1;
        for (byte[] chunk : chunks) {
            System.out.println("Chunk " + i + " - Hash: " + hashChunk(chunk));
            i++;
        }
    }
}