package com.votingsystem.api;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import static com.mongodb.client.model.Sorts.ascending;
import static spark.Spark.*;

import java.security.MessageDigest;
import java.util.*;

/**
 * VotingApiServer — 100% MongoDB Atlas, zero local files.
 *
 * All votes, blocks, and voters live in MongoDB Atlas only.
 * blockchain.dat and voters.dat are never read or written.
 * The server can be restarted / redeployed on Railway/Render
 * unlimited times without losing any data.
 *
 * MongoDB collections used:
 *   votingdb.blocks   — blockchain (one document per block)
 *   votingdb.voters   — voter credentials + hasVoted flag
 */
public class VotingApiServer {

    // ── MongoDB ───────────────────────────────────────────────────────────
    private static final String MONGO_URI =
            "mongodb+srv://kunaltailor5555_db_user:T69jfdh47D0lr3Ws@cluster0.hhhinv6.mongodb.net/?appName=Cluster0";

    private static MongoCollection<Document> blocksCol;
    private static MongoVoterService         mongoVoterService;
    private static BlockchainMongoService    blockchainMongoService;

    // Proof-of-work difficulty (must match what was used when blocks were mined)
    private static final int    DIFFICULTY = 3;
    private static final String TARGET     = "0".repeat(DIFFICULTY);

    // ── Hardcoded genesis values — MUST stay the same forever ────────────
    // These match Block.java's genesis constructor (timestamp hardcoded to
    // "2024-01-01 00:00:00" so the hash never changes across restarts).
    private static final String GENESIS_VOTER      = "SYSTEM";
    private static final String GENESIS_CANDIDATE  = "GENESIS";
    private static final String GENESIS_PREV_HASH  = "0";
    private static final String GENESIS_TIMESTAMP  = "2024-01-01 00:00:00";

    // ─────────────────────────────────────────────────────────────────────

