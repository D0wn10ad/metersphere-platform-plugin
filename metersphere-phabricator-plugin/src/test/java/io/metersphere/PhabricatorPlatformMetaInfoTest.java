package io.metersphere;

import io.metersphere.platform.impl.PhabricatorPlatformMetaInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PhabricatorPlatformMetaInfoTest {

    @Test
    @DisplayName("getKey should return Phabricator")
    void testGetKey() {
        PhabricatorPlatformMetaInfo metaInfo = new PhabricatorPlatformMetaInfo();
        assertEquals("Phabricator", metaInfo.getKey());
    }

    @Test
    @DisplayName("getLabel should return Phabricator")
    void testGetLabel() {
        PhabricatorPlatformMetaInfo metaInfo = new PhabricatorPlatformMetaInfo();
        assertEquals("Phabricator", metaInfo.getLabel());
    }

    @Test
    @DisplayName("getVersion should return 2.10.0")
    void testGetVersion() {
        PhabricatorPlatformMetaInfo metaInfo = new PhabricatorPlatformMetaInfo();
        assertEquals("2.10.0", metaInfo.getVersion());
    }

    @Test
    @DisplayName("isXpack should return false")
    void testIsXpack() {
        PhabricatorPlatformMetaInfo metaInfo = new PhabricatorPlatformMetaInfo();
        assertFalse(metaInfo.isXpack());
    }

    @Test
    @DisplayName("isThirdPartTemplateSupport should return false")
    void testIsThirdPartTemplateSupport() {
        PhabricatorPlatformMetaInfo metaInfo = new PhabricatorPlatformMetaInfo();
        assertFalse(metaInfo.isThirdPartTemplateSupport());
    }
}
