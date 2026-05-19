package io.metersphere.platform.client;

import io.metersphere.platform.domain.PhabricatorConfig;
import io.metersphere.plugin.exception.MSPluginException;
import io.metersphere.plugin.utils.JSON;
import io.metersphere.plugin.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhabricatorClient {
    private PhabricatorConfig config;
    private PhabricatorHttpClient httpClient;

    public PhabricatorClient() {
    }

    public PhabricatorClient(PhabricatorConfig config) {
        this(config, new DefaultPhabricatorHttpClient());
    }

    public PhabricatorClient(PhabricatorConfig config, PhabricatorHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public void setConfig(PhabricatorConfig config) {
        this.config = config;
        if (httpClient == null) {
            this.httpClient = new DefaultPhabricatorHttpClient();
        }
    }

    public void setHttpClient(PhabricatorHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> callConduit(String method, Map<String, Object> params) {
        return callConduitWithRetry(method, params, 3, 1000);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> callConduitWithRetry(String method, Map<String, Object> params, int maxRetries, long initialDelayMs) {
        if (config == null || config.getUrl() == null) {
            MSPluginException.throwException("Phabricator configuration is not set");
            return null;
        }

        String baseUrl = config.getUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/api/" + method;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> apiParams = params != null ? new HashMap<>(params) : new HashMap<>();
                
                Map<String, Object> conduitParams = new HashMap<>();
                conduitParams.put("token", config.getApiToken());
                apiParams.put("__conduit__", conduitParams);
                
                String jsonParams = JSON.toJSONString(apiParams);
                String formBody = "params=" + URLEncoder.encode(jsonParams, StandardCharsets.UTF_8) + "&output=json";

                if (config.isDebugMode()) {
                    LogUtil.info("[Phabricator DEBUG] Calling API: " + method + " (attempt " + (attempt + 1) + ")");
                    LogUtil.info("[Phabricator DEBUG] URL: " + url);
                    LogUtil.info("[Phabricator DEBUG] Request: " + maskToken(formBody));
                    LogUtil.info("[Phabricator DEBUG] Request JSON (pretty): " + maskToken(prettyPrintJson(jsonParams)));
                }

                String responseBody = httpClient.executePost(url, formBody);

                if (config.isDebugMode()) {
                    String prettyResponse = prettyPrintJson(responseBody);
                    if (prettyResponse.length() > 2000) {
                        prettyResponse = prettyResponse.substring(0, 2000) + "... [truncated]";
                    }
                    LogUtil.info("[Phabricator DEBUG] Response (pretty): " + maskToken(prettyResponse));
                }

                if (responseBody == null || responseBody.isBlank()) {
                    throw new RuntimeException("Empty response body from Phabricator");
                }

                Map<String, Object> body = JSON.parseObject(responseBody, Map.class);

                if (body == null) {
                    throw new RuntimeException("Failed to parse JSON response from Phabricator");
                }

                if (body.get("error_code") != null) {
                    MSPluginException.throwException("Phabricator API error: " + body.get("error_info"));
                }

                return body;
            } catch (MSPluginException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                boolean isRetryable = isRetryableException(e);
                
                if (attempt < maxRetries && isRetryable) {
                    long delayMs = initialDelayMs * (1L << attempt);
                    LogUtil.warn("Phabricator API call failed (attempt " + (attempt + 1) + "/" + (maxRetries + 1) + 
                        "), retrying in " + delayMs + "ms: " + method + " - " + e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        MSPluginException.throwException("Interrupted while waiting to retry Phabricator API call");
                        return null;
                    }
                } else {
                    LogUtil.error("Phabricator API call failed after " + (attempt + 1) + " attempts: " + method, e);
                }
            }
        }

        MSPluginException.throwException("Failed to call Phabricator API after " + (maxRetries + 1) + " attempts: " + 
            (lastException != null ? lastException.getMessage() : "Unknown error"));
        return null;
    }

    private boolean isRetryableException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        
        // Phabricator API errors (validation errors, logic errors) are not retryable
        if (lowerMessage.contains("phabricator api error")) {
            return false;
        }
        
        // HTTP errors (4xx, 5xx) are not retryable - they won't fix themselves
        if (lowerMessage.contains("http error")) {
            return false;
        }
        
        return lowerMessage.contains("timeout") ||
               lowerMessage.contains("connection") ||
               lowerMessage.contains("refused") ||
               lowerMessage.contains("reset") ||
               lowerMessage.contains("empty response") ||
               lowerMessage.contains("parse") ||
               lowerMessage.contains("json");
    }

    private String maskToken(String formBody) {
        if (formBody == null) {
            return null;
        }
        return formBody.replaceAll("\"token\"\\s*:\\s*\"[^\"]+\"", "\"token\": \"***MASKED***\"");
    }

    public String prettyPrintJson(String json) {
        if (json == null || json.isBlank()) return json;
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString) {
                switch (c) {
                    case '{': case '[':
                        sb.append(c).append('\n');
                        indent++;
                        sb.append("  ".repeat(indent));
                        continue;
                    case '}': case ']':
                        sb.append('\n');
                        indent--;
                        sb.append("  ".repeat(indent));
                        sb.append(c);
                        continue;
                    case ',':
                        sb.append(c).append('\n');
                        sb.append("  ".repeat(indent));
                        continue;
                    case ':':
                        sb.append(": ");
                        continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public void auth() {
        testConnection();
    }

    public boolean testConnection() {
        try {
            Map<String, Object> result = callConduit("user.whoami", new HashMap<>());
            if (result != null && result.containsKey("result")) {
                Object resultData = result.get("result");
                if (resultData instanceof Map) {
                    Map<String, Object> userData = (Map<String, Object>) resultData;
                    if (userData.containsKey("userName")) {
                        LogUtil.info("Phabricator connection test successful. User: " + userData.get("userName"));
                        return true;
                    }
                }
            }
            LogUtil.error("Unexpected response from Phabricator: " + result);
            MSPluginException.throwException("Invalid response from Phabricator");
            return false;
        } catch (Exception e) {
            LogUtil.error("Phabricator connection test failed", e);
            MSPluginException.throwException("Failed to connect to Phabricator: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchWithPagination(String method, Map<String, Object> params) {
        return searchWithPagination(method, params, 5000);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchWithPagination(String method, Map<String, Object> params, int maxResults) {
        List<Map<String, Object>> allResults = new ArrayList<>();
        String afterCursor = null;
        int pageCount = 0;
        
        do {
            if (allResults.size() >= maxResults) {
                LogUtil.info("Reached max results limit (" + maxResults + ") for " + method + ", returning partial results");
                break;
            }
            
            Map<String, Object> searchParams = new HashMap<>(params);
            int limit = Math.min(100, maxResults - allResults.size());
            searchParams.put("limit", limit);
            if (afterCursor != null) {
                searchParams.put("after", afterCursor);
            }
            
            Map<String, Object> result = callConduit(method, searchParams);
            
            if (result == null || !result.containsKey("result")) {
                LogUtil.warn("Invalid or null result from " + method + " at page " + pageCount + ", returning partial results");
                break;
            }
            
            Object resultData = result.get("result");
            if (!(resultData instanceof Map)) {
                LogUtil.warn("Unexpected result type from " + method + " at page " + pageCount + ", returning partial results");
                break;
            }
            
            Map<String, Object> resultMap = (Map<String, Object>) resultData;
            
            Object data = resultMap.get("data");
            if (data instanceof List) {
                List<Map<String, Object>> pageData = (List<Map<String, Object>>) data;
                allResults.addAll(pageData);
                pageCount++;
                
                if (config.isDebugMode()) {
                    LogUtil.info("[Phabricator DEBUG] " + method + " page " + pageCount + ": " + pageData.size() + " results, total: " + allResults.size());
                }
            } else {
                break;
            }
            
            Object cursorObj = resultMap.get("cursor");
            if (cursorObj instanceof Map) {
                Map<String, Object> cursor = (Map<String, Object>) cursorObj;
                afterCursor = (String) cursor.get("after");
            } else {
                afterCursor = null;
            }
            
        } while (afterCursor != null && !afterCursor.isEmpty());
        
        if (config.isDebugMode()) {
            LogUtil.info("[Phabricator DEBUG] " + method + " completed: " + allResults.size() + " total results from " + pageCount + " pages");
        }
        
        return allResults;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchTasks(Map<String, Object> constraints) {
        return searchTasks(constraints, 5000);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchTasks(Map<String, Object> constraints, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);
        params.put("attachments", Map.of("projects", true));

        return searchWithPagination("maniphest.search", params, limit);
    }

    @SuppressWarnings("unchecked")
    public String getTaskIdByPHID(String phid) {
        if (phid == null || phid.isBlank()) {
            return null;
        }
        
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("phids", List.of(phid));
        
        List<Map<String, Object>> results = searchTasks(constraints);
        
        if (results != null && !results.isEmpty()) {
            Map<String, Object> task = results.get(0);
            Object id = task.get("id");
            return id != null ? id.toString() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchProjects(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);
        params.put("attachments", Map.of("parent", true));

        return searchWithPagination("project.search", params);
    }

    @SuppressWarnings("unchecked")
    public boolean projectExists(String projectPHID) {
        if (StringUtils.isBlank(projectPHID)) {
            return false;
        }
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("phids", List.of(projectPHID));
        List<Map<String, Object>> results = searchProjects(constraints);
        return results != null && !results.isEmpty();
    }

    /**
     * Get project name by PHID
     * @param projectPHID The project PHID (e.g., "PHID-PROJ-XXXXX")
     * @return Project name or null if not found
     */
    @SuppressWarnings("unchecked")
    public String getProjectNameByPHID(String projectPHID) {
        if (StringUtils.isBlank(projectPHID)) {
            return null;
        }
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("phids", List.of(projectPHID));
        List<Map<String, Object>> results = searchProjects(constraints);
        if (results != null && !results.isEmpty()) {
            Map<String, Object> project = results.get(0);
            Object fieldsObj = project.get("fields");
            if (fieldsObj instanceof Map) {
                Map<String, Object> fields = (Map<String, Object>) fieldsObj;
                Object nameObj = fields.get("name");
                return nameObj != null ? nameObj.toString() : null;
            }
        }
        return null;
    }


    public Map<String, Object> getTask(String taskId) {
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("ids", List.of(Integer.parseInt(taskId)));

        List<Map<String, Object>> results = searchTasks(constraints);
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> editTask(String objectIdentifier, List<Map<String, Object>> transactions) {
        Map<String, Object> params = new HashMap<>();
        if (objectIdentifier != null) {
            params.put("objectIdentifier", objectIdentifier);
        }
        params.put("transactions", transactions);

        return callConduit("maniphest.edit", params);
    }

    public String processRemarkup(String content, String context) {
        Map<String, Object> params = new HashMap<>();
        params.put("context", context);
        params.put("contents", List.of(content));

        try {
            Map<String, Object> response = callConduit("remarkup.process", params);
            List<String> contents = (List<String>) response.get("content");
            if (contents != null && !contents.isEmpty()) {
                return contents.get(0);
            }
        } catch (Exception e) {
            LogUtil.error("Failed to process remarkup", e);
        }
        return content;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchUsers(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);

        return searchWithPagination("user.search", params);
    }

    public List<Map<String, Object>> getAvailableStatuses() {
        return List.of(
            Map.of("value", "open", "name", "Open"),
            Map.of("value", "resolved", "name", "Resolved"),
            Map.of("value", "wontfix", "name", "Will Not Fix"),
            Map.of("value", "invalid", "name", "Invalid"),
            Map.of("value", "duplicate", "name", "Duplicate"),
            Map.of("value", "spite", "name", "Spite")
        );
    }

    public List<Map<String, Object>> getAvailableSubtypes() {
        return List.of(
            Map.of("value", "default", "name", "Task"),
            Map.of("value", "bug", "name", "Bug"),
            Map.of("value", "defect", "name", "Defect"),
            Map.of("value", "feature", "name", "Feature"),
            Map.of("value", "businessrequest", "name", "Epic"),
            Map.of("value", "issue", "name", "Issue"),
            Map.of("value", "general", "name", "General"),
            Map.of("value", "tech", "name", "Tech"),
            Map.of("value", "design", "name", "Design"),
            Map.of("value", "change", "name", "Change")
        );
    }


    /**
     * Map MeterSphere severity to Phabricator priority keyword
     * @param msSeverity MeterSphere severity (e.g., P0, P1, P2, P3, Critical, High, Medium, Low)
     * @return Phabricator priority keyword
     */
    public String mapSeverityToPriority(String msSeverity) {
        if (msSeverity == null) return "normal";
        String upper = msSeverity.toUpperCase();
        if (upper.startsWith("P0") || upper.contains("CRITICAL")) return "unbreak";
        if (upper.startsWith("P1") || upper.contains("HIGH")) return "high";
        if (upper.startsWith("P2") || upper.contains("MEDIUM")) return "normal";
        if (upper.startsWith("P3") || upper.contains("LOW")) return "low";
        return "wish";
    }

    /**
     * Upload a file to Phabricator
     * @param fileName The name of the file
     * @param data Base64 encoded file content
     * @return Map containing the file PHID
     */
    public Map<String, Object> uploadFile(String fileName, String data) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", fileName);
        params.put("data_base64", data);

        if (config.isDebugMode()) {
            LogUtil.info("[Phabricator DEBUG] file.upload invoked: " + params.toString());
        }

        return callConduit("file.upload", params);
    }

    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (Exception e) {
            LogUtil.error("Failed to close HTTP client", e);
        }
    }
}
