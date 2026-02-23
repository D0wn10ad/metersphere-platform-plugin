package io.metersphere.platform.impl;

import io.metersphere.platform.api.AbstractPlatformMetaInfo;

public class PhabricatorPlatformMetaInfo extends AbstractPlatformMetaInfo {

    public static final String KEY = "Phabricator";

    public PhabricatorPlatformMetaInfo() {
        super(PhabricatorPlatformMetaInfo.class.getClassLoader());
    }

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
        return false;
    }

    @Override
    public boolean isThirdPartTemplateSupport() {
        return false;
    }
}
