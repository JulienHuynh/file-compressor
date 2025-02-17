package org.hetic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public abstract class DeduplicationSystem {
    protected final MessageDigest hashAlgorithm;

    public DeduplicationSystem(String algorithmName) throws NoSuchAlgorithmException {
        this.hashAlgorithm = MessageDigest.getInstance(algorithmName);
    }

    public static class ChunkMetadata {
        private final String hash;
        private final long size;
        private final String location;

        public ChunkMetadata(String hash, long size, String location) {
            this.hash = hash;
            this.size = size;
            this.location = location;
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
    }

    public abstract ChunkMetadata addChunk(byte[] chunk, String filename, int chunkNumber);

    protected String calculateHash(byte[] data) {
        byte[] hash = hashAlgorithm.digest(data);
        return HexFormat.of().formatHex(hash);
    }
}