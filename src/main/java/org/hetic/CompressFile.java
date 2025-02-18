package org.hetic;

import org.hetic.models.DeduplicationStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class CompressFile {
    private static final ContentDefinedChunking chunker = new ContentDefinedChunking();

    public static void main(String[] args) {
        try {
            // 1. Initialiser le système
            SQLChunkStorageSystem chunkStorageSystem = new SQLChunkStorageSystem();
            FileReconstructor reconstructor = new FileReconstructor(chunkStorageSystem);

            // 2. Traiter les fichiers
            String folderPath = "data-files";
            System.out.println("Traitement des fichiers...\n");
            processFolder(chunkStorageSystem, folderPath);

            // 3. Afficher les statistiques de déduplication
            chunkStorageSystem.printChunkDetails();
            DeduplicationStats stats = chunkStorageSystem.calculateDetailedStats();
            System.out.println(stats.toString());

            // 4. Afficher les fichiers disponibles pour reconstruction
            System.out.println("\nListe des fichiers disponibles pour reconstruction :");
            reconstructor.listAvailableFiles();

            // 5. Reconstruire un fichier spécifique
            String fileToReconstruct = "test.txt";
            System.out.println("\nReconstruction du fichier : " + fileToReconstruct);
            reconstructor.reconstructFile(fileToReconstruct);

        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processFolder(SQLChunkStorageSystem chunkStorageSystem, String folderPath) {
        try {
            Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .forEach(file -> processFile(chunkStorageSystem, file));
        } catch (IOException e) {
            System.err.println("Erreur lors du parcours du dossier : " + e.getMessage());
        }
    }

    private static void processFile(SQLChunkStorageSystem chunkStorageSystem, Path filePath) {
        try {
            List<byte[]> chunks = chunker.chunkFile(String.valueOf(filePath));
            String fileName = filePath.getFileName().toString();

            for (int i = 0; i < chunks.size(); i++) {
                chunkStorageSystem.addChunk(chunks.get(i), fileName, i);
            }

            System.out.println("Traitement terminé pour : " + fileName);

        } catch (IOException e) {
            System.err.println("Erreur lors du traitement du fichier " + filePath + " : " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}