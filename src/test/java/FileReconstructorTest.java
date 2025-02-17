import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import org.hetic.SQLDeduplicationSystem;
import org.hetic.FileReconstructor;

public class FileReconstructorTest {
    private SQLDeduplicationSystem deduplicationSystem;
    private FileReconstructor fileReconstructor;
    private static final String TEST_FILENAME = "test.txt";
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        deduplicationSystem = new SQLDeduplicationSystem("SHA-256");
        fileReconstructor = new FileReconstructor(deduplicationSystem);
    }

    @Test
    void testBasicReconstruction() throws IOException {
        // Créer un fichier de test
        String originalContent = "Contenu du fichier de test";
        byte[] content = originalContent.getBytes();
        
        // Ajouter comme un chunk
        deduplicationSystem.addChunk(content, TEST_FILENAME + "_chunk_0");

        // Reconstruire le fichier
        fileReconstructor.reconstructFile(TEST_FILENAME);

        // Vérifier le fichier reconstruit
        Path reconstructedPath = Paths.get("reconstructed", "reconstructed_" + TEST_FILENAME);
        assertTrue(Files.exists(reconstructedPath));
        
        String reconstructedContent = Files.readString(reconstructedPath);
        assertEquals(originalContent, reconstructedContent);
    }

    @Test
    void testMultiChunkReconstruction() throws IOException {
        // Créer plusieurs chunks
        String chunk1 = "Première partie ";
        String chunk2 = "Deuxième partie ";
        String chunk3 = "Troisième partie";

        // Ajouter les chunks
        deduplicationSystem.addChunk(chunk1.getBytes(), TEST_FILENAME + "_chunk_0");
        deduplicationSystem.addChunk(chunk2.getBytes(), TEST_FILENAME + "_chunk_1");
        deduplicationSystem.addChunk(chunk3.getBytes(), TEST_FILENAME + "_chunk_2");

        // Reconstruire le fichier
        fileReconstructor.reconstructFile(TEST_FILENAME);

        // Vérifier le résultat
        Path reconstructedPath = Paths.get("reconstructed", "reconstructed_" + TEST_FILENAME);
        String reconstructedContent = Files.readString(reconstructedPath);
        assertEquals(chunk1 + chunk2 + chunk3, reconstructedContent);
    }

    @Test
    void testNonExistentFile() {
        // Tenter de reconstruire un fichier inexistant
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileReconstructor.reconstructFile("nonexistent.txt");
        });

        assertTrue(exception.getMessage().contains("Fichier non trouvé"));
    }

    @Test
    void testListAvailableFiles() throws IOException {
        // Ajouter quelques fichiers de test
        deduplicationSystem.addChunk("Test1".getBytes(), "file1.txt_chunk_0");
        deduplicationSystem.addChunk("Test2".getBytes(), "file2.txt_chunk_0");

        // Vérifier la liste des fichiers disponibles
        final boolean[] fileFound = {false};
        
        // Rediriger System.out pour capturer la sortie
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try (PrintStream originalOut = System.out;
             PrintStream capturedOut = new PrintStream(outContent)) {
            
            System.setOut(capturedOut);
            fileReconstructor.listAvailableFiles();
            System.setOut(originalOut);
            
            String output = outContent.toString();
            assertTrue(output.contains("file1.txt"));
            assertTrue(output.contains("file2.txt"));
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // Nettoyage des fichiers temporaires
        Files.walk(Paths.get("reconstructed"))
             .sorted((a, b) -> b.compareTo(a))
             .forEach(path -> {
                 try {
                     Files.deleteIfExists(path);
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             });
    }
}