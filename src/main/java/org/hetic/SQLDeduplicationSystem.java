package org.hetic;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class SQLDeduplicationSystem extends DeduplicationSystem {
    private final HikariDataSource dataSource;
    private static final String STORAGE_BASE_PATH = "/storage";

    public SQLDeduplicationSystem(String algorithmName) throws NoSuchAlgorithmException {
        super(algorithmName);
        this.dataSource = setupDataSource();
        initializeDatabase();
        resetDatabase();
    }

    private HikariDataSource setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/filecompressor");
        config.setUsername("postgres");
        config.setPassword("root");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Table pour stocker les chunks uniques
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS chunks (
                    chunk_hash VARCHAR(64) PRIMARY KEY,
                    file_path TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Table pour la relation fichiers-chunks
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS file_chunks (
                    id SERIAL PRIMARY KEY,
                    filename TEXT NOT NULL,
                    chunk_hash VARCHAR(64) REFERENCES chunks(chunk_hash),
                    chunk_number INT NOT NULL,
                    UNIQUE(filename, chunk_number)
                )
            """);
            
        } catch (SQLException e) {
            throw new RuntimeException("Erreur d'initialisation de la base de données", e);
        }
    }

    private void resetDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Vider les tables dans le bon ordre (à cause des clés étrangères)
            stmt.execute("TRUNCATE TABLE file_chunks CASCADE");
            stmt.execute("TRUNCATE TABLE chunks CASCADE");
            System.out.println("Base de données réinitialisée pour une nouvelle analyse.");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la réinitialisation de la base de données", e);
        }
    }

   private String generateStoragePath(String hash) {
        return Paths.get(STORAGE_BASE_PATH, hash).toString();
    }

    @Override
    public ChunkMetadata addChunk(byte[] chunk, String location) {
        String hash = calculateHash(chunk);
        String filename = location.substring(0, location.lastIndexOf("_chunk_"));
        int chunkNumber = Integer.parseInt(location.substring(location.lastIndexOf("_") + 1));
        String storagePath = generateStoragePath(hash);
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Vérifier si le chunk existe déjà
                boolean chunkExists = false;
                try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT 1 FROM chunks WHERE chunk_hash = ?")) {
                    checkStmt.setString(1, hash);
                    ResultSet rs = checkStmt.executeQuery();
                    chunkExists = rs.next();
                }

                // Si le chunk n'existe pas, l'insérer avec son chemin de stockage
                if (!chunkExists) {
                    try (PreparedStatement insertChunkStmt = conn.prepareStatement(
                        "INSERT INTO chunks (chunk_hash, file_path) VALUES (?, ?)")) {
                        insertChunkStmt.setString(1, hash);
                        insertChunkStmt.setString(2, storagePath);
                        insertChunkStmt.executeUpdate();
                        
                        // Ici, vous pourriez ajouter le code pour sauvegarder physiquement le chunk
                        // saveChunkToStorage(chunk, storagePath);
                    }
                }

                // Ajouter l'entrée dans file_chunks
                try (PreparedStatement insertFileChunkStmt = conn.prepareStatement(
                    "INSERT INTO file_chunks (filename, chunk_hash, chunk_number) VALUES (?, ?, ?)")) {
                    insertFileChunkStmt.setString(1, filename);
                    insertFileChunkStmt.setString(2, hash);
                    insertFileChunkStmt.setInt(3, chunkNumber);
                    insertFileChunkStmt.executeUpdate();
                }

                conn.commit();
                return new ChunkMetadata(hash, chunk.length, storagePath);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du chunk", e);
        }
    }

    // Méthode pour afficher les détails incluant le chemin de stockage
    public void printChunkDetails() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("""
                SELECT c.chunk_hash, 
                       c.file_path,
                       COUNT(fc.id) as reference_count
                FROM chunks c
                JOIN file_chunks fc ON c.chunk_hash = fc.chunk_hash
                GROUP BY c.chunk_hash, c.file_path
                ORDER BY reference_count DESC
            """);

            System.out.println("\nDétails des chunks en base de données :");
            System.out.println("----------------------------------------");
            while (rs.next()) {
                System.out.printf("Hash: %-10s | Stockage: %-40s | Références: %d%n",
                    rs.getString("chunk_hash").substring(0, 8) + "...",
                    rs.getString("file_path"),
                    rs.getInt("reference_count")
                );
            }
            System.out.println("----------------------------------------\n");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'affichage des détails des chunks", e);
        }
    }
    public DeduplicationStats calculateDetailedStats() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("""
                WITH chunk_stats AS (
                    SELECT c.chunk_hash,
                           COUNT(fc.id) as ref_count
                    FROM chunks c
                    JOIN file_chunks fc ON c.chunk_hash = fc.chunk_hash
                    GROUP BY c.chunk_hash
                ),
                summary AS (
                    SELECT 
                        COUNT(*) as total_unique_chunks,
                        SUM(ref_count) as total_references,
                        COUNT(CASE WHEN ref_count > 1 THEN 1 END) as duplicated_chunks
                    FROM chunk_stats
                )
                SELECT 
                    total_references as total_chunks,
                    total_unique_chunks as unique_chunks,
                    duplicated_chunks as duplicate_chunks,
                    CASE 
                        WHEN total_references > 0 
                        THEN ROUND(CAST((total_references - total_unique_chunks)::FLOAT / total_references * 100 as NUMERIC), 2)
                        ELSE 0.0 
                    END as deduplication_ratio
                FROM summary
            """);

            if (rs.next()) {
                return new DeduplicationStats(
                    rs.getLong("total_chunks"),
                    rs.getLong("unique_chunks"),
                    rs.getLong("duplicate_chunks"),
                    rs.getLong("total_chunks"),
                    rs.getLong("total_chunks") - rs.getLong("unique_chunks"),
                    rs.getDouble("deduplication_ratio")
                );
            }

            return new DeduplicationStats(0, 0, 0, 0, 0, 0.0);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du calcul des statistiques", e);
        }
    }
}