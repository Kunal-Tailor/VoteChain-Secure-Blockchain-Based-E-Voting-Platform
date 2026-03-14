package com.votingsystem.service;

import com.votingsystem.integration.GetBlockClient;
import com.votingsystem.model.Block;

import java.util.List;
import java.util.Map;

/**
 * Service layer for admin operations
 * Handles admin authentication, blockchain validation,
 * result retrieval, tamper detection, and external
 * blockchain (GetBlock) connectivity checks.
 */
public class AdminService {

    // ⚠️ In production this should be stored securely (hashed)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    // Replace with your actual GetBlock endpoint (keep this private in real apps)
    private static final String GETBLOCK_RPC_URL =
            "https://go.getblock.io/b16ee0001dec43289807ff911e7bbccc";

    private final BlockchainService blockchainService;
    private final GetBlockClient getBlockClient;

    public AdminService(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
        this.getBlockClient = new GetBlockClient(GETBLOCK_RPC_URL);
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

    // ===== GetBlock (external node) integration =====

    /**
     * Quick reachability check for the configured GetBlock node.
     */
    public boolean isGetBlockReachable() {
        return getBlockClient.isReachable();
    }

    /**
     * Returns the latest block number reported by the GetBlock node,
     * or -1 if unavailable.
     */
    public long getGetBlockLatestBlock() throws Exception {
        return getBlockClient.getLatestBlockNumber();
    }
}

