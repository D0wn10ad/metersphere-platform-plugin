package io.metersphere.platform.impl;

import io.metersphere.platform.api.PluginMetaInfo;

public class PhabricatorPlatformMetaInfo implements PluginMetaInfo {
    public static final String KEY = "Phabricator";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getLabel() {
        return "Phabricator";
    }

    @Override
    public String getVersion() {
        return "2.10.0";
    }

    @Override
    public boolean isXpack() {
        return true;
    }

    @Override
    public boolean isThirdPartTemplateSupport() {
        return false;
    }

    @Override
    public String getFrontendMetaData() {
        return null;
    }
}
