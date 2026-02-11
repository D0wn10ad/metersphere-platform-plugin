# MeterSphere Phabricator Plugin

A MeterSphere platform plugin for integrating with Phabricator/Phorge issue tracking via the Conduit API.

**Version**: 0.1.0  
**MeterSphere Compatibility**: v2.10+  
**Status**: AI-Generated Prototype (OpenCode AI)

---

⚠️ **IMPORTANT**: This plugin was generated with AI assistance and serves as a functional prototype. Please review and test thoroughly before production use.

## Features

- Phabricator Conduit API integration
- 4 project mapping strategies:
  - ROOT_ONLY: Always map to root project
  - CURRENT_CONTEXT: Use current project context
  - HIERARCHICAL: Maintain full hierarchy path
  - SMART_RESOLUTION: Automatic context detection
- Mandatory project tags with full hierarchy traversal
- Configurable closure statuses (resolved, wontfix, invalid, spite, duplicate)
- Maniphest subtype support (Task, Story, Epic)
- Remarkup to Markdown conversion via `remarkup.process` API
- Link-based attachment handling (no double storage)

## Requirements

- MeterSphere v2.10.x
- Java 17+ (Java 21 recommended)
- Phabricator/Phorge instance with API token access

## Installation

1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Copy the JAR to MeterSphere plugin directory:
   ```bash
   cp target/metersphere-phabricator-plugin-0.1.0-jar-with-dependencies.jar /path/to/metersphere/plugins/
   ```

3. Restart MeterSphere

4. Configure via Settings → Service Integration → Phabricator

## Configuration

See [CONFIGURATION.md](CONFIGURATION.md) for detailed configuration options.

## Architecture

### Core Components

- **PhabricatorClient**: Conduit API client for all Phabricator communication
- **PhabricatorProjectResolver**: Implements 3-layer project mapping (Primary + Tags + Default)
- **PhabricatorStatusHandler**: Manages closure status detection and mapping
- **PhabricatorMarkupUtils**: Handles Remarkup to Markdown conversion
- **PhabricatorPlatform**: Main platform implementation integrating with MeterSphere

### Project Mapping Strategy

The plugin uses a 3-layer approach:
1. **Primary Strategy** (Required): Choose from 4 mapping strategies
2. **Mandatory Tags**: All project relationships extracted as tags
3. **Default Project** (Required): Fallback when primary mapping fails

## Development

### Building

```bash
# Build entire project
mvn clean install -DskipTests

# Build only phabricator plugin
mvn clean package -pl metersphere-phabricator-plugin -am -DskipTests
```

### Project Structure

```
metersphere-phabricator-plugin/
├── src/main/java/io/metersphere/platform/
│   ├── client/         # API client
│   ├── domain/         # Domain objects (Config, Task, Project, etc.)
│   ├── dto/            # Data transfer objects
│   ├── enums/          # Enumeration types
│   ├── handler/        # Status and logic handlers
│   ├── impl/           # Main platform implementation
│   ├── resolver/       # Project mapping resolver
│   ├── response/       # API response objects
│   └── utils/          # Utility classes
├── src/main/resources/
│   └── frontend.json   # UI configuration
└── pom.xml
```

## Known Limitations

- Platform interface implementation uses stub classes for compilation
- `@Override` annotations commented out for stub compatibility
- Requires alignment with actual MeterSphere SDK for production use
- AI-generated code requires review before production deployment

## Documentation

- [CONFIGURATION.md](CONFIGURATION.md) - Configuration guide
- [CHANGELOG.md](CHANGELOG.md) - Version history
- See `metersphere-phabricator-plugin-plan/` for implementation plan (not included in repo)

## License

GPL-3.0 (same as MeterSphere)

## Contributing

This is an AI-generated prototype. Contributions welcome after review and testing.

---

**Note**: This plugin was developed using OpenCode AI as a proof-of-concept for Phabricator integration with MeterSphere.