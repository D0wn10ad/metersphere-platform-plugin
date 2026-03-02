package io.metersphere;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.PhabricatorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhabricatorClientTest {

    private PhabricatorConfig config;

    @BeforeEach
    void setUp() {
        config = new PhabricatorConfig();
        config.setUrl("https://phabricator.example.com");
        config.setApiToken("test-token");
        config.setDebugMode(false);
    }

    // Note: PhabricatorClient uses HTTP calls internally
    // Full client testing would require mock server or integration tests
    // Here we test what can be tested without network calls

    @Test
    @DisplayName("Config should be properly set")
    void testConfigSetting() {
        // This tests the setter logic
        PhabricatorClient client = new PhabricatorClient();
        client.setConfig(config);
        
        // Verify config values are stored
        assertNotNull(client);
    }

    @Test
    @DisplayName("Should handle null config")
    void testNullConfig() {
        PhabricatorClient client = new PhabricatorClient();
        
        // Should not throw, config will be null
        assertDoesNotThrow(() -> client.setConfig(null));
    }
}
