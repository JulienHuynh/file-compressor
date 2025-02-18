package org.hetic;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileReconstructor {
    private final SQLChunkStorageSystem deduplicationSystem;
    private static final String OUTPUT_DIR = "reconstructed";

    public FileReconstructor(SQLChunkStorageSystem deduplicationSystem) {
        this.deduplicationSystem = deduplicationSystem;
        createOutputDirectory();
    }

    private void createOutputDirectory() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la création du dossier de sortie", e);
        }
    }

    public void listAvailableFiles() {
        try (Connection conn = deduplicationSystem.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("""
                    SELECT DISTINCT 
                           REGEXP_REPLACE(filename, '_chunk_\\d+$', '') as original_filename,
                           COUNT(chunk_number) as chunks,
                           MIN(created_at) as created
                    FROM file_chunks fc
                    JOIN chunks c ON fc.chunk_hash = c.chunk_hash
                    GROUP BY REGEXP_REPLACE(filename, '_chunk_\\d+$', '')
                    ORDER BY created
                """);

                System.out.println("\nFichiers disponibles pour reconstruction :");
                System.out.println("----------------------------------------");
                while (rs.next()) {
                    System.out.printf("Fichier: %-30s | Chunks: %d | Créé le: %s%n",
                        rs.getString("original_filename"),
                        rs.getInt("chunks"),
                        rs.getTimestamp("created").toString()
                    );
                }
                System.out.println("----------------------------------------\n");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la liste des fichiers disponibles", e);
        }
    }

    private List<ChunkInfo> getFileChunks(Connection conn, String filename) throws SQLException {
        List<ChunkInfo> chunks = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement("""
            SELECT fc.chunk_number, c.chunk_hash, c.file_path
            FROM file_chunks fc
            JOIN chunks c ON fc.chunk_hash = c.chunk_hash
            WHERE REGEXP_REPLACE(fc.filename, '_chunk_\\d+$', '') = ?
            ORDER BY fc.chunk_number
        """)) {
            stmt.setString(1, filename);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                chunks.add(new ChunkInfo(
                    rs.getString("chunk_hash"),
                    rs.getString("file_path"),
                    rs.getInt("chunk_number")
                ));
            }
        }
        
        return chunks;
    }
    
    public void reconstructFile(String filename) {
        try (Connection conn = deduplicationSystem.getConnection()) {
            // 1. Récupérer tous les chunks pour ce fichier, ordonnés par numéro
            List<ChunkInfo> chunks = getFileChunks(conn, filename);
            
            if (chunks.isEmpty()) {
                System.out.println("Recherche des chunks pour : " + filename);
                throw new IllegalArgumentException("Fichier non trouvé: " + filename);
            }

            // 2. Créer le fichier de sortie
            Path outputPath = Paths.get(OUTPUT_DIR, "reconstructed_" + filename);
            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                
                System.out.println("Reconstruction en cours...");
                System.out.println("Nombre de chunks trouvés : " + chunks.size());
                
                // 3. Pour chaque chunk
                for (ChunkInfo chunk : chunks) {
                    System.out.printf("Traitement du chunk %d depuis %s%n", 
                        chunk.number(), chunk.storagePath);
                    
                    // 4. Lire le chunk depuis le stockage
                    Path chunkPath = Paths.get(chunk.storagePath);
                    if (!Files.exists(chunkPath)) {
                        throw new IOException("Chunk introuvable: " + chunkPath);
                    }
                    
                    byte[] chunkData = Files.readAllBytes(chunkPath);
                    
                    // 5. Écrire le chunk dans le fichier de sortie
                    outputStream.write(chunkData);
                }
            }
            
            System.out.println("Fichier reconstruit avec succès: " + outputPath);
            
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Erreur lors de la reconstruction du fichier: " + filename, e);
        }
    }
    private record ChunkInfo(String hash, String storagePath, int number) {}
}