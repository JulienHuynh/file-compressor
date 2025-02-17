package org.hetic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.HexFormat;

public class DeduplicationSystem {
    private final MessageDigest hashAlgorithm;
    private final Map<String, ChunkMetadata> chunkIndex;

    public DeduplicationSystem(String algorithmName) throws NoSuchAlgorithmException {
        this.hashAlgorithm = MessageDigest.getInstance(algorithmName);
        this.chunkIndex = new HashMap<>();
    }

    public static class ChunkMetadata {
        private final String hash;
        private final long size;
        private final String location;
        private int referenceCount;

        public ChunkMetadata(String hash, long size, String location) {
            this.hash = hash;
            this.size = size;
            this.location = location;
            this.referenceCount = 1;
        }

        public void incrementReferenceCount() {
            this.referenceCount++;
        }

        public String getHash() {
            return hash;
        }

        public long getSize() {
            return size;
        }

        public String getLocation() {
            return location;
        }

        public int getReferenceCount() {
            return referenceCount;
        }
    }

    public Optional<ChunkMetadata> findDuplicate(byte[] chunk) {
        String hash = calculateHash(chunk);
        // Retourne le chunk s'il est présent dans l'index
        return Optional.ofNullable(chunkIndex.get(hash));
    }

    public ChunkMetadata addChunk(byte[] chunk, String location) {
        
        String hash = calculateHash(chunk);
        ChunkMetadata metadata = chunkIndex.get(hash);
        
        if (metadata != null) {
            metadata.incrementReferenceCount();
            return metadata;
        }

        metadata = new ChunkMetadata(hash, chunk.length, location);
        chunkIndex.put(hash, metadata);
        return metadata;
    }

    public String calculateHash(byte[] data) {
        byte[] hash = hashAlgorithm.digest(data);
        return HexFormat.of().formatHex(hash);
    }

    public Map<String, ChunkMetadata> getChunkIndex() {
        return new HashMap<>(chunkIndex);
    }

    public long getTotalDuplicateSize() {
        return chunkIndex.values().stream()
            .filter(metadata -> metadata.getReferenceCount() > 1)
            .mapToLong(metadata -> metadata.getSize() * (metadata.getReferenceCount() - 1))
            .sum();
    }

    public long getUniqueChunksCount() {
        return chunkIndex.size();
    }

    public long getDuplicateChunksCount() {
        return chunkIndex.values().stream()
            .filter(metadata -> metadata.getReferenceCount() > 1)
            .count();
    }
}
