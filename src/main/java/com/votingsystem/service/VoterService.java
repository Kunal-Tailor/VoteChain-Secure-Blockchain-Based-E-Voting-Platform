package com.votingsystem.service;

import com.votingsystem.storage.FileStorage;
import java.util.Map;

/**
 * Service layer for voter operations
 * Handles voter authentication and vote casting
 */
public class VoterService {
    
    private Map<String, String> voters; // VoterID -> Password
    private BlockchainService blockchainService;
    
    public VoterService(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
        this.voters = FileStorage.loadVoters();
    }
    
    /**
     * Authenticate voter
     */
    public boolean authenticateVoter(String voterId, String password) {
        return voters.containsKey(voterId) && 
               voters.get(voterId).equals(password);
    }
    
    /**
     * Check if voter has already voted
     */
    public boolean hasVoted(String voterId) {
        return blockchainService.hasVoted(voterId);
    }
    
    /**
     * Cast vote for a candidate
     */
    public boolean castVote(String voterId, String candidateName) {
        try {
            blockchainService.castVote(voterId, candidateName);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
    
    /**
     * Get election results
     */
    public Map<String, Integer> getElectionResults() {
        return blockchainService.getElectionResults();
    }
    
    /**
     * Get total votes
     */
    public int getTotalVotes() {
        return blockchainService.getTotalVotes();
    }
    
    /**
     * Register a new voter (for admin use)
     */
    public void registerVoter(String voterId, String password) {
        voters.put(voterId, password);
        FileStorage.saveVoters(voters);
    }
}
