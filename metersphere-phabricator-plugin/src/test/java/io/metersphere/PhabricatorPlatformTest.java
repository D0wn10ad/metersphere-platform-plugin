package io.metersphere;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.PhabricatorProjectConfig;
import io.metersphere.platform.domain.PlatformIssuesDTO;
import io.metersphere.platform.domain.PlatformIssuesUpdateRequest;
import io.metersphere.platform.domain.PlatformRequest;
import io.metersphere.platform.domain.SelectOption;
import io.metersphere.platform.domain.SyncIssuesRequest;
import io.metersphere.platform.impl.PhabricatorPlatform;
import io.metersphere.plugin.utils.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PhabricatorPlatformTest {

    private io.metersphere.platform.domain.PhabricatorConfig integrationConfig;
    private PhabricatorProjectConfig projectConfig;

    @BeforeEach
    void setUp() {
        integrationConfig = new io.metersphere.platform.domain.PhabricatorConfig();
        integrationConfig.setUrl("https://phabricator.example.com");
        integrationConfig.setApiToken("test-token");
        integrationConfig.setDebugMode(false);

        projectConfig = new PhabricatorProjectConfig();
        projectConfig.setProjectPHID("PHID-PROJ-123");
        projectConfig.setDefaultSubtype("bug");
    }

    private PlatformRequest createRequest() {
        PlatformRequest request = new PlatformRequest();
        request.setIntegrationConfig(JSON.toJSONString(integrationConfig));
        request.setWorkspaceId("ws-123");
        return request;
    }

    private void setClient(PhabricatorPlatform platform, PhabricatorClient client) throws Exception {
        Field field = PhabricatorPlatform.class.getDeclaredField("phabricatorClient");
        field.setAccessible(true);
        field.set(platform, client);
    }

    @Nested
    @DisplayName("ProjectConfig tests")
    class ProjectConfigTests {

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

    @Nested
    @DisplayName("PhabricatorPlatform constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Should create platform with valid request")
        void testValidRequest() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            assertNotNull(platform);
        }

        @Test
        @DisplayName("Should create platform with null request")
        void testNullRequest() {
            PhabricatorPlatform platform = new PhabricatorPlatform(null);
            assertNotNull(platform);
        }
    }

    @Nested
    @DisplayName("getThirdPartCustomField")
    class GetThirdPartCustomFieldTests {

        @Test
        @DisplayName("Should return empty list")
        void testReturnsEmptyList() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            var result = platform.getThirdPartCustomField(JSON.toJSONString(projectConfig));

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("isAttachmentUploadSupport")
    class IsAttachmentUploadSupportTests {

        @Test
        @DisplayName("Should return true")
        void testReturnsTrue() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            assertTrue(platform.isAttachmentUploadSupport());
        }
    }

    @Nested
    @DisplayName("getFormOptions")
    class GetFormOptionsTests {

        @Test
        @DisplayName("Should return empty list when method is blank")
        void testBlankMethod() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            io.metersphere.platform.domain.GetOptionRequest optionRequest = new io.metersphere.platform.domain.GetOptionRequest();
            optionRequest.setOptionMethod("");
            optionRequest.setProjectConfig(JSON.toJSONString(projectConfig));

            List<SelectOption> result = platform.getFormOptions(optionRequest);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for unknown method")
        void testUnknownMethod() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            io.metersphere.platform.domain.GetOptionRequest optionRequest = new io.metersphere.platform.domain.GetOptionRequest();
            optionRequest.setOptionMethod("unknownMethod");
            optionRequest.setProjectConfig(JSON.toJSONString(projectConfig));

            List<SelectOption> result = platform.getFormOptions(optionRequest);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when optionMethod is null")
        void testNullMethod() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            io.metersphere.platform.domain.GetOptionRequest optionRequest = new io.metersphere.platform.domain.GetOptionRequest();
            optionRequest.setOptionMethod(null);
            optionRequest.setProjectConfig(JSON.toJSONString(projectConfig));

            List<SelectOption> result = platform.getFormOptions(optionRequest);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("syncIssuesAttachment")
    class SyncIssuesAttachmentTests {

        @Test
        @DisplayName("Should do nothing when request is null")
        void testDoesNothingWithNull() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            assertDoesNotThrow(() -> platform.syncIssuesAttachment(null));
        }
    }

    @Nested
    @DisplayName("validateProjectConfig")
    class ValidateProjectConfigTests {

        @Test
        @DisplayName("Should throw when config is blank")
        void testBlankConfigThrows() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            assertThrows(Exception.class, () -> platform.validateProjectConfig(""));
        }

        @Test
        @DisplayName("Should throw when projectPHID and defaultProjectId are blank")
        void testMissingBothIdsThrows() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            PhabricatorProjectConfig emptyConfig = new PhabricatorProjectConfig();
            assertThrows(Exception.class, () -> platform.validateProjectConfig(JSON.toJSONString(emptyConfig)));
        }
    }

    @Nested
    @DisplayName("validateUserConfig")
    class ValidateUserConfigTests {

        @Test
        @DisplayName("Should do nothing when config is blank")
        void testBlankConfigDoesNothing() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            assertDoesNotThrow(() -> platform.validateUserConfig(""));
            assertDoesNotThrow(() -> platform.validateUserConfig(null));
        }

        @Test
        @DisplayName("Should do nothing when config has no apiToken")
        void testNoTokenDoesNothing() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            io.metersphere.platform.domain.PhabricatorConfig userConfig = new io.metersphere.platform.domain.PhabricatorConfig();
            userConfig.setUrl("https://test.com");
            assertDoesNotThrow(() -> platform.validateUserConfig(JSON.toJSONString(userConfig)));
        }
    }

    @Nested
    @DisplayName("getStatusList")
    class GetStatusListTests {

        @Test
        @DisplayName("Should return statuses")
        void testReturnsStatuses() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            List<io.metersphere.platform.domain.PlatformStatusDTO> statuses = platform.getStatusList(JSON.toJSONString(projectConfig));

            assertNotNull(statuses);
            assertFalse(statuses.isEmpty());
        }
    }

    @Nested
    @DisplayName("getDemands")
    class GetDemandsTests {

        @Test
        @DisplayName("Should return empty list when projectConfig is blank")
        void testBlankProjectConfig() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            var result = platform.getDemands("");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when projectPHID is blank")
        void testBlankProjectPHID() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);

            PhabricatorProjectConfig configNoPHID = new PhabricatorProjectConfig();
            var result = platform.getDemands(JSON.toJSONString(configNoPHID));

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return demands when API returns results")
        void testReturnsDemands() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            List<Map<String, Object>> mockResults = List.of(
                Map.of("id", 123, "fields", Map.of("name", "Test Task"))
            );
            when(mockClient.searchTasks(anyMap())).thenReturn(mockResults);
            
            var result = platform.getDemands(JSON.toJSONString(projectConfig));
            
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return empty list when API returns null")
        void testNullApiResult() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            when(mockClient.searchTasks(anyMap())).thenReturn(null);
            
            var result = platform.getDemands(JSON.toJSONString(projectConfig));
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when API throws exception")
        void testApiException() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            when(mockClient.searchTasks(anyMap())).thenThrow(new RuntimeException("API Error"));
            
            var result = platform.getDemands(JSON.toJSONString(projectConfig));
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getUserSearchOptions")
    class GetUserSearchOptionsTests {

        @Test
        @DisplayName("Should return users from API")
        void testReturnsUsers() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            List<Map<String, Object>> mockUsers = List.of(
                Map.of("phid", "PHID-USER-1", "fields", Map.of("userName", "testuser", "realName", "Test User"))
            );
            when(mockClient.searchUsers(anyMap())).thenReturn(mockUsers);
            
            io.metersphere.platform.domain.GetOptionRequest optionRequest = new io.metersphere.platform.domain.GetOptionRequest();
            optionRequest.setOptionMethod("getUserSearchOptions");
            optionRequest.setProjectConfig(JSON.toJSONString(projectConfig));
            
            List<SelectOption> result = platform.getUserSearchOptions(optionRequest);
            
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when API returns null")
        void testNullApiResult() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            when(mockClient.searchUsers(anyMap())).thenReturn(null);
            
            io.metersphere.platform.domain.GetOptionRequest optionRequest = new io.metersphere.platform.domain.GetOptionRequest();
            optionRequest.setOptionMethod("getUserSearchOptions");
            optionRequest.setProjectConfig(JSON.toJSONString(projectConfig));
            
            List<SelectOption> result = platform.getUserSearchOptions(optionRequest);
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getIssueTypes")
    class GetIssueTypesTests {

        @Test
        @DisplayName("Should return issue types from API")
        void testReturnsTypes() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            List<Map<String, Object>> mockTypes = List.of(
                Map.of("value", "bug", "name", "Bug"),
                Map.of("value", "feature", "name", "Feature")
            );
            when(mockClient.getAvailableSubtypes()).thenReturn(mockTypes);
            
            List<SelectOption> result = platform.getIssueTypes(new io.metersphere.platform.domain.GetOptionRequest());
            
            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("syncIssues")
    class SyncIssuesTests {

        @Test
        @DisplayName("Should return empty result when issues is null")
        void testNullIssues() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            SyncIssuesRequest syncRequest = new SyncIssuesRequest();
            syncRequest.setIssues(null);
            
            var result = platform.syncIssues(syncRequest);
            
            assertNotNull(result);
            assertTrue(result.getAddIssues().isEmpty());
            assertTrue(result.getUpdateIssues().isEmpty());
        }

        @Test
        @DisplayName("Should return empty result when issues is empty")
        void testEmptyIssues() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            SyncIssuesRequest syncRequest = new SyncIssuesRequest();
            syncRequest.setIssues(List.of());
            
            var result = platform.syncIssues(syncRequest);
            
            assertNotNull(result);
            assertTrue(result.getAddIssues().isEmpty());
        }

        @Test
        @DisplayName("Should skip issues with blank platformId")
        void testSkipsBlankPlatformId() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            PlatformIssuesDTO issue = new PlatformIssuesDTO();
            issue.setPlatformId("");
            
            SyncIssuesRequest syncRequest = new SyncIssuesRequest();
            syncRequest.setIssues(List.of(issue));
            
            var result = platform.syncIssues(syncRequest);
            
            assertNotNull(result);
            verify(mockClient, never()).searchTasks(anyMap());
        }

        @Test
        @DisplayName("Should sync issue with valid platformId")
        void testSyncsValidIssue() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            List<Map<String, Object>> mockResults = List.of(
                Map.of("id", 123, "fields", Map.of(
                    "name", "Test Bug",
                    "description", Map.of("raw", "Test description"),
                    "status", Map.of("value", "open")
                ))
            );
            when(mockClient.searchTasks(anyMap())).thenReturn(mockResults);
            
            PlatformIssuesDTO issue = new PlatformIssuesDTO();
            issue.setPlatformId("123");
            
            SyncIssuesRequest syncRequest = new SyncIssuesRequest();
            syncRequest.setIssues(List.of(issue));
            
            var result = platform.syncIssues(syncRequest);
            
            assertNotNull(result);
            assertFalse(result.getUpdateIssues().isEmpty());
        }
    }

    @Nested
    @DisplayName("addIssue")
    class AddIssueTests {

        @Test
        @DisplayName("Should generate UUID for new issue")
        void testGeneratesUuid() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            Map<String, Object> mockResult = Map.of(
                "result", Map.of("object", Map.of("phid", "PHID-TASK-123"))
            );
            when(mockClient.editTask(any(), anyList())).thenReturn(mockResult);
            when(mockClient.getTaskIdByPHID(anyString())).thenReturn("123");
            when(mockClient.mapSeverityToPriority(any())).thenReturn("normal");
            
            PlatformIssuesUpdateRequest updateRequest = new PlatformIssuesUpdateRequest();
            updateRequest.setTitle("Test Bug");
            updateRequest.setDescription("Description");
            updateRequest.setProjectConfig(JSON.toJSONString(projectConfig));
            
            var result = platform.addIssue(updateRequest);
            
            assertNotNull(result);
            assertNotNull(result.getId());
        }

        @Test
        @DisplayName("Should use custom field values")
        void testCustomFields() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            Map<String, Object> mockResult = Map.of(
                "result", Map.of("object", Map.of("phid", "PHID-TASK-123"))
            );
            when(mockClient.editTask(any(), anyList())).thenReturn(mockResult);
            when(mockClient.getTaskIdByPHID(anyString())).thenReturn("123");
            when(mockClient.mapSeverityToPriority(any())).thenReturn("high");
            
            io.metersphere.platform.domain.PlatformCustomFieldItemDTO customField = new io.metersphere.platform.domain.PlatformCustomFieldItemDTO();
            customField.setName("severity");
            customField.setValue("P1");
            
            PlatformIssuesUpdateRequest updateRequest = new PlatformIssuesUpdateRequest();
            updateRequest.setTitle("Test Bug");
            updateRequest.setCustomFieldList(List.of(customField));
            updateRequest.setProjectConfig(JSON.toJSONString(projectConfig));
            var result = platform.addIssue(updateRequest);
            
            assertNotNull(result);
            verify(mockClient).mapSeverityToPriority("P1");
        }

        @Test
        @DisplayName("Should handle issue without description")
        void testNoDescription() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            Map<String, Object> mockResult = Map.of(
                "result", Map.of("object", Map.of("phid", "PHID-TASK-456"))
            );
            when(mockClient.editTask(any(), anyList())).thenReturn(mockResult);
            when(mockClient.getTaskIdByPHID(anyString())).thenReturn("456");
            when(mockClient.mapSeverityToPriority(any())).thenReturn("normal");
            
            PlatformIssuesUpdateRequest updateRequest = new PlatformIssuesUpdateRequest();
            updateRequest.setTitle("Test Issue");
            updateRequest.setProjectConfig(JSON.toJSONString(projectConfig));
            
            var result = platform.addIssue(updateRequest);
            
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should use default subtype from project config")
        void testDefaultSubtype() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            Map<String, Object> mockResult = Map.of(
                "result", Map.of("object", Map.of("phid", "PHID-TASK-789"))
            );
            when(mockClient.editTask(any(), anyList())).thenReturn(mockResult);
            when(mockClient.getTaskIdByPHID(anyString())).thenReturn("789");
            when(mockClient.mapSeverityToPriority(any())).thenReturn("normal");
            
            PlatformIssuesUpdateRequest updateRequest = new PlatformIssuesUpdateRequest();
            updateRequest.setTitle("Issue with default subtype");
            updateRequest.setProjectConfig(JSON.toJSONString(projectConfig));
            
            var result = platform.addIssue(updateRequest);
            
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("updateIssue")
    class UpdateIssueTests {

        @Test
        @DisplayName("Should throw when platformId is blank")
        void testBlankPlatformIdThrows() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            PlatformIssuesUpdateRequest updateRequest = new PlatformIssuesUpdateRequest();
            updateRequest.setPlatformId("");
            assertThrows(Exception.class, () -> platform.updateIssue(updateRequest));
        }

        @Test
        @DisplayName("Should update issue successfully")
        void testUpdatesIssue() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            Map<String, Object> mockTask = Map.of(
                "fields", Map.of("subtype", Map.of("value", "bug"))
            );
            when(mockClient.getTask(anyString())).thenReturn(mockTask);
            
            Map<String, Object> mockResult = Map.of(
                "result", Map.of("object", Map.of("phid", "PHID-TASK-123"))
            );
            when(mockClient.editTask(any(), anyList())).thenReturn(mockResult);
            when(mockClient.getTaskIdByPHID(anyString())).thenReturn("123");
            
            PlatformIssuesUpdateRequest updateRequest = new PlatformIssuesUpdateRequest();
            updateRequest.setPlatformId("123");
            updateRequest.setTitle("Updated Bug");
            var result = platform.updateIssue(updateRequest);
            
            assertNotNull(result);
            verify(mockClient).editTask(eq("123"), anyList());
        }
    }

    @Nested
    @DisplayName("deleteIssue")
    class DeleteIssueTests {

        @Test
        @DisplayName("Should throw when platformId is blank")
        void testBlankPlatformIdThrows() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            assertThrows(Exception.class, () -> platform.deleteIssue(""));
        }

        @Test
        @DisplayName("Should delete issue successfully")
        void testDeletesIssue() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            Map<String, Object> mockResult = Map.of("result", "ok");
            when(mockClient.editTask(anyString(), anyList())).thenReturn(mockResult);
            
            assertDoesNotThrow(() -> platform.deleteIssue("123"));
            verify(mockClient).editTask(eq("123"), anyList());
        }

        @Test
        @DisplayName("Should handle T-prefix in platformId")
        void testHandlesTPrefix() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            Map<String, Object> mockResult = Map.of("result", "ok");
            when(mockClient.editTask(anyString(), anyList())).thenReturn(mockResult);
            
            assertDoesNotThrow(() -> platform.deleteIssue("T123"));
            verify(mockClient).editTask(eq("123"), anyList());
        }
    }

    @Nested
    @DisplayName("validateIntegrationConfig")
    class ValidateIntegrationConfigTests {

        @Test
        @DisplayName("Should validate successfully when connection works")
        void testValidatesSuccessfully() throws Exception {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            PhabricatorClient mockClient = mock(PhabricatorClient.class);
            setClient(platform, mockClient);
            
            when(mockClient.testConnection()).thenReturn(true);
            
            assertDoesNotThrow(() -> platform.validateIntegrationConfig());
            verify(mockClient).testConnection();
        }
    }

    @Nested
    @DisplayName("validateProjectConfig additional tests")
    class ValidateProjectConfigAdditionalTests {

        @Test
        @DisplayName("Should validate when defaultProjectId exists")
        void testValidatesWithDefaultProjectId() {
            PlatformRequest request = createRequest();
            PhabricatorPlatform platform = new PhabricatorPlatform(request);
            
            String configJson = "{\"defaultProjectId\":\"proj-123\"}";
            assertDoesNotThrow(() -> platform.validateProjectConfig(configJson));
        }
    }
}