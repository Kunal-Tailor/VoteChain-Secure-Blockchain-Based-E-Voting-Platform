package com.votingsystem.api;

import com.votingsystem.model.Block;
import com.votingsystem.service.AdminService;
import com.votingsystem.service.BlockchainService;
import com.votingsystem.service.VoterService;
import com.votingsystem.storage.FileStorage;
import org.bson.Document;

import static spark.Spark.*;
import java.util.List;
import java.util.Map;

public class VotingApiServer {

    private static BlockchainService blockchainService;
    private static VoterService      voterService;
    private static AdminService      adminService;
    private static MongoVoterService mongoVoterService;

    // ── PASTE YOUR MONGODB URI HERE ────────────────────────────────────────
    private static final String MONGO_URI = "mongodb+srv://kunaltailor5555_db_user:T69jfdh47D0lr3Ws@cluster0.hhhinv6.mongodb.net/?appName=Cluster0";

    public static void startServer() {

        // ── Start Java blockchain services (same as before) ─────────────────
        blockchainService = new BlockchainService();
        voterService      = new VoterService(blockchainService);
        adminService      = new AdminService(blockchainService);

        // ── Connect to MongoDB for voter authentication ──────────────────────
        mongoVoterService = new MongoVoterService(MONGO_URI);

        // ── CORS ─────────────────────────────────────────────────────────────
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin",  "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
            res.header("Access-Control-Max-Age",       "3600");
            res.type("application/json");
            if (req.requestMethod().equalsIgnoreCase("OPTIONS")) {
                halt(200, "OK");
            }
        });

        // ════════════════════════════════════════════════════════════════════
        //  VOTER LOGIN — now checks MongoDB
        // ════════════════════════════════════════════════════════════════════

        // POST /api/voter/login
        // 1. Checks voterId + password against MongoDB voters collection
        // 2. Also checks blockchain hasVoted status
        post("/api/voter/login", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String voterId  = body.getOrDefault("voterId",  "").trim().toUpperCase();
                String password = body.getOrDefault("password", "").trim();

                if (voterId.isEmpty() || password.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("Voter ID and password are required");
                }

                // ← checks MongoDB Atlas voters collection
                Document voter = mongoVoterService.authenticate(voterId, password);
                if (voter == null) {
                    res.status(401);
                    return JsonUtil.error("Invalid Voter ID or Password");
                }

                // Get name from MongoDB
                String name = voter.getString("name");

                // Check hasVoted from MongoDB
                boolean hasVoted = mongoVoterService.hasVoted(voterId);

                return JsonUtil.obj(
                    "success",  true,
                    "voterId",  voterId,
                    "name",     name,
                    "hasVoted", hasVoted
                );
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // ════════════════════════════════════════════════════════════════════
        //  CAST VOTE — mines block in Java + marks voted in MongoDB
        // ════════════════════════════════════════════════════════════════════

        // POST /api/voter/cast-vote
        // 1. Checks MongoDB hasVoted (double vote prevention)
        // 2. Calls Java BlockchainService to mine block (SHA-256 PoW + RSA)
        // 3. Marks voter as voted in MongoDB
        // 4. Saves block to blockchain.dat via FileStorage
        post("/api/voter/cast-vote", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String voterId       = body.getOrDefault("voterId",       "").trim().toUpperCase();
                String candidateName = body.getOrDefault("candidateName", "").trim();

                if (voterId.isEmpty() || candidateName.isEmpty()) {
                    res.status(400);
                    return JsonUtil.error("voterId and candidateName are required");
                }

                // Double vote check in MongoDB
                if (mongoVoterService.hasVoted(voterId)) {
                    res.status(400);
                    return JsonUtil.error("You have already cast your vote");
                }

                // Mine the block using your Java blockchain
                boolean ok = voterService.castVote(voterId, candidateName);
                if (!ok) {
                    res.status(400);
                    return JsonUtil.error("Vote could not be cast");
                }

                // Mark voted in MongoDB
                mongoVoterService.markVoted(voterId);

                // Return block details
                List<Block> chain  = adminService.getAllBlocks();
                Block       latest = chain.get(chain.size() - 1);

                return JsonUtil.obj(
                    "success",      true,
                    "blockIndex",   latest.getIndex(),
                    "currentHash",  latest.getCurrentHash(),
                    "previousHash", latest.getPreviousHash(),
                    "miningTime",   latest.getMiningTime(),
                    "nonce",        latest.getNonce(),
                    "timestamp",    latest.getTimestamp()
                );
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // GET /api/voter/status?voterId=V001
        // Checks MongoDB for voter status
        get("/api/voter/status", (req, res) -> {
            String voterId = req.queryParams("voterId");
            if (voterId == null || voterId.isEmpty()) {
                res.status(400);
                return JsonUtil.error("voterId is required");
            }
            boolean hasVoted = mongoVoterService.hasVoted(voterId.toUpperCase());
            return JsonUtil.obj("voterId", voterId.toUpperCase(), "hasVoted", hasVoted);
        });

        // ════════════════════════════════════════════════════════════════════
        //  RESULTS (public)
        // ════════════════════════════════════════════════════════════════════

        get("/api/results", (req, res) -> {
            Map<String, Integer> results = voterService.getElectionResults();
            int total = voterService.getTotalVotes();

            StringBuilder sb = new StringBuilder();
            sb.append("{\"totalVotes\":").append(total).append(",\"results\":{");
            boolean first = true;
            for (Map.Entry<String, Integer> e : results.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
                first = false;
            }
            sb.append("}}");
            return sb.toString();
        });

        // ════════════════════════════════════════════════════════════════════
        //  ADMIN ENDPOINTS
        // ════════════════════════════════════════════════════════════════════

        post("/api/admin/login", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                String username = body.getOrDefault("username", "");
                String password = body.getOrDefault("password", "");
                if (!adminService.authenticateAdmin(username, password)) {
                    res.status(401);
                    return JsonUtil.error("Invalid admin credentials");
                }
                return JsonUtil.obj("success", true, "role", "admin");
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        get("/api/admin/blockchain", (req, res) -> {
            try {
                boolean     isValid = adminService.validateBlockchain();
                List<Block> blocks  = adminService.getAllBlocks();

                StringBuilder sb = new StringBuilder();
                sb.append("{\"isValid\":").append(isValid)
                  .append(",\"totalBlocks\":").append(blocks.size())
                  .append(",\"blocks\":[");

                for (int i = 0; i < blocks.size(); i++) {
                    Block b = blocks.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                      .append("\"index\":").append(b.getIndex()).append(",")
                      .append("\"voterId\":\"").append(esc(b.getVoterId())).append("\",")
                      .append("\"candidateName\":\"").append(esc(b.getCandidateName())).append("\",")
                      .append("\"timestamp\":\"").append(esc(b.getTimestamp())).append("\",")
                      .append("\"previousHash\":\"").append(esc(b.getPreviousHash())).append("\",")
                      .append("\"currentHash\":\"").append(esc(b.getCurrentHash())).append("\",")
                      .append("\"nonce\":").append(b.getNonce()).append(",")
                      .append("\"miningTime\":").append(b.getMiningTime())
                      .append("}");
                }
                sb.append("]}");
                return sb.toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // GET /api/admin/voters — now reads from MongoDB
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
                sb.append("]}");
                return sb.toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // POST /api/admin/register-voter — saves to MongoDB
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

                // saves to MongoDB
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

        get("/api/admin/getblock", (req, res) -> {
            try {
                long    blockNum  = adminService.getGetBlockLatestBlock();
                boolean reachable = blockNum >= 0;
                return JsonUtil.obj("reachable", reachable, "blockNumber", blockNum);
            } catch (Exception e) {
                return JsonUtil.obj("reachable", false, "blockNumber", -1);
            }
        });

        System.out.println("✅ Voting API Server started on http://localhost:4567");
        System.out.println("   Open index.html in your browser to use the web UI.");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}