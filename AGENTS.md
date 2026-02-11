# AGENTS.md - MeterSphere Platform Plugin Development Guide

This file contains build commands, code style guidelines, and development conventions for agentic coding agents working in this repository.

## Build System & Commands

This is a multi-module Maven project with the following structure:
- `metersphere-plugin-sdk` - Core plugin SDK
- `metersphere-platform-plugin-sdk` - Platform plugin SDK  
- `metersphere-jira-plugin` - Jira integration plugin
- `metersphere-zentao-plugin` - Zentao integration plugin

### Essential Commands

```bash
# Build entire project (skip tests)
mvn clean install -DskipTests

# Build with tests
mvn clean install

# Build specific module
mvn clean install -pl metersphere-jira-plugin

# Run single test class
mvn test -Dtest=RegularTest

# Package plugin with dependencies (creates jar-with-dependencies)
mvn clean package

# Build with specific Java version (Jira plugin uses Java 17)
export JAVA_HOME=/opt/jdk-17 && mvn clean install
```

### Java Version Requirements
- Core SDK modules: Java 11
- Jira plugin: Java 17  
- Zentao plugin: Java 11

## Code Style Guidelines

### Package Structure
- Use `io.metersphere.platform` for platform-specific code
- Use `io.metersphere.plugin` for core plugin functionality
- Domain objects: `io.metersphere.platform.domain`
- Client implementations: `io.metersphere.platform.client`
- Platform implementations: `io.metersphere.platform.impl`

### Import Organization
```java
// Standard Java imports first
import java.io.File;
import java.util.*;

// Third-party imports (Apache Commons, Spring, etc.)
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;

// MeterSphere imports last
import io.metersphere.platform.api.AbstractPlatform;
import io.metersphere.plugin.utils.JSON;
```

### Class & Method Naming
- Platform implementations: `[PlatformName]Platform` (e.g., `JiraPlatform`, `ZentaoPlatform`)
- Client classes: `[PlatformName]Client` or `[PlatformName][Version]Client` (e.g., `JiraClientV2`)
- Domain objects: Descriptive names with platform prefix if needed (e.g., `JiraUser`, `ZentaoConfig`)
- Methods: Use clear, descriptive verbs (`getProjectOptions`, `syncIssues`, `parseJiraLink`)

### Constants & Fields
```java
// Constants in UPPER_SNAKE_CASE
private static final String MD_IMAGE_DIR = "/opt/metersphere/data/image/markdown";
private static final String ATTACHMENT_NAME = "attachment";

// Protected fields for subclass access
protected JiraClientV2 jiraClientV2;
protected SimpleDateFormat sdfWithZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
```

### Error Handling
- Use `MSPluginException.throwException("message")` for plugin-specific errors
- Always validate configuration before use:
```java
public <T> T getIntegrationConfig(Class<T> clazz) {
    String config = request.getIntegrationConfig();
    if (StringUtils.isBlank(config)) {
        MSPluginException.throwException("配置为空");
    }
    return JSON.parseObject(config, clazz);
}
```

### JSON Processing
- Use the custom `JSON` utility class for all JSON operations
- Configure ObjectMapper to fail on unknown properties: `false`
- Use TypeReference for complex generic types

### Logging
- Use `LogUtil` utility for consistent logging
- Log important operations and errors with context

### HTTP Client Usage
- Extend `BaseClient` or `AbstractClient` for platform-specific clients
- Use Spring's `ResponseEntity` for HTTP responses
- Handle `HttpClientErrorException` appropriately

### Date/Time Handling
- Use `SimpleDateFormat` with timezone for API compatibility
- Consider Java 8 time API for new code (`LocalDateTime`, `OffsetDateTime`)
- Be consistent with date formats across platform integrations

### Testing Conventions
- Test classes in `src/test/java` with same package structure
- Use JUnit 4 (as seen in existing tests)
- Focus on regex patterns, link parsing, and data transformation logic
- Name test methods descriptively: `testParseJiraLink2MsLink()`

### Platform Integration Patterns
- Extend `AbstractPlatform` for new platform implementations
- Implement required methods from `Platform` interface
- Use `BeanUtils` for object copying when needed
- Handle attachment synchronization with proper path management

### Code Comments & Documentation
- Use Chinese comments for user-facing messages (as seen in existing code)
- Add JavaDoc for public APIs and complex logic
- Comment regex patterns and link transformation logic

### Dependencies Management
- Core SDK provides Jackson, Apache Commons, SLF4J, Lombok
- Platform SDK adds Spring Web, HttpClient5, JSoup
- Plugin modules use `provided` scope for platform SDK to avoid conflicts

### Security Considerations
- Never log sensitive configuration data
- Use proper encoding/decoding for URLs and file paths
- Validate all external inputs and API responses

## Plugin Development Workflow

1. Create new platform module following existing structure
2. Implement platform-specific domain objects
3. Create client class extending appropriate base client
4. Implement platform class extending `AbstractPlatform`
5. Add platform meta info implementation
6. Create assembly configuration for jar-with-dependencies
7. Add comprehensive tests for link parsing and data transformation
8. Update parent POM with new module reference