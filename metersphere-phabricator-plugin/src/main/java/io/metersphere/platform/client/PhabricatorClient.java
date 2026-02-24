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
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> callConduit(String method, Map<String, Object> params) {
        if (config == null || config.getUrl() == null) {
            MSPluginException.throwException("Phabricator configuration is not set");
            return null;
        }

        String url = config.getUrl() + "/api/" + method;

        try {
            Map<String, Object> apiParams = params != null ? new HashMap<>(params) : new HashMap<>();
            
            Map<String, Object> conduitParams = new HashMap<>();
            conduitParams.put("token", config.getApiToken());
            apiParams.put("__conduit__", conduitParams);
            
            String jsonParams = JSON.toJSONString(apiParams);
            String formBody = "params=" + jsonParams + "&output=json";

            if (config.isDebugMode()) {
                LogUtil.info("[Phabricator DEBUG] Calling API: " + method);
                LogUtil.info("[Phabricator DEBUG] URL: " + url);
                LogUtil.info("[Phabricator DEBUG] Request: " + formBody);
            }

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setEntity(new StringEntity(formBody, ContentType.APPLICATION_FORM_URLENCODED));

            try (org.apache.hc.core5.http.ClassicHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (config.isDebugMode()) {
                    String truncatedResponse = responseBody.length() > 2000 
                        ? responseBody.substring(0, 2000) + "... [truncated]" 
                        : responseBody;
                    LogUtil.info("[Phabricator DEBUG] Response: " + truncatedResponse);
                }

                Map<String, Object> body = JSON.parseObject(responseBody, Map.class);

                if (body != null && body.get("error_code") != null) {
                    MSPluginException.throwException("Phabricator API error: " + body.get("error_info"));
                }

                return body;
            }
        } catch (MSPluginException e) {
            throw e;
        } catch (Exception e) {
            LogUtil.error("Failed to call Conduit method: " + method, e);
            MSPluginException.throwException("Failed to call Phabricator API: " + e.getMessage());
            return null;
        }
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
    public Map<String, Object> searchTasks(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);
        params.put("attachments", Map.of("projects", true));

        return callConduit("maniphest.search", params);
    }

    @SuppressWarnings("unchecked")
    public String getTaskIdByPHID(String phid) {
        if (phid == null || phid.isBlank()) {
            return null;
        }
        
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("phids", List.of(phid));
        
        Map<String, Object> result = searchTasks(constraints);
        
        if (result != null && result.containsKey("result")) {
            Object resultData = result.get("result");
            if (resultData instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) resultData;
                Object data = resultMap.get("data");
                if (data instanceof List && !((List<?>) data).isEmpty()) {
                    Map<String, Object> task = (Map<String, Object>) ((List<?>) data).get(0);
                    Object id = task.get("id");
                    return id != null ? id.toString() : null;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> searchProjects(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);
        params.put("attachments", Map.of("parent", true));

        return callConduit("project.search", params);
    }

    @SuppressWarnings("unchecked")
    public boolean projectExists(String projectPHID) {
        if (StringUtils.isBlank(projectPHID)) {
            return false;
        }
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("phids", List.of(projectPHID));
        Map<String, Object> result = searchProjects(constraints);
        if (result != null && result.containsKey("result")) {
            Object resultData = result.get("result");
            if (resultData instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) resultData;
                Object data = resultMap.get("data");
                if (data instanceof List) {
                    return !((List<?>) data).isEmpty();
                }
            }
        }
        return false;
    }

    public Map<String, Object> getTask(String taskId) {
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("ids", List.of(Integer.parseInt(taskId)));

        Map<String, Object> response = searchTasks(constraints);
        if (response != null && response.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data != null && !data.isEmpty()) {
                return (Map<String, Object>) data.values().iterator().next();
            }
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
    public Map<String, Object> searchUsers(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);

        return callConduit("user.search", params);
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
