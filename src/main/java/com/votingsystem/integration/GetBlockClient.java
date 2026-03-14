package com.votingsystem.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Lightweight HTTP client for interacting with a GetBlock JSON-RPC endpoint.
 *
 * This class is intentionally simple and avoids external dependencies.
 * It currently supports a basic connectivity / health check by calling
 * the {@code eth_blockNumber} method on an Ethereum-compatible node.
 */
public class GetBlockClient {

    // IMPORTANT: treat this URL as a secret in real projects
    private final String rpcUrl;
    private final HttpClient httpClient;

    public GetBlockClient(String rpcUrl) {
        this.rpcUrl = rpcUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    private String sendRpcRequest(String method, String paramsJsonArray)
            throws IOException, InterruptedException {

        // paramsJsonArray example: "[]" or "[\"0x1\"]"
        String body = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"method\":\"" + method + "\","
                + "\"params\":" + paramsJsonArray + ","
                + "\"id\":1"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rpcUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return response.body();
    }

    /**
     * Calls eth_blockNumber on the remote node and returns the raw hex string
     * result (for example "0x1234").
     */
    public String getLatestBlockNumberHex() throws IOException, InterruptedException {
        String responseJson = sendRpcRequest("eth_blockNumber", "[]");

        int idx = responseJson.indexOf("\"result\"");
        if (idx == -1) {
            return "unknown";
        }

        int colon = responseJson.indexOf(':', idx);
        int quoteStart = responseJson.indexOf('"', colon);
        int quoteEnd = responseJson.indexOf('"', quoteStart + 1);
        if (quoteStart == -1 || quoteEnd == -1) {
            return "unknown";
        }

        return responseJson.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * Returns the latest block number as a long, or -1 on error.
     */
    public long getLatestBlockNumber() throws IOException, InterruptedException {
        String hex = getLatestBlockNumberHex();
        if (hex == null || !hex.startsWith("0x")) {
            return -1L;
        }
        try {
            return Long.parseLong(hex.substring(2), 16);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * Simple reachability check for UI: returns true if the node responds
     * with a valid block number.
     */
    public boolean isReachable() {
        try {
            return getLatestBlockNumber() >= 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}

