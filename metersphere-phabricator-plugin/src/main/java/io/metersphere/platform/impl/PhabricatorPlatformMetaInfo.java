package io.metersphere.platform.impl;

import io.metersphere.platform.api.PlatformMetaInfo;

/**
 * Phabricator plugin metadata
 */
public class PhabricatorPlatformMetaInfo implements PlatformMetaInfo {

    @Override
    public String getKey() {
        return "phabricator";
    }

    @Override
    public String getLabel() {
        return "Phabricator";
    }

    @Override
    public String getIcon() {
        return "phabricator.png";
    }

    @Override
    public String getDescription() {
        return "Integrate with Phabricator/Phorge Maniphest issue tracking";
    }

    @Override
    public String getVersion() {
        return "2.10.0";
    }

    @Override
    public String getServiceIntegration() {
        return "phabricator";
    }

    @Override
    public String getProjectIntegration() {
        return "phabricator";
    }
}