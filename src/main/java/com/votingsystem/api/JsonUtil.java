package com.votingsystem.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal JSON helper — no external library needed.
 * Parses simple flat JSON objects and builds JSON strings.
 */
public class JsonUtil {

    /** Build a JSON error response: {"error":"message"} */
    public static String error(String message) {
        return "{\"error\":\"" + esc(message) + "\"}";
    }

    /**
     * Build a JSON object from key-value pairs.
     * Values can be: String, Boolean, Integer, Long, Double.
     * Example: JsonUtil.obj("success", true, "voterId", "V001")
     */
    public static String obj(Object... pairs) {
        if (pairs.length % 2 != 0)
            throw new IllegalArgumentException("Must provide key-value pairs");

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) sb.append(",");
            String key = pairs[i].toString();
            Object val = pairs[i + 1];
            sb.append("\"").append(esc(key)).append("\":");
            if (val instanceof Boolean || val instanceof Integer
                    || val instanceof Long   || val instanceof Double) {
                sb.append(val);
            } else {
                sb.append("\"").append(esc(val == null ? "" : val.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Parse a flat JSON object like {"key":"value","key2":"value2"}
     * into a Map<String, String>.  Handles strings, numbers, booleans.
     * Sufficient for all API request bodies in this project.
     */
    public static Map<String, String> parseMap(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;

        // Strip outer braces
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))   json = json.substring(0, json.length() - 1);
        json = json.trim();
        if (json.isEmpty()) return map;

        // Split on commas NOT inside quotes
        String[] pairs = splitTopLevel(json);
        for (String pair : pairs) {
            int colon = findColon(pair);
            if (colon < 0) continue;
            String key = unquote(pair.substring(0, colon).trim());
            String val = unquote(pair.substring(colon + 1).trim());
            map.put(key, val);
        }
        return map;
    }

    // ── internal helpers ────────────────────────────────────────────────────

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        return s; // boolean / number — return as-is
    }

    private static int findColon(String s) {
        boolean inStr = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i-1) != '\\')) inStr = !inStr;
            if (c == ':' && !inStr) return i;
        }
        return -1;
    }

    private static String[] splitTopLevel(String s) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        boolean inStr = false;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i-1) != '\\')) inStr = !inStr;
            if (c == ',' && !inStr) {
                parts.add(s.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(s.substring(start).trim());
        return parts.toArray(new String[0]);
    }
}
