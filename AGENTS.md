# AGENTS.md - MeterSphere Platform Plugin Development Guide

## Overview
This is a MeterSphere platform plugin project for integrating with third-party issue management systems (Jira, ZenTao, Phabricator, etc.). The project is a Maven-based multi-module Java project.

## Project Structure
```
metersphere-platform-plugin/
├── pom.xml                           # Parent POM
├── metersphere-plugin-sdk/           # Core plugin SDK
├── metersphere-platform-plugin-sdk/  # Platform-specific SDK
└── [plugin-name]-plugin/            # Plugin implementations (Jira, ZenTao, Phabricator)
```

## Build Commands

### Build All Modules
```bash
mvn clean install
```

### Build Single Module
```bash
mvn clean install -pl metersphere-platform-plugin-sdk -am
```

### Run Tests
```bash
# Run all tests
mvn test

# Run single test class
mvn test -Dtest=RegularTest

# Run single test method
mvn test -Dtest=RegularTest#getJiraImageFileName
```

### Package Plugin
```bash
mvn clean package
# Output: target/*-jar-with-dependencies.jar
```

### Skip Tests
```bash
mvn clean install -DskipTests
```

## Code Style Guidelines

### Java Version
- Java 11 for SDK modules
- Java 17 for plugin modules

### Package Naming
```java
package io.metersphere.platform.impl;        // Platform implementation
package io.metersphere.platform.domain;       // DTOs/Entities
package io.metersphere.platform.client;      // API clients
package io.metersphere.platform.constants;   // Constants
package io.metersphere.platform.utils;       // Utilities
```

### Class Naming Conventions
- Platform implementation: `{Platform}Platform.java` (e.g., `JiraPlatform.java`, `ZentaoPlatform.java`)
- Meta info: `{Platform}PlatformMetaInfo.java`
- Config: `{Platform}Config.java`
- Domain models: `{Platform}{Entity}.java`

### Imports Order (IDE should organize automatically)
1. `java.*` and `javax.*`
2. `org.*` (Spring, Apache Commons)
3. `io.metersphere.*` (internal SDKs)
4. Static imports

### Code Patterns

#### Platform Meta Info (Frontend JSON)
The frontend.json defines the UI fields in three sections:

1. **serviceIntegration** - Platform-level config (appears in 系统设置 → 服务集成)
2. **projectConfig** - Project-level config (appears in 项目管理)
3. **accountConfig** - Personal override config (optional, appears in 用户信息)

```json
{
  "serviceIntegration": {
    "label": "Phabricator",
    "image": "/static/index.png",
    "tips": "Help text shown to user",
    "formItems": [
      {
        "name": "url",
        "type": "input",
        "defaultValue": "",
        "required": true,
        "i18n": true,
        "label": "organization.integration.phabricator_url",
        "message": "Error message key"
      }
    ]
  },
  "projectConfig": {
    "formItems": [
      {
        "name": "projectPHID",
        "type": "input",
        "required": true,
        "i18n": true,
        "label": "project.phabricator_project",
        "withProjectCheck": true
      }
    ]
  },
  "accountConfig": {
    "formItems": [
      {
        "name": "apiToken",
        "type": "password",
        "required": false
      }
    ]
  }
}
```

#### Platform Implementation Class
```java
public class PhabricatorPlatform extends AbstractPlatform {

    protected PhabricatorClient phabricatorClient;
    
    public PhabricatorPlatform(PlatformRequest request) {
        super.key = PhabricatorPlatformMetaInfo.KEY;
        super.request = request;
        phabricatorClient = new PhabricatorClient();
        setConfig();
    }

    private PhabricatorConfig setConfig() {
        PhabricatorConfig config = getIntegrationConfig();
        validateConfig(config);
        phabricatorClient.setConfig(config);
        return config;
    }

    private void validateConfig(PhabricatorConfig config) {
        if (config == null) {
            MSPluginException.throwException("phabricator config is null");
        }
    }
}
```

