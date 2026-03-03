# Testing Guidelines

## Test Location
Place tests in `src/test/java/io/metersphere/`:

```java
package io.metersphere;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PhabricatorClientTest {

    @BeforeEach
    void setUp() {
        // Setup
    }

    @Test
    @DisplayName("Should handle null config")
    void testNullConfig() {
        assertDoesNotThrow(() -> {/* test */});
    }
}
```

## Testing Best Practices
- Use `@DisplayName` for readable test names
- Use JUnit 5 (Jupiter): `org.junit.jupiter.api.*`
- Use assertions from `org.junit.jupiter.api.Assertions` or static imports
- Mock external dependencies with Mockito

## Running Tests

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
