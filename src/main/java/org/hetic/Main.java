package org.hetic;

import java.nio.file.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main {
    private static final int CHUNK_SIZE = 20 * 1024; // 1MB par chunk

    public static void main(String[] args) {
        try {
            // 1. Initialiser le système
            SQLDeduplicationSystem deduplicationSystem = new SQLDeduplicationSystem("SHA-256");
            FileReconstructor reconstructor = new FileReconstructor(deduplicationSystem);
            
            // 2. Traiter les fichiers
            String folderPath = "data-files";
            System.out.println("Traitement des fichiers...\n");
            processFolder(deduplicationSystem, folderPath);
            
            // 3. Afficher les statistiques de déduplication
            deduplicationSystem.printChunkDetails();
            DeduplicationStats stats = deduplicationSystem.calculateDetailedStats();
            System.out.println(stats.toString());

            // 4. Afficher les fichiers disponibles pour reconstruction
            System.out.println("\nListe des fichiers disponibles pour reconstruction :");
            reconstructor.listAvailableFiles();

            // 5. Reconstruire un fichier spécifique
            String fileToReconstruct = "calendrier (1).pdf";
            System.out.println("\nReconstruction du fichier : " + fileToReconstruct);
            reconstructor.reconstructFile(fileToReconstruct);
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur d'algorithme de hachage : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processFolder(SQLDeduplicationSystem deduplicationSystem, String folderPath) {
        try {
            Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .forEach(file -> processFile(deduplicationSystem, file));
        } catch (IOException e) {
            System.err.println("Erreur lors du parcours du dossier : " + e.getMessage());
        }
    }

    private static void processFile(SQLDeduplicationSystem deduplicationSystem, Path filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            String fileName = filePath.getFileName().toString();

            // Découper le fichier en chunks
            for (int i = 0; i < fileContent.length; i += CHUNK_SIZE) {
                int chunkSize = Math.min(CHUNK_SIZE, fileContent.length - i);
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(fileContent, i, chunk, 0, chunkSize);

                // Ajouter directement le chunk avec le nom de fichier et le numéro
                deduplicationSystem.addChunk(chunk, fileName, i / CHUNK_SIZE);
            }

            System.out.println("Traitement terminé pour : " + fileName);

        } catch (IOException e) {
            System.err.println("Erreur lors du traitement du fichier " + filePath + " : " + e.getMessage());
        }
    }
}