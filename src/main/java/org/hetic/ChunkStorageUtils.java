package org.hetic;

import org.hetic.models.ChunkMetadata;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ChunkStorageUtils {
    private final Map<String, ChunkMetadata> chunkIndex;

    public ChunkStorageUtils() {
        this.chunkIndex = new HashMap<>();
    }

    public Optional<ChunkMetadata> findDuplicate(byte[] chunk) throws NoSuchAlgorithmException {
        String hash = hashChunk(chunk);
        // Retourne le chunk s'il est présent dans l'index
        return Optional.ofNullable(chunkIndex.get(hash));
    }

    public ChunkMetadata addChunk(byte[] chunk, String location) throws NoSuchAlgorithmException {
        String hash = hashChunk(chunk);
        ChunkMetadata metadata = chunkIndex.get(hash);
        
        if (metadata != null) {
            metadata.incrementReferenceCount();
            return metadata;
        }

        metadata = new ChunkMetadata(hash, chunk.length, location);
        chunkIndex.put(hash, metadata);
        return metadata;
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
}
