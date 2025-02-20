package org.hetic;

import java.nio.ByteBuffer;

import com.github.luben.zstd.Zstd;

public class ChunkCompressor {
    private static final int COMPRESSION_LEVEL = 22; // Entre 1 et 22

    public byte[] compressChunk(byte[] chunk) {
        // Estimation de la taille maximale compressée
        long maxCompressedSize = Zstd.compressBound(chunk.length);
        byte[] compressedData = new byte[(int) maxCompressedSize];

        // Compression du chunk
        long compressedSize = Zstd.compress(compressedData,chunk,COMPRESSION_LEVEL);

        if (compressedSize < 0) {
            throw new RuntimeException("Échec de la compression: " + Zstd.getErrorName(compressedSize));
        }

        if (compressedSize >= chunk.length) {
            return chunk;
        }

        // Créer un nouveau tableau de la taille exacte des données compressées
        byte[] result = new byte[(int) compressedSize];
        System.arraycopy(compressedData, 0, result, 0, (int) compressedSize);
        return result;
    }


    public byte[] decompressChunk(byte[] compressedChunk, long originalSize) {
        if (originalSize < 0) {
            // Si la taille originale n'est pas connue, on la détermine
            originalSize = Zstd.decompress(compressedChunk, new byte[0]);
        }

        if (originalSize == 0) {
            throw new RuntimeException("Impossible de déterminer la taille originale du chunk");
        }

        byte[] result = new byte[(int) originalSize];
        long decompressedSize = Zstd.decompress(result, compressedChunk);

        if (decompressedSize < 0) {
            throw new RuntimeException("Échec de la décompression: " + Zstd.getErrorName(decompressedSize));
        }

        return result;
    }

    public byte[] createCompressedChunkWithMetadata(byte[] chunk) {
            byte[] compressedData = compressChunk(chunk);
            
            // Format: [taille originale (8 bytes)][données compressées]
            ByteBuffer buffer = ByteBuffer.allocate(8 + compressedData.length);
            buffer.putLong(chunk.length);  // Taille originale
            buffer.put(compressedData);    // Données compressées
            
            return buffer.array();
        }

    public byte[] decompressChunkWithMetadata(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            
            // Lecture de la taille originale
            long originalSize = buffer.getLong();
            
            // Extraction des données compressées
            byte[] compressedData = new byte[data.length - 8];
            buffer.get(compressedData);
            
            return decompressChunk(compressedData, originalSize);
        }


    
}
