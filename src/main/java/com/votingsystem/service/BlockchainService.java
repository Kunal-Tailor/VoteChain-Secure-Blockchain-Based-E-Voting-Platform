package com.votingsystem.service;

import com.votingsystem.model.Block;
import com.votingsystem.model.Blockchain;
import com.votingsystem.security.DigitalSignatureUtil;
import com.votingsystem.storage.FileStorage;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;

public class BlockchainService {

    private Blockchain blockchain;

    public BlockchainService() {
        this.blockchain = FileStorage.loadBlockchain();
    }

    public Block castVote(String voterId, String candidateName) {

        KeyPair keyPair = DigitalSignatureUtil.generateKeyPair();

        Block block = blockchain.addVote(
                voterId,
                candidateName,
                keyPair.getPrivate(),
                keyPair.getPublic()
        );

        FileStorage.saveBlockchain(blockchain);
        return block;
    }

    public boolean hasVoted(String voterId) {
        return blockchain.hasVoted(voterId);
    }

    public boolean validateBlockchain() {
        return blockchain.isChainValid();
    }

    public Map<String, Integer> getElectionResults() {
        return blockchain.getElectionResults();
    }

    public int getTotalVotes() {
        return blockchain.getTotalVotes();
    }

    public List<Block> getAllBlocks() {
        return blockchain.getChain();
    }

}

