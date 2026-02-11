package io.metersphere.platform.impl;

import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.platform.api.AbstractPlatform;
import io.metersphere.platform.api.Platform;
import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.*;
import io.metersphere.platform.dto.PhabricatorMarkupConversion;
import io.metersphere.platform.dto.ProjectResolutionResult;
import io.metersphere.platform.handler.PhabricatorStatusHandler;
import io.metersphere.platform.resolver.PhabricatorProjectResolver;
import io.metersphere.platform.utils.PhabricatorMarkupUtils;
import io.metersphere.plugin.utils.JSON;
import io.metersphere.plugin.exception.MSPluginException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Phabricator platform implementation for MeterSphere
 * Note: This class implements the Platform interface. When building in a real MeterSphere environment,
 * uncomment the extends/implements and  annotations.
 */
public class PhabricatorPlatform /* extends AbstractPlatform implements Platform */ {

    private PhabricatorClient client;
    private PhabricatorProjectResolver projectResolver;
    private PhabricatorStatusHandler statusHandler;
    private PhabricatorMarkupUtils markupUtils;
    private PhabricatorConfig config;
    private PhabricatorProjectConfig projectConfig;

    // 
    public void init(String configuration) {
        if (StringUtils.isNotBlank(configuration)) {
            this.config = JSON.parseObject(configuration, PhabricatorConfig.class);
            this.client = new PhabricatorClient(config);
            this.projectResolver = new PhabricatorProjectResolver(client);
            this.markupUtils = new PhabricatorMarkupUtils(client);
        }
    }

    // 
    public void setProjectConfig(String projectConfig) {
        // super.setProjectConfig(projectConfig);
        if (StringUtils.isNotBlank(projectConfig)) {
            this.projectConfig = JSON.parseObject(projectConfig, PhabricatorProjectConfig.class);
            this.statusHandler = new PhabricatorStatusHandler(this.projectConfig);
        }
    }

