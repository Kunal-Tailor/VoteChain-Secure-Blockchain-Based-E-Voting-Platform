package com.votingsystem.api;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import static com.mongodb.client.model.Filters.eq;

/**
 * MongoVoterService
 *
 * Handles all voter operations with MongoDB Atlas.
 * Passwords are stored as BCrypt hashes — never plain text.
 *
 * BCrypt automatically:
 *   - Generates a random salt for every password
 *   - Hashes with cost factor 12 (slow enough to resist brute force)
 *   - Stores the salt inside the hash string itself
 *
 * Example stored password in MongoDB:
 *   plain:  "voter1"
 *   stored: "$2a$12$eImiTXuWVxfM37uY4JANjQ.."  (60 chars)
 */
public class MongoVoterService {

    private final MongoCollection<Document> voters;

    public MongoVoterService(String mongoUri) {
        MongoClient   client   = MongoClients.create(mongoUri);
        MongoDatabase database = client.getDatabase("votingdb");
        this.voters = database.getCollection("voters");

        seedDefaultVoters();
        System.out.println("✅ MongoDB connected — voters collection ready");
    }

    // ── Seed default voters if collection is empty ─────────────────────────
    // Passwords are BCrypt hashed before saving — plain text never stored
    private void seedDefaultVoters() {
        if (voters.countDocuments() == 0) {
            voters.insertMany(java.util.List.of(
                    makeVoter("V001", "Aarav Sharma",  "voter1"),
                    makeVoter("V002", "Priya Singh",   "voter2"),
                    makeVoter("V003", "Rahul Patel",   "voter3"),
                    makeVoter("V004", "Ananya Reddy",  "voter4"),
                    makeVoter("V005", "Vikram Nair",   "voter5")
            ));
            System.out.println("✅ Default voters seeded with hashed passwords");
        }
    }

    // ── Build a voter document with BCrypt hashed password ─────────────────
    private Document makeVoter(String voterId, String name, String plainPassword) {
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        return new Document("voterId",  voterId.toUpperCase())
                .append("name",     name)
                .append("password", hashedPassword)
                .append("district", "")
                .append("division", "")
                .append("constituencyVS", "")
                .append("constituencyLS", "")
                .append("hasVoted", false)
                .append("votedElections", new org.bson.Document());
    }

    // ── Build a voter with full constituency details (Maharashtra) ─────────
    private Document makeVoterFull(String voterId, String name, String plainPassword,
                                    String district, String division,
                                    String constituencyVS, String constituencyLS) {
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        return new Document("voterId",  voterId.toUpperCase())
                .append("name",     name)
                .append("password", hashedPassword)
                .append("district", district)
                .append("division", division)
                .append("constituencyVS", constituencyVS)
                .append("constituencyLS", constituencyLS)
                .append("hasVoted", false)
                .append("votedElections", new org.bson.Document());
    }

    // ── Authenticate voter ─────────────────────────────────────────────────
    // Finds voter by ID, then uses BCrypt.checkpw() to verify password
    // BCrypt.checkpw() extracts the salt from the stored hash automatically
    public Document authenticate(String voterId, String plainPassword) {
        // Step 1: find voter by ID
        Document voter = voters.find(
                eq("voterId", voterId.toUpperCase())
        ).first();

        if (voter == null) return null; // voter ID not found

        // Step 2: verify password using BCrypt
        // BCrypt.checkpw() compares plain text against stored hash safely
        String storedHash = voter.getString("password");
        boolean passwordMatches = BCrypt.checkpw(plainPassword, storedHash);

        return passwordMatches ? voter : null;
    }

    // ── Check if voter exists ───────────────────────────────────────────────
    public boolean voterExists(String voterId) {
        return voters.find(eq("voterId", voterId.toUpperCase())).first() != null;
    }

    // ── Get voter name ──────────────────────────────────────────────────────
    public String getVoterName(String voterId) {
        Document doc = voters.find(eq("voterId", voterId.toUpperCase())).first();
        if (doc == null) return voterId;
        return doc.getString("name");
    }

