package io.metersphere;

import io.metersphere.platform.domain.PhabricatorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PhabricatorConfigTest {

    private PhabricatorConfig config;

    @BeforeEach
    void setUp() {
        config = new PhabricatorConfig();
    }

    @Test
    @DisplayName("Default debugMode should be true")
    void testDefaultDebugMode() {
        assertTrue(config.isDebugMode());
    }

    @Test
    @DisplayName("Should set and get URL")
    void testUrl() {
        config.setUrl("https://phabricator.example.com");
        assertEquals("https://phabricator.example.com", config.getUrl());
    }

    @Test
    @DisplayName("Should set and get API token")
    void testApiToken() {
        config.setApiToken("test-token-123");
        assertEquals("test-token-123", config.getApiToken());
    }

    @Test
    @DisplayName("Should set and get user")
    void testUser() {
        config.setUser("testuser");
        assertEquals("testuser", config.getUser());
    }

    @Test
    @DisplayName("Should set and get MS URL")
    void testMsUrl() {
        config.setMsUrl("https://metersphere.example.com");
        assertEquals("https://metersphere.example.com", config.getMsUrl());
    }

    @Test
    @DisplayName("Should toggle debug mode")
    void testDebugModeToggle() {
        assertTrue(config.isDebugMode());
        
        config.setDebugMode(false);
        assertFalse(config.isDebugMode());
        
        config.setDebugMode(true);
        assertTrue(config.isDebugMode());
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullValues() {
        assertNull(config.getUrl());
        assertNull(config.getApiToken());
        assertNull(config.getUser());
        assertNull(config.getMsUrl());
    }
}