    // 
    public IssuesWithBLOBs getIssue(String issueId) {
        if (client == null) {
            throw new RuntimeException("Phabricator client not initialized");
        }

        try {
            Map<String, Object> taskData = client.getTask(issueId);
            if (taskData == null) {
                return null;
            }

            PhabricatorTask task = parseTask(taskData);

            // Check if we should skip closed issues
            if (statusHandler != null && statusHandler.shouldSkipTask(task)) {
                return null;
            }

            // Resolve project mapping
            ProjectResolutionResult projectResult = null;
            if (projectResolver != null && projectConfig != null) {
                projectResult = projectResolver.resolveProject(task, projectConfig);
            }

            // Convert to MeterSphere format
            return convertToMeterSphereIssue(task, projectResult);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get issue from Phabricator: " + e.getMessage(), e);
        }
    }

    
    public List<SelectOption> getProjectOptions(GetOptionRequest request) {
        if (client == null) {
            return new ArrayList<>();
        }

        try {
            Map<String, Object> constraints = new HashMap<>();
            // Fetch active projects
            var response = client.searchProjects(constraints);

            List<SelectOption> options = new ArrayList<>();
            if (response.getData() != null) {
                for (Map.Entry<String, Map<String, Object>> entry : response.getData().entrySet()) {
                    Map<String, Object> fields = (Map<String, Object>) entry.getValue().get("fields");
                    if (fields != null) {
                        SelectOption option = new SelectOption(entry.getKey(), (String) fields.get("name"));
                        options.add(option);
                    }
                }
            }

            return options;

        } catch (Exception e) {
            System.err.println("Failed to get project options: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    
    public void validateProjectConfig(String projectConfig) {
        if (StringUtils.isBlank(projectConfig)) {
            MSPluginException.throwException("Phabricator project configuration is required");
        }

        PhabricatorProjectConfig config = JSON.parseObject(projectConfig, PhabricatorProjectConfig.class);

        if (config.getPrimaryStrategy() == null) {
            MSPluginException.throwException("Primary project mapping strategy is required");
        }

        if (StringUtils.isBlank(config.getDefaultProjectId())) {
            MSPluginException.throwException("Default project is required");
        }
    }

    
    public void validateUserConfig(String userConfig) {
        if (StringUtils.isBlank(userConfig)) {
            MSPluginException.throwException("Phabricator configuration is required");
        }

        PhabricatorConfig cfg = JSON.parseObject(userConfig, PhabricatorConfig.class);

        if (StringUtils.isBlank(cfg.getUrl())) {
            MSPluginException.throwException("Phabricator URL is required");
        }

        if (StringUtils.isBlank(cfg.getApiToken())) {
            MSPluginException.throwException("API Token is required");
        }
    }

    
    public IssuesWithBLOBs addIssue(IssuesUpdateRequest request) {
        if (client == null || projectConfig == null) {
            throw new RuntimeException("Phabricator not properly initialized");
        }

        try {
            List<Map<String, Object>> transactions = new ArrayList<>();

            // Title
            transactions.add(Map.of(
                "type", "title",
                "value", request.getTitle()
            ));

            // Description
            if (StringUtils.isNotBlank(request.getDescription())) {
                String remarkup = markupUtils.markdownToRemarkup(request.getDescription());
                transactions.add(Map.of(
                    "type", "description",
                    "value", remarkup
                ));
            }

            // Subtype
            transactions.add(Map.of(
                "type", "subtype",
                "value", projectConfig.getDefaultSubtype()
            ));

            // Create the task
            Map<String, Object> result = client.editTask(null, transactions);

            // Fetch and return the created issue
            if (result != null && result.containsKey("object")) {
                Map<String, Object> object = (Map<String, Object>) result.get("object");
                String phid = (String) object.get("phid");
                String id = phid.substring(phid.lastIndexOf("-") + 1);
                return getIssue(id);
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create issue in Phabricator: " + e.getMessage(), e);
        }
    }

    
    public IssuesWithBLOBs updateIssue(IssuesUpdateRequest request) {
        if (client == null) {
            throw new RuntimeException("Phabricator client not initialized");
        }

        try {
            String issueId = request.getId();
            IssuesWithBLOBs existingIssue = getIssue(issueId);
            if (existingIssue == null) {
                throw new RuntimeException("Issue not found: " + issueId);
            }

            List<Map<String, Object>> transactions = new ArrayList<>();

            // Title
            if (StringUtils.isNotBlank(request.getTitle())) {
                transactions.add(Map.of(
                    "type", "title",
                    "value", request.getTitle()
                ));
            }

            // Description
            if (StringUtils.isNotBlank(request.getDescription())) {
                String remarkup = markupUtils.markdownToRemarkup(request.getDescription());
                transactions.add(Map.of(
                    "type", "description",
                    "value", remarkup
                ));
            }

            // Status (map from MeterSphere to Phabricator)
            if (StringUtils.isNotBlank(request.getStatus())) {
                String phabStatus = statusHandler.mapToPhabricatorStatus(request.getStatus());
                transactions.add(Map.of(
                    "type", "status",
                    "value", phabStatus
                ));
            }

            if (!transactions.isEmpty()) {
                // Get the PHID from the issue
                String phid = "PHID-TASK-" + issueId;
                client.editTask(phid, transactions);
            }

            return getIssue(issueId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update issue in Phabricator: " + e.getMessage(), e);
        }
    }

    
    public void deleteIssue(String issueId) {
        // Phabricator doesn't support deletion, so we close the issue instead
        if (client == null) {
            throw new RuntimeException("Phabricator client not initialized");
        }

        try {
            List<Map<String, Object>> transactions = List.of(
                Map.of("type", "status", "value", "resolved"),
                Map.of("type", "comment", "value", "Closed by MeterSphere")
            );

            String phid = "PHID-TASK-" + issueId;
            client.editTask(phid, transactions);

        } catch (Exception e) {
            throw new RuntimeException("Failed to close issue in Phabricator: " + e.getMessage(), e);
        }
    }

    
    public void syncIssues(SyncIssuesRequest request) {
        // Implementation for bulk sync
        // This would fetch multiple issues based on project criteria
        throw new UnsupportedOperationException("Bulk sync not yet implemented");
    }

    
    public List<SelectOption> getStatusTransitions(String issueKey) {
        // Return available status transitions
        return List.of(
            new SelectOption("open", "Open"),
            new SelectOption("resolved", "Resolved"),
            new SelectOption("wontfix", "Won't Fix"),
            new SelectOption("invalid", "Invalid"),
            new SelectOption("duplicate", "Duplicate")
        );
    }

    
    public String getProjectId(String projectKey) {
        // Return project ID
        return projectKey;
    }

    /**
     * Get available statuses for configuration UI
     */
    public List<SelectOption> getAvailableStatuses() {
        if (client == null) {
            return new ArrayList<>();
        }

        var statuses = client.getAvailableStatuses();
        List<SelectOption> options = new ArrayList<>();

        for (var status : statuses) {
            SelectOption option = new SelectOption((String) status.get("value"), (String) status.get("name"));
            options.add(option);
        }

        return options;
    }

    /**
     * Get available subtypes for configuration UI
     */
    public List<SelectOption> getAvailableSubtypes() {
        if (client == null) {
            return new ArrayList<>();
        }

        var subtypes = client.getAvailableSubtypes();
        List<SelectOption> options = new ArrayList<>();

        for (var subtype : subtypes) {
            SelectOption option = new SelectOption((String) subtype.get("value"), (String) subtype.get("name"));
            options.add(option);
        }

        return options;
    }

    @SuppressWarnings("unchecked")
    private PhabricatorTask parseTask(Map<String, Object> taskData) {
        PhabricatorTask task = new PhabricatorTask();

        task.setPhid((String) taskData.get("phid"));
        task.setId(String.valueOf(taskData.get("id")));

        Map<String, Object> fields = (Map<String, Object>) taskData.get("fields");
        if (fields != null) {
            task.setName((String) fields.get("name"));
            task.setSubtype((String) fields.get("subtype"));

            Map<String, Object> desc = (Map<String, Object>) fields.get("description");
            if (desc != null) {
                task.setDescription((String) desc.get("raw"));
            }

            task.setAuthorPHID((String) fields.get("authorPHID"));
            task.setOwnerPHID((String) fields.get("ownerPHID"));

            Map<String, Object> status = (Map<String, Object>) fields.get("status");
            if (status != null) {
                task.setStatus((String) status.get("value"));
            }

            Map<String, Object> priority = (Map<String, Object>) fields.get("priority");
            if (priority != null) {
                Object prioValue = priority.get("value");
                if (prioValue instanceof Number) {
                    task.setPriority(((Number) prioValue).intValue());
                }
            }
        }

        // Parse project attachments
        Map<String, Object> attachments = (Map<String, Object>) taskData.get("attachments");
        if (attachments != null) {
            Map<String, Object> projects = (Map<String, Object>) attachments.get("projects");
            if (projects != null) {
                List<String> projectPHIDs = (List<String>) projects.get("projectPHIDs");
                task.setProjectPHIDs(projectPHIDs);
            }
        }

        return task;
    }

    private IssuesWithBLOBs convertToMeterSphereIssue(PhabricatorTask task, ProjectResolutionResult projectResult) {
        IssuesWithBLOBs issue = new IssuesWithBLOBs();

        issue.setId(task.getId());
        issue.setTitle(task.getTitle());

        // Convert description
        if (StringUtils.isNotBlank(task.getDescription())) {
            PhabricatorMarkupConversion conversion = markupUtils.convertRemarkupToMarkdown(
                task.getDescription(), "maniphest"
            );
            issue.setDescription(conversion.getConvertedMarkdown());
        }

        // Map status
        if (statusHandler != null) {
            issue.setStatus(statusHandler.mapToMeterSphereStatus(task.getStatus()));
        } else {
            issue.setStatus("Open");
        }

        // Set project
        if (projectResult != null) {
            issue.setProjectId(projectResult.getFinalProjectId());

            // Set custom fields with project tags
            if (projectResult.getProjectTags() != null && !projectResult.getProjectTags().isEmpty()) {
                String tagsJson = JSON.toJSONString(projectResult.getProjectTags());
                issue.setCustomFields(tagsJson);
            }
        }

        // Set platform
        issue.setPlatform("Phabricator");

        // Set resource type based on subtype
        if (StringUtils.isNotBlank(task.getSubtype())) {
            Map<String, String> subtypeMapping = projectConfig != null ?
                projectConfig.getSubtypeMapping() : null;

            if (subtypeMapping != null && subtypeMapping.containsKey(task.getSubtype())) {
                issue.setResourceType(subtypeMapping.get(task.getSubtype()));
            } else {
                issue.setResourceType(task.getSubtype());
            }
        }

        return issue;
    }
}