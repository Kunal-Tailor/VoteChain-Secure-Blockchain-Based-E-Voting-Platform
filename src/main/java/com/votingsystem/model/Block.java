package com.votingsystem.model;

import com.votingsystem.security.DigitalSignatureUtil;
import com.votingsystem.security.HashUtil;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Block implements Serializable {

    private static final long serialVersionUID = 4L;

    private int index;
    private String voterId;
    private String candidateName;
    private String timestamp;
    private String previousHash;
    private String currentHash;

    private int nonce;
    private long miningTime; // ⏱ NEW
    private static int difficulty = 3; // 🔥 Dynamic difficulty

    private String digitalSignature;
    private String publicKey;

    public static void setDifficulty(int diff) {
        difficulty = diff;
    }

    public static int getDifficulty() {
        return difficulty;
    }

    // GENESIS
    public Block(int index) {
        this.index = index;
        this.voterId = "SYSTEM";
        this.candidateName = "GENESIS";
        this.previousHash = "0";
        this.nonce = 0;

        // ⚠️ FIXED: hardcoded timestamp so genesis hash is ALWAYS the same.
        // If you use LocalDateTime.now() here, every restart produces a new
        // genesis hash, which breaks the previousHash link for Block #1 stored
        // in MongoDB → "TAMPERED" even on a fresh chain.
        this.timestamp = "2024-01-01 00:00:00";

        mineBlock();
    }

    // NORMAL BLOCK
    public Block(int index,
                 String voterId,
                 String candidateName,
                 String previousHash,
                 PrivateKey privateKey,
                 PublicKey publicKey) {

        this.index = index;
        this.voterId = voterId;
        this.candidateName = candidateName;
        this.previousHash = previousHash;

        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String dataToSign = index + voterId + candidateName + timestamp + previousHash;
        this.digitalSignature = DigitalSignatureUtil.signData(dataToSign, privateKey);
        this.publicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        mineBlock();
    }

    public String calculateHash() {
        String data = index + voterId + candidateName + timestamp + previousHash + nonce;
        return HashUtil.calculateSHA256(data);
    }

    private void mineBlock() {

        long startTime = System.currentTimeMillis();

        String target = new String(new char[difficulty]).replace('\0', '0');

        while (true) {
            currentHash = calculateHash();
            if (currentHash.substring(0, difficulty).equals(target)) break;
            nonce++;
        }

        miningTime = System.currentTimeMillis() - startTime;

        System.out.println("Block Mined! Hash: " + currentHash);
    }

    public boolean isValidHash() {
        return currentHash.equals(calculateHash());
    }

    // GETTERS
    public int getIndex() { return index; }
    public String getVoterId() { return voterId; }
    public String getCandidateName() { return candidateName; }
    public String getTimestamp() { return timestamp; }
    public String getPreviousHash() { return previousHash; }
    public String getCurrentHash() { return currentHash; }
    public int getNonce() { return nonce; }
    public long getMiningTime() { return miningTime; }
    public String getDigitalSignature() { return digitalSignature; }
    public String getPublicKey() { return publicKey; }
}