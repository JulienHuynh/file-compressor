package org.hetic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;


public class CompressedChunkStorageSystem extends SQLChunkStorageSystem {
    private final ChunkCompressor compressor;
    
    public CompressedChunkStorageSystem() {
        super();
        this.compressor = new ChunkCompressor();
    }
    
    @Override
    protected void saveChunkToStorage(byte[] chunk, String storagePath) throws IOException {
        // Compresser le chunk avec ses métadonnées
        byte[] compressedData = compressor.createCompressedChunkWithMetadata(chunk);
        
        // Sauvegarder le chunk compressé
        Path path = Paths.get(storagePath);
        Files.createDirectories(path.getParent());
        Files.write(path, compressedData);
        
        double compressionRatio = (1.0 - (double) compressedData.length / chunk.length) * 100;
        // System.out.printf("Chunk compressé : %s (réduction: %.2f%%)%n", 
        //     path.toAbsolutePath(), compressionRatio);
    }
    
    @Override
    protected byte[] readChunkFromStorage(String storagePath) throws IOException {
        byte[] compressedData = Files.readAllBytes(Paths.get(storagePath));
        return compressor.decompressChunkWithMetadata(compressedData);
    }
    
    public void printCompressionStats() throws SQLException{
        try {
            long totalOriginalSize = 0;
            long totalCompressedSize = 0;
            int chunkCount = 0;
            
            Path storagePath = Paths.get(STORAGE_BASE_PATH);
            if (Files.exists(storagePath)) {
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT chunk_hash, file_path FROM chunks")) {
                    
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        String filePath = rs.getString("file_path");
                        String chunkHash = rs.getString("chunk_hash");
                        Path path = Paths.get(filePath);
                        
                        if (Files.exists(path)) {
                            byte[] compressedData = Files.readAllBytes(path);
                            ByteBuffer buffer = ByteBuffer.wrap(compressedData);
                            long originalSize = buffer.getLong(); // Lire la taille originale
                            long compressedSize = compressedData.length - 8; // Soustraire la taille des métadonnées
                            
                            System.out.println("Chunk détails - Hash: " + chunkHash);
                            System.out.println("  Taille originale: " + originalSize + " bytes");
                            System.out.println("  Taille compressée: " + compressedSize + " bytes");
                            System.out.println("  Ratio: " + String.format("%.2f%%", ((double)compressedSize/originalSize) * 100));
                            
                            totalOriginalSize += originalSize;
                            totalCompressedSize += compressedSize;
                            chunkCount++;
                        } else {
                            System.out.println("ATTENTION: Fichier non trouvé: " + filePath);
                        }
                    }
                }
            }
            
            if (chunkCount > 0) {
                System.out.println("\nTaille totale originale (bytes): " + totalOriginalSize);
                System.out.println("Taille totale compressée (bytes): " + totalCompressedSize);
                double avgCompressionRatio = (double) totalCompressedSize / totalOriginalSize * 100;
                
                System.out.println("\nStatistiques de compression :");
                System.out.println("----------------------------------------");
                System.out.printf("Nombre total de chunks : %d%n", chunkCount);
                System.out.printf("Taille totale originale : %.2f KB%n", totalOriginalSize / 1024.0);
                System.out.printf("Taille totale compressée : %.2f KB%n", totalCompressedSize / 1024.0);
                System.out.printf("Ratio de compression moyen : %.2f%%%n", avgCompressionRatio);
                System.out.printf("Espace économisé : %.2f KB%n", (totalOriginalSize - totalCompressedSize) / 1024.0);
                System.out.println("----------------------------------------\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du calcul des statistiques de compression", e);
        }
    }
}