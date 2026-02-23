package io.metersphere.platform.impl;

import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.platform.api.AbstractPlatform;
import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.*;
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
        return request;
    }

    @Override
    public IssuesWithBLOBs updateIssue(PlatformIssuesUpdateRequest request) {
        setConfig();
        return request;
    }

    @Override
    public void deleteIssue(String id) {
        setConfig();
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
