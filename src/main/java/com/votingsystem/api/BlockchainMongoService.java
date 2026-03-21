package com.votingsystem.api;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Sorts.ascending;

/**
 * BlockchainMongoService
 *
 * Saves every mined block to MongoDB Atlas so the full chain
 * is accessible from ANY device — not just the machine that
 * has blockchain.dat
 *
 * Also validates the chain stored in MongoDB by checking:
 *   1. previousHash matches previous block's currentHash  (linkValid)
 *   2. currentHash starts with "000"                      (difficultyValid)
 *   3. Recomputed SHA-256 matches stored hash             (hashValid)
 *
 * Collection: votingdb.blocks
 */
public class BlockchainMongoService {

    private static final int    DIFFICULTY = 3;
    private static final String TARGET     = "0".repeat(DIFFICULTY);

    private final MongoCollection<Document> blocks;

    public BlockchainMongoService(String mongoUri) {
        MongoClient   client   = MongoClients.create(mongoUri);
        MongoDatabase database = client.getDatabase("votingdb");
        this.blocks = database.getCollection("blocks");
        System.out.println("✅ MongoDB blockchain collection ready");
    }

    // ── Save a block (skips duplicates safely) ────────────────────────────
    public void saveBlock(int index, String voterId, String candidateName,
                          String timestamp, String previousHash,
                          String currentHash, int nonce, long miningTime) {

        if (blocks.find(new Document("index", index)).first() != null) return;

        blocks.insertOne(new Document("index",        index)
                .append("voterId",       voterId)
                .append("candidateName", candidateName)
                .append("timestamp",     timestamp)
                .append("previousHash",  previousHash)
                .append("currentHash",   currentHash)
                .append("nonce",         nonce)
                .append("miningTime",    miningTime));

        System.out.println("✅ Block #" + index + " saved to MongoDB");
    }

    // ── Clear all blocks then re-insert from the given list ───────────────
    // Call this on server startup so MongoDB always matches blockchain.dat.
    // Without this, a deleted/recreated blockchain.dat produces a new genesis
    // hash while MongoDB still holds the old one → TAMPERED false positive.
    public void clearAndResync(java.util.List<com.votingsystem.model.Block> chain) {
        blocks.deleteMany(new Document()); // wipe all existing blocks
        for (com.votingsystem.model.Block b : chain) {
            blocks.insertOne(new Document("index",        b.getIndex())
                    .append("voterId",       b.getVoterId())
                    .append("candidateName", b.getCandidateName())
                    .append("timestamp",     b.getTimestamp())
                    .append("previousHash",  b.getPreviousHash())
                    .append("currentHash",   b.getCurrentHash())
                    .append("nonce",         b.getNonce())
                    .append("miningTime",    b.getMiningTime()));
        }
        System.out.println("✅ MongoDB blocks collection cleared and re-synced (" + chain.size() + " blocks)");
    }

    // ── Get all blocks sorted by index ────────────────────────────────────
    public List<Document> getAllBlocks() {
        List<Document> list = new ArrayList<>();
        blocks.find().sort(ascending("index")).into(list);
        return list;
    }

    // ── Validate entire chain in MongoDB ──────────────────────────────────
    public ValidationResult validateChain() {
        List<Document> chain = getAllBlocks();
        ValidationResult result = new ValidationResult();
        result.totalBlocks = chain.size();
        result.isValid     = true;

        for (int i = 1; i < chain.size(); i++) {
            Document cur  = chain.get(i);
            Document prev = chain.get(i - 1);

            BlockValidation bv = new BlockValidation();
            bv.index         = cur.getInteger("index");
            bv.currentHash   = cur.getString("currentHash");
            bv.previousHash  = cur.getString("previousHash");
            bv.prevBlockHash = prev.getString("currentHash");

            // Check 1 — hash link
            bv.linkValid = bv.previousHash.equals(bv.prevBlockHash);

            // Check 2 — proof of work
            bv.difficultyValid = bv.currentHash != null && bv.currentHash.startsWith(TARGET);

            // Check 3 — recompute SHA-256 (must match Block.calculateHash())
            String recomputed = recomputeHash(cur);
            bv.hashValid = recomputed.equals(bv.currentHash);

            bv.blockValid = bv.linkValid && bv.difficultyValid && bv.hashValid;
            if (!bv.blockValid) result.isValid = false;

            result.blockValidations.add(bv);
        }
        return result;
    }

    // ── Recompute SHA-256 exactly as Block.calculateHash() does ──────────
    // Block.calculateHash():
    //   String data = index + voterId + candidateName + timestamp + previousHash + nonce;
    //   return HashUtil.calculateSHA256(data);
    private String recomputeHash(Document b) {
        try {
            String data = b.getInteger("index")
                    + b.getString("voterId")
                    + b.getString("candidateName")
                    + b.getString("timestamp")
                    + b.getString("previousHash")
                    + b.getInteger("nonce");

            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    data.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte by : hash) {
                String h = Integer.toHexString(0xff & by);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return "error";
        }
    }

    // ── Validation result ─────────────────────────────────────────────────
    public static class ValidationResult {
        public boolean               isValid;
        public int                   totalBlocks;
        public List<BlockValidation> blockValidations = new ArrayList<>();

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"isValid\":").append(isValid)
              .append(",\"totalBlocks\":").append(totalBlocks)
              .append(",\"blockValidations\":[");
            for (int i = 0; i < blockValidations.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(blockValidations.get(i).toJson());
            }
            return sb.append("]}").toString();
        }
    }

    public static class BlockValidation {
        public int     index;
        public String  currentHash;
        public String  previousHash;
        public String  prevBlockHash;
        public boolean linkValid;
        public boolean difficultyValid;
        public boolean hashValid;
        public boolean blockValid;

        public String toJson() {
            return "{"
                + "\"index\":"           + index                  + ","
                + "\"currentHash\":\""   + esc(currentHash)       + "\","
                + "\"previousHash\":\""  + esc(previousHash)      + "\","
                + "\"prevBlockHash\":\"" + esc(prevBlockHash)     + "\","
                + "\"linkValid\":"       + linkValid               + ","
                + "\"difficultyValid\":" + difficultyValid         + ","
                + "\"hashValid\":"       + hashValid               + ","
                + "\"blockValid\":"      + blockValid
                + "}";
        }

        private String esc(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}