#### Constants
```java
public class CustomFieldType {
    public static final CustomFieldType RICH_TEXT = new CustomFieldType("richText");
    public static final CustomFieldType INPUT = new CustomFieldType("input");
    // ...
}
```

### Error Handling
- Use `MSPluginException.throwException("message")` for plugin-specific errors
- Use `LogUtil.error(e)` for logging errors
- Always wrap API calls in try-catch blocks
- Return `null` or empty collections appropriately

### Logging
```java
import io.metersphere.plugin.utils.LogUtil;

// Usage
LogUtil.error(e);           // Log exception with stack trace
LogUtil.info("message");    // Log info
```

### JSON Handling
```java
import io.metersphere.plugin.utils.JSON;

// Parse JSON
MyObject obj = JSON.parseObject(jsonString, MyObject.class);

// To JSON
String json = JSON.toJSONString(obj);
```

### HTTP Client
Use Spring's `RestTemplate` or `HttpClient` for API calls. See `BaseClient` and existing implementations.

## Creating a New Plugin

### 1. Create Plugin Module
Create a new Maven module under root:
```bash
mkdir metersphere-phabricator-plugin
```

### 2. Add to Parent POM
```xml
<modules>
    <module>metersphere-plugin-sdk</module>
    <module>metersphere-platform-plugin-sdk</module>
    <module>metersphere-phabricator-plugin</module>
</modules>
```

### 3. Create pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.metersphere</groupId>
    <artifactId>metersphere-phabricator-plugin</artifactId>
    <version>2.10.0</version>
    
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.metersphere</groupId>
            <artifactId>metersphere-platform-plugin-sdk</artifactId>
            <version>1.6.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4. Implement Required Classes

#### Platform Meta Info (CRITICAL - Must extend AbstractPlatformMetaInfo)
```java
package io.metersphere.platform.impl;

import io.metersphere.platform.api.AbstractPlatformMetaInfo;

public class PhabricatorPlatformMetaInfo extends AbstractPlatformMetaInfo {

    public static final String KEY = "Phabricator";  // Must be capitalized

    public PhabricatorPlatformMetaInfo() {
        super(PhabricatorPlatformMetaInfo.class.getClassLoader());  // Required!
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getVersion() {
        return "2.10.0";
    }

    @Override
    public boolean isXpack() {
        return false;  // false for open source, true for enterprise only
    }

    @Override
    public boolean isThirdPartTemplateSupport() {
        return false;
    }
}
```

**IMPORTANT: Do NOT implement PluginMetaInfo directly. You MUST extend AbstractPlatformMetaInfo.**

#### Platform Implementation
Extend `AbstractPlatform` and implement required methods from `Platform` interface:
- `getProjectOptions()`
- `getFormOptions()`
- `addIssue()`
- `updateIssue()`
- `deleteIssue()`
- `syncIssues()`
- `getStatusList()`
- `getDemands()`
- `validateIntegrationConfig()`
- `validateProjectConfig()`
- `validateUserConfig()`

### Testing
Place tests in `src/test/java/io/metersphere/`:
```java
package io.metersphere;

import org.junit.Test;

public class PhabricatorTest {
    @Test
    public void testMethod() {
        // Test code
    }
}
```

