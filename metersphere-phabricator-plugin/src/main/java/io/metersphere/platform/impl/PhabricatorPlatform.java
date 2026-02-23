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
    public List<DemandDTO> getDemands(String projectConfig) {
        return new ArrayList<>();
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
        return new SyncIssuesResult();
    }

    @Override
    public List<PlatformCustomFieldItemDTO> getThirdPartCustomField(String projectConfig) {
        return new ArrayList<>();
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
