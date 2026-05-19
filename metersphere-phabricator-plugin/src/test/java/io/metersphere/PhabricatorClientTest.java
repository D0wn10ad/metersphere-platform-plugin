package io.metersphere;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.PhabricatorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhabricatorClientTest {

    private PhabricatorConfig config;

    @BeforeEach
    void setUp() {
        config = new PhabricatorConfig();
        config.setUrl("https://phabricator.example.com");
        config.setApiToken("test-token");
        config.setDebugMode("false");
    }

    @Test
    @DisplayName("Default constructor should create instance")
    void testDefaultConstructor() {
        PhabricatorClient client = new PhabricatorClient();
        assertNotNull(client);
    }

    @Test
    @DisplayName("Config constructor should initialize with config")
    void testConfigConstructor() {
        PhabricatorClient client = new PhabricatorClient(config);
        assertNotNull(client);
    }

    @Test
    @DisplayName("SetConfig should update config and recreate HTTP client")
    void testSetConfig() {
        PhabricatorClient client = new PhabricatorClient();
        client.setConfig(config);
        assertDoesNotThrow(() -> client.setConfig(config));
    }

    @Test
    @DisplayName("SetConfig with null should not throw")
    void testSetNullConfig() {
        PhabricatorClient client = new PhabricatorClient();
        assertDoesNotThrow(() -> client.setConfig(null));
    }

    @Nested
    @DisplayName("mapSeverityToPriority")
    class MapSeverityToPriorityTests {

        @Test
        @DisplayName("P0 should map to unbreak")
        void testP0Priority() {
            assertEquals("unbreak", new PhabricatorClient().mapSeverityToPriority("P0"));
            assertEquals("unbreak", new PhabricatorClient().mapSeverityToPriority("p0"));
        }

        @Test
        @DisplayName("P1 should map to high")
        void testP1MapsToHigh() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("high", client.mapSeverityToPriority("P1"));
            assertEquals("high", client.mapSeverityToPriority("p1"));
        }

        @Test
        @DisplayName("P2 should map to normal")
        void testP2MapsToNormal() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("normal", client.mapSeverityToPriority("P2"));
            assertEquals("normal", client.mapSeverityToPriority("p2"));
        }

        @Test
        @DisplayName("P3 should map to low")
        void testP3MapsToLow() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("low", client.mapSeverityToPriority("P3"));
            assertEquals("low", client.mapSeverityToPriority("p3"));
        }

        @Test
        @DisplayName("Critical should map to unbreak")
        void testCriticalPriority() {
            assertEquals("unbreak", new PhabricatorClient().mapSeverityToPriority("Critical"));
            assertEquals("unbreak", new PhabricatorClient().mapSeverityToPriority("CRITICAL"));
            assertEquals("unbreak", new PhabricatorClient().mapSeverityToPriority("critical"));
        }

        @Test
        @DisplayName("High should map to high")
        void testHighMapsToHigh() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("high", client.mapSeverityToPriority("High"));
            assertEquals("high", client.mapSeverityToPriority("HIGH"));
        }

        @Test
        @DisplayName("Medium should map to normal")
        void testMediumMapsToNormal() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("normal", client.mapSeverityToPriority("Medium"));
            assertEquals("normal", client.mapSeverityToPriority("MEDIUM"));
        }

        @Test
        @DisplayName("Low should map to low")
        void testLowMapsToLow() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("low", client.mapSeverityToPriority("Low"));
            assertEquals("low", client.mapSeverityToPriority("LOW"));
        }

        @Test
        @DisplayName("Null should map to normal")
        void testNullMapsToNormal() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("normal", client.mapSeverityToPriority(null));
        }

        @Test
        @DisplayName("Unknown severity should map to wish")
        void testUnknownMapsToWish() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("wish", client.mapSeverityToPriority("Unknown"));
            assertEquals("wish", client.mapSeverityToPriority(""));
        }
    }

    @Nested
    @DisplayName("getAvailableStatuses")
    class GetAvailableStatusesTests {

        @Test
        @DisplayName("Should return list of statuses")
        void testReturnsStatuses() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> statuses = client.getAvailableStatuses();
            assertNotNull(statuses);
            assertFalse(statuses.isEmpty());
            assertEquals(6, statuses.size());
        }

        @Test
        @DisplayName("Should include Open status")
        void testIncludesOpen() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> statuses = client.getAvailableStatuses();
            assertTrue(statuses.stream().anyMatch(s -> "open".equals(s.get("value"))));
        }

        @Test
        @DisplayName("Should include Resolved status")
        void testIncludesResolved() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> statuses = client.getAvailableStatuses();
            assertTrue(statuses.stream().anyMatch(s -> "resolved".equals(s.get("value"))));
        }

        @Test
        @DisplayName("All statuses should have value and name")
        void testAllHaveValueAndName() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> statuses = client.getAvailableStatuses();
            for (Map<String, Object> status : statuses) {
                assertNotNull(status.get("value"));
                assertNotNull(status.get("name"));
            }
        }
    }

    @Nested
    @DisplayName("getAvailableSubtypes")
    class GetAvailableSubtypesTests {

        @Test
        @DisplayName("Should return list of subtypes")
        void testReturnsSubtypes() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> subtypes = client.getAvailableSubtypes();
            assertNotNull(subtypes);
            assertFalse(subtypes.isEmpty());
            assertEquals(10, subtypes.size());
        }

        @Test
        @DisplayName("Should include default subtype")
        void testIncludesDefault() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> subtypes = client.getAvailableSubtypes();
            assertTrue(subtypes.stream().anyMatch(s -> "default".equals(s.get("value"))));
        }

        @Test
        @DisplayName("Should include bug subtype")
        void testIncludesBug() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> subtypes = client.getAvailableSubtypes();
            assertTrue(subtypes.stream().anyMatch(s -> "bug".equals(s.get("value"))));
        }

        @Test
        @DisplayName("Should include feature subtype")
        void testIncludesFeature() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> subtypes = client.getAvailableSubtypes();
            assertTrue(subtypes.stream().anyMatch(s -> "feature".equals(s.get("value"))));
        }

        @Test
        @DisplayName("All subtypes should have value and name")
        void testAllHaveValueAndName() {
            PhabricatorClient client = new PhabricatorClient();
            List<Map<String, Object>> subtypes = client.getAvailableSubtypes();
            for (Map<String, Object> subtype : subtypes) {
                assertNotNull(subtype.get("value"));
                assertNotNull(subtype.get("name"));
            }
        }
    }

    @Nested
    @DisplayName("getTaskIdByPHID")
    class GetTaskIdByPHIDTests {

        @Test
        @DisplayName("Null PHID should return null")
        void testNullPHID() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertNull(client.getTaskIdByPHID(null));
        }

        @Test
        @DisplayName("Blank PHID should return null")
        void testBlankPHID() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertNull(client.getTaskIdByPHID(""));
            assertNull(client.getTaskIdByPHID("   "));
        }
    }

    @Nested
    @DisplayName("projectExists")
    class ProjectExistsTests {

        @Test
        @DisplayName("Null PHID should return false")
        void testNullPHID() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertFalse(client.projectExists(null));
        }

        @Test
        @DisplayName("Blank PHID should return false")
        void testBlankPHID() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertFalse(client.projectExists(""));
            assertFalse(client.projectExists("   "));
        }
    }

    @Nested
    @DisplayName("getProjectNameByPHID")
    class GetProjectNameByPHIDTests {

        @Test
        @DisplayName("Null PHID should return null")
        void testNullPHID() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertNull(client.getProjectNameByPHID(null));
        }

        @Test
        @DisplayName("Blank PHID should return null")
        void testBlankPHID() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertNull(client.getProjectNameByPHID(""));
            assertNull(client.getProjectNameByPHID("   "));
        }
    }

    @Nested
    @DisplayName("getTask")
    class GetTaskTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testValidTaskIdWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.getTask("123"));
        }

        @Test
        @DisplayName("Should throw NumberFormatException for T-prefix")
        void testTaskIdWithPrefixThrows() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(NumberFormatException.class, () -> client.getTask("T123"));
        }
    }

    @Nested
    @DisplayName("editTask")
    class EditTaskTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testNullObjectIdentifierWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.editTask(null, List.of(Map.of("type", "title", "value", "Test"))));
        }

        @Test
        @DisplayName("Should throw when config is null for empty transactions")
        void testEmptyTransactionsWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.editTask("123", List.of()));
        }
    }

    @Nested
    @DisplayName("searchTasks")
    class SearchTasksTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testNullConstraintsWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchTasks(null));
        }

        @Test
        @DisplayName("Should throw when config is null")
        void testEmptyConstraintsWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchTasks(Map.of()));
        }

        @Test
        @DisplayName("Should throw when config is null")
        void testConstraintsWithLimitWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchTasks(Map.of(), 100));
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsersTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testNullConstraintsWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchUsers(null));
        }

        @Test
        @DisplayName("Should throw when config is null")
        void testEmptyConstraintsWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchUsers(Map.of()));
        }
    }

    @Nested
    @DisplayName("uploadFile")
    class UploadFileTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testValidUploadWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.uploadFile("test.png", "dGVzdA=="));
        }

        @Test
        @DisplayName("Should throw when config is null")
        void testEmptyFilenameWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.uploadFile("", "dGVzdA=="));
        }

        @Test
        @DisplayName("Should throw when config is null")
        void testEmptyDataWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.uploadFile("test.png", ""));
        }
    }

    @Nested
    @DisplayName("processRemarkup")
    class ProcessRemarkupTests {

        @Test
        @DisplayName("Should handle valid content")
        void testValidContent() {
            PhabricatorClient client = new PhabricatorClient(config);
            String result = client.processRemarkup("test content", "maniphest");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle null context")
        void testNullContext() {
            PhabricatorClient client = new PhabricatorClient(config);
            String result = client.processRemarkup("test content", null);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("close")
    class CloseTests {

        @Test
        @DisplayName("Should close without error when client is initialized")
        void testCloseWithClient() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertDoesNotThrow(() -> client.close());
        }

        @Test
        @DisplayName("Should close without error when client is null")
        void testCloseWithNullClient() {
            PhabricatorClient client = new PhabricatorClient();
            assertDoesNotThrow(() -> client.close());
        }
    }

    @Nested
    @DisplayName("auth")
    class AuthTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testAuthThrowsWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.auth());
        }
    }

    @Nested
    @DisplayName("testConnection")
    class TestConnectionTests {
    }

    @Nested
    @DisplayName("callConduit")
    class CallConduitTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testCallConduitWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.callConduit("test.method", Map.of()));
        }
    }

    @Nested
    @DisplayName("searchTasks")
    class SearchTasksWithLimitTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testSearchTasksLimitWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchTasks(Map.of(), 10));
        }
    }

    @Nested
    @DisplayName("searchProjects")
    class SearchProjectsTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testSearchProjectsWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchProjects(Map.of()));
        }
    }

    @Nested
    @DisplayName("getTask")
    class GetTaskMoreTests {

        @Test
        @DisplayName("Should throw when taskId is null")
        void testGetTaskWithNullId() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.getTask(null));
        }

        @Test
        @DisplayName("Should throw when taskId is blank")
        void testGetTaskWithBlankId() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.getTask("   "));
        }

        @Test
        @DisplayName("Should throw when taskId is empty")
        void testGetTaskWithEmptyId() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.getTask(""));
        }
    }

    @Nested
    @DisplayName("editTask")
    class EditTaskMoreTests {

        @Test
        @DisplayName("Should throw when objectIdentifier is null")
        void testEditTaskWithNullIdentifier() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.editTask(null, List.of(Map.of("type", "title", "value", "Test"))));
        }

        @Test
        @DisplayName("Should throw when objectIdentifier is blank")
        void testEditTaskWithBlankIdentifier() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.editTask("   ", List.of(Map.of("type", "title", "value", "Test"))));
        }

        @Test
        @DisplayName("Should throw when transactions is null")
        void testEditTaskWithNullTransactions() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.editTask("123", null));
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsersMoreTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testSearchUsersWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchUsers(Map.of()));
        }
    }

    @Nested
    @DisplayName("processRemarkup")
    class ProcessRemarkupMoreTests {

        @Test
        @DisplayName("Should return original content when config is null")
        void testProcessRemarkupWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("test", client.processRemarkup("test", "context"));
        }

        @Test
        @DisplayName("Should throw when content is null")
        void testProcessRemarkupWithNullContent() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.processRemarkup(null, "context"));
        }
    }

    @Nested
    @DisplayName("uploadFile")
    class UploadFileMoreTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testUploadWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.uploadFile("test.png", "dGVzdA=="));
        }

        @Test
        @DisplayName("Should throw when filename is null")
        void testUploadWithNullFilename() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.uploadFile(null, "dGVzdA=="));
        }

        @Test
        @DisplayName("Should throw when filename is blank")
        void testUploadWithBlankFilename() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.uploadFile("   ", "dGVzdA=="));
        }

        @Test
        @DisplayName("Should throw when data is null")
        void testUploadWithNullData() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.uploadFile("test.png", null));
        }
    }

    @Nested
    @DisplayName("prettyPrintJson")
    class PrettyPrintJsonTests {

        @Test
        @DisplayName("Null input returns null")
        void testNullInput() {
            PhabricatorClient client = new PhabricatorClient();
            assertNull(client.prettyPrintJson(null));
        }

        @Test
        @DisplayName("Blank input returns blank")
        void testBlankInput() {
            PhabricatorClient client = new PhabricatorClient();
            assertEquals("", client.prettyPrintJson(""));
            assertTrue(client.prettyPrintJson("   ").isBlank());
        }

        @Test
        @DisplayName("Simple object formats correctly")
        void testSimpleObject() {
            PhabricatorClient client = new PhabricatorClient();
            String result = client.prettyPrintJson("{\"a\":\"b\"}");
            assertNotNull(result);
            assertTrue(result.contains("\"a\": \"b\""));
        }

        @Test
        @DisplayName("Nested object formats with indentation")
        void testNestedObject() {
            PhabricatorClient client = new PhabricatorClient();
            String result = client.prettyPrintJson("{\"a\":{\"b\":\"c\"}}");
            assertNotNull(result);
            assertTrue(result.contains("\"a\""));
            assertTrue(result.contains("\"b\": \"c\""));
        }

        @Test
        @DisplayName("Array formats correctly")
        void testArray() {
            PhabricatorClient client = new PhabricatorClient();
            String result = client.prettyPrintJson("[1,2,3]");
            assertNotNull(result);
            assertTrue(result.contains("1"));
            assertTrue(result.contains("2"));
            assertTrue(result.contains("3"));
        }

        @Test
        @DisplayName("Mixed object with array formats correctly")
        void testMixedObject() {
            PhabricatorClient client = new PhabricatorClient();
            String result = client.prettyPrintJson("{\"a\":[1,2],\"b\":{\"c\":\"d\"}}");
            assertNotNull(result);
            assertTrue(result.contains("\"a\""));
            assertTrue(result.contains("\"b\""));
            assertTrue(result.contains("\"c\": \"d\""));
        }

        @Test
        @DisplayName("String with colon inside value is handled correctly")
        void testStringWithColon() {
            PhabricatorClient client = new PhabricatorClient();
            String result = client.prettyPrintJson("{\"url\":\"https://example.com:8080/path\"}");
            assertNotNull(result);
            assertTrue(result.contains("\"url\""));
            assertTrue(result.contains("https://example.com:8080/path"));
        }
    }

    @Nested
    @DisplayName("getTaskIdByPHID edge cases")
    class GetTaskIdByPHIDEdgeTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.getTaskIdByPHID("PHID-TASK-123"));
        }
    }

    @Nested
    @DisplayName("getProjectNameByPHID edge cases")
    class GetProjectNameByPHIDEdgeTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.getProjectNameByPHID("PHID-PROJ-123"));
        }
    }

    @Nested
    @DisplayName("searchTasks with limit edge cases")
    class SearchTasksWithLimitEdgeTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchTasks(Map.of(), 10));
        }

        @Test
        @DisplayName("Should throw when constraints is null")
        void testWithNullConstraints() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.searchTasks(null, 10));
        }
    }

    @Nested
    @DisplayName("searchProjects edge cases")
    class SearchProjectsEdgeTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchProjects(Map.of()));
        }
    }

    @Nested
    @DisplayName("searchUsers edge cases")
    class SearchUsersEdgeTests {

        @Test
        @DisplayName("Should throw when config is null")
        void testWithNullConfig() {
            PhabricatorClient client = new PhabricatorClient();
            assertThrows(Exception.class, () -> client.searchUsers(Map.of()));
        }

        @Test
        @DisplayName("Should throw when constraints is null")
        void testWithNullConstraints() {
            PhabricatorClient client = new PhabricatorClient(config);
            assertThrows(Exception.class, () -> client.searchUsers(null));
        }
    }
}