## Common Dependencies
```xml
<!-- Spring Web (for RestTemplate) -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>

<!-- Apache Commons -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-collections4</artifactId>
</dependency>

<!-- HTTP Client -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

## Best Practices
1. Always validate config before use
2. Use meaningful exception messages (preferably in Chinese for user-facing errors)
3. Handle null checks with `StringUtils.isNotBlank()` and `CollectionUtils.isNotEmpty()`
4. Use `LinkedHashMap` for ordered JSON fields
5. Log errors with `LogUtil.error(e)` before throwing exceptions
6. Use builder pattern for complex request objects
7. Keep domain classes immutable where possible

## Troubleshooting

### Plugin Not Showing in Service Integration

**Symptom:** Plugin doesn't appear in the platform list, or error "Cannot invoke Class.getConstructor because clazz is null"

**Common Causes:**

1. **Wrong class hierarchy**: Must extend `AbstractPlatformMetaInfo`, not implement `PluginMetaInfo` directly
2. **Missing constructor**: Must have `super(ClassLoader)` constructor
3. **isXpack() returns true**: For open source MeterSphere, must return `false`
4. **Old plugin versions**: Delete ALL old plugin versions before uploading new one
5. **Plugin cache**: Restart MeterSphere after uploading new plugin

### Deployment Checklist

Before deploying a new plugin version:
1. Delete old plugin from MeterSphere UI (both 企业版 and 开源版 tabs)
2. Verify no old jars remain in `/opt/metersphere/data/body/plugin/`
3. Clean old plugins from MinIO storage
4. Upload new jar
5. Restart MeterSphere service
6. Check logs for successful load

### SDK Versions

| Component | Version |
|-----------|---------|
| metersphere-platform-plugin-sdk | 1.6.0 |
| metersphere-plugin-sdk | 1.2.0 |
| Java (plugin modules) | 17 |

## Known Issues

### validateUserConfig() - API Token Validation Bug

**Status:** Known Bug - Not Yet Fixed

**Description:** When a user configures a personal API token override in "accountConfig" (用户信息), the validation fails because:

1. The `accountConfig` in `frontend.json` only contains `apiToken` field (no `url`)
2. The URL should come from the integration config (serviceIntegration)
3. Current code in `validateUserConfig()` doesn't merge configs properly

**Current Behavior:**
- User provides personal apiToken in accountConfig → validation fails with "Phabricator configuration is not set"

**Expected Behavior:**
- User provides personal apiToken → validation uses URL from integration config + apiToken from user config
- User doesn't provide apiToken (blank) → silently skip validation (use integration token)

**Location:**
- `metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/impl/PhabricatorPlatform.java:97-107`

**Fix Required:**
```java
@Override
public void validateUserConfig(String userConfig) {
    if (StringUtils.isBlank(userConfig)) {
        return;
    }
    PhabricatorConfig userConfigObj = JSON.parseObject(userConfig, PhabricatorConfig.class);
    if (userConfigObj == null || StringUtils.isBlank(userConfigObj.getApiToken())) {
        return;  // No valid apiToken - silently skip
    }
    PhabricatorConfig integrationConfig = getIntegrationConfig();
    if (integrationConfig == null || StringUtils.isBlank(integrationConfig.getUrl())) {
        return;  // Can't validate without URL - silently skip
    }
    // Merge configs and test
    PhabricatorConfig testConfig = new PhabricatorConfig();
    testConfig.setUrl(integrationConfig.getUrl());
    testConfig.setApiToken(userConfigObj.getApiToken());
    
    PhabricatorClient testClient = new PhabricatorClient(testConfig);
    testClient.testConnection();
    testClient.close();
}
```

**Discussion (2026-02-24):**
- apiToken in accountConfig is optional (user can skip personal override)
- If user provides blank apiToken, should pass silently (not throw error)
- If user provides valid apiToken, should merge with URL from integration config

---

## Phabricator Conduit API Reference

### maniphest.edit

Create or update Maniphest tasks.

**Endpoint:** `POST /api/maniphest.edit`

**Parameters:**
- `transactions` - List of transaction objects
- `objectIdentifier` - (optional) Task ID, PHID, or monogram (e.g., "T123"). Omit to create new task.

**Transaction Types:**
| Type | Description | Value Type |
|------|-------------|------------|
| `title` | Set task title | string |
| `description` | Set task description | string |
| `status` | Change task status | string (e.g., "open", "resolved") |
| `priority` | Change priority | string |
| `subtype` | Set task subtype | string (e.g., "task", "bug", "feature") |
| `projects.add` | Add project tags | list of PHIDs |
| `projects.remove` | Remove project tags | list of PHIDs |
| `projects.set` | Set project tags (overwrite) | list of PHIDs |
| `owner` | Assign owner | PHID |
| `comment` | Add comment | string |

**Response:**
```json
{
  "object": {
    "phid": "PHID-TASK-XXXXX"
  },
  "transactions": [
    {"phid": "PHID-YYYY-1111"},
    {"phid": "PHID-YYYY-2222"}
  ]
}
```

**Note:** The response only returns `phid`, NOT the numeric ID. Use `maniphest.search` to get the numeric ID.

---

### maniphest.search

Search for Maniphest tasks.

**Endpoint:** `POST /api/maniphest.search`

**Parameters:**
- `constraints` - Search constraints
- `attachments` - Optional attachments (e.g., `{"projects": true}`)

**Common Constraints:**
| Key | Description | Value Type |
|-----|-------------|------------|
| `ids` | Filter by numeric IDs | list of integers |
| `phids` | Filter by PHIDs | list of PHIDs |
| `status` | Filter by status | string |
| `projectPHIDs` | Filter by project | list of PHIDs |

**Response:**
```json
{
  "result": {
    "data": [
      {
        "id": 123,
        "phid": "PHID-TASK-XXXXX",
        "fields": {
          "name": "Task Title",
          "status": {"value": "open"},
          ...
        }
      }
    ],
    "cursor": {"limit": 100, "after": null}
  }
}
```

---

### Task ID vs PHID

- **Task ID (Monogram)**: User-facing identifier like `T123`
- **PHID**: Internal Phabricator identifier like `PHID-TASK-XXXXX`

**Conversion:**
```java
// ID to PHID (for updates)
String phid = "PHID-TASK-" + id;  // "T123" -> "PHID-TASK-123"

