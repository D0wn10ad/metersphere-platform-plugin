# Platform Implementation Checklist

## Required: Extend AbstractPlatformMetaInfo
```java
public class PhabricatorPlatformMetaInfo extends AbstractPlatformMetaInfo {
    public static final String KEY = "Phabricator";  // Must be capitalized

    public PhabricatorPlatformMetaInfo() {
        super(PhabricatorPlatformMetaInfo.class.getClassLoader());  // Required!
    }

    @Override public String getKey() { return KEY; }
    @Override public String getVersion() { return "2.10.0"; }
    @Override public boolean isXpack() { return false; }  // false for open source
}
```

## Required Interface Methods
Implement from `Platform` interface:
- `getProjectOptions()`, `getFormOptions()`
- `addIssue()`, `updateIssue()`, `deleteIssue()`, `syncIssues()`
- `getStatusList()`, `getDemands()`
- `validateIntegrationConfig()`, `validateProjectConfig()`, `validateUserConfig()`

## Data Model: IssuesWithBLOBs Inheritance Chain

The plugin's core data classes form a chain through two SDKs:

```
metersphere:domain (external dependency, version 2.10)
  └── Issues (base: id, title, status, platformStatus, platformId)
      └── IssuesWithBLOBs (extends Issues: adds description)

metersphere-platform-plugin-sdk (this project)
  └── PlatformIssuesDTO (extends IssuesWithBLOBs: used for sync results)
      └── PlatformIssuesUpdateRequest (extends PlatformIssuesDTO: holds request data)
```

Custom fields are represented as:
```
CustomField (MeterSphere domain)
  └── PlatformCustomFieldItemDTO (SDK: name, value, customData, etc.)
```

Key SDK classes live in `metersphere-platform-plugin-sdk`:
- `io.metersphere.platform.domain.PlatformIssuesUpdateRequest`
- `io.metersphere.platform.domain.PlatformIssuesDTO`
- `io.metersphere.platform.domain.PlatformCustomFieldItemDTO`

These are imported via `io.metersphere.platform.domain.*`.

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
