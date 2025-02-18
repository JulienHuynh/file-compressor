package org.hetic;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChunkStorageUtils {
    public static String hashChunk(byte[] chunk) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(chunk);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