// PHID to ID (after create)
String id = phid.substring(phid.lastIndexOf("-") + 1);  // "PHID-TASK-XXXXX" -> "XXXXX"

// Store as T{id}
String platformId = "T" + id;  // "T123"
```

---

### Example: Create Task Flow

```java
// 1. Create task with maniphest.edit
List<Map<String, Object>> transactions = new ArrayList<>();
transactions.add(Map.of("type", "title", "value", "My Task"));
transactions.add(Map.of("type", "description", "value", "Task description"));

Map<String, Object> result = client.editTask(null, transactions);

// 2. Get PHID from response
String phid = (String) ((Map) ((Map) result.get("result")).get("object")).get("phid");

// 3. Search to get numeric ID
String numericId = client.getTaskIdByPHID(phid);

// 4. Store as T{id}
String platformId = "T" + numericId;  // e.g., "T123"
```

---

## Feature Comparison: Phabricator vs Jira

Comparison with Jira plugin (v2.x-jira9) to identify missing features:

### Implemented Features

| Feature | Status |
|---------|--------|
| validateIntegrationConfig() | ✅ Implemented |
| validateProjectConfig() | ✅ Implemented |
| validateUserConfig() | ✅ Implemented |
| getStatusList() | ✅ Implemented |
| getDemands() | ✅ Implemented |
| addIssue() | ✅ Implemented |
| updateIssue() | ✅ Implemented |
| deleteIssue() | ✅ Implemented |
| syncIssues() | ✅ Implemented |
| isAttachmentUploadSupport() | ✅ Returns true |
| remarkupToMarkdown() | ✅ Implemented |
| markdownToRemarkup() | ✅ Implemented |
| file.upload API | ✅ Implemented |
| projectExists() | ✅ Implemented |
| getTaskIdByPHID() | ✅ Implemented |
| getFormOptions() | ✅ Implemented |
| getIssueTypes() | ✅ Implemented |
### Bug Fixes Applied (2026-02-25)

| Issue | Fix |
|-------|-----|
| Transaction type `"project"` invalid | Changed to `"projects.add"` for adding project tags |
| Constructed fake PHID from ID | Now uses numeric ID directly with maniphest.edit |
| Default subtype was "task" | Changed to "bug" (more common in Phorge) |
| HTTP errors being retried | HTTP 4xx/5xx now fail immediately (not retried) |
| Debug logs using LogUtil.info() | Changed to LogUtil.debug() |
| API token logged in plain text | Added maskToken() to mask token as ***MASKED*** |
| Comments didn't show task ID | Added humanReadableId (T+id) in delete comments |

### Subtype Handling

- **Create new issue**: Default subtype is "bug"
- **Update existing issue**: Preserves existing subtype, defaults to "bug" if none
- **Available subtypes**: default, bug, defect, feature, businessrequest, issue, general, tech, design, change

### Missing/Incomplete Features (vs Jira)

| Priority | Method | Current Status | Description |
|----------|--------|----------------|-------------|
| **High** | getThirdPartCustomField() | Returns empty list | Custom field support |
| **High** | setUserConfig() | NOT IMPLEMENTED | User-specific config |
| **Medium** | syncIssuesAttachment() | Empty stub | Attachment sync |
| **Medium** | getTransitions() | NOT IMPLEMENTED | Workflow transitions |
| **Low** | getProjectAllComponents() | NOT IMPLEMENTED | Project components |
| **Low** | getSprintOptions() | NOT IMPLEMENTED | Available sprints |
---

## Retry Logic and Error Handling

### Overview

The PhabricatorClient implements automatic retry logic to handle transient failures when communicating with Phabricator/Phorge instances.

### Configuration

| Setting | Default | Description |
|---------|-------- Max Retries |-|-------------|
| 3 | Number of retry attempts before giving up |
| Initial Delay | 1000ms | Starting delay between retries |
| Backoff Factor | 2 | Exponential backoff multiplier (1s → 2s → 4s) |
| Connect Timeout | 30s | Time to establish connection |
| Response Timeout | 120s | Time to wait for response |
| Connection Request Timeout | 30s | Time to wait for connection from pool |

### Retry Conditions

The client automatically retries on these error types:

| Error Pattern | Examples | Retryable |
|---------------|----------|-----------|
| Timeout | "timeout", "SocketTimeoutException" | ✅ Yes |
| Connection | "connection refused", "connection reset" | ✅ Yes |
| Empty Response | "Empty response body" | ✅ Yes |
| Parse Error | "Failed to parse JSON" | ✅ Yes |

**Non-retryable errors (won't fix themselves by retrying):**
- HTTP 4xx errors (400 Bad Request, 401 Unauthorized, 403 Forbidden)
- HTTP 5xx errors (500 Server Error, 503 Service Unavailable)
- Phabricator API errors with `error_code` in response body

### Implementation

```java
// PhabricatorClient.java - callConduitWithRetry()
public Map<String, Object> callConduit(String method, Map<String, Object> params) {
    return callConduitWithRetry(method, params, 3, 1000);
}

