package io.metersphere.platform.impl;

import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.platform.api.AbstractPlatform;
import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.*;
import io.metersphere.platform.utils.PhabricatorMarkupUtils;
import io.metersphere.plugin.exception.MSPluginException;
import io.metersphere.plugin.utils.JSON;
import io.metersphere.plugin.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class PhabricatorPlatform extends AbstractPlatform {

    protected PhabricatorClient phabricatorClient;

    public PhabricatorPlatform(PlatformRequest request) {
        super.key = PhabricatorPlatformMetaInfo.KEY;
        super.request = request;
        phabricatorClient = new PhabricatorClient();
    }

    private void setConfig() {
        PhabricatorConfig config = getIntegrationConfig();
        validateConfig(config);
        phabricatorClient.setConfig(config);
    }

    private void validateConfig(PhabricatorConfig config) {
        if (config == null) {
            MSPluginException.throwException("phabricator config is null");
        }
    }

    public PhabricatorConfig getIntegrationConfig() {
        return getIntegrationConfig(PhabricatorConfig.class);
    }

    private PhabricatorProjectConfig getProjectConfig(String configStr) {
        if (StringUtils.isBlank(configStr)) {
            MSPluginException.throwException("请在项目中添加项目配置！");
        }
        return JSON.parseObject(configStr, PhabricatorProjectConfig.class);
    }

    @Override
    public List<DemandDTO> getDemands(String projectConfigStr) {
        List<DemandDTO> demands = new ArrayList<>();
        
        try {
            setConfig();
            PhabricatorProjectConfig projectConfig = getProjectConfig(projectConfigStr);
            
            if (StringUtils.isBlank(projectConfig.getProjectPHID())) {
                return demands;
            }
            
            Map<String, Object> constraints = new HashMap<>();
            constraints.put("projects", List.of(projectConfig.getProjectPHID()));
            
            List<Map<String, Object>> results = phabricatorClient.searchTasks(constraints);
            
            if (results != null) {
                for (Map<String, Object> task : results) {
                    Object id = task.get("id");
                    Object fields = task.get("fields");
                    if (fields instanceof Map) {
                        Map<String, Object> taskFields = (Map<String, Object>) fields;
                        Object nameObj = taskFields.get("name");
                        if (nameObj != null) {
                            DemandDTO demand = new DemandDTO();
                            demand.setId(id != null ? "T" + id : null);
                            demand.setName(nameObj.toString());
                            demand.setPlatform(PhabricatorPlatformMetaInfo.KEY);
                            demands.add(demand);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error("Failed to get demands from Phabricator", e);
        }
        
        return demands;
    }

    @Override
    public IssuesWithBLOBs addIssue(PlatformIssuesUpdateRequest request) {
        setConfig();
        
        PhabricatorProjectConfig projectConfig = getProjectConfig(request.getProjectConfig());
        
        // Debug logging when debug mode is enabled
        PhabricatorConfig integrationConfig = getIntegrationConfig();
        if (integrationConfig != null && integrationConfig.isDebugMode()) {
            LogUtil.info("[Phabricator DEBUG] request.getId(): " + request.getId());
            LogUtil.info("[Phabricator DEBUG] getIntegrationConfig(): " + JSON.toJSONString(integrationConfig));
            LogUtil.info("[Phabricator DEBUG] getProjectConfig(): " + JSON.toJSONString(projectConfig));
            LogUtil.info("[Phabricator DEBUG] getCustomFieldList(): " + JSON.toJSONString(request.getCustomFieldList()));
        }
        PhabricatorMarkupUtils markupUtils = new PhabricatorMarkupUtils(phabricatorClient);
        
        List<Map<String, Object>> transactions = new ArrayList<>();
        
        // Title (required)
        if (StringUtils.isNotBlank(request.getTitle())) {
            transactions.add(Map.of(
                "type", "title",
                "value", request.getTitle()
            ));
        }
        
        // Description (convert Markdown to Remarkup)
        if (StringUtils.isNotBlank(request.getDescription())) {
            String remarkup = markupUtils.markdownToRemarkup(request.getDescription());
            transactions.add(Map.of(
                "type", "description",
                "value", remarkup
            ));
        }
        
        // Subtype (default to "bug" for new tasks)
        String subtype = projectConfig.getDefaultSubtype();
        if (StringUtils.isBlank(subtype)) {
            subtype = "bug";
        }
        transactions.add(Map.of(
            "type", "subtype",
            "value", subtype
        ));
        
        // Project PHID - use projects.add to add project tag
        if (StringUtils.isNotBlank(projectConfig.getProjectPHID())) {
            transactions.add(Map.of(
                "type", "projects.add",
                "value", List.of(projectConfig.getProjectPHID())
            ));
        }

        // Priority - map from severity custom field
        String severity = getSeverityFromRequest(request);
        String priority = phabricatorClient.mapSeverityToPriority(severity);
        transactions.add(Map.of(
            "type", "priority",
            "value", priority
        ));
        
        try {
            Map<String, Object> result = phabricatorClient.editTask(null, transactions);
            
            if (result != null && result.containsKey("result")) {
                Object resultData = result.get("result");
                if (resultData instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) resultData;
                    Object objectData = resultMap.get("object");
                    if (objectData instanceof Map) {
                        Map<String, Object> object = (Map<String, Object>) objectData;
                        String phid = (String) object.get("phid");
                        if (phid != null) {
                            // Get numeric ID from PHID using search
                            String id = phabricatorClient.getTaskIdByPHID(phid);
                            if (id != null) {
                                // Store as numeric ID directly (matches maniphest.edit expectation)
                                request.setPlatformId(id); // Store as numeric ID directly
                                
                                // Add MS URL as first comment
                                try {
                                    PhabricatorConfig config = getIntegrationConfig();
                                    String msUrl = config != null ? config.getMsUrl() : null;
                                    if (StringUtils.isNotBlank(msUrl) && id != null) {
                                        String msInternalId = request.getId();
                                        String comment = "[[" + msUrl + "/#/track/issue?id=" + msInternalId + " | View in MeterSphere]]";
                                        List<Map<String, Object>> commentTransactions = new ArrayList<>();
                                        commentTransactions.add(Map.of(
                                            "type", "comment",
                                            "value", comment
                                        ));
                                        phabricatorClient.editTask(id, commentTransactions);
                                    }
                                } catch (Exception e) {
                                    // Don't fail the main task if comment fails
                                    LogUtil.warn("Failed to add MS URL comment: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            
            request.setPlatformStatus("open");
            
        } catch (Exception e) {
            LogUtil.error("Failed to add issue to Phabricator", e);
            MSPluginException.throwException("创建问题失败: " + e.getMessage());
        }
        
        return request;
    }

    @Override
    public IssuesWithBLOBs updateIssue(PlatformIssuesUpdateRequest request) {
        setConfig();
        
        String platformId = request.getPlatformId();
        if (StringUtils.isBlank(platformId)) {
            MSPluginException.throwException("平台ID不能为空");
        }
        
        PhabricatorMarkupUtils markupUtils = new PhabricatorMarkupUtils(phabricatorClient);
        
        List<Map<String, Object>> transactions = new ArrayList<>();
        
        // Title
        if (StringUtils.isNotBlank(request.getTitle())) {
            transactions.add(Map.of(
                "type", "title",
                "value", request.getTitle()
            ));
        }
        
        // Description (convert Markdown to Remarkup)
        if (StringUtils.isNotBlank(request.getDescription())) {
            String remarkup = markupUtils.markdownToRemarkup(request.getDescription());
            transactions.add(Map.of(
                "type", "description",
                "value", remarkup
            ));
        }
        
        // Status
        if (StringUtils.isNotBlank(request.getPlatformStatus())) {
            String phabStatus = request.getPlatformStatus();
            transactions.add(Map.of(
                "type", "status",
                "value", phabStatus
            ));
        }
        
        // Subtype - preserve existing or default to "bug"
        String id = platformId.startsWith("T") ? platformId.substring(1) : platformId;
            String humanReadableId = "T" + id;
        String subtypeToUse = "bug";
        try {
            Map<String, Object> currentTask = phabricatorClient.getTask(id);
            if (currentTask != null) {
                Object fields = currentTask.get("fields");
                if (fields instanceof Map) {
                    Object subtypeObj = ((Map<String, Object>) fields).get("subtype");
                    if (subtypeObj instanceof Map) {
                        String existingSubtype = (String) ((Map<String, Object>) subtypeObj).get("value");
                        if (StringUtils.isNotBlank(existingSubtype)) {
                            subtypeToUse = existingSubtype;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.warn("Failed to get current task subtype, using default 'bug': " + e.getMessage());
        }
        transactions.add(Map.of(
            "type", "subtype",
            "value", subtypeToUse
        ));
        
        try {
            // Use numeric ID directly with maniphest.edit
            Map<String, Object> result = phabricatorClient.editTask(id, transactions);
            
            if (result != null && result.containsKey("result")) {
                Object resultData = result.get("result");
                if (resultData instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) resultData;
                    Object objectData = resultMap.get("object");
                    if (objectData instanceof Map) {
                        Map<String, Object> object = (Map<String, Object>) objectData;
                        String newPhid = (String) object.get("phid");
                        if (newPhid != null) {
                            // Get numeric ID from PHID using search
                            String newId = phabricatorClient.getTaskIdByPHID(newPhid);
                            if (newId != null) {
                                request.setPlatformId(id); // Keep existing ID
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LogUtil.error("Failed to update issue in Phabricator", e);
            MSPluginException.throwException("更新问题失败: " + e.getMessage());
        }
        
        return request;
    }

    @Override
    public void deleteIssue(String platformId) {
        setConfig();
        
        if (StringUtils.isBlank(platformId)) {
            MSPluginException.throwException("平台ID不能为空");
        }
        
        try {
            // Use numeric ID directly with maniphest.edit
            String id = platformId.startsWith("T") ? platformId.substring(1) : platformId;
            String humanReadableId = "T" + id;
            
            // Close the issue instead of deleting (Phabricator doesn't support hard delete)
            List<Map<String, Object>> transactions = new ArrayList<>();
            transactions.add(Map.of(
                "type", "status",
                "value", "resolved"
            ));
            transactions.add(Map.of(
                "type", "comment",
                "value", "Closed by MeterSphere (" + humanReadableId + ")"
            ));
            
            phabricatorClient.editTask(id, transactions);
            
        } catch (Exception e) {
            LogUtil.error("Failed to delete issue in Phabricator", e);
            MSPluginException.throwException("删除问题失败: " + e.getMessage());
        }
    }

    @Override
    public void validateIntegrationConfig() {
        setConfig();
        phabricatorClient.testConnection();
    }

    @Override
    public void validateProjectConfig(String projectConfigStr) {
        PhabricatorProjectConfig config = getProjectConfig(projectConfigStr);
        if (config == null) {
            MSPluginException.throwException("请在项目中添加项目配置！");
        }
        if (StringUtils.isBlank(config.getProjectPHID()) && StringUtils.isBlank(config.getDefaultProjectId())) {
            MSPluginException.throwException("请配置项目PHID或默认项目ID！");
        }
        if (StringUtils.isNotBlank(config.getProjectPHID())) {
            PhabricatorConfig integrationConfig = getIntegrationConfig();
            if (integrationConfig != null) {
                PhabricatorClient tempClient = new PhabricatorClient(integrationConfig);
                if (!tempClient.projectExists(config.getProjectPHID())) {
                    MSPluginException.throwException("项目PHID不存在！");
                }
                tempClient.close();
            }
        }
    }

    @Override
    public void validateUserConfig(String userConfig) {
        if (StringUtils.isBlank(userConfig)) {
            return;
        }
        PhabricatorConfig config = JSON.parseObject(userConfig, PhabricatorConfig.class);
        if (config != null && StringUtils.isNotBlank(config.getApiToken())) {
            PhabricatorClient testClient = new PhabricatorClient(config);
            testClient.testConnection();
            testClient.close();
        }
    }

    @Override
    public boolean isAttachmentUploadSupport() {
        return true;
    }

    @Override
    public SyncIssuesResult syncIssues(SyncIssuesRequest request) {
        setConfig();
        
        SyncIssuesResult result = new SyncIssuesResult();
        PhabricatorMarkupUtils markupUtils = new PhabricatorMarkupUtils(phabricatorClient);
        
        List<PlatformIssuesDTO> issues = request.getIssues();
        if (issues == null || issues.isEmpty()) {
            return result;
        }
        
        for (PlatformIssuesDTO issue : issues) {
            String platformId = issue.getPlatformId();
            if (StringUtils.isBlank(platformId)) {
                continue;
            }
            
            try {
                // Strip "T" prefix if present
                String id = platformId.startsWith("T") ? platformId.substring(1) : platformId;
            String humanReadableId = "T" + id;
                
                Map<String, Object> constraints = new HashMap<>();
                constraints.put("ids", List.of(Integer.parseInt(id)));
                
                List<Map<String, Object>> results = phabricatorClient.searchTasks(constraints);
                
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> task = results.get(0);
                    Object fields = task.get("fields");
                    
                    if (fields instanceof Map) {
                        Map<String, Object> taskFields = (Map<String, Object>) fields;
                        
                        PlatformIssuesDTO updatedIssue = new PlatformIssuesDTO();
                        updatedIssue.setPlatformId(platformId);
                        
                        // Title
                        Object nameObj = taskFields.get("name");
                        if (nameObj != null) {
                            updatedIssue.setTitle(nameObj.toString());
                        }
                        
                        // Description - convert from Remarkup to Markdown
                        Object descObj = taskFields.get("description");
                        if (descObj instanceof Map) {
                            Map<String, Object> descMap = (Map<String, Object>) descObj;
                            Object rawDesc = descMap.get("raw");
                            if (rawDesc != null) {
                                String markdown = markupUtils.remarkupToMarkdown(rawDesc.toString(), "maniphest");
                                updatedIssue.setDescription(markdown);
                            }
                        }
                        
                        // Status
                        Object statusObj = taskFields.get("status");
                        if (statusObj instanceof Map) {
                            Map<String, Object> statusMap = (Map<String, Object>) statusObj;
                            Object statusValue = statusMap.get("value");
                            if (statusValue != null) {
                                updatedIssue.setPlatformStatus(statusValue.toString());
                            }
                        }
                        
                        result.getUpdateIssues().add(updatedIssue);
                    }
                }
            } catch (Exception e) {
                LogUtil.error("Failed to sync issue " + platformId + " from Phabricator", e);
            }
        }
        
        return result;
    }

    @Override
    public List<PlatformCustomFieldItemDTO> getThirdPartCustomField(String projectConfig) {
        return new ArrayList<>();
    }

    @Override
    public List<SelectOption> getFormOptions(GetOptionRequest request) {
        String method = request.getOptionMethod();
        if (StringUtils.isBlank(method)) {
            return new ArrayList<>();
        }
        
        try {
            return switch (method) {
                case "getUserSearchOptions" -> getUserSearchOptions(request);
                case "getIssueTypes" -> getIssueTypes(request);
                default -> new ArrayList<>();
            };
        } catch (Exception e) {
            LogUtil.error("Error getting form options for method: " + method, e);
            return new ArrayList<>();
        }
    }

    public List<SelectOption> getUserSearchOptions(GetOptionRequest request) {
        List<SelectOption> options = new ArrayList<>();
        
        try {
            setConfig();
            
            Map<String, Object> constraints = new HashMap<>();
            if (StringUtils.isNotBlank(request.getQuery())) {
                constraints.put("nameLike", request.getQuery());
            }
            
            List<Map<String, Object>> results = phabricatorClient.searchUsers(constraints);
            
            if (results != null) {
                for (Map<String, Object> user : results) {
                    Object fields = user.get("fields");
                    if (fields instanceof Map) {
                        Map<String, Object> userFields = (Map<String, Object>) fields;
                        Object userName = userFields.get("userName");
                        Object realName = userFields.get("realName");
                        Object phid = user.get("phid");
                        
                        String displayName = userName != null ? userName.toString() : "";
                        if (realName != null && !realName.toString().isBlank()) {
                            displayName += " (" + realName.toString() + ")";
                        }
                        
                        SelectOption option = new SelectOption(displayName, phid != null ? phid.toString() : "");
                        options.add(option);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error("Failed to get user search options", e);
        }
        
        return options;
    }

    public List<SelectOption> getIssueTypes(GetOptionRequest request) {
        List<SelectOption> options = new ArrayList<>();
        
        List<Map<String, Object>> subtypes = phabricatorClient.getAvailableSubtypes();
        for (Map<String, Object> subtype : subtypes) {
            String value = (String) subtype.get("value");
            String name = (String) subtype.get("name");
            options.add(new SelectOption(name, value));
        }
        
        return options;
    }

    @Override
    public List<PlatformStatusDTO> getStatusList(String projectConfig) {
        List<PlatformStatusDTO> statuses = new ArrayList<>();
        List<Map<String, Object>> availableStatuses = phabricatorClient.getAvailableStatuses();
        
        for (Map<String, Object> status : availableStatuses) {
            PlatformStatusDTO dto = new PlatformStatusDTO();
            dto.setValue((String) status.get("value"));
            dto.setLabel((String) status.get("name"));
            statuses.add(dto);
        }
        
        return statuses;
    }

    @Override
    public void syncIssuesAttachment(SyncIssuesAttachmentRequest request) {
    }

    /**
     * Get severity from request custom fields
     * @param request Platform issues update request
     * @return severity value or null
     */
    private String getSeverityFromRequest(PlatformIssuesUpdateRequest request) {
        if (request == null) {
            return null;
        }
        List<PlatformCustomFieldItemDTO> customFields = request.getCustomFieldList();
        if (customFields == null || customFields.isEmpty()) {
            return null;
        }
        for (PlatformCustomFieldItemDTO field : customFields) {
            String fieldName = field.getName();
            if (fieldName != null && fieldName.toLowerCase().contains("severity")) {
                Object value = field.getValue();
                if (value != null) {
                    return value.toString();
                }
            }
        }
        return null;
    }
}
