package io.metersphere.platform.impl;

import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.platform.api.AbstractPlatform;
import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.*;
import io.metersphere.plugin.exception.MSPluginException;
import io.metersphere.plugin.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PhabricatorPlatform extends AbstractPlatform {

    protected PhabricatorClient phabricatorClient;

    public PhabricatorPlatform(PlatformRequest request) {
        super.key = PhabricatorPlatformMetaInfo.KEY;
        super.request = request;
        phabricatorClient = new PhabricatorClient();
        setConfig();
    }

    private PhabricatorConfig setConfig() {
        PhabricatorConfig config = getIntegrationConfig();
        validateConfig(config);
        phabricatorClient.setConfig(config);
        return config;
    }

    private void validateConfig(PhabricatorConfig config) {
        if (config == null) {
            MSPluginException.throwException("phabricator config is null");
        }
    }

    public PhabricatorConfig getIntegrationConfig() {
        return getIntegrationConfig(PhabricatorConfig.class);
    }

    @Override
    public List<DemandDTO> getDemands(String projectConfig) {
        return new ArrayList<>();
    }

    @Override
    public IssuesWithBLOBs addIssue(PlatformIssuesUpdateRequest request) {
        return request;
    }

    @Override
    public IssuesWithBLOBs updateIssue(PlatformIssuesUpdateRequest request) {
        return request;
    }

    @Override
    public void deleteIssue(String id) {
        // Stub
    }

    @Override
    public void validateIntegrationConfig() {
        phabricatorClient.auth();
    }

    @Override
    public void validateProjectConfig(String projectConfigStr) {
        if (StringUtils.isBlank(projectConfigStr)) {
            MSPluginException.throwException("请在项目中添加项目配置！");
        }
    }

    @Override
    public void validateUserConfig(String userConfig) {
        // Stub
    }

    @Override
    public boolean isAttachmentUploadSupport() {
        return false;
    }

    @Override
    public SyncIssuesResult syncIssues(SyncIssuesRequest request) {
        return new SyncIssuesResult();
    }

    @Override
    public List<PlatformCustomFieldItemDTO> getThirdPartCustomField(String projectConfig) {
        return new ArrayList<>();
    }

    @Override
    public List<PlatformStatusDTO> getStatusList(String projectConfig) {
        return new ArrayList<>();
    }
}