private Map<String, Object> callConduitWithRetry(String method, Map<String, Object> params, 
                                                   int maxRetries, long initialDelayMs) {
    for (int attempt = 0; attempt <= maxRetries; attempt++) {
        try {
            // ... API call logic
        } catch (Exception e) {
            if (attempt < maxRetries && isRetryableException(e)) {
                long delayMs = initialDelayMs * (1L << attempt);
                LogUtil.warn("Phabricator API call failed (attempt " + (attempt + 1) + 
                    "), retrying in " + delayMs + "ms: " + method);
                Thread.sleep(delayMs);
            }
        }
    }
}
```

### Pagination and Partial Results

When fetching large datasets (e.g., 15,000 tasks), the client:

1. Fetches 100 results per page (Phabricator limit)
2. Automatically follows `cursor.after` tokens for pagination
3. **Returns partial results** if max limit is reached or error occurs mid-pagination

```java
// Default max 5000 results to prevent memory exhaustion
private List<Map<String, Object>> searchWithPagination(String method, Map<String, Object> params) {
    return searchWithPagination(method, params, 5000);
}
```

### Pitfalls to Avoid

| Pitfall | Problem | Solution |
|---------|---------|----------|
| No timeouts | Requests hang indefinitely on slow Phabricator | Added 30s/120s timeouts |
| Retry on HTTP errors | Wasting time retrying invalid params | HTTP 4xx/5xx not retried |
| No retry on parse error | Single malformed JSON loses all progress | Added retry for parse errors |
| Load all 15K tasks | Memory exhaustion with large datasets | Default 5000 result limit |
| Silent failures | Can't tell if pagination ended normally or due to error | Added warning logs |
| Blocking retries | Thread blocks during retry delay | Exponential backoff reduces impact |

### Memory Considerations

For 15,000 tasks with long descriptions:

| Scenario | Est. Memory |
|----------|-------------|
| 15K tasks (no limit) | 750MB+ (10KB-100KB per task) |
| Limited to 5000 | ~250MB max |
| 100 per page, 120s timeout | Plenty of time per page |

---

## Debug Mode

### Enabling Debug Mode

Add `debugMode: true` to the integration configuration in `frontend.json`:

```json
{
  "serviceIntegration": {
    "formItems": [
      {
        "name": "url",
        "type": "input",
        "required": true,
        "label": "organization.integration.phabricator_url"
      },
      {
        "name": "apiToken",
        "type": "password",
        "required": true,
        "label": "organization.integration.phabricator_token"
      },
      {
        "name": "debugMode",
        "type": "switch",
        "label": "Enable Debug Logging",
        "defaultValue": false
      }
    ]
  }
}
```

### What Gets Logged

When `debugMode` is enabled, the client logs (using `LogUtil.debug()`):

| Log Type | Example |
|----------|---------|
| API Method | `[Phabricator DEBUG] Calling API: maniphest.search (attempt 1)` |
| URL | `[Phabricator DEBUG] URL: https://phorge.example.com/api/maniphest.search` |
| Request | `[Phabricator DEBUG] Request: params={"token":"***MASKED***",...}&output=json` |
| Response | `[Phabricator DEBUG] Response: {"result":{...}} [truncated]` |
| Pagination | `[Phabricator DEBUG] maniphest.search page 5: 100 results, total: 500` |

