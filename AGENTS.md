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

#### Platform Meta Info
```java
package io.metersphere.platform.impl;

import io.metersphere.platform.api.PluginMetaInfo;

public class PhabricatorPlatformMetaInfo extends PluginMetaInfo {
    public static final String KEY = "Phabricator";
    
    @Override
    public String getId() {
        return KEY;
    }

    @Override
    public String getName() {
        return "Phabricator";
    }

    @Override
    public String getDescription() {
        return "Phabricator issue management platform";
    }
}
```

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
