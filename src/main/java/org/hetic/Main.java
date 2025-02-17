package org.hetic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Main {
    private static final int CHUNK_SIZE = 20 * 1024; // 1MB par chunk

    public static void main(String[] args) {
        try {
            // Initialiser le système de déduplication avec PostgreSQL
            SQLDeduplicationSystem deduplicationSystem = new SQLDeduplicationSystem("SHA-256");
            
            // Dossier à analyser
            String folderPath = "data-files";
            processFolder(deduplicationSystem, folderPath);
            deduplicationSystem.printChunkDetails();
            // Afficher les statistiques
            DeduplicationStats stats = deduplicationSystem.calculateDetailedStats();
            System.out.println(stats.toString());
            
            // Afficher l'historique des 5 dernières analyses
            // System.out.println("\nHistorique des analyses :");
            // deduplicationSystem.getHistoricalStats(5).forEach(historicalStats -> 
            //     System.out.println("- " + historicalStats.toString())
            // );
            
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
                byte[] chunk = Arrays.copyOfRange(fileContent, i, i + chunkSize);
                
                // Ajouter le chunk au système de déduplication
                String location = fileName + "_chunk_" + (i / CHUNK_SIZE);
                deduplicationSystem.addChunk(chunk, location);
            }
            
            System.out.println("Traitement terminé pour : " + fileName);
            
        } catch (IOException e) {
            System.err.println("Erreur lors du traitement du fichier " + filePath + " : " + e.getMessage());
        }
    }
 
}