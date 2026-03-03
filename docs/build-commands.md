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
