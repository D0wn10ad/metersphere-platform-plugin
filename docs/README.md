# MeterSphere Platform Plugin Development Guide

## Overview

This is a MeterSphere platform plugin project for integrating with third-party issue management systems (Jira, ZenTao, Phabricator). Maven-based multi-module Java project.

## Project Structure

```
metersphere-platform-plugin/
├── pom.xml                           # Parent POM
├── metersphere-plugin-sdk/           # Core plugin SDK (Java 11)
├── metersphere-platform-plugin-sdk/  # Platform-specific SDK
└── [plugin-name]-plugin/             # Plugin implementations (Java 17)
```

## Documentation

- [Build Commands](build-commands.md)
- [Code Style Guidelines](code-style.md)
- [Testing Guidelines](testing.md)
- [Platform Implementation](platform-implementation.md)
- [Best Practices](best-practices.md)

## Quick Start

### Build
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

### Package Plugin
```bash
mvn clean package
```
