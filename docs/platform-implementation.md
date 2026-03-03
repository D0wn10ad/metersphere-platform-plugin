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
