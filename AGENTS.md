# AGENTS.md - MeterSphere Platform Plugin Development Guide

> For detailed documentation, see the [docs/](docs/) folder.

## Quick Reference

### Build Commands
```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Single test class
mvn test -Dtest=PhabricatorClientTest

# Single test method
mvn test -Dtest=PhabricatorClientTest#testConfigSetting

# Package plugin
mvn clean package
```

### Project Structure
- **SDK modules**: Java 11
- **Plugin modules**: Java 17

### Key Conventions
- Package: `io.metersphere.platform.*`
- Error handling: `MSPluginException.throwException()`
- Logging: `LogUtil.error(e)`, `LogUtil.info()`
- JSON: `io.metersphere.plugin.utils.JSON`

## Documentation

| Document | Description |
|----------|-------------|
| [docs/README.md](docs/README.md) | Overview and project structure |
| [docs/build-commands.md](docs/build-commands.md) | Build, test, and package commands |
| [docs/code-style.md](docs/code-style.md) | Java version, naming, imports, types, error handling |
| [docs/testing.md](docs/testing.md) | Test location, JUnit 5 conventions |
| [docs/platform-implementation.md](docs/platform-implementation.md) | AbstractPlatformMetaInfo, required interface methods |
| [docs/best-practices.md](docs/best-practices.md) | Config validation, API calls, data structures |
