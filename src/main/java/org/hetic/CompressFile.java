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
            // Initialiser le système de déduplication avec PostgreSQL
            SQLChunkStorageSystem chunkStorageSystem = new SQLChunkStorageSystem();

            // Dossier à analyser
            String folderPath = "data-files";
            processFolder(chunkStorageSystem, folderPath);

            chunkStorageSystem.printChunkDetails();

            // Afficher les statistiques
            DeduplicationStats stats = chunkStorageSystem.calculateDetailedStats();
            System.out.println(stats.toString());
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