package com.votingsystem.api;

import com.votingsystem.model.Block;
import com.votingsystem.service.AdminService;
import com.votingsystem.service.BlockchainService;
import com.votingsystem.service.VoterService;
import org.bson.Document;

import static spark.Spark.*;
import java.util.List;
import java.util.Map;

public class VotingApiServer {

    private static BlockchainService      blockchainService;
    private static VoterService           voterService;
    private static AdminService           adminService;
    private static MongoVoterService      mongoVoterService;
    private static BlockchainMongoService blockchainMongoService;

    private static final String MONGO_URI =
        "mongodb+srv://kunaltailor5555_db_user:T69jfdh47D0lr3Ws@cluster0.hhhinv6.mongodb.net/?appName=Cluster0";

    public static void startServer() {

        blockchainService      = new BlockchainService();
        voterService           = new VoterService(blockchainService);
        adminService           = new AdminService(blockchainService);
        mongoVoterService      = new MongoVoterService(MONGO_URI);
        blockchainMongoService = new BlockchainMongoService(MONGO_URI);

        // Sync all existing blocks from blockchain.dat → MongoDB on startup
        List<Block> existing = adminService.getAllBlocks();
        for (Block b : existing) {
            blockchainMongoService.saveBlock(
                b.getIndex(), b.getVoterId(), b.getCandidateName(),
                b.getTimestamp(), b.getPreviousHash(), b.getCurrentHash(),
                b.getNonce(), b.getMiningTime()
            );
        }
        System.out.println("✅ Blockchain synced to MongoDB (" + existing.size() + " blocks)");

        // ── CORS ─────────────────────────────────────────────────────────
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

        // ════════════════════════════════════════════════════════════════
        //  VOTER LOGIN — checks MongoDB (BCrypt)
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
        //  CAST VOTE — mines block in Java + saves to MongoDB
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

                if (mongoVoterService.hasVoted(voterId)) {
                    res.status(400);
                    return JsonUtil.error("You have already cast your vote");
                }

                // Mine block in Java → saves to blockchain.dat
                boolean ok = voterService.castVote(voterId, candidateName);
                if (!ok) {
                    res.status(400);
                    return JsonUtil.error("Vote could not be cast");
                }

                // Mark voted in MongoDB
                mongoVoterService.markVoted(voterId);

                // Get the newly mined block
                List<Block> chain  = adminService.getAllBlocks();
                Block       latest = chain.get(chain.size() - 1);

                // Save new block to MongoDB so any device can see it
                blockchainMongoService.saveBlock(
                    latest.getIndex(), latest.getVoterId(), latest.getCandidateName(),
                    latest.getTimestamp(), latest.getPreviousHash(), latest.getCurrentHash(),
                    latest.getNonce(), latest.getMiningTime()
                );

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

        // GET /api/voter/status
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
        //  RESULTS (public)
        // ════════════════════════════════════════════════════════════════
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
            return sb.append("}}").toString();
        });

        // ════════════════════════════════════════════════════════════════
        //  ADMIN ENDPOINTS
        // ════════════════════════════════════════════════════════════════
        post("/api/admin/login", (req, res) -> {
            try {
                Map<String, String> body = JsonUtil.parseMap(req.body());
                if (!adminService.authenticateAdmin(
                        body.getOrDefault("username", ""),
                        body.getOrDefault("password", ""))) {
                    res.status(401);
                    return JsonUtil.error("Invalid admin credentials");
                }
                return JsonUtil.obj("success", true, "role", "admin");
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // GET /api/admin/blockchain — from blockchain.dat via Java
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
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // GET /api/admin/voters — from MongoDB
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

        // POST /api/admin/register-voter
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

        // GET /api/admin/getblock
        get("/api/admin/getblock", (req, res) -> {
            try {
                long blockNum = adminService.getGetBlockLatestBlock();
                return JsonUtil.obj("reachable", blockNum >= 0, "blockNumber", blockNum);
            } catch (Exception e) {
                return JsonUtil.obj("reachable", false, "blockNumber", -1);
            }
        });

        // ════════════════════════════════════════════════════════════════
        //  MONGODB BLOCKCHAIN ENDPOINTS
        //  Works from any device — reads/validates from MongoDB Atlas
        // ════════════════════════════════════════════════════════════════

        // GET /api/admin/mongo-blockchain
        // Returns all blocks stored in MongoDB — accessible from ANY device
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
                      .append("\"miningTime\":").append(b.getLong("miningTime"))
                      .append("}");
                }
                return sb.append("]}").toString();
            } catch (Exception e) {
                res.status(500);
                return JsonUtil.error(e.getMessage());
            }
        });

        // GET /api/admin/validate-mongo
        // Validates hash links between all blocks stored in MongoDB
        // Check: Block N's previousHash == Block N-1's currentHash
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

        System.out.println("✅ Voting API Server started on http://localhost:4567");
        System.out.println("   Open voting_index.html in your browser.");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
