package io.metersphere;

import io.metersphere.platform.domain.PhabricatorProjectConfig;
import io.metersphere.plugin.utils.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PhabricatorPlatformTest {

    // Test PhabricatorProjectConfig parsing (used by PhabricatorPlatform)
    
    @Test
    @DisplayName("ProjectConfig - should parse valid JSON")
    void testProjectConfigParse() {
        String json = "{\"projectPHID\":\"PHID-PROJ-123\"}";
        PhabricatorProjectConfig config = JSON.parseObject(json, PhabricatorProjectConfig.class);
        
        assertNotNull(config);
        assertEquals("PHID-PROJ-123", config.getProjectPHID());
    }

    @Test
    @DisplayName("ProjectConfig - should parse JSON with all fields")
    void testProjectConfigFullParse() {
        String json = "{\"projectPHID\":\"PHID-PROJ-456\",\"primaryStrategy\":\"default\",\"defaultSubtype\":\"bug\"}";
        PhabricatorProjectConfig config = JSON.parseObject(json, PhabricatorProjectConfig.class);
        
        assertNotNull(config);
        assertEquals("PHID-PROJ-456", config.getProjectPHID());
        assertEquals("default", config.getPrimaryStrategy());
        assertEquals("bug", config.getDefaultSubtype());
    }

    @Test
    @DisplayName("ProjectConfig - should have null projectPHID by default")
    void testProjectConfigDefault() {
        PhabricatorProjectConfig config = new PhabricatorProjectConfig();
        assertNull(config.getProjectPHID());
    }

    @Test
    @DisplayName("ProjectConfig - should set and get projectPHID")
    void testProjectConfigSetter() {
        PhabricatorProjectConfig config = new PhabricatorProjectConfig();
        config.setProjectPHID("PHID-TEST-456");
        
        assertEquals("PHID-TEST-456", config.getProjectPHID());
    }

    @Test
    @DisplayName("ProjectConfig - should serialize to JSON")
    void testProjectConfigSerialize() {
        PhabricatorProjectConfig config = new PhabricatorProjectConfig();
        config.setProjectPHID("PHID-SERIAL-789");
        
        String json = JSON.toJSONString(config);
        
        assertNotNull(json);
        assertTrue(json.contains("PHID-SERIAL-789"));
    }
}
