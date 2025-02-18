package org.hetic.models;

public class ChunkMetadata {
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