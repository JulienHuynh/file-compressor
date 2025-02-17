import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hetic.SQLDeduplicationSystem;
import org.hetic.DeduplicationSystem.ChunkMetadata;
import org.hetic.DeduplicationStats;

public class SQLDeduplicationSystemTest {
    private SQLDeduplicationSystem deduplicationSystem;
    private static final String TEST_CONTENT_1 = "Contenu de test 1";
    private static final String TEST_CONTENT_2 = "Contenu de test 2";
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        deduplicationSystem = new SQLDeduplicationSystem("SHA-256");
    }

    @Test
    void testAddUniqueChunk() {
        // Créer un chunk de test
        byte[] chunk = TEST_CONTENT_1.getBytes();
        String location = "test.txt_chunk_0";

        // Ajouter le chunk
        ChunkMetadata metadata = deduplicationSystem.addChunk(chunk, location);

        // Vérifications
        assertNotNull(metadata, "Le metadata ne devrait pas être null");
        assertNotNull(metadata.getHash(), "Le hash ne devrait pas être null");
        assertTrue(metadata.getHash().length() > 0, "Le hash devrait avoir une longueur > 0");
        assertEquals(chunk.length, metadata.getSize(), "La taille du chunk devrait correspondre");
        
        // Vérifier que le chemin existe
        Path storagePath = Paths.get(metadata.getLocation());
        assertTrue(storagePath.toString().contains("storage"), 
            "Le chemin devrait contenir 'storage': " + storagePath);
    }

    @Test
    void testAddDuplicateChunk() {
        // Ajouter le même contenu deux fois
        byte[] chunk = TEST_CONTENT_1.getBytes();
        ChunkMetadata metadata1 = deduplicationSystem.addChunk(chunk, "test1.txt_chunk_0");
        ChunkMetadata metadata2 = deduplicationSystem.addChunk(chunk, "test2.txt_chunk_0");

        // Vérifier que les hashs sont identiques
        assertEquals(metadata1.getHash(), metadata2.getHash(), 
            "Les hashs devraient être identiques pour le même contenu");
        assertEquals(metadata1.getLocation(), metadata2.getLocation(), 
            "Les chemins de stockage devraient être identiques");
    }

    @Test
    void testCalculateStats() {
        // Ajouter des chunks uniques et dupliqués
        byte[] chunk1 = TEST_CONTENT_1.getBytes();
        byte[] chunk2 = TEST_CONTENT_2.getBytes();

        deduplicationSystem.addChunk(chunk1, "file1_chunk_0");
        deduplicationSystem.addChunk(chunk1, "file2_chunk_0"); // Doublon
        deduplicationSystem.addChunk(chunk2, "file3_chunk_0");

        // Calculer les statistiques
        DeduplicationStats stats = deduplicationSystem.calculateDetailedStats();

        // Vérifications
        assertEquals(3, stats.totalChunks(), "Nombre total de chunks incorrect");
        assertEquals(2, stats.uniqueChunks(), "Nombre de chunks uniques incorrect");
        assertEquals(1, stats.duplicateChunks(), "Nombre de chunks dupliqués incorrect");
        assertTrue(stats.deduplicationRatio() > 0, "Le taux de déduplication devrait être > 0");
    }

    @Test
    void testPhysicalStorage() throws IOException {
        // Créer un chunk et vérifier son stockage physique
        byte[] chunk = TEST_CONTENT_1.getBytes();
        ChunkMetadata metadata = deduplicationSystem.addChunk(chunk, "test.txt_chunk_0");

        // Vérifier que le fichier existe
        Path storagePath = Paths.get(metadata.getLocation());
        assertTrue(Files.exists(storagePath), 
            "Le fichier devrait exister: " + storagePath);

        // Vérifier le contenu
        byte[] storedContent = Files.readAllBytes(storagePath);
        assertArrayEquals(chunk, storedContent, 
            "Le contenu stocké devrait être identique au chunk original");
    }

    @AfterEach
    void tearDown() {
        // Nettoyage du storage
        try {
            Path storagePath = Paths.get("storage");
            if (Files.exists(storagePath)) {
                Files.walk(storagePath)
                     .sorted((a, b) -> b.compareTo(a))
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             System.err.println("Erreur lors du nettoyage: " + e.getMessage());
                         }
                     });
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du nettoyage du storage: " + e.getMessage());
        }
    }
}