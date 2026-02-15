package com.votingsystem.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for SHA-256 hashing
 * Provides secure hash calculation for blockchain blocks
 */
public class HashUtil {
    
    /**
     * Calculate SHA-256 hash of the input string
     * 
     * @param input The string to hash
     * @return Hexadecimal representation of the hash
     */
    public static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Verify if a hash matches the expected value
     */
    public static boolean verifyHash(String input, String expectedHash) {
        return calculateSHA256(input).equals(expectedHash);
    }
}