**Note:** The API token is always masked as `***MASKED***` in request logs.

### Response Truncation

Debug logs truncate responses longer than 2000 characters to prevent log flooding:

```
[Phabricator DEBUG] Response: {"result":{"data":[{"id":1,...[truncated]
```

### Security Considerations

⚠️ **Warning:** Debug mode logs sensitive data:

- API tokens (in request body)
- Full request/response payloads
- Task descriptions (may contain sensitive info)

**Only enable debug mode:**
- During development/testing
- When diagnosing issues
- Never in production with real credentials

### Implementation

The debug mode uses `LogUtil.debug()` for logging (not `info`), and masks the API token:

```java
if (config.isDebugMode()) {
    LogUtil.debug("[Phabricator DEBUG] Calling API: " + method + " (attempt " + (attempt + 1) + ")");
    LogUtil.debug("[Phabricator DEBUG] URL: " + url);
    LogUtil.debug("[Phabricator DEBUG] Request: " + maskToken(formBody));
}

// Response logging (truncated)
if (config.isDebugMode()) {
    String truncatedResponse = responseBody.length() > 2000 
        ? responseBody.substring(0, 2000) + "... [truncated]" 
        : responseBody;
    LogUtil.debug("[Phabricator DEBUG] Response: " + truncatedResponse);
}

// Token masking helper
private String maskToken(String formBody) {
    return formBody.replaceAll("\"token\":\"[^\"]+\"", "\"token\":\"***MASKED***\"");
}
```

---

| Java (SDK modules) | 11 |
