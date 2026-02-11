# Configuration Guide

## Overview

This guide covers all configuration options for the MeterSphere Phabricator plugin.

## Service Integration (Global Settings)

Configure in MeterSphere: **Settings → Service Integration → Phabricator**

### Required Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `url` | text | Phabricator instance URL | `https://phabricator.example.com` |
| `apiToken` | password | Conduit API token from Phabricator Settings | `api-xxxxxxxxxxxx` |

**How to get API Token:**
1. Log into your Phabricator instance
2. Go to Settings → API Tokens
3. Generate a new token with appropriate permissions
4. Copy the token to MeterSphere

### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `verifySSL` | checkbox | true | Verify SSL certificates. Disable only for development with self-signed certificates. |
| `timeout` | number | 30000 | HTTP request timeout in milliseconds (5000-120000) |

### Example Service Configuration

```json
{
  "url": "https://phabricator.company.com",
  "apiToken": "api-abc123xyz789",
  "verifySSL": true,
  "timeout": 30000
}
```

## Project Integration (Project-Level Settings)

Configure per MeterSphere project: **Project Settings → Phabricator Integration**

### Project Mapping

#### Primary Strategy (Required)

Select one of 4 strategies:

| Strategy | Description | Use Case |
|----------|-------------|----------|
| **ROOT_ONLY** | Always map to the root/parent project | Flatten organizational hierarchy |
| **CURRENT_CONTEXT** | Map to the current project where the issue resides | Track issues in current sprint/milestone |
| **HIERARCHICAL** | Maintain full project path: Root/Subproject/Milestone | Preserve complete organizational structure |
| **SMART_RESOLUTION** | Automatically choose best context | Flexible mapping that adapts to ticket context |

#### Default Project (Required)

Fallback MeterSphere project when primary strategy cannot determine mapping. This ensures no issues are lost during sync.

### Status Configuration

#### Closure Statuses

Phabricator statuses that indicate an issue is "closed". Issues with these statuses will be marked as "Closed" in MeterSphere.

**Default:** resolved, wontfix, invalid, spite, duplicate

**Additional common statuses:**
- `declined` - Issue declined
- `obsolete` - Issue no longer relevant
- `closed` - Generic closed status

#### Sync Closed Issues

- **Enabled (default)**: Sync all issues including closed ones
- **Disabled**: Only sync open issues

### Subtype Configuration

#### Allowed Subtypes

Which Maniphest subtypes to sync with MeterSphere:

| Subtype | Description |
|---------|-------------|
| `task` | Regular task |
| `story` | User story |
| `epic` | Epic/feature |
| `bug` | Bug report |
| `feature` | Feature request |

**Default:** task, story, epic

#### Default Subtype

Default subtype for new issues created from MeterSphere.

**Default:** task

### Tag Configuration

Project tags are **mandatory** and always extracted. Format: `[{type}] {name}`

Examples:
- `[Project] MainProject`
- `[Subproject] Sprint5`
- `[Milestone] Release1.0`

## Example Complete Configuration

### Service Integration

```json
{
  "serviceIntegration": {
    "url": "https://phabricator.company.com",
    "apiToken": "api-abc123xyz789",
    "verifySSL": true,
    "timeout": 30000
  }
}
```

### Project Integration

```json
{
  "projectIntegration": {
    "primaryStrategy": "SMART_RESOLUTION",
    "defaultProjectId": "default-backlog",
    "closureStatuses": ["resolved", "wontfix", "invalid"],
    "syncClosedIssues": true,
    "allowedSubtypes": ["task", "story", "epic"],
    "defaultSubtype": "task",
    "tagFormat": "[{type}] {name}"
  }
}
```

## Advanced Configuration

### Custom Status Mapping

Map Phabricator statuses to MeterSphere statuses:

```json
{
  "statusMapping": {
    "resolved": "Closed",
    "wontfix": "Closed - Won't Fix",
    "in-progress": "In Progress",
    "open": "Open"
  }
}
```

### Subtype Mapping

Map Phabricator subtypes to MeterSphere issue types:

```json
{
  "subtypeMapping": {
    "task": "Issue",
    "story": "Requirement",
    "epic": "Requirement",
    "bug": "Defect"
  }
}
```

## Troubleshooting

### Connection Issues

**Problem**: Cannot connect to Phabricator
- Verify URL is correct (include https://)
- Check API token is valid and not expired
- Ensure Phabricator instance is accessible from MeterSphere server

### Authentication Errors

**Problem**: "Authentication failed"
- Verify API token has required permissions
- Check if token is revoked in Phabricator
- Ensure token is copied correctly (no extra spaces)

### Project Mapping Issues

**Problem**: Issues not appearing in expected project
- Check primary strategy selection
- Verify default project is set
- Review project hierarchy in Phabricator

### Status Sync Issues

**Problem**: Issue status not syncing correctly
- Verify closure statuses configuration
- Check status mapping if custom mapping is used
- Ensure Phabricator status names match exactly (case-sensitive)

## Best Practices

1. **Start Simple**: Begin with ROOT_ONLY strategy, add complexity as needed
2. **Test First**: Use a test project before applying to production
3. **Tag Everything**: Use project tags for filtering and reporting
4. **Monitor Logs**: Check MeterSphere logs for sync errors
5. **Regular Review**: Periodically review and update configuration

## Security Considerations

- Store API tokens securely in MeterSphere (encrypted)
- Use HTTPS for all Phabricator connections
- Verify SSL certificates in production
- Limit API token permissions to minimum required
- Rotate API tokens periodically

## Support

For issues and questions:
- Review this configuration guide
- Check MeterSphere logs for error details
- Verify Phabricator API accessibility
- Test with minimal configuration first