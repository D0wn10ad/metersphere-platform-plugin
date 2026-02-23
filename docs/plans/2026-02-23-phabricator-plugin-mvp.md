# Phabricator Plugin MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a compilable MeterSphere Phabricator plugin with UI configuration fields for service integration, project config, and user account settings.

**Architecture:** Create a new Maven module following the existing Jira/ZenTao plugin pattern. The plugin will have stub implementations of the Platform interface for MVP, with actual Conduit API integration to be added in future phases.

**Tech Stack:** Java 17, Maven, Spring Web (RestTemplate), metersphere-platform-plugin-sdk 1.6.0

---

### Task 1: Create Plugin Module Structure and pom.xml

**Files:**
- Create: `metersphere-phabricator-plugin/pom.xml`
- Modify: `pom.xml` (add module)

**Step 1: Create pom.xml**

Create `metersphere-phabricator-plugin/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 2: Update parent pom.xml**

Modify `pom.xml` to add the new module:

```xml
<modules>
    <module>metersphere-plugin-sdk</module>
    <module>metersphere-platform-plugin-sdk</module>
    <module>metersphere-jira-plugin</module>
    <module>metersphere-zentao-plugin</module>
    <module>metersphere-phabricator-plugin</module>
</modules>
```

**Step 3: Commit**

```bash
git add metersphere-phabricator-plugin/pom.xml pom.xml
git commit -m "feat: add Phabricator plugin module structure"
```

---

### Task 2: Create Domain Classes

**Files:**
- Create: `metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/domain/PhabricatorConfig.java`
- Create: `metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/domain/PhabricatorProjectConfig.java`

**Step 1: Create PhabricatorConfig.java**

```java
package io.metersphere.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhabricatorConfig {
    private String url;
    private String apiToken;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
}
```

**Step 2: Create PhabricatorProjectConfig.java**

```java
package io.metersphere.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhabricatorProjectConfig {
    private String projectPHID;

    public String getProjectPHID() {
        return projectPHID;
    }

    public void setProjectPHID(String projectPHID) {
        this.projectPHID = projectPHID;
    }
}
```

**Step 3: Commit**

```bash
git add metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/domain/
git commit -m "feat: add Phabricator config domain classes"
```

---

### Task 3: Create Platform Meta Info

**Files:**
- Create: `metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/impl/PhabricatorPlatformMetaInfo.java`

**Step 1: Create PhabricatorPlatformMetaInfo.java**

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
    public String getLabel() {
        return "Phabricator";
    }

    @Override
    public String getVersion() {
        return "2.10.0";
    }

    @Override
    public boolean isXpack() {
        return true;
    }

    @Override
    public boolean isThirdPartTemplateSupport() {
        return false;
    }
}
```

**Step 2: Commit**

```bash
git add metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/impl/PhabricatorPlatformMetaInfo.java
git commit -m "feat: add PhabricatorPlatformMetaInfo"
```

---

### Task 4: Create PhabricatorClient (Stub)

**Files:**
- Create: `metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/client/PhabricatorClient.java`

**Step 1: Create PhabricatorClient.java**

```java
package io.metersphere.platform.client;

import io.metersphere.platform.domain.PhabricatorConfig;
import org.springframework.web.client.RestTemplate;

public class PhabricatorClient {
    private PhabricatorConfig config;
    private RestTemplate restTemplate;

    public void setConfig(PhabricatorConfig config) {
        this.config = config;
    }

    public void auth() {
        // Stub - to be implemented in future phase
    }

    public boolean testConnection() {
        // Stub - to be implemented in future phase
        return true;
    }
}
```

**Step 2: Commit**

```bash
git add metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/client/PhabricatorClient.java
git commit -m "feat: add PhabricatorClient stub"
```

---

### Task 5: Create PhabricatorPlatform (Stub Implementation)

**Files:**
- Create: `metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/impl/PhabricatorPlatform.java`

**Step 1: Create PhabricatorPlatform.java**

```java
package io.metersphere.platform.impl;

import io.metersphere.base.domain.IssuesWithBLOBs;
import io.metersphere.platform.api.AbstractPlatform;
import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.domain.*;
import io.metersphere.plugin.exception.MSPluginException;
import io.metersphere.plugin.utils.LogUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

    public PhabricatorConfig getIntegrationConfig() {
        return getIntegrationConfig(PhabricatorConfig.class);
    }

    @Override
    public List<DemandDTO> getDemands(String projectConfig) {
        return new ArrayList<>();
    }

    @Override
    public IssuesWithBLOBs addIssue(PlatformIssuesUpdateRequest request) {
        return request;
    }

    @Override
    public IssuesWithBLOBs updateIssue(PlatformIssuesUpdateRequest request) {
        return request;
    }

    @Override
    public void deleteIssue(String id) {
        // Stub
    }

    @Override
    public void validateIntegrationConfig() {
        phabricatorClient.auth();
    }

    @Override
    public void validateProjectConfig(String projectConfigStr) {
        if (StringUtils.isBlank(projectConfigStr)) {
            MSPluginException.throwException("请在项目中添加项目配置！");
        }
    }

    @Override
    public void validateUserConfig(String userConfig) {
        // Stub
    }

    @Override
    public boolean isAttachmentUploadSupport() {
        return false;
    }

    @Override
    public SyncIssuesResult syncIssues(SyncIssuesRequest request) {
        return new SyncIssuesResult();
    }

    @Override
    public List<PlatformCustomFieldItemDTO> getThirdPartCustomField(String projectConfig) {
        return new ArrayList<>();
    }

    @Override
    public List<PlatformStatusDTO> getStatusList(String projectConfig) {
        return new ArrayList<>();
    }
}
```

