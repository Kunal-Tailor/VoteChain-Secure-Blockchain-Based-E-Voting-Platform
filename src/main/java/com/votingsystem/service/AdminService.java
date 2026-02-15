package com.votingsystem.service;

import com.votingsystem.model.Block;
import java.util.List;
import java.util.Map;

/**
 * Service layer for admin operations
 * Handles admin authentication, blockchain validation,
 * result retrieval, and tamper detection.
 */
public class AdminService {

    // ⚠️ In production this should be stored securely (hashed)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final BlockchainService blockchainService;

    public AdminService(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    /**
     * Authenticate admin credentials
     */
    public boolean authenticateAdmin(String username, String password) {
        return ADMIN_USERNAME.equals(username)
                && ADMIN_PASSWORD.equals(password);
    }

    /**
     * Validate blockchain integrity
     */
    public boolean validateBlockchain() {
        return blockchainService.validateBlockchain();
    }

    /**
     * Retrieve election results
     */
    public Map<String, Integer> getElectionResults() {
        return blockchainService.getElectionResults();
    }

    /**
     * Get total number of votes cast
     */
    public int getTotalVotes() {
        return blockchainService.getTotalVotes();
    }

    /**
     * Get all blocks for visualization
     * Used by Admin Dashboard for blockchain viewer
     */
    public List<Block> getAllBlocks() {
        return blockchainService.getAllBlocks();
    }

    /**
     * Detect tampering and return detailed message
     */
    public String detectTampering() {
        if (!validateBlockchain()) {
            return "⚠️ TAMPERING DETECTED!\n\n" +
                    "Blockchain integrity has been compromised.\n" +
                    "One or more blocks may have been modified.";
        }

        return "✅ Blockchain is Secure.\n\n" +
                "All blocks are valid and properly linked.";
    }

    /**
     * Get blockchain health summary (for dashboard badge)
     */
    public String getBlockchainHealthStatus() {
        return validateBlockchain() ? "SECURE" : "TAMPERED";
    }
}
