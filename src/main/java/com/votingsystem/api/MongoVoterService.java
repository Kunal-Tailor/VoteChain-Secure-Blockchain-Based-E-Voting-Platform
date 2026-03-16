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
        // BCrypt.hashpw() hashes the password with a random salt
        // cost factor 12 = 2^12 = 4096 iterations (secure but not too slow)
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        return new Document("voterId",  voterId.toUpperCase())
                .append("name",     name)
                .append("password", hashedPassword)  // never store plain text
                .append("hasVoted", false);
                // .append("votedFor", null);
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
                        // .append("votedFor", candidateName))
        );
    }

    // ── Register new voter — password is hashed before saving ──────────────
    public boolean registerVoter(String voterId, String name, String plainPassword) {
        if (voterExists(voterId)) return false;

        // Hash the password before inserting into MongoDB
        voters.insertOne(makeVoter(voterId.toUpperCase(), name, plainPassword));
        System.out.println("✅ Voter " + voterId.toUpperCase() + " registered with hashed password");
        return true;
    }

    // ── Get all voters for admin dashboard ─────────────────────────────────
    // Note: password field is NOT included in the returned list
    public java.util.List<Document> getAllVoters() {
        java.util.List<Document> list = new java.util.ArrayList<>();
        // Exclude the password field from results — never expose hashes
        for (Document doc : voters.find()) {
            Document safe = new Document();
            safe.append("voterId",  doc.getString("voterId"));
            safe.append("name",     doc.getString("name"));
            safe.append("hasVoted", doc.getBoolean("hasVoted", false));
            // safe.append("votedFor", doc.getString("votedFor"));
            list.add(safe);
        }
        return list;
    }
}