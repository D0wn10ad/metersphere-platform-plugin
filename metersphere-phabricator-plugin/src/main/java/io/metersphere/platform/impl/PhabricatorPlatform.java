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
            constraints.put("projectPHIDs", List.of(projectConfig.getProjectPHID()));
            
            Map<String, Object> result = phabricatorClient.searchTasks(constraints);
            
            if (result != null && result.containsKey("result")) {
                Object resultData = result.get("result");
                if (resultData instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) resultData;
                    Object data = resultMap.get("data");
                    if (data instanceof List) {
                        List<?> dataList = (List<?>) data;
                        for (Object item : dataList) {
                            if (item instanceof Map) {
                                Map<String, Object> task = (Map<String, Object>) item;
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
        
        // Subtype (default to "task")
        String subtype = projectConfig.getDefaultSubtype();
        if (StringUtils.isBlank(subtype)) {
            subtype = "task";
        }
        transactions.add(Map.of(
            "type", "subtype",
            "value", subtype
        ));
        
        // Project PHID
        if (StringUtils.isNotBlank(projectConfig.getProjectPHID())) {
            transactions.add(Map.of(
                "type", "project",
                "value", List.of(projectConfig.getProjectPHID())
            ));
        }
        
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
                                // Store as T{id} format (e.g., T123)
                                request.setPlatformId("T" + id);
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
        
        try {
            // Build PHID from ID - strip "T" prefix if present (e.g., "T123" -> "123")
            String id = platformId.startsWith("T") ? platformId.substring(1) : platformId;
            String phid = "PHID-TASK-" + id;
            Map<String, Object> result = phabricatorClient.editTask(phid, transactions);
            
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
                                request.setPlatformId("T" + newId);
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
            // Build PHID from ID - strip "T" prefix if present (e.g., "T123" -> "123")
            String id = platformId.startsWith("T") ? platformId.substring(1) : platformId;
            String phid = "PHID-TASK-" + id;
            
            // Close the issue instead of deleting (Phabricator doesn't support hard delete)
            List<Map<String, Object>> transactions = new ArrayList<>();
            transactions.add(Map.of(
                "type", "status",
                "value", "resolved"
            ));
            transactions.add(Map.of(
                "type", "comment",
                "value", "Closed by MeterSphere"
            ));
            
            phabricatorClient.editTask(phid, transactions);
            
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
                
                Map<String, Object> constraints = new HashMap<>();
                constraints.put("ids", List.of(Integer.parseInt(id)));
                
                Map<String, Object> searchResult = phabricatorClient.searchTasks(constraints);
                
                if (searchResult != null && searchResult.containsKey("result")) {
                    Object resultData = searchResult.get("result");
                    if (resultData instanceof Map) {
                        Map<String, Object> resultMap = (Map<String, Object>) resultData;
                        Object data = resultMap.get("data");
                        if (data instanceof List && !((List<?>) data).isEmpty()) {
                            Map<String, Object> task = (Map<String, Object>) ((List<?>) data).get(0);
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
            
            Map<String, Object> result = phabricatorClient.searchUsers(constraints);
            
            if (result != null && result.containsKey("result")) {
                Object resultData = result.get("result");
                if (resultData instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) resultData;
                    Object data = resultMap.get("data");
                    if (data instanceof List) {
                        List<?> dataList = (List<?>) data;
                        for (Object item : dataList) {
                            if (item instanceof Map) {
                                Map<String, Object> user = (Map<String, Object>) item;
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
        
        // Phabricator Maniphest supports subtypes, but they're not strictly enforced
        // Return common subtypes as issue type options
        String[] subtypes = {"task", "bug", "feature", "improvement", "epic"};
        for (String subtype : subtypes) {
            SelectOption option = new SelectOption(
                subtype.substring(0, 1).toUpperCase() + subtype.substring(1),
                subtype
            );
            options.add(option);
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
}