    // ── Check if voter has already voted ───────────────────────────────────
    public boolean hasVoted(String voterId) {
        Document doc = voters.find(eq("voterId", voterId.toUpperCase())).first();
        if (doc == null) return false;
        return doc.getBoolean("hasVoted", false);
    }

    // ── Mark voter as voted after successful vote cast ─────────────────────
    public void markVoted(String voterId) {
        voters.updateOne(
                eq("voterId", voterId.toUpperCase()),
                new Document("$set", new Document("hasVoted", true))
        );
    }

    // ── Mark voter as voted for a specific election ───────────────────────
    public void markVotedForElection(String voterId, String electionId) {
        voters.updateOne(
                eq("voterId", voterId.toUpperCase()),
                new Document("$set", new Document("votedElections." + electionId.replace(".", "_"), true)
                        .append("hasVoted", true))
        );
    }

    // ── Check if voter has voted in a specific election ───────────────────
    public boolean hasVotedInElection(String voterId, String electionId) {
        Document doc = voters.find(eq("voterId", voterId.toUpperCase())).first();
        if (doc == null) return false;
        Document votedElections = doc.get("votedElections", Document.class);
        if (votedElections == null) return false;
        return votedElections.getBoolean(electionId.replace(".", "_"), false);
    }

    // ── Register new voter — password is hashed before saving ──────────────
    public boolean registerVoter(String voterId, String name, String plainPassword) {
        if (voterExists(voterId)) return false;
        voters.insertOne(makeVoter(voterId.toUpperCase(), name, plainPassword));
        System.out.println("✅ Voter " + voterId.toUpperCase() + " registered with hashed password");
        return true;
    }

    // ── Register voter with constituency info (Maharashtra) ────────────────
    public boolean registerVoterWithConstituency(String voterId, String name, String plainPassword,
                                                  String district, String division,
                                                  String constituencyVS, String constituencyLS) {
        if (voterExists(voterId)) return false;
        voters.insertOne(makeVoterFull(voterId.toUpperCase(), name, plainPassword,
                district, division, constituencyVS, constituencyLS));
        System.out.println("✅ Voter " + voterId.toUpperCase() + " registered ("
                + district + ", " + division + ")");
        return true;
    }

    // ── Get all voters for admin dashboard ─────────────────────────────────
    public java.util.List<Document> getAllVoters() {
        java.util.List<Document> list = new java.util.ArrayList<>();
        for (Document doc : voters.find()) {
            Document safe = new Document();
            safe.append("voterId",  doc.getString("voterId"));
            safe.append("name",     doc.getString("name"));
            safe.append("district", doc.getString("district"));
            safe.append("division", doc.getString("division"));
            safe.append("constituencyVS", doc.getString("constituencyVS"));
            safe.append("constituencyLS", doc.getString("constituencyLS"));
            safe.append("hasVoted", doc.getBoolean("hasVoted", false));
            Document votedElections = doc.get("votedElections", Document.class);
            safe.append("votedElections", votedElections != null ? votedElections : new Document());
            list.add(safe);
        }
        return list;
    }

    // ── Get voter document (safe, no password) ────────────────────────────
    public Document getVoterInfo(String voterId) {
        Document doc = voters.find(eq("voterId", voterId.toUpperCase())).first();
        if (doc == null) return null;
        Document safe = new Document();
        safe.append("voterId",  doc.getString("voterId"));
        safe.append("name",     doc.getString("name"));
        safe.append("district", doc.getString("district"));
        safe.append("division", doc.getString("division"));
        safe.append("constituencyVS", doc.getString("constituencyVS"));
        safe.append("constituencyLS", doc.getString("constituencyLS"));
        safe.append("hasVoted", doc.getBoolean("hasVoted", false));
        Document votedElections = doc.get("votedElections", Document.class);
        safe.append("votedElections", votedElections != null ? votedElections : new Document());
        return safe;
    }
}