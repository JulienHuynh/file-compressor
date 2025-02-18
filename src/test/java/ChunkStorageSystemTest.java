import org.hetic.SQLChunkStorageSystem;
import org.hetic.models.ChunkMetadata;
import org.hetic.models.DeduplicationStats;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.io.*;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkStorageSystemTest {
    private SQLChunkStorageSystem deduplicationSystem;
    private static final String TEST_CONTENT_1 = "Contenu de test 1";
    private static final String TEST_CONTENT_2 = "Contenu de test 2";
    private static final String TEST_FILENAME = "test.txt";

    @BeforeEach
    void setUp() {
        deduplicationSystem = new SQLChunkStorageSystem();
    }

    @Test
    void testAddUniqueChunk() throws NoSuchAlgorithmException {
        // Créer un chunk de test
        byte[] chunk = TEST_CONTENT_1.getBytes();

        // Ajouter le chunk
        ChunkMetadata metadata = deduplicationSystem.addChunk(chunk, TEST_FILENAME, 0);

        // Vérifications
        assertNotNull(metadata, "Le metadata ne devrait pas être null");
        assertTrue(metadata.getHash().length() > 0, "Le hash devrait être généré");
        assertEquals(chunk.length, metadata.getSize(), "La taille devrait correspondre");
        assertTrue(metadata.getLocation().contains("storage"),
                "Le chemin devrait contenir 'storage': " + metadata.getLocation());
    }

    @Test
    void testAddDuplicateChunk() throws NoSuchAlgorithmException {
        // Ajouter le même contenu deux fois
        byte[] chunk = TEST_CONTENT_1.getBytes();
        ChunkMetadata metadata1 = deduplicationSystem.addChunk(chunk, "file1.txt", 0);
        ChunkMetadata metadata2 = deduplicationSystem.addChunk(chunk, "file2.txt", 0);

        // Vérifier que les hashs sont identiques
        assertEquals(metadata1.getHash(), metadata2.getHash(),
                "Les hashs devraient être identiques pour le même contenu");
        assertEquals(metadata1.getLocation(), metadata2.getLocation(),
                "Les chemins de stockage devraient être identiques");
    }

    @Test
    void testMultipleChunks() throws NoSuchAlgorithmException {
        // Tester l'ajout de plusieurs chunks pour le même fichier
        byte[] chunk1 = TEST_CONTENT_1.getBytes();
        byte[] chunk2 = TEST_CONTENT_2.getBytes();

        ChunkMetadata metadata1 = deduplicationSystem.addChunk(chunk1, TEST_FILENAME, 0);
        ChunkMetadata metadata2 = deduplicationSystem.addChunk(chunk2, TEST_FILENAME, 1);

        assertNotEquals(metadata1.getHash(), metadata2.getHash(),
                "Les hashs devraient être différents pour des contenus différents");
    }

    @Test
    void testPhysicalStorage() throws IOException, NoSuchAlgorithmException {
        // Créer un chunk et vérifier son stockage physique
        byte[] chunk = TEST_CONTENT_1.getBytes();
        ChunkMetadata metadata = deduplicationSystem.addChunk(chunk, TEST_FILENAME, 0);

        // Vérifier que le fichier existe
        Path storagePath = Paths.get(metadata.getLocation());
        assertTrue(Files.exists(storagePath),
                "Le fichier devrait exister dans le stockage");

        // Vérifier le contenu
        byte[] storedContent = Files.readAllBytes(storagePath);
        assertArrayEquals(chunk, storedContent,
                "Le contenu stocké devrait être identique au chunk original");
    }

    @Test
    void testStatisticsWithDuplicates() throws NoSuchAlgorithmException {
        // Ajouter des chunks avec des doublons
        byte[] chunk1 = TEST_CONTENT_1.getBytes();
        byte[] chunk2 = TEST_CONTENT_2.getBytes();

        // Même contenu, fichiers différents
        deduplicationSystem.addChunk(chunk1, "file1.txt", 0);
        deduplicationSystem.addChunk(chunk1, "file2.txt", 0);
        deduplicationSystem.addChunk(chunk2, "file3.txt", 0);

        DeduplicationStats stats = deduplicationSystem.calculateDetailedStats();

        assertEquals(3, stats.totalChunks(), "Nombre total de chunks incorrect");
        assertEquals(2, stats.uniqueChunks(), "Nombre de chunks uniques incorrect");
        assertEquals(1, stats.duplicateChunks(), "Nombre de chunks dupliqués incorrect");
        assertTrue(stats.deduplicationRatio() > 0, "Le taux de déduplication devrait être positif");
    }

    @Test
    void testChunkDetails() throws NoSuchAlgorithmException {
        // Ajouter des chunks pour tester l'affichage des détails
        byte[] chunk = TEST_CONTENT_1.getBytes();
        deduplicationSystem.addChunk(chunk, "file1.txt", 0);
        deduplicationSystem.addChunk(chunk, "file2.txt", 0);

        // Rediriger la sortie standard pour vérification
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        deduplicationSystem.printChunkDetails();

        // Restaurer la sortie standard
        System.setOut(originalOut);

        String output = outContent.toString();
        assertTrue(output.contains("Hash:"), "Les détails devraient inclure le hash");
        assertTrue(output.contains("Stockage:"), "Les détails devraient inclure le chemin de stockage");
        assertTrue(output.contains("Références: 2"), "Les détails devraient montrer 2 références");
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