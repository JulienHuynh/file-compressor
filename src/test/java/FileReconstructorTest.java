import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import org.hetic.SQLDeduplicationSystem;
import org.hetic.FileReconstructor;

public class FileReconstructorTest {
    private SQLDeduplicationSystem deduplicationSystem;
    private FileReconstructor fileReconstructor;
    private static final String TEST_FILENAME = "test.txt";
    
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
        deduplicationSystem.addChunk(content, TEST_FILENAME, 0);

        // Reconstruire le fichier
        fileReconstructor.reconstructFile(TEST_FILENAME);

        // Vérifier le fichier reconstruit
        Path reconstructedPath = Paths.get("reconstructed", "reconstructed_" + TEST_FILENAME);
        assertTrue(Files.exists(reconstructedPath), "Le fichier reconstruit devrait exister");
        
        String reconstructedContent = Files.readString(reconstructedPath);
        assertEquals(originalContent, reconstructedContent, "Le contenu reconstruit devrait être identique");
    }

    @Test
    void testMultiChunkReconstruction() throws IOException {
        // Simuler un fichier en plusieurs chunks
        String[] chunks = {
            "Première partie ",
            "Deuxième partie ",
            "Troisième partie"
        };

        // Ajouter les chunks
        for (int i = 0; i < chunks.length; i++) {
            deduplicationSystem.addChunk(chunks[i].getBytes(), TEST_FILENAME, i);
        }

        // Reconstruire le fichier
        fileReconstructor.reconstructFile(TEST_FILENAME);

        // Vérifier le résultat
        Path reconstructedPath = Paths.get("reconstructed", "reconstructed_" + TEST_FILENAME);
        String reconstructedContent = Files.readString(reconstructedPath);
        String expectedContent = String.join("", chunks);
        
        assertEquals(expectedContent, reconstructedContent, 
            "Le fichier reconstruit devrait contenir tous les chunks dans l'ordre");
    }

    @Test
    void testNonExistentFile() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileReconstructor.reconstructFile("nonexistent.txt");
        });

        assertTrue(exception.getMessage().contains("Fichier non trouvé"), 
            "Devrait lever une exception pour un fichier inexistant");
    }

    @Test
    void testListAvailableFiles() throws IOException {
        // Ajouter quelques fichiers de test
        deduplicationSystem.addChunk("Test1".getBytes(), "file1.txt", 0);
        deduplicationSystem.addChunk("Test2".getBytes(), "file2.txt", 0);

        // Capturer la sortie standard
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        fileReconstructor.listAvailableFiles();

        // Restaurer la sortie standard
        System.setOut(originalOut);

        String output = outContent.toString();
        assertTrue(output.contains("file1.txt"), "Devrait lister le premier fichier");
        assertTrue(output.contains("file2.txt"), "Devrait lister le deuxième fichier");
    }

    @AfterEach
    void tearDown() throws Exception {
        // Nettoyer les fichiers reconstruits
        Path reconstructedDir = Paths.get("reconstructed");
        if (Files.exists(reconstructedDir)) {
            Files.walk(reconstructedDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         System.err.println("Erreur lors du nettoyage: " + e.getMessage());
                     }
                 });
        }
    }
}