# Code Style Guidelines

## Java Version
- **SDK modules**: Java 11
- **Plugin modules**: Java 17

## Package Naming
```java
package io.metersphere.platform.impl;      // Platform implementation
package io.metersphere.platform.domain;    // DTOs/Entities
package io.metersphere.platform.client;     // API clients
package io.metersphere.platform.constants;  // Constants
package io.metersphere.platform.utils;      // Utilities
```

## Class Naming Conventions
| Type | Pattern | Example |
|------|---------|---------|
| Platform impl | `{Platform}Platform.java` | `PhabricatorPlatform.java` |
| Meta info | `{Platform}PlatformMetaInfo.java` | `PhabricatorPlatformMetaInfo.java` |
| Config | `{Platform}Config.java` | `PhabricatorConfig.java` |
| Domain models | `{Platform}{Entity}.java` | `PhabricatorTask.java` |

## Imports Order (IDE auto-organize)
1. `java.*` and `javax.*`
2. `org.*` (Spring, Apache Commons)
3. `io.metersphere.*` (internal SDKs)
4. Static imports

## Type Safety (MANDATORY)
- **NEVER** suppress errors: `as any`, `@ts-ignore`, `@ts-expect-error`
- **NEVER** use raw types: `List` → `List<String>`, `Map` → `Map<String, Object>`
- Use proper null handling: `StringUtils.isNotBlank()`, `CollectionUtils.isNotEmpty()`

## Error Handling
```java
// Plugin-specific errors
MSPluginException.throwException("message in Chinese for user-facing errors");

// Logging errors
LogUtil.error(e);           // Exception with stack trace
LogUtil.info("message");   // Info messages (NOT debug - MS doesn't handle debug well)

// NEVER empty catch blocks
try {
    // API call
} catch (Exception e) {
    LogUtil.error(e);
    // Handle appropriately
}
```

## JSON Handling
```java
import io.metersphere.plugin.utils.JSON;

// Parse JSON
MyObject obj = JSON.parseObject(jsonString, MyObject.class);

// Serialize to JSON
String json = JSON.toJSONString(obj);
```

## Logging
- Use `LogUtil.info()` for debug info (MS logging framework doesn't handle DEBUG well)
- Use `LogUtil.error(e)` for errors
- Mask sensitive data (API tokens) in logs:
```java
private String maskToken(String formBody) {
    return formBody.replaceAll("\"token\"\\s*:\\s*\"[^\"]+\"", "\"token\":\"***MASKED***\"");
}
```
- Use `JSON.toPrettyJSONString()` for human-readable debug output:
```java
LogUtil.info("[Phabricator DEBUG] Request JSON (pretty): " + maskToken(JSON.toPrettyJSONString(apiParams)));
```

## Common Dependencies
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```
