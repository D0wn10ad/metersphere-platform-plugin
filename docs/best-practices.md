# Best Practices

1. **Validate config** before use - throw `MSPluginException` if invalid
2. **Return null/empty collections** appropriately, not exceptions
3. **Use builder pattern** for complex request objects
4. **Keep domain classes immutable** where possible
5. **Use LinkedHashMap** for ordered JSON fields
6. **Wrap API calls** in try-catch, log before throwing

## Plugin-Specific Guidelines

### Configuration
- Always validate config before use
- Throw `MSPluginException` with Chinese messages for user-facing errors

### API Calls
- Wrap all API calls in try-catch blocks
- Log errors with `LogUtil.error(e)` before throwing
- Return null or empty collections appropriately (not exceptions)

### Data Structures
- Use builder pattern for complex request objects
- Keep domain classes immutable where possible
- Use `LinkedHashMap` for ordered JSON fields

### Logging
- Use `LogUtil.info()` for debug info (MS logging doesn't handle DEBUG well)
- Mask sensitive data (API tokens) in logs
