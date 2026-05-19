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
import java.io.File;
import java.nio.file.Files;

import java.util.*;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                            demand.setName("T" + id + " " + nameObj.toString());
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
        
        // Generate UUID for new issues if not present
        if (StringUtils.isBlank(request.getId())) {
            request.setId(UUID.randomUUID().toString());
        }

        PhabricatorConfig integrationConfig = getIntegrationConfig();

        String msUrl = integrationConfig != null ? integrationConfig.getMsUrl() : null;
        if (msUrl != null && msUrl.endsWith("/")) {
            msUrl = msUrl.substring(0, msUrl.length() - 1);
        }

        // Debug logging when debug mode is enabled
        if (integrationConfig != null && integrationConfig.isDebugMode()) {
            LogUtil.info("[Phabricator DEBUG] request.getId(): " + request.getId());
            LogUtil.info("[Phabricator DEBUG] getIntegrationConfig(): " + JSON.toJSONString(integrationConfig).replaceAll("\"apiToken\":\"[^\"]+\"", "\"apiToken\":\"***MASKED***\""));
            LogUtil.info("[Phabricator DEBUG] getProjectConfig(): " + JSON.toJSONString(projectConfig));
            LogUtil.info("[Phabricator DEBUG] getCustomFieldList(): " + phabricatorClient.prettyPrintJson(JSON.toJSONString(request.getCustomFieldList())));
        }
        List<Map<String, Object>> transactions = buildCommonTransactions(request, projectConfig, integrationConfig);
        
        // Set env to empty string on creation; actual value updated in follow-up
        if (integrationConfig.isSyncEnvironment()) {
            transactions.removeIf(t -> "custom.igus.env".equals(t.get("type")));
            transactions.add(Map.of("type", "custom.igus.env", "value", ""));
        }
        
        // Subtype
        String subtype = projectConfig.getDefaultSubtype();
        if (StringUtils.isBlank(subtype)) {
            subtype = "bug";
        }
        transactions.add(Map.of("type", "subtype", "value", subtype));
        
        // Custom field: MS URL -> custom.igus.related-to
        if (StringUtils.isNotBlank(msUrl)) {
            String relatedToUrl = msUrl + "/#/track/issue?id=" + request.getId();
            transactions.add(Map.of("type", "custom.igus.related-to", "value", relatedToUrl));
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
                                // Store as numeric ID directly (matches maniphest.edit expectation)
                                request.setPlatformId(id); // Store as numeric ID directly
                                
                                // Add MS URL as first comment
                                try {
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
                                
                                // Update env with actual value from custom fields
                                if (integrationConfig.isSyncEnvironment()) {
                                    try {
                                        String envValue = getEnvFromRequest(request);
                                        if (StringUtils.isNotBlank(envValue)) {
                                            List<Map<String, Object>> envTransactions = new ArrayList<>();
                                            envTransactions.add(Map.of("type", "custom.igus.env", "value", envValue));
                                            phabricatorClient.editTask(id, envTransactions);
                                        }
                                    } catch (Exception e) {
                                        LogUtil.warn("Failed to update env: " + e.getMessage());
                                    }
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
        
        PhabricatorProjectConfig projectConfig = getProjectConfig(request.getProjectConfig());
        PhabricatorConfig integrationConfig = getIntegrationConfig();
        
        List<Map<String, Object>> transactions = buildCommonTransactions(request, projectConfig, integrationConfig);
        
        // Status
        if (StringUtils.isNotBlank(request.getPlatformStatus())) {
            transactions.add(Map.of("type", "status", "value", request.getPlatformStatus()));
        }
        
        // Subtype - preserve existing or default to "bug"
        String id = platformId.startsWith("T") ? platformId.substring(1) : platformId;
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
        transactions.add(Map.of("type", "subtype", "value", subtypeToUse));
        
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
            String id = platformId.startsWith("T") ? platformId.substring(1) : platformId;
            
            // Preserve existing subtype
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
            
            List<Map<String, Object>> transactions = new ArrayList<>();
            transactions.add(Map.of("type", "subtype", "value", subtypeToUse));
            transactions.add(Map.of("type", "status", "value", "resolved"));
            transactions.add(Map.of("type", "comment", "value", "Closed by MeterSphere (T" + id + ")"));
            
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
            if (fieldName != null && (fieldName.toLowerCase().contains("severity") || fieldName.contains("严重程度"))) {
                Object value = field.getValue();
                if (value != null) {
                    return value.toString();
                }
            }
        }
        return null;
    }

    private String getEnvFromRequest(PlatformIssuesUpdateRequest request) {
        List<PlatformCustomFieldItemDTO> customFields = request.getCustomFieldList();
        if (customFields != null) {
            for (PlatformCustomFieldItemDTO field : customFields) {
                String fieldName = field.getName();
                if (fieldName != null && (fieldName.toLowerCase().contains("environment") || fieldName.contains("发现环境"))) {
                    Object envValue = field.getValue();
                    return envValue != null ? envValue.toString() : null;
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> buildCommonTransactions(PlatformIssuesUpdateRequest request, PhabricatorProjectConfig projectConfig, PhabricatorConfig integrationConfig) {
        List<Map<String, Object>> transactions = new ArrayList<>();

        if (StringUtils.isNotBlank(request.getTitle())) {
            transactions.add(Map.of("type", "title", "value", request.getTitle()));
        }

        if (StringUtils.isNotBlank(request.getDescription())) {
            String remarkup = processInlineImages(request.getDescription());
            LogUtil.info("[Phabricator] Converted remarkup: " + remarkup.substring(0, Math.min(500, remarkup.length())));
            transactions.add(Map.of("type", "description", "value", remarkup));
        }

        if (integrationConfig.isSyncPriority()) {
            String severity = getSeverityFromRequest(request);
            String priority = phabricatorClient.mapSeverityToPriority(severity);
            transactions.add(Map.of("type", "priority", "value", priority));
        }

        if (StringUtils.isNotBlank(projectConfig.getProjectPHID())) {
            transactions.add(Map.of("type", "projects.add", "value", List.of(projectConfig.getProjectPHID())));
        }

        if (integrationConfig.isSyncEnvironment()) {
            String envValue = getEnvFromRequest(request);
            if (envValue != null) {
                transactions.add(Map.of("type", "custom.igus.env", "value", envValue));
            }
        }

        return transactions;
    }

    /**
     * Process inline images in MS description: upload to Phabricator and replace with {guid}
     * MS format: ![[/resource/md/get?fileName=xxx.png|Filename.png]]
     * Target format: {guid-from-file-upload}
     */
    public String processInlineImages(String description) {
        if (StringUtils.isBlank(description)) {
            return description;
        }

        // Pattern to find standard Markdown images: ![alt](url)
        Pattern fullPattern = Pattern.compile("!\\[([^\\]]+)\\]\\(([^)]+)\\)");
        Matcher matcher = fullPattern.matcher(description);

        if (!matcher.find()) {
            LogUtil.info("[processInlineImages] NO Images found in description, pattern not matched");
            return new PhabricatorMarkupUtils(phabricatorClient).markdownToRemarkup(description);
        }

        LogUtil.info("[processInlineImages] Images found, processing...");
        matcher = fullPattern.matcher(description);
        String result = description;

        while (matcher.find()) {
            String match = matcher.group();
            String altText = matcher.group(1);  // logo.png
            String url = matcher.group(2);      // /resource/md/get?fileName=b849ad99.png

            LogUtil.info("[processInlineImages] Found match: " + match);
            LogUtil.info("[processInlineImages] Alt: " + altText + ", URL: " + url);

            // Extract filename from URL parameter: ?fileName=b849ad99.png
            int fileNameIndex = url.lastIndexOf("=");
            if (fileNameIndex < 0) {
                LogUtil.warn("[processInlineImages] Unable to extract filename from URL: " + url);
                continue;
            }

            String filename = url.substring(fileNameIndex + 1);
            String imageUrl = url;
            LogUtil.info("[processInlineImages] URL: " + imageUrl + ", Filename: " + filename);

            if (imageUrl == null || !imageUrl.contains("/resource/md/get")) {
                LogUtil.info("[processInlineImages] Not MS resource, skipping");
                continue;
            }

            try {
                LogUtil.info("[processInlineImages] Reading image from local disk: " + filename);
                File imageFile = getRealMdFile(filename);
                if (!imageFile.exists()) {
                    LogUtil.warn("Image file not found: " + imageFile.getAbsolutePath());
                    continue;
                }
                String base64Data = encodeFileToBase64(imageFile);
                if (base64Data == null) {
                    LogUtil.warn("Failed to read image file: " + filename);
                    continue;
                }

                Map<String, Object> uploadResult = phabricatorClient.uploadFile(filename, base64Data);
                if (uploadResult == null) {
                    LogUtil.warn("Failed to upload image: " + filename);
                    continue;
                }

                // Get PHID from upload result
                String phid = (String) uploadResult.get("result");
                if (phid == null) {
                    LogUtil.warn("No PHID returned for image: " + filename);
                    continue;
                }

                // Query phid to get file info (includes GUID like "F29271")
                Map<String, Object> phidParams = new HashMap<>();
                phidParams.put("phids", Collections.singletonList(phid));
                Map<String, Object> phidResponse = phabricatorClient.callConduit("phid.query", phidParams);
                Map<String, Object> phidResult = (Map<String, Object>) phidResponse.get("result");
                
                Map<String, Object> fileInfo = (Map<String, Object>) phidResult.get(phid);
                if (fileInfo == null) {
                    LogUtil.warn("No file info returned for PHID: " + phid);
                    continue;
                }

                String guid = (String) fileInfo.get("name");  // e.g., "F29271"
                if (guid == null) {
                    LogUtil.warn("No guid (name) returned for image: " + filename);
                    continue;
                }

                result = result.replace(match, "{" + guid + "}");
                LogUtil.info("Uploaded image: " + filename + " -> {" + guid + "}");

            } catch (Exception e) {
                LogUtil.error("Failed to process image: " + filename, e);
            }
        }

        LogUtil.info("[processInlineImages] Returning result: " + result.substring(0, Math.min(200, result.length())));
        return new PhabricatorMarkupUtils(phabricatorClient).markdownToRemarkup(result);
    }


    /**
     * Read file and encode to base64
     */
    public String encodeFileToBase64(File file) {
        try {
            byte[] fileData = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileData);
        } catch (Exception e) {
            LogUtil.error("Failed to read image file: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Extract guid from file.upload response
     */
    public String extractGuid(Map<String, Object> result) {
        if (result == null) return null;
        Object guid = result.get("guid");
        return guid != null ? guid.toString() : null;
    }
}
