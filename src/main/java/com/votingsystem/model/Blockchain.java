package com.votingsystem.model;

import com.votingsystem.security.DigitalSignatureUtil;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class Blockchain implements Serializable {

    private static final long serialVersionUID = 2L;

    private List<Block> chain;
    private Map<String, Boolean> votedVoters;

    public Blockchain() {
        chain = new ArrayList<>();
        votedVoters = new HashMap<>();
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        Block genesis = new Block(0);
        chain.add(genesis);
    }

    public Block addVote(String voterId,
                         String candidateName,
                         java.security.PrivateKey privateKey,
                         java.security.PublicKey publicKey) {

        if (hasVoted(voterId)) {
            throw new IllegalStateException("Voter already voted!");
        }

        Block previous = getLatestBlock();

        Block newBlock = new Block(
                chain.size(),
                voterId,
                candidateName,
                previous.getCurrentHash(),
                privateKey,
                publicKey
        );

        chain.add(newBlock);
        votedVoters.put(voterId, true);

        return newBlock;
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public boolean hasVoted(String voterId) {
        return votedVoters.getOrDefault(voterId, false);
    }

    public boolean isChainValid() {

        for (int i = 1; i < chain.size(); i++) {

            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.isValidHash()) return false;

            String target = new String(new char[Block.getDifficulty()]).replace('\0', '0');

            if (!current.getCurrentHash().substring(0, Block.getDifficulty()).equals(target))
                return false;



            if (!current.getPreviousHash().equals(previous.getCurrentHash()))
                return false;

            try {
                byte[] keyBytes = Base64.getDecoder().decode(current.getPublicKey());
                KeyFactory factory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = factory.generatePublic(
                        new X509EncodedKeySpec(keyBytes));

                String data = current.getIndex()
                        + current.getVoterId()
                        + current.getCandidateName()
                        + current.getTimestamp()
                        + current.getPreviousHash();

                if (!DigitalSignatureUtil.verifySignature(
                        data,
                        current.getDigitalSignature(),
                        publicKey))
                    return false;

            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Integer> getElectionResults() {

        Map<String, Integer> results = new HashMap<>();

        for (int i = 1; i < chain.size(); i++) {
            Block block = chain.get(i);
            results.put(block.getCandidateName(),
                    results.getOrDefault(block.getCandidateName(), 0) + 1);
        }

        return results;
    }

    public int getTotalVotes() {
        return chain.size() - 1;
    }

    public List<Block> getChain() {
        return chain;
    }

    public void setVotedVoters(Map<String, Boolean> votedVoters) {
        this.votedVoters = votedVoters;
    }
}