import org.hetic.models.ChunkMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.hetic.ChunkStorageUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class ChunkStorageSystemTest {
    private ChunkStorageUtils chunkStorageUtils;

    @BeforeEach
    void setUp() {
        chunkStorageUtils = new ChunkStorageUtils();
    }

    @Test
    void testDuplicateDetection() throws NoSuchAlgorithmException {
        // Simuler deux chunks identiques
        byte[] chunk1 = "Test content".getBytes();
        byte[] chunk2 = "Test content".getBytes();
        byte[] chunk3 = "Different content".getBytes();

        // Ajouter le premier chunk
        ChunkMetadata metadata1 = chunkStorageUtils.addChunk(chunk1, "location1");
        assertNotNull(metadata1);
        assertEquals(1, metadata1.getReferenceCount());

        // Vérifier la détection du doublon
        Optional<ChunkMetadata> duplicate = chunkStorageUtils.findDuplicate(chunk2);
        assertTrue(duplicate.isPresent());
        assertEquals(metadata1.getHash(), duplicate.get().getHash());

        // Ajouter le doublon et vérifier l'incrémentation du compteur
        ChunkMetadata metadata2 = chunkStorageUtils.addChunk(chunk2, "location2");
        assertEquals(2, metadata2.getReferenceCount());
        assertEquals(metadata1.getHash(), metadata2.getHash());

        // Vérifier qu'un contenu différent n'est pas détecté comme doublon
        Optional<ChunkMetadata> nonDuplicate = chunkStorageUtils.findDuplicate(chunk3);
        assertFalse(nonDuplicate.isPresent());
    }
}