**Step 2: Commit**

```bash
git add metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/impl/PhabricatorPlatform.java
git commit -m "feat: add PhabricatorPlatform stub implementation"
```

---

### Task 6: Create Frontend JSON Configuration

**Files:**
- Create: `metersphere-phabricator-plugin/src/main/resources/json/frontend.json`

**Step 1: Create frontend.json**

```json
{
  "serviceIntegration": {
    "label": "Phabricator",
    "image": "/static/index.png",
    "tips": "请在 Phabricator 设置中生成 API Token (Settings → API Tokens)",
    "formItems": [
      {
        "name": "url",
        "type": "input",
        "defaultValue": "",
        "required": true,
        "i18n": true,
        "label": "organization.integration.phabricator_url",
        "message": "organization.integration.input_phabricator_url"
      },
      {
        "name": "apiToken",
        "type": "password",
        "defaultValue": "",
        "required": true,
        "i18n": true,
        "label": "organization.integration.api_token",
        "message": "organization.integration.input_api_token"
      }
    ]
  },

  "projectConfig": {
    "formItems": [
      {
        "name": "projectPHID",
        "type": "input",
        "defaultValue": "",
        "required": true,
        "i18n": true,
        "label": "project.phabricator_project",
        "withProjectCheck": true,
        "message": "project.platform.phabricator_project_require"
      }
    ]
  },

  "accountConfig": {
    "label": "organization.integration.phabricator_info",
    "instructionsInfo": "organization.integration.phabricator_prompt_information",
    "i18n": true,
    "formItems": [
      {
        "name": "apiToken",
        "type": "password",
        "defaultValue": "",
        "required": false,
        "i18n": true,
        "label": "organization.integration.api_token",
        "message": "organization.integration.input_api_token"
      }
    ]
  }
}
```

**Step 2: Commit**

```bash
git add metersphere-phabricator-plugin/src/main/resources/json/frontend.json
git commit -m "feat: add frontend.json configuration"
```

---

### Task 7: Create Plugin Icon (Placeholder)

**Files:**
- Create: `metersphere-phabricator-plugin/src/main/resources/static/index.png`

**Step 1: Copy placeholder icon**

Copy from Jira plugin as placeholder:

```bash
cp metersphere-jira-plugin/src/main/resources/static/index.png metersphere-phabricator-plugin/src/main/resources/static/index.png
```

**Step 2: Commit**

```bash
git add metersphere-phabricator-plugin/src/main/resources/static/index.png
git commit -m "feat: add plugin icon placeholder"
```

---

### Task 8: Build and Verify

**Step 1: Build the plugin**

```bash
mvn clean install -pl metersphere-phabricator-plugin -am
```

Expected: BUILD SUCCESS

**Step 2: Package the plugin**

```bash
mvn clean package -pl metersphere-phabricator-plugin
```

Expected: Generate `metersphere-phabricator-plugin/target/metersphere-phabricator-plugin-2.10.0-jar-with-dependencies.jar`

**Step 3: Verify jar exists**

```bash
ls -la metersphere-phabricator-plugin/target/*.jar
```

Expected: Jar file exists

**Step 4: Commit**

```bash
git add metersphere-phabricator-plugin/target/
git commit -m "feat: build Phabricator plugin"
```

---

### Task 9: Create Initial Test (Optional for MVP)

**Files:**
- Create: `metersphere-phabricator-plugin/src/test/java/io/metersphere/PhabricatorTest.java`

**Step 1: Create test class**

```java
package io.metersphere;

import org.junit.Test;

public class PhabricatorTest {
    @Test
    public void testPlaceholder() {
        // Placeholder test for future implementation
    }
}
```

**Step 2: Run test**

```bash
mvn test -pl metersphere-phabricator-plugin -Dtest=PhabricatorTest
```

Expected: PASS

**Step 3: Commit**

```bash
git add metersphere-phabricator-plugin/src/test/
git commit -m "test: add placeholder test"
```

---

## Summary

After completing all tasks, you will have:
- Compilable Maven module `metersphere-phabricator-plugin`
- UI configuration in 3 sections: serviceIntegration, projectConfig, accountConfig
- Stub implementations of Platform interface
- Runnable jar with dependencies

**Total commits expected: 8-9**
