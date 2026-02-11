package io.metersphere.platform.client;

import io.metersphere.platform.api.BaseClient;
import io.metersphere.platform.domain.PhabricatorConfig;
import io.metersphere.platform.response.PhabricatorSearchResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phabricator Conduit API client
 */
public class PhabricatorClient extends BaseClient {

    private PhabricatorConfig config;
    private RestTemplate restTemplate;

    public PhabricatorClient(PhabricatorConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    protected void setAuthHeaders(HttpHeaders headers) {
        headers.set("Authorization", "Bearer " + config.getApiToken());
        headers.set("Content-Type", "application/json");
    }

    /**
     * Call any Conduit API method
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> callConduit(String method, Map<String, Object> params) {
        String url = config.getUrl() + "/api/" + method;

        HttpHeaders headers = new HttpHeaders();
        setAuthHeaders(headers);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("error_code")) {
            throw new RuntimeException("Phabricator API error: " + body.get("error_info"));
        }

        return body;
    }

    /**
     * Search for Maniphest tasks
     */
    @SuppressWarnings("unchecked")
    public PhabricatorSearchResponse<Map<String, Object>> searchTasks(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);
        params.put("attachments", Map.of("projects", true));

        Map<String, Object> response = callConduit("maniphest.search", params);

        PhabricatorSearchResponse<Map<String, Object>> result = new PhabricatorSearchResponse<>();
        result.setData((Map<String, Map<String, Object>>) response.get("data"));
        result.setMaps((Map<String, Object>) response.get("maps"));
        result.setQuery((Map<String, Object>) response.get("query"));

        return result;
    }

    /**
     * Search for projects
     */
    @SuppressWarnings("unchecked")
    public PhabricatorSearchResponse<Map<String, Object>> searchProjects(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);
        params.put("attachments", Map.of("parent", true));

        Map<String, Object> response = callConduit("project.search", params);

        PhabricatorSearchResponse<Map<String, Object>> result = new PhabricatorSearchResponse<>();
        result.setData((Map<String, Map<String, Object>>) response.get("data"));
        return result;
    }

    /**
     * Get a single task by ID
     */
    public Map<String, Object> getTask(String taskId) {
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("ids", List.of(Integer.parseInt(taskId)));

        PhabricatorSearchResponse<Map<String, Object>> response = searchTasks(constraints);
        if (response.getData() != null && !response.getData().isEmpty()) {
            return response.getData().values().iterator().next();
        }
        return null;
    }

    /**
     * Edit/create a task
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> editTask(String objectIdentifier, List<Map<String, Object>> transactions) {
        Map<String, Object> params = new HashMap<>();
        if (objectIdentifier != null) {
            params.put("objectIdentifier", objectIdentifier);
        }
        params.put("transactions", transactions);

        Map<String, Object> response = callConduit("maniphest.edit", params);
        return response;
    }

    /**
     * Process remarkup to HTML
     */
    @SuppressWarnings("unchecked")
    public String processRemarkup(String content, String context) {
        Map<String, Object> params = new HashMap<>();
        params.put("context", context);
        params.put("contents", List.of(content));

        Map<String, Object> response = callConduit("remarkup.process", params);
        List<String> contents = (List<String>) response.get("content");
        if (contents != null && !contents.isEmpty()) {
            return contents.get(0);
        }
        return content;
    }

    /**
     * Search for users
     */
    @SuppressWarnings("unchecked")
    public PhabricatorSearchResponse<Map<String, Object>> searchUsers(Map<String, Object> constraints) {
        Map<String, Object> params = new HashMap<>();
        params.put("constraints", constraints);

        Map<String, Object> response = callConduit("user.search", params);

        PhabricatorSearchResponse<Map<String, Object>> result = new PhabricatorSearchResponse<>();
        result.setData((Map<String, Map<String, Object>>) response.get("data"));
        return result;
    }

    /**
     * Get available Maniphest statuses
     */
    public List<Map<String, Object>> getAvailableStatuses() {
        // In Phabricator, statuses are configured per-installation
        // For now, return default statuses
        return List.of(
            Map.of("value", "open", "name", "Open"),
            Map.of("value", "resolved", "name", "Resolved"),
            Map.of("value", "wontfix", "name", "Won't Fix"),
            Map.of("value", "invalid", "name", "Invalid"),
            Map.of("value", "spite", "name", "Spite"),
            Map.of("value", "duplicate", "name", "Duplicate")
        );
    }

    /**
     * Get available task subtypes
     */
    public List<Map<String, Object>> getAvailableSubtypes() {
        // These are the common Maniphest subtypes
        return List.of(
            Map.of("value", "task", "name", "Task"),
            Map.of("value", "story", "name", "Story"),
            Map.of("value", "epic", "name", "Epic"),
            Map.of("value", "bug", "name", "Bug"),
            Map.of("value", "feature", "name", "Feature")
        );
    }
}