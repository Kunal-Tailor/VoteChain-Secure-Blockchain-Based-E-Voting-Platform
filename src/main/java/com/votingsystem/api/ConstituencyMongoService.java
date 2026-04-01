package com.votingsystem.api;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.ascending;

/**
 * ConstituencyMongoService
 *
 * Manages constituency data in MongoDB Atlas.
 * Each constituency belongs to an election and contains its candidate list.
 *
 * Maharashtra structure:
 *   288 Vidhan Sabha constituencies across 36 districts in 6 divisions
 *   48 Lok Sabha constituencies
 *
 * Collection: votingdb.constituencies
 */
public class ConstituencyMongoService {

    private final MongoCollection<Document> constituencies;

    public ConstituencyMongoService(String mongoUri) {
        MongoClient client = MongoClients.create(mongoUri);
        MongoDatabase database = client.getDatabase("votingdb");
        this.constituencies = database.getCollection("constituencies");
        System.out.println("✅ MongoDB constituencies collection ready");
    }

    /**
     * Insert a constituency with its candidates
     */
    public void addConstituency(Document constituency) {
        String cId = constituency.getString("constituencyId");
        if (constituencies.find(eq("constituencyId", cId)).first() != null) return;
        constituencies.insertOne(constituency);
    }

    /**
     * Get all constituencies for a given election
     */
    public List<Document> getByElection(String electionId) {
        List<Document> list = new ArrayList<>();
        constituencies.find(eq("electionId", electionId))
                .sort(ascending("number"))
                .into(list);
        return list;
    }

    /**
     * Get a single constituency by its ID
     */
    public Document getById(String constituencyId) {
        return constituencies.find(eq("constituencyId", constituencyId)).first();
    }

    /**
     * Get constituencies filtered by district and election
     */
    public List<Document> getByDistrict(String electionId, String district) {
        List<Document> list = new ArrayList<>();
        constituencies.find(and(
                eq("electionId", electionId),
                eq("district", district)
        )).sort(ascending("number")).into(list);
        return list;
    }

    /**
     * Get constituencies filtered by division and election
     */
    public List<Document> getByDivision(String electionId, String division) {
        List<Document> list = new ArrayList<>();
        constituencies.find(and(
                eq("electionId", electionId),
                eq("division", division)
        )).sort(ascending("number")).into(list);
        return list;
    }

    /**
     * Get a voter's constituency for a given election type
     */
    public Document getVoterConstituency(String electionId, String constituencyId) {
        return constituencies.find(and(
                eq("electionId", electionId),
                eq("constituencyId", constituencyId)
        )).first();
    }

    /**
     * Clear all constituencies (for re-seeding)
     */
    public void clearAll() {
        constituencies.deleteMany(new Document());
        System.out.println("✅ All constituencies cleared from MongoDB");
    }

    /**
     * Count constituencies for an election
     */
    public long countByElection(String electionId) {
        return constituencies.countDocuments(eq("electionId", electionId));
    }
}
