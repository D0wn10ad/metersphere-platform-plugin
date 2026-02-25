package io.metersphere.platform.client;

import io.metersphere.platform.domain.PhabricatorConfig;
import io.metersphere.plugin.exception.MSPluginException;
import io.metersphere.plugin.utils.JSON;
import io.metersphere.plugin.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hc.core5.util.Timeout;

public class PhabricatorClient {
    private PhabricatorConfig config;
    private CloseableHttpClient httpClient;

    public PhabricatorClient() {
    }

    public PhabricatorClient(PhabricatorConfig config) {
        this.config = config;
        this.httpClient = createHttpClient();
    }

    public void setConfig(PhabricatorConfig config) {
        this.config = config;
        this.httpClient = createHttpClient();
    }

    private CloseableHttpClient createHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(20)
            .setMaxConnPerRoute(10)
            .build();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(30))
            .setResponseTimeout(Timeout.ofSeconds(120))
            .setConnectionRequestTimeout(Timeout.ofSeconds(30))
            .build();

        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> callConduit(String method, Map<String, Object> params) {
        return callConduitWithRetry(method, params, 3, 1000);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callConduitWithRetry(String method, Map<String, Object> params, int maxRetries, long initialDelayMs) {
        if (config == null || config.getUrl() == null) {
            MSPluginException.throwException("Phabricator configuration is not set");
            return null;
        }

        String url = config.getUrl() + "/api/" + method;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> apiParams = params != null ? new HashMap<>(params) : new HashMap<>();
                
                Map<String, Object> conduitParams = new HashMap<>();
                conduitParams.put("token", config.getApiToken());
                apiParams.put("__conduit__", conduitParams);
                
                String jsonParams = JSON.toJSONString(apiParams);
                String formBody = "params=" + jsonParams + "&output=json";

                if (config.isDebugMode()) {
                    LogUtil.info("[Phabricator DEBUG] Calling API: " + method + " (attempt " + (attempt + 1) + ")");
                    LogUtil.info("[Phabricator DEBUG] URL: " + url);
                    LogUtil.info("[Phabricator DEBUG] Request: " + formBody);
                }

                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setEntity(new StringEntity(formBody, ContentType.APPLICATION_FORM_URLENCODED));

                try (org.apache.hc.core5.http.ClassicHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getCode();
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                    if (statusCode >= 400) {
                        throw new RuntimeException("HTTP error " + statusCode + " from Phabricator: " + responseBody);
                    }

                    if (config.isDebugMode()) {
                        String truncatedResponse = responseBody.length() > 2000 
                            ? responseBody.substring(0, 2000) + "... [truncated]" 
                            : responseBody;
                        LogUtil.info("[Phabricator DEBUG] Response: " + truncatedResponse);
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
                }
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
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);
        params.put("attachments", Map.of("projects", true));

        return searchWithPagination("maniphest.search", params);
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
            Map.of("value", "task", "name", "Task"),
            Map.of("value", "story", "name", "Story"),
            Map.of("value", "epic", "name", "Epic"),
            Map.of("value", "bug", "name", "Bug"),
            Map.of("value", "feature", "name", "Feature")
        );
    }

    public List<Map<String, Object>> getAvailablePriorities() {
        return List.of(
            Map.of("value", "100", "name", "Unbreak Now!"),
            Map.of("value", "90", "name", "Needs Triage"),
            Map.of("value", "80", "name", "High"),
            Map.of("value", "50", "name", "Normal"),
            Map.of("value", "25", "name", "Low"),
            Map.of("value", "0", "name", "Wishlist")
        );
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
