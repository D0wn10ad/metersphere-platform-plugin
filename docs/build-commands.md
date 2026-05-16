# Build Commands

## Build All Modules
```bash
mvn clean install
```

## Build Single Module
```bash
mvn clean install -pl metersphere-phabricator-plugin -am
```

## Run Tests

### All tests
```bash
mvn test
```

### Single test class
```bash
mvn test -Dtest=PhabricatorClientTest
```

### Single test method
```bash
mvn test -Dtest=PhabricatorClientTest#testConfigSetting
```

### Skip tests
```bash
mvn clean install -DskipTests
```

## Package Plugin
```bash
mvn clean package
# Output: target/*-jar-with-dependencies.jar
```

## Dependency Resolution

### Problem

The plugin depends on two artifacts that may not be available in a standard Maven repository:

| Artifact | Source |
|---|---|
| `io.metersphere:domain:2.10` | MeterSphere internal library (not in public repos) |
| `io.metersphere:metersphere-platform-plugin-sdk:1.6.0` | Built locally from this project |

If these are missing, the build fails with:
```
Could not resolve dependencies: domain:jar:2.10 was not found
```

### Setup Steps

Before building, install these artifacts to your local Maven repository:

```bash
# 1. Build and install the platform SDK module first
mvn clean install -pl metersphere-platform-plugin-sdk -DskipTests

# 2. Build and install the plugin SDK module
mvn clean install -pl metersphere-plugin-sdk -DskipTests

# 3. Now build the plugin
mvn clean package -pl metersphere-phabricator-plugin -DskipTests
```

> **Note:** If `domain:2.10` is still missing after step 1, it means the SDK module also depends on it externally. In that case, the `metersphere-platform-plugin-sdk` artifact (compiled with dependencies) can be installed manually from a pre-built jar using `mvn install:install-file`.

### Checking Local Repository

To verify whether an artifact is cached locally:
```bash
ls ~/.m2/repository/io/metersphere/domain/2.10/
ls ~/.m2/repository/io/metersphere/metersphere-platform-plugin-sdk/1.6.0/
```

Each directory should contain a `.jar` and a `.pom` file. If only `.lastUpdated` files are present (from failed downloads), delete them and retry the setup steps above.
