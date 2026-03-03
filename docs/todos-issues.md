# Known Issues & TODOs

## Known Bugs (Not Yet Fixed)

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

## Missing/Incomplete Features (vs Jira)

| Priority | Method | Current Status | Description |
|----------|--------|----------------|-------------|
| **High** | getThirdPartCustomField() | Returns empty list | Custom field support |
| **High** | setUserConfig() | NOT IMPLEMENTED | User-specific config |
| **Medium** | syncIssuesAttachment() | Empty stub | Attachment sync |
| **Medium** | getTransitions() | NOT IMPLEMENTED | Workflow transitions |
| **Low** | getProjectAllComponents() | NOT IMPLEMENTED | Project components |
| **Low** | getSprintOptions() | NOT IMPLEMENTED | Available sprints |

---

## Implemented Features (Phabricator vs Jira)

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

---

## Bug Fixes Applied (2026-02-25)

| Issue | Fix |
|-------|-----|
| Transaction type `"project"` invalid | Changed to `"projects.add"` for adding project tags |
| Constructed fake PHID from ID | Now uses numeric ID directly with maniphest.edit |
| Default subtype was "task" | Changed to "bug" (more common in Phorge) |
| HTTP errors being retried | HTTP 4xx/5xx now fail immediately (not retried) |
| Debug logs using LogUtil.info() | Changed to LogUtil.debug() |
| API token logged in plain text | Added maskToken() to mask token as ***MASKED*** |
| Comments didn't show task ID | Added humanReadableId (T+id) in delete comments |

---

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
| Java (SDK modules) | 11 |
