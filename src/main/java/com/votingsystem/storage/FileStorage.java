package com.votingsystem.storage;

import com.votingsystem.model.Blockchain;
import java.io.*;

/**
 * Handles file persistence for blockchain data
 * Saves and loads blockchain from disk
 */
public class FileStorage {
    
    private static final String BLOCKCHAIN_FILE = "blockchain.dat";
    private static final String VOTERS_FILE = "voters.dat";
    
    /**
     * Save blockchain to file
     */
    public static void saveBlockchain(Blockchain blockchain) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(BLOCKCHAIN_FILE))) {
            oos.writeObject(blockchain);
            System.out.println("Blockchain saved successfully to " + BLOCKCHAIN_FILE);
        } catch (IOException e) {
            System.err.println("Error saving blockchain: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load blockchain from file
     */
    public static Blockchain loadBlockchain() {
        File file = new File(BLOCKCHAIN_FILE);
        if (!file.exists()) {
            System.out.println("Blockchain file not found. Creating new blockchain.");
            return new Blockchain();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(BLOCKCHAIN_FILE))) {
            Blockchain blockchain = (Blockchain) ois.readObject();
            System.out.println("Blockchain loaded successfully from " + BLOCKCHAIN_FILE);
            
            // Rebuild voted voters map from chain
            rebuildVotedVoters(blockchain);
            
            return blockchain;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading blockchain: " + e.getMessage());
            e.printStackTrace();
            return new Blockchain();
        }
    }
    
    /**
     * Rebuild voted voters map from blockchain (ensures consistency)
     */
    private static void rebuildVotedVoters(Blockchain blockchain) {
        // Rebuild voted voters map from chain to ensure consistency
        var chain = blockchain.getChain();
        java.util.Map<String, Boolean> votedVoters = new java.util.HashMap<>();
        
        // Rebuild from chain (skip genesis block)
        for (var block : chain) {
            if (block.getIndex() > 0 && !block.getVoterId().equals("SYSTEM")) {
                votedVoters.put(block.getVoterId(), true);
            }
        }
        
        // Update blockchain with rebuilt map
        blockchain.setVotedVoters(votedVoters);
    }
    
    /**
     * Save voter credentials to file
     */
    public static void saveVoters(java.util.Map<String, String> voters) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(VOTERS_FILE))) {
            oos.writeObject(voters);
            System.out.println("Voters saved successfully to " + VOTERS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving voters: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load voter credentials from file
     */
    @SuppressWarnings("unchecked")
    public static java.util.Map<String, String> loadVoters() {
        File file = new File(VOTERS_FILE);
        if (!file.exists()) {
            System.out.println("Voters file not found. Creating default voters.");
            java.util.Map<String, String> defaultVoters = new java.util.HashMap<>();
            // Add some default voters for testing
            defaultVoters.put("V001", "voter1");
            defaultVoters.put("V002", "voter2");
            defaultVoters.put("V003", "voter3");
            defaultVoters.put("V004", "voter4");
            defaultVoters.put("V005", "voter5");
            saveVoters(defaultVoters);
            return defaultVoters;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(VOTERS_FILE))) {
            return (java.util.Map<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading voters: " + e.getMessage());
            e.printStackTrace();
            return new java.util.HashMap<>();
        }
    }
    
    /**
     * Check if blockchain file exists
     */
    public static boolean blockchainFileExists() {
        return new File(BLOCKCHAIN_FILE).exists();
    }
}
