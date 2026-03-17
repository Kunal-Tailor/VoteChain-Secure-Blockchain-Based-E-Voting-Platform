package com.votingsystem.api;

/**
 * ApiOnlyMain — starts ONLY the REST API server (no JavaFX window).
 *
 * Use this for:
 *   ✅ Railway / Render deployment (no display available)
 *   ✅ Testing the API without opening the desktop app
 *
 * Run ApiMain.java instead if you want both the desktop + API.
 */
public class ApiOnlyMain {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Voting API Server (no JavaFX)...");
        VotingApiServer.startServer();

        // Keep the process alive forever
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Server stopped.");
        }
    }
}
