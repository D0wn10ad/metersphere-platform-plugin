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
- `metersphere-phabricator-plugin/src/main/java/io/metersphere/platform/impl/PhabricatorPlatform.java:330-340`

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
| validateUserConfig() | ✅ Implemented (see Known Bug above) |
| getStatusList() | ✅ Implemented |
| getDemands() | ✅ Implemented |
| addIssue() | ✅ Implemented |
| updateIssue() | ✅ Implemented (aligned with addIssue via buildCommonTransactions) |
| deleteIssue() | ✅ Implemented (subtype preserved, same pattern as updateIssue) |
| syncIssues() | ✅ Implemented |
| isAttachmentUploadSupport() | ✅ Returns true |
| remarkupToMarkdown() | ✅ Implemented |
| markdownToRemarkup() | ✅ Implemented |
| file.upload API | ✅ Implemented |
| projectExists() | ✅ Implemented |
| getTaskIdByPHID() | ✅ Implemented |
| getFormOptions() | ✅ Implemented |
| getIssueTypes() | ✅ Implemented |
| buildCommonTransactions() | ✅ Extracted (shared by addIssue, updateIssue, deleteIssue) |
| getEnvFromRequest() | ✅ Extracted (custom.igus.env field extraction) |
| toPrettyJSONString() | ✅ Added to SDK JSON utility |
| Pretty-print debug logging | ✅ Human-readable JSON in debug output |

---

## Bug Fixes Applied

| Date | Issue | Fix |
|------|-------|-----|
| 2026-02-25 | Transaction type `"project"` invalid | Changed to `"projects.add"` for adding project tags |
| 2026-02-25 | Constructed fake PHID from ID | Now uses numeric ID directly with maniphest.edit |
| 2026-02-25 | Default subtype was "task" | Changed to "bug" (more common in Phorge) |
| 2026-02-25 | HTTP errors being retried | HTTP 4xx/5xx now fail immediately (not retried) |
| 2026-02-25 | Debug logs using LogUtil.info() | Changed to LogUtil.debug() |
| 2026-02-25 | API token logged in plain text | Added maskToken() to mask token as ***MASKED*** |
| 2026-02-25 | Comments didn't show task ID | Added humanReadableId (T+id) in delete comments |
| 2026-03-04 | Debug logs show URL-encoded params | Added `toPrettyJSONString()` and pretty-printed JSON debug line; kept original formBody log too |
| 2026-03-04 | Duplicate junit-jupiter & maven-surefire-plugin in POM | Removed duplicate entries from phabricator plugin pom.xml |
| 2026-03-04 | addIssue/updateIssue transaction mismatch | Extracted `buildCommonTransactions()` so both methods send same fields (title, description, priority, projects.add, custom.igus.env). updateIssue now also sends priority, projects.add, and env (previously missing). |
| 2026-03-04 | deleteIssue hardcoded subtype to "bug" | Now preserves existing subtype before closing (same pattern as updateIssue) |
| 2026-03-04 | Unused imports in PhabricatorPlatform.java | Removed `java.net.HttpURLConnection` and `java.net.URL` |

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
---

## Inline Image Processing (CRITICAL)

**Pattern:** Standard Markdown images: `![alt](url)`

**Regex:**
```java
Pattern fullPattern = Pattern.compile("!\\[([^\\]]+)\\]\\(([^)]+)\\)");
```

**Matches:** `![logo.png](/resource/md/get?fileName=b849ad99.png)`

**Capture groups:**
- Group 1: Alt text (e.g., `logo.png`)
- Group 2: URL (e.g., `/resource/md/get?fileName=b849ad99.png`)

**Filename extraction:** From URL parameter `?fileName=xxx.png` using `lastIndexOf("=")`

**IMPORTANT:** Do NOT change this pattern without testing. Common mistakes:
- Missing closing `)` in regex - causes `PatternSyntaxException`
- Wrong escape sequence - use `\\[` not `\[`
- Extra spaces before semicolon

**History:**
| Date | Pattern | Notes |
|------|---------|-------|
| 2026-03-01 | `!\\[\\[([^\\]]+)\\|([^\\]]+)\\]\\]` | Initial - matches `![[URL\|filename]]` |
| 2026-03-02 | `!\\[\\[[^\\]]+\\]\\]` | Simplified - matches `![[...]]`, extracts via string split |
| 2026-03-03 | `!\\[([^\\]]+)\\]\\(([^)]+)\\)` | Standard Markdown - matches `![alt](url)` |

### File Upload and GUID Retrieval

**Process to embed inline images in Phabricator Remarkup:**

1. **Upload file** via `file.upload` API:
   - Request: `{name: "filename.png", data_base64: "...", __conduit__: {token: "..."}}`
   - Response: `{"result": "PHID-FILE-xxx", ...}`

2. **Query PHID** via `phid.query` API to get GUID:
   - Request: `{phids: ["PHID-FILE-xxx"]}`
   - Response:
     ```json
     {
       "PHID-FILE-xxx": {
         "phid": "PHID-FILE-xxx",
         "uri": "https://phablab.igus.cn/F29271",
         "name": "F29271",  // <-- This is the GUID
         "fullName": "F29271: filename.png",
         "status": "open"
       }
     }
     ```

3. **Embed image** in Remarkup using `{GUID}` format:
   - Format: `{F29271}`
   - This displays the uploaded image inline in the task description

**Key insight:** The `name` field from `phid.query` response is the GUID (e.g., "F29271"), NOT the PHID. Use `{F29271}` for inline embedding.

---

## Future Considerations

### File Attachment Sync (syncIssuesAttachment)

**Current state:** `isAttachmentUploadSupport()` returns `true` but `syncIssuesAttachment()` is a no-op.  
**Inline image upload** in descriptions works via `processInlineImages()` → `file.upload` → `{Fnnn}` embed.

**Why file sync was deferred:**  
The Phabricator Conduit API has two hard limitations:
1. **No `file.delete`** — uploaded files are permanent (append-only)
2. **No comment modification/deletion** — once a `{Fnnn}` reference is added as a comment, it cannot be removed via API

**Approaches evaluated:**

| Approach | Upload | Delete | Viable? |
|----------|--------|--------|---------|
| **Comment-based** | `file.upload` → `{Fnnn}` → new comment | ❌ Can't remove comment | Simple upload, but DELETE impossible |
| **Description-based** | Append `{Fnnn}` to description footer | Remove from description via `editTask` | True unlink, but next `updateIssue()` could overwrite the footer |
| **Custom field** | Store refs in a `custom.igus.*` text field | Remove ref from field value | Stable, but requires Phabricator form config |

**To implement in the future:**  
Choose a storage approach (description footer or custom field), then wire `syncIssuesAttachment()` for UPLOAD + matching DELETE logic. The building blocks already exist (`uploadFile`, `encodeFileToBase64`, `phid.query`, `editTask`).