    public static void startServer() {

        // Connect to MongoDB
        MongoClient   client   = MongoClients.create(MONGO_URI);
        MongoDatabase database = client.getDatabase("votingdb");
        blocksCol              = database.getCollection("blocks");

        mongoVoterService      = new MongoVoterService(MONGO_URI);
        blockchainMongoService = new BlockchainMongoService(MONGO_URI);

        // Ensure genesis block exists in MongoDB (first-ever startup only)
        ensureGenesisBlock();

        // ── CORS ──────────────────────────────────────────────────────────
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin",  "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
            res.header("Access-Control-Max-Age",       "3600");
            res.type("application/json");
            if (req.requestMethod().equalsIgnoreCase("OPTIONS")) halt(200, "OK");
        });

        // ════════════════════════════════════════════════════════════════
        //  VOTER LOGIN
        // ════════════════════════════════════════════════════════════════
        post("/api/voter/login", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String voterId  = body.getOrDefault("voterId",  "").trim().toUpperCase();
                String password = body.getOrDefault("password", "").trim();

                if (voterId.isEmpty() || password.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("Voter ID and password are required");
                }

                Document voter = mongoVoterService.authenticate(voterId, password);
                if (voter == null) {
                    res.status(401);
                    return JsonUtil.error("Invalid Voter ID or Password");
                }

                return JsonUtil.obj(
                        "success",  true,
                        "voterId",  voterId,
                        "name",     voter.getString("name"),
                        "hasVoted", mongoVoterService.hasVoted(voterId)
                );
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  CAST VOTE — mines block entirely in-memory, saves to MongoDB only
        // ════════════════════════════════════════════════════════════════
        post("/api/voter/cast-vote", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String voterId       = body.getOrDefault("voterId",       "").trim().toUpperCase();
                String candidateName = body.getOrDefault("candidateName", "").trim();

                if (voterId.isEmpty() || candidateName.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("voterId and candidateName are required");
                }

                // MongoDB is the single source of truth for duplicate check
                if (mongoVoterService.hasVoted(voterId)) {
                    res.status(400);
                    return JsonUtil.error("You have already cast your vote");
                }

                // Get the latest block from MongoDB to find previousHash and next index
                Document latest = getLatestBlockFromMongo();
                if (latest == null) {
                    res.status(500);
                    return JsonUtil.error("Blockchain not initialised — genesis block missing");
                }

                int    nextIndex    = latest.getInteger("index") + 1;
                String previousHash = latest.getString("currentHash");
                String timestamp    = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date());

                // Mine the block (proof of work) — purely in-memory, no local file
                int  nonce     = 0;
                long startTime = System.currentTimeMillis();
                String currentHash;
                while (true) {
                    currentHash = sha256(nextIndex + voterId + candidateName
                            + timestamp + previousHash + nonce);
                    if (currentHash.startsWith(TARGET)) break;
                    nonce++;
                }
                long miningTime = System.currentTimeMillis() - startTime;

                // Save the newly mined block directly to MongoDB
                blockchainMongoService.saveBlock(
                        nextIndex, voterId, candidateName,
                        timestamp, previousHash, currentHash,
                        nonce, miningTime
                );

                // Mark voter as voted in MongoDB
                mongoVoterService.markVoted(voterId);

                System.out.println("✅ Block #" + nextIndex + " mined in "
                        + miningTime + "ms → saved to MongoDB");

                return JsonUtil.obj(
                        "success",      true,
                        "blockIndex",   nextIndex,
                        "currentHash",  currentHash,
                        "previousHash", previousHash,
                        "miningTime",   miningTime,
                        "nonce",        nonce,
                        "timestamp",    timestamp
                );

            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  VOTER STATUS
        // ════════════════════════════════════════════════════════════════
        get("/api/voter/status", (req, res) -> {
            String voterId = req.queryParams("voterId");
            if (voterId == null || voterId.isEmpty()) {
                res.status(400);
                return JsonUtil.error("voterId is required");
            }
            boolean hasVoted = mongoVoterService.hasVoted(voterId.toUpperCase());
            return JsonUtil.obj("voterId", voterId.toUpperCase(), "hasVoted", hasVoted);
        });

        // ════════════════════════════════════════════════════════════════
        //  PUBLIC RESULTS — counts votes directly from MongoDB blocks
        // ════════════════════════════════════════════════════════════════
        get("/api/results", (req, res) -> {
            List<Document> allBlocks = blockchainMongoService.getAllBlocks();

            Map<String, Integer> results = new LinkedHashMap<>();
            int totalVotes = 0;

            for (Document b : allBlocks) {
                if (b.getInteger("index") == 0) continue; // skip genesis
                String candidate = b.getString("candidateName");
                results.put(candidate, results.getOrDefault(candidate, 0) + 1);
                totalVotes++;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"totalVotes\":").append(totalVotes).append(",\"results\":{");
            boolean first = true;
            for (Map.Entry<String, Integer> e : results.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(esc(e.getKey())).append("\":").append(e.getValue());
                first = false;
            }
            return sb.append("}}").toString();
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN LOGIN
        // ════════════════════════════════════════════════════════════════
        post("/api/admin/login", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String username = body.getOrDefault("username", "");
                String password = body.getOrDefault("password", "");
                if ("admin".equals(username) && "admin123".equals(password)) {
                    return JsonUtil.obj("success", true, "role", "admin");
                }
                res.status(401);
                return JsonUtil.error("Invalid admin credentials");
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN — BLOCKCHAIN  (reads directly from MongoDB)
        // ════════════════════════════════════════════════════════════════
        get("/api/admin/blockchain", (req, res) -> {
            try {
                List<Document> allBlocks = blockchainMongoService.getAllBlocks();

                // Inline chain validation — no Java Blockchain object needed
                boolean isValid = true;
                for (int i = 1; i < allBlocks.size(); i++) {
                    Document cur  = allBlocks.get(i);
                    Document prev = allBlocks.get(i - 1);
                    String recomputed = sha256(
                            cur.getInteger("index")
                                    + cur.getString("voterId")
                                    + cur.getString("candidateName")
                                    + cur.getString("timestamp")
                                    + cur.getString("previousHash")
                                    + cur.getInteger("nonce")
                    );
                    boolean hashOk = recomputed.equals(cur.getString("currentHash"));
                    boolean linkOk = cur.getString("previousHash")
                            .equals(prev.getString("currentHash"));
                    boolean powOk  = cur.getString("currentHash").startsWith(TARGET);
                    if (!hashOk || !linkOk || !powOk) { isValid = false; break; }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{\"isValid\":").append(isValid)
                        .append(",\"totalBlocks\":").append(allBlocks.size())
                        .append(",\"blocks\":[");

                for (int i = 0; i < allBlocks.size(); i++) {
                    Document b = allBlocks.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                            .append("\"index\":").append(b.getInteger("index")).append(",")
                            .append("\"voterId\":\"").append(esc(b.getString("voterId"))).append("\",")
                            .append("\"candidateName\":\"").append(esc(b.getString("candidateName"))).append("\",")
                            .append("\"timestamp\":\"").append(esc(b.getString("timestamp"))).append("\",")
                            .append("\"previousHash\":\"").append(esc(b.getString("previousHash"))).append("\",")
                            .append("\"currentHash\":\"").append(esc(b.getString("currentHash"))).append("\",")
                            .append("\"nonce\":").append(b.getInteger("nonce")).append(",")
                            .append("\"miningTime\":").append(toLong(b, "miningTime"))
                            .append("}");
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN — VOTERS  (from MongoDB)
        // ════════════════════════════════════════════════════════════════
        get("/api/admin/voters", (req, res) -> {
            try {
                List<Document> voters = mongoVoterService.getAllVoters();
                StringBuilder sb = new StringBuilder();
                sb.append("{\"voters\":[");
                boolean first = true;
                for (Document v : voters) {
                    if (!first) sb.append(",");
                    sb.append("{")
                            .append("\"voterId\":\"").append(esc(v.getString("voterId"))).append("\",")
                            .append("\"name\":\"").append(esc(v.getString("name"))).append("\",")
                            .append("\"hasVoted\":").append(v.getBoolean("hasVoted", false)).append(",")
                            .append("\"votedFor\":\"").append(esc(v.getString("votedFor"))).append("\"")
                            .append("}");
                    first = false;
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN — REGISTER VOTER  (saves to MongoDB)
        // ════════════════════════════════════════════════════════════════
        post("/api/admin/register-voter", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String voterId  = body.getOrDefault("voterId",  "").trim().toUpperCase();
                String name     = body.getOrDefault("name",     "").trim();
                String password = body.getOrDefault("password", "").trim();
                if (voterId.isEmpty() || password.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("voterId and password are required");
                }
                boolean ok = mongoVoterService.registerVoter(voterId, name, password);
                if (!ok) {
                    res.status(400);
                    return JsonUtil.error("Voter ID already exists");
                }
                return JsonUtil.obj("success", true, "voterId", voterId);
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN — GETBLOCK test
        // ════════════════════════════════════════════════════════════════
        get("/api/admin/getblock", (req, res) -> {
            try {
                String rpcUrl = System.getenv("GETBLOCK_RPC_URL") != null
                        ? System.getenv("GETBLOCK_RPC_URL")
                        : "https://go.getblock.io/YOUR_KEY_HERE";
                com.votingsystem.integration.GetBlockClient gbClient =
                        new com.votingsystem.integration.GetBlockClient(rpcUrl);
                long blockNum = gbClient.getLatestBlockNumber();
                return JsonUtil.obj("reachable", blockNum >= 0, "blockNumber", blockNum);
            } catch (Exception e) {
                return JsonUtil.obj("reachable", false, "blockNumber", -1);
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN — MONGO BLOCKCHAIN (direct passthrough)
        // ════════════════════════════════════════════════════════════════
        get("/api/admin/mongo-blockchain", (req, res) -> {
            try {
                List<Document> mongoBlocks = blockchainMongoService.getAllBlocks();
                StringBuilder sb = new StringBuilder();
                sb.append("{\"blocks\":[");
                for (int i = 0; i < mongoBlocks.size(); i++) {
                    Document b = mongoBlocks.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                            .append("\"index\":").append(b.getInteger("index")).append(",")
                            .append("\"voterId\":\"").append(esc(b.getString("voterId"))).append("\",")
                            .append("\"candidateName\":\"").append(esc(b.getString("candidateName"))).append("\",")
                            .append("\"timestamp\":\"").append(esc(b.getString("timestamp"))).append("\",")
                            .append("\"previousHash\":\"").append(esc(b.getString("previousHash"))).append("\",")
                            .append("\"currentHash\":\"").append(esc(b.getString("currentHash"))).append("\",")
                            .append("\"nonce\":").append(b.getInteger("nonce")).append(",")
                            .append("\"miningTime\":").append(toLong(b, "miningTime"))
                            .append("}");
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN — VALIDATE MONGO CHAIN
        // ════════════════════════════════════════════════════════════════
        get("/api/admin/validate-mongo", (req, res) -> {
            try {
                BlockchainMongoService.ValidationResult result =
                        blockchainMongoService.validateChain();
                return result.toJson();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        System.out.println("✅ Voting API Server started — MongoDB-only mode");
        System.out.println("   All data lives in MongoDB Atlas. No local files used.");
    }

    // ════════════════════════════════════════════════════════════════
    //  Ensure genesis block exists (runs ONLY on first-ever deployment)
    //  After that, MongoDB already has the block and this is a no-op.
    // ════════════════════════════════════════════════════════════════
    private static void ensureGenesisBlock() {
        boolean exists = blocksCol.find(new Document("index", 0)).first() != null;
        if (exists) {
            long total = blocksCol.countDocuments();
            System.out.println("✅ MongoDB: " + total + " block(s) found — data intact.");
            return;
        }

        System.out.println("⚙️  First startup: mining genesis block...");
        int  nonce     = 0;
        long startTime = System.currentTimeMillis();
        String genesisHash;
        while (true) {
            genesisHash = sha256(0 + GENESIS_VOTER + GENESIS_CANDIDATE
                    + GENESIS_TIMESTAMP + GENESIS_PREV_HASH + nonce);
            if (genesisHash.startsWith(TARGET)) break;
            nonce++;
        }
        long miningTime = System.currentTimeMillis() - startTime;

        blocksCol.insertOne(new Document("index",        0)
                .append("voterId",       GENESIS_VOTER)
                .append("candidateName", GENESIS_CANDIDATE)
                .append("timestamp",     GENESIS_TIMESTAMP)
                .append("previousHash",  GENESIS_PREV_HASH)
                .append("currentHash",   genesisHash)
                .append("nonce",         nonce)
                .append("miningTime",    miningTime));

        System.out.println("✅ Genesis block saved to MongoDB. Hash: " + genesisHash);
    }

    // Get the block with the highest index from MongoDB
    private static Document getLatestBlockFromMongo() {
        List<Document> all = blockchainMongoService.getAllBlocks();
        if (all.isEmpty()) return null;
        return all.get(all.size() - 1);
    }

    // SHA-256 — identical to HashUtil.calculateSHA256()
    private static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    // MongoDB stores miningTime as Long or Integer depending on driver version
    private static long toLong(Document doc, String key) {
        Object val = doc.get(key);
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        return 0L;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}