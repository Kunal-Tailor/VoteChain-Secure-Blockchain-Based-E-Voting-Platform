package com.votingsystem.api;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

/**
 * ElectionMongoService
 *
 * Manages election lifecycle in MongoDB Atlas.
 * Supports multiple election types specific to Maharashtra:
 *   - VIDHAN_SABHA (MLA - 288 seats)
 *   - LOK_SABHA (MP - 48 seats)
 *   - MUNICIPAL (Ward-level)
 *   - ZILLA_PARISHAD (District-level)
 *   - VIDHAN_PARISHAD (MLC - 78 seats, indirect)
 *
 * Collection: votingdb.elections
 */
public class ElectionMongoService {

    private final MongoCollection<Document> elections;

    public ElectionMongoService(String mongoUri) {
        MongoClient client = MongoClients.create(mongoUri);
        MongoDatabase database = client.getDatabase("votingdb");
        this.elections = database.getCollection("elections");
        System.out.println("✅ MongoDB elections collection ready");
    }

    /**
     * Create a new election
     */
    public boolean createElection(String electionId, String type, String title,
                                  int totalSeats, String startDate, String endDate) {
        if (elections.find(eq("electionId", electionId)).first() != null) {
            return false; // already exists
        }

        elections.insertOne(new Document("electionId", electionId)
                .append("type", type)
                .append("title", title)
                .append("state", "Maharashtra")
                .append("totalSeats", totalSeats)
                .append("status", "UPCOMING")
                .append("startDate", startDate)
                .append("endDate", endDate)
                .append("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                        .format(new java.util.Date())));

        System.out.println("✅ Election created: " + electionId + " — " + title);
        return true;
    }

    /**
     * Get all elections
     */
    public List<Document> getAllElections() {
        List<Document> list = new ArrayList<>();
        elections.find().sort(descending("createdAt")).into(list);
        return list;
    }

    /**
     * Get election by ID
     */
    public Document getElection(String electionId) {
        return elections.find(eq("electionId", electionId)).first();
    }

    /**
     * Update election status: UPCOMING → ACTIVE → COMPLETED
     */
    public boolean updateStatus(String electionId, String newStatus) {
        Document existing = elections.find(eq("electionId", electionId)).first();
        if (existing == null) return false;

        elections.updateOne(
                eq("electionId", electionId),
                new Document("$set", new Document("status", newStatus))
        );
        System.out.println("✅ Election " + electionId + " status → " + newStatus);
        return true;
    }

    /**
     * Get the currently active election of a given type
     */
    public Document getActiveElection(String type) {
        return elections.find(
                new Document("type", type).append("status", "ACTIVE")
        ).first();
    }

    /**
     * Delete all elections (for re-seeding)
     */
    public void clearAll() {
        elections.deleteMany(new Document());
        System.out.println("✅ All elections cleared from MongoDB");
    }
}
