import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.hetic.DeduplicationSystem;
import java.util.Optional;
import org.hetic.DeduplicationSystem.ChunkMetadata;

public class DeduplicationSystemTest {
    private DeduplicationSystem deduplicationSystem;

    @BeforeEach
    void setUp() throws Exception {
        deduplicationSystem = new DeduplicationSystem("SHA-256");
    }

    @Test
    void testDuplicateDetection() {
        // Simuler deux chunks identiques
        byte[] chunk1 = "Test content".getBytes();
        byte[] chunk2 = "Test content".getBytes();
        byte[] chunk3 = "Different content".getBytes();

        // Ajouter le premier chunk
        ChunkMetadata metadata1 = deduplicationSystem.addChunk(chunk1, "location1");
        assertNotNull(metadata1);
        assertEquals(1, metadata1.getReferenceCount());

        // Vérifier la détection du doublon
        Optional<ChunkMetadata> duplicate = deduplicationSystem.findDuplicate(chunk2);
        assertTrue(duplicate.isPresent());
        assertEquals(metadata1.getHash(), duplicate.get().getHash());

        // Ajouter le doublon et vérifier l'incrémentation du compteur
        ChunkMetadata metadata2 = deduplicationSystem.addChunk(chunk2, "location2");
        assertEquals(2, metadata2.getReferenceCount());
        assertEquals(metadata1.getHash(), metadata2.getHash());

        // Vérifier qu'un contenu différent n'est pas détecté comme doublon
        Optional<ChunkMetadata> nonDuplicate = deduplicationSystem.findDuplicate(chunk3);
        assertFalse(nonDuplicate.isPresent());

        // Vérifier les statistiques
        assertEquals(1, deduplicationSystem.getUniqueChunksCount());
        assertEquals(1, deduplicationSystem.getDuplicateChunksCount());
        assertEquals(chunk1.length, deduplicationSystem.getTotalDuplicateSize());
    }
}