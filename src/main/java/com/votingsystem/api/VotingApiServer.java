package com.votingsystem.api;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
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

    // ── Configuration keys ────────────────────────────────────────────────
    private static final String ENV_MONGO_URI      = "MONGO_URI";
    private static final String ENV_ADMIN_USER     = "ADMIN_USERNAME";
    private static final String ENV_ADMIN_PASSWORD = "ADMIN_PASSWORD";
    private static final String ENV_AUTO_SEED      = "AUTO_SEED";
    private static final String DEFAULT_ADMIN_USER = "admin";
    private static final String DEFAULT_ADMIN_PW   = "admin123";

    // ── MongoDB ───────────────────────────────────────────────────────────
    /**
     * Mongo connection string is loaded from environment variable MONGO_URI.
     * This allows different URIs per environment (dev/stage/prod) without
     * changing or recompiling the code.
     */
    private static final String MONGO_URI =
            Optional.ofNullable(System.getenv(ENV_MONGO_URI))
                    .filter(s -> !s.isBlank())
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing Mongo configuration: please set environment variable " + ENV_MONGO_URI));

    private static MongoCollection<Document> blocksCol;
    private static MongoVoterService         mongoVoterService;
    private static BlockchainMongoService    blockchainMongoService;
    private static ElectionMongoService      electionMongoService;
    private static ConstituencyMongoService  constituencyMongoService;

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

        // Basic config diagnostics (without printing secrets)
        System.out.println("🔧 Using Mongo URI from env: " + ENV_MONGO_URI);
        String adminUser = Optional.ofNullable(System.getenv(ENV_ADMIN_USER)).orElse(DEFAULT_ADMIN_USER);
        System.out.println("🔧 Admin username: " + adminUser + " (set " + ENV_ADMIN_USER + " to override)");

        // Connect to MongoDB
        MongoClient   client   = MongoClients.create(MONGO_URI);
        MongoDatabase database = client.getDatabase("votingdb");
        blocksCol              = database.getCollection("blocks");

        mongoVoterService       = new MongoVoterService(MONGO_URI);
        blockchainMongoService  = new BlockchainMongoService(MONGO_URI);
        electionMongoService    = new ElectionMongoService(MONGO_URI);
        constituencyMongoService = new ConstituencyMongoService(MONGO_URI);

        // Ensure genesis block exists in MongoDB (first-ever startup only)
        ensureGenesisBlock();

        // Optional: auto-seed Maharashtra data if DB is empty
        boolean autoSeed = "true".equalsIgnoreCase(System.getenv(ENV_AUTO_SEED));
        if (autoSeed) {
            boolean noElections = electionMongoService.isEmpty();
            boolean noConstituencies = constituencyMongoService.countAll() == 0;
            if (noElections || noConstituencies) {
                System.out.println("🌱 AUTO_SEED enabled — seeding Maharashtra data...");
                MaharashtraDataSeeder.seed(electionMongoService, constituencyMongoService, mongoVoterService);
            } else {
                System.out.println("ℹ️  AUTO_SEED enabled — existing data detected, skipping seed");
            }
        }

        // ── CORS ──────────────────────────────────────────────────────────
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin",  "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
            res.header("Access-Control-Max-Age",       "3600");
            res.type("application/json");
            if (req.requestMethod().equalsIgnoreCase("OPTIONS")) halt(200, "OK");
        });

        // Return JSON for unknown routes / server errors (avoid HTML responses)
        notFound((req, res) -> {
            res.type("application/json");
            res.status(404);
            return JsonUtil.error("Route not found");
        });
        internalServerError((req, res) -> {
            res.type("application/json");
            return JsonUtil.error("Internal server error");
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

                // Return enhanced voter info with constituency details
                Document voterInfo = mongoVoterService.getVoterInfo(voterId);
                StringBuilder sb = new StringBuilder();
                sb.append("{")
                  .append("\"success\":true,")
                  .append("\"voterId\":\"").append(esc(voterId)).append("\",")
                  .append("\"name\":\"").append(esc(voter.getString("name"))).append("\",")
                  .append("\"district\":\"").append(esc(voterInfo != null ? voterInfo.getString("district") : "")).append("\",")
                  .append("\"division\":\"").append(esc(voterInfo != null ? voterInfo.getString("division") : "")).append("\",")
                  .append("\"constituencyVS\":\"").append(esc(voterInfo != null ? voterInfo.getString("constituencyVS") : "")).append("\",")
                  .append("\"constituencyLS\":\"").append(esc(voterInfo != null ? voterInfo.getString("constituencyLS") : "")).append("\",")
                  .append("\"hasVoted\":").append(mongoVoterService.hasVoted(voterId))
                  .append("}");
                return sb.toString();
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

                String expectedUser = Optional.ofNullable(System.getenv(ENV_ADMIN_USER))
                        .filter(s -> !s.isBlank())
                        .orElse(DEFAULT_ADMIN_USER);
                String expectedPw = Optional.ofNullable(System.getenv(ENV_ADMIN_PASSWORD))
                        .filter(s -> !s.isBlank())
                        .orElse(DEFAULT_ADMIN_PW);

                if (expectedUser.equals(username) && expectedPw.equals(password)) {
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
                            .append("\"district\":\"").append(esc(v.getString("district"))).append("\",")
                            .append("\"division\":\"").append(esc(v.getString("division"))).append("\",")
                            .append("\"constituencyVS\":\"").append(esc(v.getString("constituencyVS"))).append("\",")
                            .append("\"constituencyLS\":\"").append(esc(v.getString("constituencyLS"))).append("\",")
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

        // ════════════════════════════════════════════════════════════════
        //  MAHARASHTRA — SEED ALL DATA
        // ════════════════════════════════════════════════════════════════
        post("/api/admin/seed-maharashtra", (req, res) -> {
            try {
                MaharashtraDataSeeder.seed(
                    electionMongoService, constituencyMongoService, mongoVoterService
                );
                return JsonUtil.obj("success", true, "message",
                    "Maharashtra data seeded: 288 VS + 48 LS constituencies + 30 demo voters");
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ELECTIONS API
        // ════════════════════════════════════════════════════════════════
        get("/api/elections", (req, res) -> {
            try {
                List<Document> allElections = electionMongoService.getAllElections();
                StringBuilder sb = new StringBuilder();
                sb.append("{\"elections\":[");
                for (int i = 0; i < allElections.size(); i++) {
                    Document e = allElections.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                      .append("\"electionId\":\"").append(esc(e.getString("electionId"))).append("\",")
                      .append("\"type\":\"").append(esc(e.getString("type"))).append("\",")
                      .append("\"title\":\"").append(esc(e.getString("title"))).append("\",")
                      .append("\"totalSeats\":").append(e.getInteger("totalSeats", 0)).append(",")
                      .append("\"status\":\"").append(esc(e.getString("status"))).append("\",")
                      .append("\"startDate\":\"").append(esc(e.getString("startDate"))).append("\",")
                      .append("\"endDate\":\"").append(esc(e.getString("endDate"))).append("\"")
                      .append("}");
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  CONSTITUENCIES API
        // ════════════════════════════════════════════════════════════════
        get("/api/constituencies", (req, res) -> {
            try {
                String electionId = req.queryParams("electionId");
                String district = req.queryParams("district");
                String division = req.queryParams("division");

                List<Document> list;
                if (electionId == null || electionId.isEmpty()) {
                    return JsonUtil.error("electionId is required");
                }
                if (district != null && !district.isEmpty()) {
                    list = constituencyMongoService.getByDistrict(electionId, district);
                } else if (division != null && !division.isEmpty()) {
                    list = constituencyMongoService.getByDivision(electionId, division);
                } else {
                    list = constituencyMongoService.getByElection(electionId);
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{\"count\":").append(list.size()).append(",\"constituencies\":[");
                for (int i = 0; i < list.size(); i++) {
                    Document c = list.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                      .append("\"constituencyId\":\"").append(esc(c.getString("constituencyId"))).append("\",")
                      .append("\"name\":\"").append(esc(c.getString("name"))).append("\",")
                      .append("\"number\":").append(c.getInteger("number", 0)).append(",")
                      .append("\"district\":\"").append(esc(c.getString("district"))).append("\",")
                      .append("\"division\":\"").append(esc(c.getString("division"))).append("\",")
                      .append("\"reservationCategory\":\"").append(esc(c.getString("reservationCategory"))).append("\",");

                    // Serialize candidates array
                    @SuppressWarnings("unchecked")
                    List<Document> candidates = (List<Document>) c.get("candidates");
                    sb.append("\"candidates\":[");
                    if (candidates != null) {
                        for (int j = 0; j < candidates.size(); j++) {
                            Document cand = candidates.get(j);
                            if (j > 0) sb.append(",");
                            sb.append("{")
                              .append("\"candidateId\":\"").append(esc(cand.getString("candidateId"))).append("\",")
                              .append("\"name\":\"").append(esc(cand.getString("name"))).append("\",")
                              .append("\"party\":\"").append(esc(cand.getString("party"))).append("\",")
                              .append("\"partyFull\":\"").append(esc(cand.getString("partyFull"))).append("\",")
                              .append("\"symbol\":\"").append(esc(cand.getString("symbol"))).append("\",")
                              .append("\"color\":\"").append(esc(cand.getString("color"))).append("\"")
                              .append("}");
                        }
                    }
                    sb.append("]}");
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // Single constituency by ID
        get("/api/constituencies/:id", (req, res) -> {
            try {
                String id = req.params("id");
                Document c = constituencyMongoService.getById(id);
                if (c == null) { res.status(404); return JsonUtil.error("Constituency not found"); }

                StringBuilder sb = new StringBuilder();
                sb.append("{")
                  .append("\"constituencyId\":\"").append(esc(c.getString("constituencyId"))).append("\",")
                  .append("\"name\":\"").append(esc(c.getString("name"))).append("\",")
                  .append("\"number\":").append(c.getInteger("number", 0)).append(",")
                  .append("\"district\":\"").append(esc(c.getString("district"))).append("\",")
                  .append("\"division\":\"").append(esc(c.getString("division"))).append("\",")
                  .append("\"reservationCategory\":\"").append(esc(c.getString("reservationCategory"))).append("\",");

                @SuppressWarnings("unchecked")
                List<Document> candidates = (List<Document>) c.get("candidates");
                sb.append("\"candidates\":[");
                if (candidates != null) {
                    for (int j = 0; j < candidates.size(); j++) {
                        Document cand = candidates.get(j);
                        if (j > 0) sb.append(",");
                        sb.append("{")
                          .append("\"candidateId\":\"").append(esc(cand.getString("candidateId"))).append("\",")
                          .append("\"name\":\"").append(esc(cand.getString("name"))).append("\",")
                          .append("\"party\":\"").append(esc(cand.getString("party"))).append("\",")
                          .append("\"partyFull\":\"").append(esc(cand.getString("partyFull"))).append("\",")
                          .append("\"symbol\":\"").append(esc(cand.getString("symbol"))).append("\",")
                          .append("\"color\":\"").append(esc(cand.getString("color"))).append("\"")
                          .append("}");
                    }
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  DIVISIONS API — returns all 6 Maharashtra divisions
        // ════════════════════════════════════════════════════════════════
        get("/api/divisions", (req, res) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"divisions\":[");
            int i = 0;
            for (Map.Entry<String, String[]> entry : MaharashtraDataSeeder.DIVISIONS.entrySet()) {
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(esc(entry.getKey())).append("\",\"districts\":[");
                String[] districts = entry.getValue();
                for (int d = 0; d < districts.length; d++) {
                    if (d > 0) sb.append(",");
                    sb.append("\"").append(esc(districts[d])).append("\"");
                }
                sb.append("]}");
                i++;
            }
            return sb.append("]}").toString();
        });

        // ════════════════════════════════════════════════════════════════
        //  PARTIES API — returns all political parties with symbols
        // ════════════════════════════════════════════════════════════════
        get("/api/parties", (req, res) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"parties\":[");
            for (int i = 0; i < MaharashtraDataSeeder.PARTIES.length; i++) {
                String[] p = MaharashtraDataSeeder.PARTIES[i];
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"abbr\":\"").append(esc(p[0])).append("\",")
                  .append("\"name\":\"").append(esc(p[1])).append("\",")
                  .append("\"symbol\":\"").append(esc(p[2])).append("\",")
                  .append("\"color\":\"").append(esc(p[3])).append("\"")
                  .append("}");
            }
            return sb.append("]}").toString();
        });

        // ════════════════════════════════════════════════════════════════
        //  ENHANCED RESULTS — per election, constituency, division
        // ════════════════════════════════════════════════════════════════
        get("/api/results/election", (req, res) -> {
            try {
                String electionId = req.queryParams("electionId");
                String constituencyId = req.queryParams("constituencyId");
                String division = req.queryParams("division");
                String district = req.queryParams("district");

                List<Document> allBlocks = blockchainMongoService.getAllBlocks();
                Map<String, Integer> results = new LinkedHashMap<>();
                Map<String, Map<String, Integer>> partyResults = new LinkedHashMap<>();
                int totalVotes = 0;

                for (Document b : allBlocks) {
                    if (b.getInteger("index") == 0) continue;

                    // Filter by electionId if specified
                    if (electionId != null && !electionId.isEmpty()) {
                        String blockElection = b.getString("electionId");
                        if (blockElection == null || !blockElection.equals(electionId)) continue;
                    }

                    // Filter by constituencyId if specified
                    if (constituencyId != null && !constituencyId.isEmpty()) {
                        String blockConst = b.getString("constituencyId");
                        if (blockConst == null || !blockConst.equals(constituencyId)) continue;
                    }

                    String candidate = b.getString("candidateName");
                    String party = b.getString("party");
                    results.put(candidate, results.getOrDefault(candidate, 0) + 1);

                    if (party != null) {
                        partyResults.computeIfAbsent(party, k -> new LinkedHashMap<>());
                        partyResults.get(party).put(candidate,
                            partyResults.get(party).getOrDefault(candidate, 0) + 1);
                    }

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
                sb.append("},\"partyResults\":{");
                first = true;
                for (Map.Entry<String, Map<String, Integer>> pe : partyResults.entrySet()) {
                    if (!first) sb.append(",");
                    int partyTotal = pe.getValue().values().stream().mapToInt(Integer::intValue).sum();
                    sb.append("\"").append(esc(pe.getKey())).append("\":").append(partyTotal);
                    first = false;
                }
                return sb.append("}}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN — UPDATE ELECTION STATUS
        // ════════════════════════════════════════════════════════════════
        post("/api/admin/election-status", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String electionId = body.getOrDefault("electionId", "");
                String status = body.getOrDefault("status", "");
                if (electionId.isEmpty() || status.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("electionId and status are required");
                }
                boolean ok = electionMongoService.updateStatus(electionId, status);
                if (!ok) { res.status(404); return JsonUtil.error("Election not found"); }
                return JsonUtil.obj("success", true, "electionId", electionId, "status", status);
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  ENHANCED CAST VOTE — with election + constituency
        // ════════════════════════════════════════════════════════════════
        post("/api/voter/cast-vote-election", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String voterId       = body.getOrDefault("voterId",       "").trim().toUpperCase();
                String candidateName = body.getOrDefault("candidateName", "").trim();
                String candidateId   = body.getOrDefault("candidateId",   "").trim();
                String party         = body.getOrDefault("party",         "").trim();
                String electionId    = body.getOrDefault("electionId",    "").trim();
                String constituencyId= body.getOrDefault("constituencyId","").trim();

                if (voterId.isEmpty() || candidateName.isEmpty() || electionId.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("voterId, candidateName, and electionId are required");
                }

                // Check if already voted in this election
                if (mongoVoterService.hasVotedInElection(voterId, electionId)) {
                    res.status(400);
                    return JsonUtil.error("You have already voted in this election");
                }

                // Get latest block and mine new one
                Document latest = getLatestBlockFromMongo();
                if (latest == null) {
                    res.status(500);
                    return JsonUtil.error("Blockchain not initialised");
                }

                int nextIndex = latest.getInteger("index") + 1;
                String previousHash = latest.getString("currentHash");
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date());

                int nonce = 0;
                long startTime = System.currentTimeMillis();
                String currentHash;
                while (true) {
                    currentHash = sha256(nextIndex + voterId + candidateName
                            + timestamp + previousHash + nonce);
                    if (currentHash.startsWith(TARGET)) break;
                    nonce++;
                }
                long miningTime = System.currentTimeMillis() - startTime;

                // Save block with election context
                Document blockDoc = new Document("index", nextIndex)
                        .append("voterId", voterId)
                        .append("candidateName", candidateName)
                        .append("candidateId", candidateId)
                        .append("party", party)
                        .append("electionId", electionId)
                        .append("constituencyId", constituencyId)
                        .append("timestamp", timestamp)
                        .append("previousHash", previousHash)
                        .append("currentHash", currentHash)
                        .append("nonce", nonce)
                        .append("miningTime", miningTime);

                blocksCol.insertOne(blockDoc);

                // Mark voter as voted for this election
                mongoVoterService.markVotedForElection(voterId, electionId);

                System.out.println("✅ Block #" + nextIndex + " mined (" + electionId
                        + " / " + constituencyId + ") in " + miningTime + "ms");

                return JsonUtil.obj(
                    "success", true,
                    "blockIndex", nextIndex,
                    "currentHash", currentHash,
                    "previousHash", previousHash,
                    "miningTime", miningTime,
                    "nonce", nonce,
                    "timestamp", timestamp,
                    "electionId", electionId,
                    "constituencyId", constituencyId
                );
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  CONSTITUENCY-LEVEL RESULTS
        // ════════════════════════════════════════════════════════════════
        get("/api/results/constituency", (req, res) -> {
            try {
                String constituencyId = req.queryParams("constituencyId");
                String electionId = req.queryParams("electionId");
                if (constituencyId == null || constituencyId.isEmpty()) {
                    res.status(400); return JsonUtil.error("constituencyId required");
                }

                // Get constituency details
                Document constDoc = constituencyMongoService.getById(constituencyId);
                String constName = constDoc != null ? constDoc.getString("name") : constituencyId;

                List<Document> allBlocks = blockchainMongoService.getAllBlocks();
                Map<String, int[]> candidateVotes = new LinkedHashMap<>(); // name -> [votes]
                Map<String, String> candidateParty = new LinkedHashMap<>(); // name -> party
                int totalVotes = 0;

                for (Document b : allBlocks) {
                    if (b.getInteger("index") == 0) continue;
                    String bConst = b.getString("constituencyId");
                    if (bConst == null || !bConst.equals(constituencyId)) continue;
                    if (electionId != null && !electionId.isEmpty()) {
                        String bElection = b.getString("electionId");
                        if (bElection == null || !bElection.equals(electionId)) continue;
                    }
                    String cand = b.getString("candidateName");
                    String party = b.getString("party");
                    candidateVotes.computeIfAbsent(cand, k -> new int[]{0})[0]++;
                    if (party != null) candidateParty.put(cand, party);
                    totalVotes++;
                }

                // Sort by votes descending
                List<Map.Entry<String, int[]>> sorted = new ArrayList<>(candidateVotes.entrySet());
                sorted.sort((a, b2) -> b2.getValue()[0] - a.getValue()[0]);

                StringBuilder sb = new StringBuilder();
                sb.append("{\"constituencyId\":\"").append(esc(constituencyId)).append("\",")
                  .append("\"constituencyName\":\"").append(esc(constName)).append("\",")
                  .append("\"totalVotes\":").append(totalVotes).append(",")
                  .append("\"candidates\":[");
                for (int i = 0; i < sorted.size(); i++) {
                    if (i > 0) sb.append(",");
                    String name = sorted.get(i).getKey();
                    int votes = sorted.get(i).getValue()[0];
                    String party = candidateParty.getOrDefault(name, "IND");
                    sb.append("{\"name\":\"").append(esc(name)).append("\",")
                      .append("\"party\":\"").append(esc(party)).append("\",")
                      .append("\"votes\":").append(votes).append(",")
                      .append("\"percentage\":").append(totalVotes > 0 ? String.format("%.1f", votes * 100.0 / totalVotes) : "0")
                      .append("}");
                }
                sb.append("],");
                // Winner
                if (!sorted.isEmpty()) {
                    String winner = sorted.get(0).getKey();
                    int margin = sorted.size() > 1 ? sorted.get(0).getValue()[0] - sorted.get(1).getValue()[0] : sorted.get(0).getValue()[0];
                    sb.append("\"winner\":\"").append(esc(winner)).append("\",")
                      .append("\"winnerParty\":\"").append(esc(candidateParty.getOrDefault(winner, "IND"))).append("\",")
                      .append("\"margin\":").append(margin);
                } else {
                    sb.append("\"winner\":null,\"winnerParty\":null,\"margin\":0");
                }
                return sb.append("}").toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  SEAT TALLY — overall party-wise seat count
        // ════════════════════════════════════════════════════════════════
        get("/api/results/seats", (req, res) -> {
            try {
                String electionId = req.queryParams("electionId");
                if (electionId == null || electionId.isEmpty()) {
                    res.status(400); return JsonUtil.error("electionId required");
                }
                List<Document> constituencies = constituencyMongoService.getByElection(electionId);
                List<Document> allBlocks = blockchainMongoService.getAllBlocks();

                // Build vote count per constituency per candidate
                Map<String, Map<String, int[]>> constVotes = new LinkedHashMap<>();
                Map<String, String> candPartyMap = new LinkedHashMap<>();
                int totalVotesCast = 0;

                for (Document b : allBlocks) {
                    if (b.getInteger("index") == 0) continue;
                    String bElection = b.getString("electionId");
                    if (bElection == null || !bElection.equals(electionId)) continue;
                    String bConst = b.getString("constituencyId");
                    String cand = b.getString("candidateName");
                    String party = b.getString("party");
                    if (bConst == null || cand == null) continue;
                    constVotes.computeIfAbsent(bConst, k -> new LinkedHashMap<>())
                              .computeIfAbsent(cand, k -> new int[]{0})[0]++;
                    if (party != null) candPartyMap.put(cand, party);
                    totalVotesCast++;
                }

                // Count seats won by each party
                Map<String, Integer> seatTally = new LinkedHashMap<>();
                Map<String, Integer> partyVotes = new LinkedHashMap<>();
                int seatsDecided = 0;

                for (Map.Entry<String, Map<String, int[]>> entry : constVotes.entrySet()) {
                    String leadingCand = null; int maxVotes = 0;
                    for (Map.Entry<String, int[]> candEntry : entry.getValue().entrySet()) {
                        int v = candEntry.getValue()[0];
                        String party = candPartyMap.getOrDefault(candEntry.getKey(), "IND");
                        partyVotes.put(party, partyVotes.getOrDefault(party, 0) + v);
                        if (v > maxVotes) { maxVotes = v; leadingCand = candEntry.getKey(); }
                    }
                    if (leadingCand != null) {
                        String winParty = candPartyMap.getOrDefault(leadingCand, "IND");
                        seatTally.put(winParty, seatTally.getOrDefault(winParty, 0) + 1);
                        seatsDecided++;
                    }
                }

                // Sort by seats descending
                List<Map.Entry<String, Integer>> sortedSeats = new ArrayList<>(seatTally.entrySet());
                sortedSeats.sort((a, b2) -> b2.getValue() - a.getValue());

                StringBuilder sb = new StringBuilder();
                sb.append("{\"electionId\":\"").append(esc(electionId)).append("\",")
                  .append("\"totalSeats\":").append(constituencies.size()).append(",")
                  .append("\"seatsDecided\":").append(seatsDecided).append(",")
                  .append("\"totalVotes\":").append(totalVotesCast).append(",")
                  .append("\"seatTally\":[");
                for (int i = 0; i < sortedSeats.size(); i++) {
                    if (i > 0) sb.append(",");
                    String party = sortedSeats.get(i).getKey();
                    sb.append("{\"party\":\"").append(esc(party)).append("\",")
                      .append("\"seats\":").append(sortedSeats.get(i).getValue()).append(",")
                      .append("\"votes\":").append(partyVotes.getOrDefault(party, 0))
                      .append("}");
                }
                sb.append("],\"partyVotes\":{");
                boolean first = true;
                for (Map.Entry<String, Integer> e : partyVotes.entrySet()) {
                    if (!first) sb.append(","); first = false;
                    sb.append("\"").append(esc(e.getKey())).append("\":").append(e.getValue());
                }
                return sb.append("}}").toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  DIVISION RESULTS — per-division seat tallies
        // ════════════════════════════════════════════════════════════════
        get("/api/results/division", (req, res) -> {
            try {
                String electionId = req.queryParams("electionId");
                String division = req.queryParams("division");
                if (electionId == null || electionId.isEmpty()) {
                    res.status(400); return JsonUtil.error("electionId required");
                }
                List<Document> allBlocks = blockchainMongoService.getAllBlocks();
                List<Document> constituencies = division != null && !division.isEmpty()
                        ? constituencyMongoService.getByDivision(electionId, division)
                        : constituencyMongoService.getByElection(electionId);

                java.util.Set<String> constIds = new java.util.HashSet<>();
                for (Document c : constituencies) constIds.add(c.getString("constituencyId"));

                // Count votes per constituency per candidate
                Map<String, Map<String, int[]>> constVotes = new LinkedHashMap<>();
                Map<String, String> candPartyMap = new LinkedHashMap<>();
                for (Document b : allBlocks) {
                    if (b.getInteger("index") == 0) continue;
                    String bElection = b.getString("electionId");
                    if (bElection == null || !bElection.equals(electionId)) continue;
                    String bConst = b.getString("constituencyId");
                    if (!constIds.contains(bConst)) continue;
                    String cand = b.getString("candidateName");
                    String party = b.getString("party");
                    constVotes.computeIfAbsent(bConst, k -> new LinkedHashMap<>())
                              .computeIfAbsent(cand, k -> new int[]{0})[0]++;
                    if (party != null) candPartyMap.put(cand, party);
                }

                Map<String, Integer> seatTally = new LinkedHashMap<>();
                int totalVotes = 0;
                for (Map.Entry<String, Map<String, int[]>> entry : constVotes.entrySet()) {
                    String leader = null; int max = 0;
                    for (Map.Entry<String, int[]> ce : entry.getValue().entrySet()) {
                        totalVotes += ce.getValue()[0];
                        if (ce.getValue()[0] > max) { max = ce.getValue()[0]; leader = ce.getKey(); }
                    }
                    if (leader != null) {
                        String p = candPartyMap.getOrDefault(leader, "IND");
                        seatTally.put(p, seatTally.getOrDefault(p, 0) + 1);
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{\"division\":\"").append(esc(division != null ? division : "All")).append("\",")
                  .append("\"totalConstituencies\":").append(constituencies.size()).append(",")
                  .append("\"totalVotes\":").append(totalVotes).append(",")
                  .append("\"seatTally\":{");
                boolean first = true;
                for (Map.Entry<String, Integer> e : seatTally.entrySet()) {
                    if (!first) sb.append(","); first = false;
                    sb.append("\"").append(esc(e.getKey())).append("\":").append(e.getValue());
                }
                return sb.append("}}").toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  GLOBAL STATS — dashboard statistics
        // ════════════════════════════════════════════════════════════════
        get("/api/stats", (req, res) -> {
            try {
                Document voterStats = mongoVoterService.getVoterStats();
                long totalBlocks = blockchainMongoService.getAllBlocks().size();
                List<Document> elections = electionMongoService.getAllElections();
                long activeElections = elections.stream()
                        .filter(e -> "ACTIVE".equals(e.getString("status"))).count();

                long vsCount = 0, lsCount = 0;
                for (Document el : elections) {
                    String eid = el.getString("electionId");
                    long count = constituencyMongoService.countByElection(eid);
                    if ("VIDHAN_SABHA".equals(el.getString("type"))) vsCount = count;
                    else if ("LOK_SABHA".equals(el.getString("type"))) lsCount = count;
                }

                long totalVoters = voterStats.getLong("totalVoters");
                long voted = voterStats.getLong("voted");
                double turnout = totalVoters > 0 ? (voted * 100.0 / totalVoters) : 0;

                StringBuilder sb = new StringBuilder();
                sb.append("{")
                  .append("\"totalVoters\":").append(totalVoters).append(",")
                  .append("\"voted\":").append(voted).append(",")
                  .append("\"pending\":").append(voterStats.getLong("pending")).append(",")
                  .append("\"turnout\":").append(String.format("%.1f", turnout)).append(",")
                  .append("\"totalBlocks\":").append(totalBlocks).append(",")
                  .append("\"activeElections\":").append(activeElections).append(",")
                  .append("\"totalElections\":").append(elections.size()).append(",")
                  .append("\"vsConstituencies\":").append(vsCount).append(",")
                  .append("\"lsConstituencies\":").append(lsCount).append(",")
                  .append("\"byDivision\":");

                // Serialize byDivision
                Document byDiv = voterStats.get("byDivision", Document.class);
                if (byDiv != null) {
                    sb.append("{");
                    boolean first = true;
                    for (String key : byDiv.keySet()) {
                        Document d = byDiv.get(key, Document.class);
                        if (!first) sb.append(","); first = false;
                        sb.append("\"").append(esc(key)).append("\":{")
                          .append("\"total\":").append(d.getLong("total")).append(",")
                          .append("\"voted\":").append(d.getLong("voted")).append("}");
                    }
                    sb.append("}");
                } else {
                    sb.append("{}");
                }
                return sb.append("}").toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  VOTER SEARCH — filter voters by query, district, division
        // ════════════════════════════════════════════════════════════════
        get("/api/admin/voters/search", (req, res) -> {
            try {
                String q = req.queryParams("q");
                String district = req.queryParams("district");
                String division = req.queryParams("division");
                List<Document> voters = mongoVoterService.searchVoters(q, district, division);

                StringBuilder sb = new StringBuilder();
                sb.append("{\"count\":").append(voters.size()).append(",\"voters\":[");
                for (int i = 0; i < voters.size(); i++) {
                    Document v = voters.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                      .append("\"voterId\":\"").append(esc(v.getString("voterId"))).append("\",")
                      .append("\"name\":\"").append(esc(v.getString("name"))).append("\",")
                      .append("\"district\":\"").append(esc(v.getString("district"))).append("\",")
                      .append("\"division\":\"").append(esc(v.getString("division"))).append("\",")
                      .append("\"constituencyVS\":\"").append(esc(v.getString("constituencyVS"))).append("\",")
                      .append("\"constituencyLS\":\"").append(esc(v.getString("constituencyLS"))).append("\",")
                      .append("\"hasVoted\":").append(v.getBoolean("hasVoted", false))
                      .append("}");
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  REGISTER VOTER (FULL) — with constituency assignments
        // ════════════════════════════════════════════════════════════════
        post("/api/admin/register-voter-full", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String voterId  = body.getOrDefault("voterId",  "").trim().toUpperCase();
                String name     = body.getOrDefault("name",     "").trim();
                String password = body.getOrDefault("password", "").trim();
                String district = body.getOrDefault("district", "").trim();
                String division = body.getOrDefault("division", "").trim();
                String constVS  = body.getOrDefault("constituencyVS", "").trim();
                String constLS  = body.getOrDefault("constituencyLS", "").trim();

                if (voterId.isEmpty() || password.isEmpty() || name.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("voterId, name, and password are required");
                }
                boolean ok = mongoVoterService.registerVoterWithConstituency(
                        voterId, name, password, district, division, constVS, constLS);
                if (!ok) {
                    res.status(400);
                    return JsonUtil.error("Voter ID already exists");
                }
                return JsonUtil.obj("success", true, "voterId", voterId,
                        "district", district, "division", division);
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  LIVE DASHBOARD — real-time election stats
        // ════════════════════════════════════════════════════════════════
        get("/api/results/live", (req, res) -> {
            try {
                String electionId = req.queryParams("electionId");
                List<Document> allBlocks = blockchainMongoService.getAllBlocks();
                Map<String, Integer> partyVotes = new LinkedHashMap<>();
                int totalVotes = 0;
                String lastVoteTime = "";

                for (Document b : allBlocks) {
                    if (b.getInteger("index") == 0) continue;
                    if (electionId != null && !electionId.isEmpty()) {
                        String bElection = b.getString("electionId");
                        if (bElection == null || !bElection.equals(electionId)) continue;
                    }
                    String party = b.getString("party");
                    if (party != null) partyVotes.put(party, partyVotes.getOrDefault(party, 0) + 1);
                    totalVotes++;
                    lastVoteTime = b.getString("timestamp");
                }

                Document voterStats = mongoVoterService.getVoterStats();
                long totalVoters = voterStats.getLong("totalVoters");
                double turnout = totalVoters > 0 ? (totalVotes * 100.0 / totalVoters) : 0;

                // Leading party
                String leadingParty = ""; int leadingVotes = 0;
                for (Map.Entry<String, Integer> e : partyVotes.entrySet()) {
                    if (e.getValue() > leadingVotes) { leadingVotes = e.getValue(); leadingParty = e.getKey(); }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{\"totalVotes\":").append(totalVotes).append(",")
                  .append("\"totalVoters\":").append(totalVoters).append(",")
                  .append("\"turnout\":").append(String.format("%.1f", turnout)).append(",")
                  .append("\"leadingParty\":\"").append(esc(leadingParty)).append("\",")
                  .append("\"leadingVotes\":").append(leadingVotes).append(",")
                  .append("\"lastVoteTime\":\"").append(esc(lastVoteTime)).append("\",")
                  .append("\"totalBlocks\":").append(allBlocks.size()).append(",")
                  .append("\"partyVotes\":{");
                boolean first = true;
                for (Map.Entry<String, Integer> e : partyVotes.entrySet()) {
                    if (!first) sb.append(","); first = false;
                    sb.append("\"").append(esc(e.getKey())).append("\":").append(e.getValue());
                }
                return sb.append("}}").toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  VOTER ELECTION STATUS — check what elections voter can participate in
        // ════════════════════════════════════════════════════════════════
        get("/api/voter/elections", (req, res) -> {
            try {
                String voterId = req.queryParams("voterId");
                if (voterId == null || voterId.isEmpty()) {
                    res.status(400); return JsonUtil.error("voterId required");
                }
                Document voter = mongoVoterService.getVoterInfo(voterId.toUpperCase());
                if (voter == null) {
                    res.status(404); return JsonUtil.error("Voter not found");
                }

                List<Document> allElections = electionMongoService.getAllElections();
                StringBuilder sb = new StringBuilder();
                sb.append("{\"elections\":[");
                boolean first = true;
                for (Document e : allElections) {
                    String eid = e.getString("electionId");
                    String status = e.getString("status");
                    if (!"ACTIVE".equals(status)) continue;

                    // Determine voter's constituency for this election
                    String type = e.getString("type");
                    String constituencyId = "";
                    if ("VIDHAN_SABHA".equals(type)) {
                        constituencyId = voter.getString("constituencyVS");
                    } else if ("LOK_SABHA".equals(type)) {
                        constituencyId = voter.getString("constituencyLS");
                    }

                    boolean hasVoted = mongoVoterService.hasVotedInElection(voterId.toUpperCase(), eid);

                    if (!first) sb.append(",");
                    sb.append("{")
                      .append("\"electionId\":\"").append(esc(eid)).append("\",")
                      .append("\"title\":\"").append(esc(e.getString("title"))).append("\",")
                      .append("\"type\":\"").append(esc(type)).append("\",")
                      .append("\"status\":\"").append(esc(status)).append("\",")
                      .append("\"constituencyId\":\"").append(esc(constituencyId != null ? constituencyId : "")).append("\",")
                      .append("\"hasVoted\":").append(hasVoted)
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
        //  MLA HUNG ASSEMBLY SIMULATION — SEED 100 VOTERS + 4 MLAs
        // ════════════════════════════════════════════════════════════════
        post("/api/mla-sim/seed", (req, res) -> {
            try {
                MongoDatabase db = MongoClients.create(MONGO_URI).getDatabase("votingdb");
                MongoCollection<Document> simVoters = db.getCollection("mla_sim_voters");
                MongoCollection<Document> simMlas = db.getCollection("mla_sim_mlas");
                MongoCollection<Document> simCross = db.getCollection("mla_sim_crossvotes");

                simVoters.deleteMany(new Document());
                simMlas.deleteMany(new Document());
                simCross.deleteMany(new Document());

                // 4 MLAs with BCrypt hashed passwords
                String[][] mlas = {
                    {"1","Rajesh Patil","BJP","Bharatiya Janata Party","rajesh.patil@mla.gov.in","#FF9933","🪷","mla@bjp123"},
                    {"2","Priya Deshmukh","INC","Indian National Congress","priya.deshmukh@mla.gov.in","#00BFFF","✋","mla@inc123"},
                    {"3","Suresh Shinde","SHS","Shiv Sena","suresh.shinde@mla.gov.in","#FF6600","🏹","mla@shs123"},
                    {"4","Meena Jadhav","NCP","Nationalist Congress Party","meena.jadhav@mla.gov.in","#004D40","🕐","mla@ncp123"}
                };
                for (String[] m : mlas) {
                    String mlaHashedPw = org.mindrot.jbcrypt.BCrypt.hashpw(m[7], org.mindrot.jbcrypt.BCrypt.gensalt(12));
                    simMlas.insertOne(new Document("mlaId", Integer.parseInt(m[0]))
                        .append("name", m[1]).append("party", m[2]).append("partyFull", m[3])
                        .append("email", m[4]).append("color", m[5]).append("symbol", m[6])
                        .append("password", mlaHashedPw));
                }

                // 100 voters with BCrypt hashed passwords
                String[] firstNames = {"Aarav","Vivaan","Aditya","Vihaan","Arjun","Sai","Reyansh","Ayaan","Krishna","Ishaan",
                    "Ananya","Diya","Saanvi","Aanya","Aadhya","Myra","Isha","Sara","Pari","Riya",
                    "Rahul","Amit","Sunil","Vijay","Rajesh","Pooja","Neha","Priya","Sunita","Kavita",
                    "Mohan","Deepak","Ramesh","Ganesh","Mahesh","Lakshmi","Gayatri","Savitri","Mangala","Bhavana",
                    "Nikhil","Rohan","Karan","Varun","Harsh","Sneha","Swati","Mansi","Komal","Pallavi",
                    "Tushar","Sachin","Ajay","Suresh","Prakash","Seema","Rekha","Meena","Geeta","Suman",
                    "Akash","Vishal","Sagar","Pranav","Dev","Ashwini","Jyoti","Shruti","Tanvi","Nidhi",
                    "Omkar","Tejas","Yash","Gaurav","Kunal","Rutuja","Gauri","Sakshi","Aditi","Ankita",
                    "Manoj","Sandip","Pramod","Nilesh","Dinesh","Vandana","Shubhangi","Madhuri","Archana","Vaishali",
                    "Amol","Prasad","Sanjay","Ashok","Bharat","Smita","Vrushali","Supriya","Dipali","Ujwala"};
                String[] lastNames = {"Patil","Deshmukh","Shinde","Jadhav","More","Pawar","Kulkarni","Chavan","Joshi","Bhosale"};

                // Distribution: 28 BJP, 26 INC, 24 SHS, 22 NCP (no 51% majority)
                int[] dist = {28, 26, 24, 22};
                int voterIdx = 0;
                String hashedPw = org.mindrot.jbcrypt.BCrypt.hashpw("vote123", org.mindrot.jbcrypt.BCrypt.gensalt(12));

                for (int mlaIdx = 0; mlaIdx < 4; mlaIdx++) {
                    for (int j = 0; j < dist[mlaIdx]; j++) {
                        voterIdx++;
                        String name = firstNames[voterIdx-1] + " " + lastNames[voterIdx % lastNames.length];
                        String email = "voter" + voterIdx + "@mh.gov.in";
                        simVoters.insertOne(new Document("simVoterId", voterIdx)
                            .append("name", name).append("email", email)
                            .append("password", hashedPw)
                            .append("initialVoteMlaId", mlaIdx + 1)
                            .append("crossVoteMlaId", null)
                            .append("crossVoteTime", null)
                            .append("hasCrossVoted", false));
                    }
                }

                System.out.println("✅ MLA Simulation seeded: 4 MLAs + 100 voters (BCrypt hashed)");
                return JsonUtil.obj("success", true, "message",
                    "MLA Simulation seeded: 4 MLAs, 100 voters, initial votes distributed (28/26/24/22)");
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  MLA SIM — PARTY MERGER (bulk transfer all votes)
        // ════════════════════════════════════════════════════════════════
        post("/api/mla-sim/merge", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String mlaEmail = body.getOrDefault("email", "").trim().toLowerCase();
                String mlaPassword = body.getOrDefault("password", "").trim();
                String toMlaIdStr = body.getOrDefault("toMlaId", "").trim();

                if (mlaEmail.isEmpty() || mlaPassword.isEmpty() || toMlaIdStr.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("MLA email, password, and target MLA are required");
                }
                int toMlaId = Integer.parseInt(toMlaIdStr);

                MongoDatabase db = MongoClients.create(MONGO_URI).getDatabase("votingdb");
                MongoCollection<Document> simVoters = db.getCollection("mla_sim_voters");
                MongoCollection<Document> simMlas = db.getCollection("mla_sim_mlas");
                MongoCollection<Document> simCross = db.getCollection("mla_sim_crossvotes");

                // Authenticate MLA by email
                Document fromMla = simMlas.find(new Document("email", mlaEmail)).first();
                if (fromMla == null) {
                    res.status(401);
                    return JsonUtil.error("MLA not found with this email");
                }

                // Verify MLA password with BCrypt
                String storedHash = fromMla.getString("password");
                if (storedHash == null || !org.mindrot.jbcrypt.BCrypt.checkpw(mlaPassword, storedHash)) {
                    res.status(401);
                    return JsonUtil.error("Invalid MLA password! Authentication failed.");
                }

                int fromMlaId = fromMla.getInteger("mlaId");
                if (fromMlaId == toMlaId) {
                    res.status(400);
                    return JsonUtil.error("Cannot merge votes to yourself");
                }

                Document toMla = simMlas.find(new Document("mlaId", toMlaId)).first();
                if (toMla == null) {
                    res.status(404);
                    return JsonUtil.error("Target MLA not found");
                }

                // Find all voters whose FINAL vote is still for the fromMla
                // (initial vote = fromMla AND no cross-vote yet)
                List<Document> votersToMerge = new java.util.ArrayList<>();
                simVoters.find(new Document("initialVoteMlaId", fromMlaId)
                    .append("hasCrossVoted", false)).into(votersToMerge);

                if (votersToMerge.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("No voters left to merge from " + fromMla.getString("name"));
                }

                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date());
                int mergedCount = 0;

                for (Document voter : votersToMerge) {
                    String voterEmail = voter.getString("email");

                    // Mine blockchain block for each merged vote
                    Document latest = getLatestBlockFromMongo();
                    int nextIndex = latest != null ? latest.getInteger("index") + 1 : 1;
                    String previousHash = latest != null ? latest.getString("currentHash") : "0";
                    String candidateName = "MERGER:" + fromMla.getString("name") + "→"
                        + toMla.getString("name") + "|" + voter.getString("name");

                    int nonce = 0;
                    long startTime = System.currentTimeMillis();
                    String currentHash;
                    while (true) {
                        currentHash = sha256(nextIndex + voterEmail + candidateName
                            + timestamp + previousHash + nonce);
                        if (currentHash.startsWith(TARGET)) break;
                        nonce++;
                    }
                    long miningTime = System.currentTimeMillis() - startTime;

                    // Save block
                    blocksCol.insertOne(new Document("index", nextIndex)
                        .append("voterId", voterEmail).append("candidateName", candidateName)
                        .append("timestamp", timestamp).append("previousHash", previousHash)
                        .append("currentHash", currentHash).append("nonce", nonce)
                        .append("miningTime", miningTime).append("electionId", "MLA-SIM")
                        .append("constituencyId", "MERGER").append("party", toMla.getString("party")));

                    // Update voter
                    simVoters.updateOne(new Document("email", voterEmail),
                        new Document("$set", new Document("crossVoteMlaId", toMlaId)
                            .append("hasCrossVoted", true)
                            .append("crossVoteTime", timestamp)));

                    // Log cross-vote
                    simCross.insertOne(new Document("voterId", voter.getInteger("simVoterId"))
                        .append("voterName", voter.getString("name")).append("voterEmail", voterEmail)
                        .append("fromMlaId", fromMlaId).append("fromMlaName", fromMla.getString("name"))
                        .append("fromParty", fromMla.getString("party"))
                        .append("toMlaId", toMlaId).append("toMlaName", toMla.getString("name"))
                        .append("toParty", toMla.getString("party"))
                        .append("timestamp", timestamp).append("blockHash", currentHash)
                        .append("blockIndex", nextIndex).append("miningTime", miningTime)
                        .append("mergerType", "PARTY_MERGER"));

                    mergedCount++;
                }

                System.out.println("✅ MLA-SIM MERGER: " + fromMla.getString("name") + " → "
                    + toMla.getString("name") + " (" + mergedCount + " votes transferred)");

                return JsonUtil.obj("success", true,
                    "fromMla", fromMla.getString("name"), "fromParty", fromMla.getString("party"),
                    "toMla", toMla.getString("name"), "toParty", toMla.getString("party"),
                    "mergedVotes", mergedCount, "timestamp", timestamp);

            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  MLA SIM — GET STATE (MLAs + tally + voters)
        // ════════════════════════════════════════════════════════════════
        get("/api/mla-sim/state", (req, res) -> {
            try {
                MongoDatabase db = MongoClients.create(MONGO_URI).getDatabase("votingdb");
                MongoCollection<Document> simVoters = db.getCollection("mla_sim_voters");
                MongoCollection<Document> simMlas = db.getCollection("mla_sim_mlas");

                // MLAs
                List<Document> mlaList = new java.util.ArrayList<>();
                simMlas.find().into(mlaList);

                // Tally (using final vote = crossVote if exists, else initialVote)
                Map<Integer, Integer> tally = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= 4; i++) tally.put(i, 0);
                long totalCross = 0;

                List<Document> voterList = new java.util.ArrayList<>();
                simVoters.find().into(voterList);

                for (Document v : voterList) {
                    Integer crossMla = v.getInteger("crossVoteMlaId");
                    int finalVote = crossMla != null ? crossMla : v.getInteger("initialVoteMlaId");
                    tally.put(finalVote, tally.getOrDefault(finalVote, 0) + 1);
                    if (v.getBoolean("hasCrossVoted", false)) totalCross++;
                }

                boolean majority = tally.values().stream().anyMatch(v -> v >= 51);

                StringBuilder sb = new StringBuilder();
                sb.append("{\"totalVoters\":").append(voterList.size())
                  .append(",\"totalCrossVotes\":").append(totalCross)
                  .append(",\"majorityThreshold\":51")
                  .append(",\"majorityAchieved\":").append(majority)
                  .append(",\"mlas\":[");
                for (int i = 0; i < mlaList.size(); i++) {
                    Document m = mlaList.get(i);
                    if (i > 0) sb.append(",");
                    int mlaId = m.getInteger("mlaId");
                    int votes = tally.getOrDefault(mlaId, 0);
                    sb.append("{\"mlaId\":").append(mlaId)
                      .append(",\"name\":\"").append(esc(m.getString("name"))).append("\"")
                      .append(",\"party\":\"").append(esc(m.getString("party"))).append("\"")
                      .append(",\"partyFull\":\"").append(esc(m.getString("partyFull"))).append("\"")
                      .append(",\"email\":\"").append(esc(m.getString("email"))).append("\"")
                      .append(",\"color\":\"").append(esc(m.getString("color"))).append("\"")
                      .append(",\"symbol\":\"").append(esc(m.getString("symbol"))).append("\"")
                      .append(",\"votes\":").append(votes)
                      .append(",\"percentage\":").append(String.format("%.1f", votes * 100.0 / Math.max(voterList.size(), 1)))
                      .append("}");
                }
                sb.append("],\"tally\":{");
                boolean first = true;
                for (Map.Entry<Integer, Integer> e : tally.entrySet()) {
                    if (!first) sb.append(","); first = false;
                    sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
                }
                sb.append("}}");
                return sb.toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  MLA SIM — CROSS-VOTE (email + password auth + blockchain)
        // ════════════════════════════════════════════════════════════════
        post("/api/mla-sim/cross-vote", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String email = body.getOrDefault("email", "").trim().toLowerCase();
                String password = body.getOrDefault("password", "").trim();
                String targetMlaIdStr = body.getOrDefault("targetMlaId", "").trim();

                if (email.isEmpty() || password.isEmpty() || targetMlaIdStr.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("Email, password, and targetMlaId are required");
                }
                int targetMlaId = Integer.parseInt(targetMlaIdStr);

                MongoDatabase db = MongoClients.create(MONGO_URI).getDatabase("votingdb");
                MongoCollection<Document> simVoters = db.getCollection("mla_sim_voters");
                MongoCollection<Document> simMlas = db.getCollection("mla_sim_mlas");
                MongoCollection<Document> simCross = db.getCollection("mla_sim_crossvotes");

                // Find voter by email
                Document voter = simVoters.find(new Document("email", email)).first();
                if (voter == null) {
                    res.status(401);
                    return JsonUtil.error("Voter not found. Use voter1@mh.gov.in to voter100@mh.gov.in");
                }

                // Verify password with BCrypt
                String storedHash = voter.getString("password");
                if (!org.mindrot.jbcrypt.BCrypt.checkpw(password, storedHash)) {
                    res.status(401);
                    return JsonUtil.error("Invalid password! Authentication failed.");
                }

                // Check duplicate cross-vote
                if (voter.getBoolean("hasCrossVoted", false)) {
                    res.status(400);
                    return JsonUtil.error("You have already cross-voted. No duplicate votes allowed.");
                }

                // Check not voting for same MLA
                int initialMla = voter.getInteger("initialVoteMlaId");
                if (initialMla == targetMlaId) {
                    res.status(400);
                    return JsonUtil.error("You already voted for this MLA initially.");
                }

                // Get MLA names for the block
                Document fromMlaDoc = simMlas.find(new Document("mlaId", initialMla)).first();
                Document toMlaDoc = simMlas.find(new Document("mlaId", targetMlaId)).first();
                String fromMlaName = fromMlaDoc != null ? fromMlaDoc.getString("name") : "MLA-" + initialMla;
                String toMlaName = toMlaDoc != null ? toMlaDoc.getString("name") : "MLA-" + targetMlaId;
                String fromParty = fromMlaDoc != null ? fromMlaDoc.getString("party") : "";
                String toParty = toMlaDoc != null ? toMlaDoc.getString("party") : "";

                // Mine a blockchain block for this cross-vote
                Document latest = getLatestBlockFromMongo();
                int nextIndex = latest != null ? latest.getInteger("index") + 1 : 1;
                String previousHash = latest != null ? latest.getString("currentHash") : "0";
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                String candidateName = "CROSS-VOTE:" + voter.getString("name") + "→" + toMlaName;

                int nonce = 0;
                long startTime = System.currentTimeMillis();
                String currentHash;
                while (true) {
                    currentHash = sha256(nextIndex + email + candidateName + timestamp + previousHash + nonce);
                    if (currentHash.startsWith(TARGET)) break;
                    nonce++;
                }
                long miningTime = System.currentTimeMillis() - startTime;

                // Save block to blockchain
                blocksCol.insertOne(new Document("index", nextIndex)
                    .append("voterId", email).append("candidateName", candidateName)
                    .append("timestamp", timestamp).append("previousHash", previousHash)
                    .append("currentHash", currentHash).append("nonce", nonce)
                    .append("miningTime", miningTime).append("electionId", "MLA-SIM")
                    .append("constituencyId", "HUNG-ASSEMBLY").append("party", toParty));

                // Update voter record
                simVoters.updateOne(new Document("email", email),
                    new Document("$set", new Document("crossVoteMlaId", targetMlaId)
                        .append("hasCrossVoted", true)
                        .append("crossVoteTime", timestamp)));

                // Save cross-vote log
                simCross.insertOne(new Document("voterId", voter.getInteger("simVoterId"))
                    .append("voterName", voter.getString("name")).append("voterEmail", email)
                    .append("fromMlaId", initialMla).append("fromMlaName", fromMlaName).append("fromParty", fromParty)
                    .append("toMlaId", targetMlaId).append("toMlaName", toMlaName).append("toParty", toParty)
                    .append("timestamp", timestamp).append("blockHash", currentHash)
                    .append("blockIndex", nextIndex).append("miningTime", miningTime));

                System.out.println("✅ MLA-SIM Cross-Vote: " + voter.getString("name") + " → " + toMlaName
                    + " (Block #" + nextIndex + ", " + miningTime + "ms)");

                return JsonUtil.obj("success", true,
                    "voterName", voter.getString("name"), "voterEmail", email,
                    "fromMla", fromMlaName, "fromParty", fromParty,
                    "toMla", toMlaName, "toParty", toParty,
                    "blockIndex", nextIndex, "blockHash", currentHash,
                    "miningTime", miningTime, "timestamp", timestamp);

            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  MLA SIM — AUDIT REPORT (end-to-end voter trail)
        // ════════════════════════════════════════════════════════════════
        get("/api/mla-sim/report", (req, res) -> {
            try {
                MongoDatabase db = MongoClients.create(MONGO_URI).getDatabase("votingdb");
                MongoCollection<Document> simVoters = db.getCollection("mla_sim_voters");
                MongoCollection<Document> simMlas = db.getCollection("mla_sim_mlas");
                MongoCollection<Document> simCross = db.getCollection("mla_sim_crossvotes");

                // Build MLA lookup
                Map<Integer, Document> mlaMap = new java.util.LinkedHashMap<>();
                for (Document m : simMlas.find()) mlaMap.put(m.getInteger("mlaId"), m);

                // Tally
                Map<Integer, Integer> tally = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= 4; i++) tally.put(i, 0);

                List<Document> allVoters = new java.util.ArrayList<>();
                simVoters.find().sort(com.mongodb.client.model.Sorts.ascending("simVoterId")).into(allVoters);

                for (Document v : allVoters) {
                    Integer crossMla = v.getInteger("crossVoteMlaId");
                    int finalVote = crossMla != null ? crossMla : v.getInteger("initialVoteMlaId");
                    tally.put(finalVote, tally.getOrDefault(finalVote, 0) + 1);
                }

                long crossCount = simCross.countDocuments();
                boolean majority = tally.values().stream().anyMatch(v -> v >= 51);

                StringBuilder sb = new StringBuilder();
                sb.append("{\"totalVoters\":").append(allVoters.size())
                  .append(",\"totalCrossVotes\":").append(crossCount)
                  .append(",\"majorityAchieved\":").append(majority)
                  .append(",\"tally\":{");
                boolean first = true;
                for (Map.Entry<Integer, Integer> e : tally.entrySet()) {
                    if (!first) sb.append(","); first = false;
                    Document mla = mlaMap.get(e.getKey());
                    String name = mla != null ? mla.getString("name") : "MLA-" + e.getKey();
                    sb.append("\"").append(esc(name)).append("\":").append(e.getValue());
                }
                sb.append("},\"voters\":[");

                for (int i = 0; i < allVoters.size(); i++) {
                    Document v = allVoters.get(i);
                    if (i > 0) sb.append(",");
                    int initId = v.getInteger("initialVoteMlaId");
                    Integer crossId = v.getInteger("crossVoteMlaId");
                    int finalId = crossId != null ? crossId : initId;
                    Document initMla = mlaMap.get(initId);
                    Document crossMla = crossId != null ? mlaMap.get(crossId) : null;
                    Document finalMla = mlaMap.get(finalId);

                    sb.append("{\"id\":").append(v.getInteger("simVoterId"))
                      .append(",\"name\":\"").append(esc(v.getString("name"))).append("\"")
                      .append(",\"email\":\"").append(esc(v.getString("email"))).append("\"")
                      .append(",\"initialVote\":\"").append(esc(initMla != null ? initMla.getString("name")+" ("+initMla.getString("party")+")" : "")).append("\"")
                      .append(",\"crossVote\":").append(crossMla != null ? "\""+esc(crossMla.getString("name")+" ("+crossMla.getString("party")+")")+"\"" : "null")
                      .append(",\"finalVote\":\"").append(esc(finalMla != null ? finalMla.getString("name")+" ("+finalMla.getString("party")+")" : "")).append("\"")
                      .append(",\"crossVoteTime\":").append(v.getString("crossVoteTime") != null ? "\""+esc(v.getString("crossVoteTime"))+"\"" : "null")
                      .append(",\"hasCrossVoted\":").append(v.getBoolean("hasCrossVoted", false))
                      .append("}");
                }
                sb.append("],\"crossVoteLog\":[");

                List<Document> logs = new java.util.ArrayList<>();
                simCross.find().sort(com.mongodb.client.model.Sorts.descending("blockIndex")).into(logs);
                for (int i = 0; i < logs.size(); i++) {
                    Document l = logs.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"voterName\":\"").append(esc(l.getString("voterName"))).append("\"")
                      .append(",\"voterEmail\":\"").append(esc(l.getString("voterEmail"))).append("\"")
                      .append(",\"fromMla\":\"").append(esc(l.getString("fromMlaName"))).append("\"")
                      .append(",\"fromParty\":\"").append(esc(l.getString("fromParty"))).append("\"")
                      .append(",\"toMla\":\"").append(esc(l.getString("toMlaName"))).append("\"")
                      .append(",\"toParty\":\"").append(esc(l.getString("toParty"))).append("\"")
                      .append(",\"timestamp\":\"").append(esc(l.getString("timestamp"))).append("\"")
                      .append(",\"blockHash\":\"").append(esc(l.getString("blockHash"))).append("\"")
                      .append(",\"blockIndex\":").append(l.getInteger("blockIndex"))
                      .append(",\"miningTime\":").append(toLong(l, "miningTime"))
                      .append("}");
                }
                sb.append("]}");
                return sb.toString();
            } catch (Exception e) {
                res.status(500); return JsonUtil.error(e.getMessage());
            }
        });

        System.out.println("✅ Voting API Server started — Maharashtra Edition (MongoDB-only)");
        System.out.println("   All data lives in MongoDB Atlas. No local files used.");
        System.out.println("   Call POST /api/admin/seed-maharashtra to load all constituency data.");
        System.out.println("   MLA Hung Assembly Simulation endpoints ready (/api/mla-sim/*)");
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
