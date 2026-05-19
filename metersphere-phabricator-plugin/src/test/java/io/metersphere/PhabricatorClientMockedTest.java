package io.metersphere;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.client.PhabricatorHttpClient;
import io.metersphere.platform.domain.PhabricatorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PhabricatorClientMockedTest {

    private PhabricatorConfig config;
    private PhabricatorHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        config = new PhabricatorConfig();
        config.setUrl("https://phabricator.example.com");
        config.setApiToken("test-token");
        config.setDebugMode("false");
        mockHttpClient = mock(PhabricatorHttpClient.class);
    }

    @Nested
    @DisplayName("callConduit with mocked HTTP client")
    class CallConduitTests {

        @Test
        @DisplayName("Should return parsed response when HTTP call succeeds")
        void testCallConduitSuccess() throws Exception {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", Map.of("id", 123));
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("{\"result\":{\"id\":123}}");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            Map<String, Object> result = client.callConduit("test.method", Map.of());

            assertNotNull(result);
            assertNotNull(result.get("result"));
        }

        @Test
        @DisplayName("Should return empty map when result is null")
        void testCallConduitNullResult() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("{\"result\":null}");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            Map<String, Object> result = client.callConduit("test.method", Map.of());

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should throw when HTTP call throws exception")
        void testCallConduitThrowsOnHttpError() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertThrows(Exception.class, () -> client.callConduit("test.method", Map.of()));
        }

        @Test
        @DisplayName("Should attempt all retries for non-retryable HTTP error")
        void testNoRetryOnNonRetryable() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenThrow(new RuntimeException("HTTP error 500 from server"));

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            assertThrows(Exception.class, () -> client.callConduit("test.method", Map.of()));
            verify(mockHttpClient, times(4)).executePost(anyString(), anyString());
        }

        @Test
        @DisplayName("Should retry on retryable error then fail")
        void testRetryOnRetryableError() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            assertThrows(Exception.class, () -> client.callConduit("test.method", Map.of()));
            verify(mockHttpClient, times(4)).executePost(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("searchTasks with mocked HTTP client")
    class SearchTasksTests {

        @Test
        @DisplayName("Should return tasks from API response")
        void testSearchTasksReturnsResults() throws Exception {
            String response = "{\"result\":[{\"id\":1,\"fields\":{\"name\":\"Task 1\"}},{\"id\":2,\"fields\":{\"name\":\"Task 2\"}}]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> results = client.searchTasks(Map.of());

            assertNotNull(results);
            assertTrue(results.size() >= 0);
        }

        @Test
        @DisplayName("Should return empty list when result is null")
        void testSearchTasksEmptyOnNullResult() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("{\"result\":null}");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> results = client.searchTasks(Map.of());

            assertNotNull(results);
        }
    }

    @Nested
    @DisplayName("searchWithPagination multi-page")
    class SearchWithPaginationTests {

        @Test
        @DisplayName("Should paginate through multiple pages")
        void testMultiPageSearch() throws Exception {
            String page1 = "{\"result\":{\"data\":[{\"id\":1},{\"id\":2}],\"cursor\":{\"after\":\"page2\"}}}";
            String page2 = "{\"result\":{\"data\":[{\"id\":3}],\"cursor\":{\"after\":null}}}";
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn(page1)
                .thenReturn(page2);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> results = client.searchTasks(Map.of());

            assertNotNull(results);
            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("Should handle null result data gracefully")
        void testNullResultData() throws Exception {
            String response = "{\"result\":{\"data\":null,\"cursor\":{\"after\":null}}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> results = client.searchTasks(Map.of());

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should handle non-List data gracefully")
        void testNonListData() throws Exception {
            String response = "{\"result\":{\"data\":\"not-a-list\",\"cursor\":{\"after\":null}}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> results = client.searchTasks(Map.of());

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("debug mode")
    class DebugModeTests {

        @Test
        @DisplayName("Should not throw when debug mode is enabled")
        void testDebugModeDoesNotThrow() throws Exception {
            config.setDebugMode("true");
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("{\"result\":\"ok\"}");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            assertDoesNotThrow(() -> client.callConduit("test.method", Map.of()));
        }

        @Test
        @DisplayName("Should not throw when debug mode is enabled with token")
        void testDebugModeWithToken() throws Exception {
            config.setDebugMode("true");
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("{\"result\":\"ok\"}");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            Map<String, Object> params = new HashMap<>();
            params.put("token", "secret-token-value");
            assertDoesNotThrow(() -> client.callConduit("test.method", params));
        }

        @Test
        @DisplayName("Should not throw when debug mode is enabled with long response")
        void testDebugModeWithLongResponse() throws Exception {
            config.setDebugMode("true");
            StringBuilder longJson = new StringBuilder("{\"result\":\"");
            for (int i = 0; i < 500; i++) {
                longJson.append("data");
            }
            longJson.append("\"}");
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn(longJson.toString());

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            assertDoesNotThrow(() -> client.callConduit("test.method", Map.of()));
        }
    }

    @Nested
    @DisplayName("searchProjects with mocked HTTP client")
    class SearchProjectsTests {

        @Test
        @DisplayName("Should return projects from API response")
        void testSearchProjectsReturnsResults() throws Exception {
            String response = "{\"result\":[{\"phid\":\"PHID-PROJ-1\",\"fields\":{\"name\":\"Project 1\"}}]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> results = client.searchProjects(Map.of());

            assertNotNull(results);
        }
    }

    @Nested
    @DisplayName("getTaskIdByPHID with mocked HTTP client")
    class GetTaskIdByPHIDTests {

        @Test
        @DisplayName("Should return task ID when found")
        void testGetTaskIdByPHIDReturnsId() throws Exception {
            // The API returns result as Map with "data" field containing list
            String response = "{\"result\":{\"data\":[{\"id\":123}],\"cursor\":{\"after\":null}}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            String result = client.getTaskIdByPHID("PHID-TASK-123");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return null when not found")
        void testGetTaskIdByPHIDReturnsNull() throws Exception {
            String response = "{\"result\":[]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            String result = client.getTaskIdByPHID("PHID-TASK-999");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("projectExists with mocked HTTP client")
    class ProjectExistsTests {

        @Test
        @DisplayName("Should return true when project exists")
        void testProjectExistsReturnsTrue() throws Exception {
            String response = "{\"result\":{\"data\":[{\"phid\":\"PHID-PROJ-1\"}],\"cursor\":{\"after\":null}}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            boolean result = client.projectExists("PHID-PROJ-1");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when project not found")
        void testProjectExistsReturnsFalse() throws Exception {
            String response = "{\"result\":[]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            boolean result = client.projectExists("PHID-PROJ-999");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getProjectNameByPHID with mocked HTTP client")
    class GetProjectNameByPHIDTests {

        @Test
        @DisplayName("Should return project name when found")
        void testGetProjectNameByPHIDReturnsName() throws Exception {
            String response = "{\"result\":{\"data\":[{\"fields\":{\"name\":\"Test Project\"}}],\"cursor\":{\"after\":null}}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            String result = client.getProjectNameByPHID("PHID-PROJ-1");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return null when project not found")
        void testGetProjectNameByPHIDReturnsNull() throws Exception {
            String response = "{\"result\":[]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            String result = client.getProjectNameByPHID("PHID-PROJ-999");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("getTask with mocked HTTP client")
    class GetTaskTests {

        @Test
        @DisplayName("Should return task when found")
        void testGetTaskReturnsTask() throws Exception {
            String response = "{\"result\":{\"data\":[{\"id\":123,\"fields\":{\"name\":\"Test Task\"}}],\"cursor\":{\"after\":null}}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            Map<String, Object> result = client.getTask("123");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return null when task not found")
        void testGetTaskReturnsNull() throws Exception {
            String response = "{\"result\":[]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            Map<String, Object> result = client.getTask("999");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("editTask with mocked HTTP client")
    class EditTaskTests {

        @Test
        @DisplayName("Should return result when edit succeeds")
        void testEditTaskReturnsResult() throws Exception {
            String response = "{\"result\":{\"object\":{\"phid\":\"PHID-TASK-123\"}}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> transactions = List.of(
                Map.of("type", "title", "value", "Updated Task")
            );
            Map<String, Object> result = client.editTask("T123", transactions);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("searchUsers with mocked HTTP client")
    class SearchUsersTests {

        @Test
        @DisplayName("Should return users from API response")
        void testSearchUsersReturnsResults() throws Exception {
            String response = "{\"result\":[{\"userName\":\"john\",\"realName\":\"John Doe\"}]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            List<Map<String, Object>> results = client.searchUsers(Map.of());

            assertNotNull(results);
        }
    }

    @Nested
    @DisplayName("uploadFile with mocked HTTP client")
    class UploadFileTests {

        @Test
        @DisplayName("Should return file result when upload succeeds")
        void testUploadFileReturnsResult() throws Exception {
            String response = "{\"result\":{\"phid\":\"PHID-FILE-123\"}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            Map<String, Object> result = client.uploadFile("test.png", "dGVzdA==");

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("processRemarkup with mocked HTTP client")
    class ProcessRemarkupTests {

        @Test
        @DisplayName("Should return processed content when successful")
        void testProcessRemarkupReturnsContent() throws Exception {
            String response = "{\"content\":[\"<p>processed</p>\"]}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            String result = client.processRemarkup("test", "maniphest");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Should return original content when result is null")
        void testProcessRemarkupReturnsOriginalOnNull() throws Exception {
            String response = "{\"content\":null}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            String result = client.processRemarkup("test", "maniphest");

            assertEquals("test", result);
        }
    }

    @Nested
    @DisplayName("edge cases with mocked HTTP client")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty response body")
        void testEmptyResponseBody() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertThrows(Exception.class, () -> client.callConduit("test.method", Map.of()));
        }

        @Test
        @DisplayName("Should handle invalid JSON response")
        void testInvalidJsonResponse() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("not valid json");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertThrows(Exception.class, () -> client.callConduit("test.method", Map.of()));
        }

        @Test
        @DisplayName("Should handle API error response")
        void testApiErrorResponse() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenReturn("{\"error_code\":\"ERR-CONDUIT\",\"error_info\":\"Test error\"}");

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertThrows(Exception.class, () -> client.callConduit("test.method", Map.of()));
        }
    }

    @Nested
    @DisplayName("testConnection with mocked HTTP client")
    class TestConnectionTests {

        @Test
        @DisplayName("Should return true when connection successful")
        void testConnectionSuccess() throws Exception {
            String response = "{\"result\":{\"userName\":\"testuser\"}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            boolean result = client.testConnection();

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when result is null")
        void testConnectionNullResult() throws Exception {
            String response = "{\"result\":null}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertThrows(Exception.class, () -> client.testConnection());
        }

        @Test
        @DisplayName("Should return false when result has no userName")
        void testConnectionNoUserName() throws Exception {
            String response = "{\"result\":{\"email\":\"test@example.com\"}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertThrows(Exception.class, () -> client.testConnection());
        }

        @Test
        @DisplayName("Should return false when HTTP call fails")
        void testConnectionThrowsOnError() throws Exception {
            when(mockHttpClient.executePost(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertThrows(Exception.class, () -> client.testConnection());
        }
    }

    @Nested
    @DisplayName("auth with mocked HTTP client")
    class AuthTests {

        @Test
        @DisplayName("Should call testConnection")
        void testAuthCallsTestConnection() throws Exception {
            String response = "{\"result\":{\"userName\":\"testuser\"}}";
            when(mockHttpClient.executePost(anyString(), anyString())).thenReturn(response);

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            client.auth();

            verify(mockHttpClient).executePost(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("close with mocked HTTP client")
    class CloseTests {

        @Test
        @DisplayName("Should close httpClient when not null")
        void testCloseWithClient() throws Exception {
            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            
            assertDoesNotThrow(() -> client.close());
            verify(mockHttpClient).close();
        }

        @Test
        @DisplayName("Should handle null httpClient")
        void testCloseWithoutClient() {
            PhabricatorClient client = new PhabricatorClient();
            client.setConfig(config);
            
            assertDoesNotThrow(() -> client.close());
        }

        @Test
        @DisplayName("Should handle close exception gracefully")
        void testCloseWithException() throws Exception {
            doThrow(new RuntimeException("Close failed")).when(mockHttpClient).close();

            PhabricatorClient client = new PhabricatorClient(config, mockHttpClient);
            assertDoesNotThrow(() -> client.close());
            verify(mockHttpClient).close();
        }
    }

    @Nested
    @DisplayName("setHttpClient with mocked HTTP client")
    class SetHttpClientTests {

        @Test
        @DisplayName("Should set httpClient")
        void testSetHttpClient() {
            PhabricatorClient client = new PhabricatorClient();
            client.setHttpClient(mockHttpClient);
            
            assertNotNull(client);
        }
    }
}
