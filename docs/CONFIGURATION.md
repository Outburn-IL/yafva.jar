# YAFVA.JAR Configuration Reference

This document provides comprehensive information about all configuration options available in YAFVA.JAR application.yaml files.

## Configuration File Structure

The application.yaml file contains several main sections for configuring different aspects of the FHIR validator service:

- **server**: Web server configuration
- **spring**: Spring framework settings
- **management**: Actuator endpoints and health checks
- **logging**: Logging configuration
- **validator**: FHIR validation specific settings

## Server Configuration

### server.port
- **Type**: Integer
- **Default**: 8080
- **Description**: The port number on which the HTTP server will listen for incoming requests.

### server.tomcat.threads.max
- **Type**: Integer
- **Default**: 50
- **Description**: Maximum number of worker threads for the Tomcat server.

### server.tomcat.threads.min-spare
- **Type**: Integer
- **Description**: Minimum number of idle worker threads to keep alive.

### server.servlet.context-path
- **Type**: String
- **Default**: /
- **Description**: The context path for the application (base URL path).

## Spring Framework Configuration

### spring.application.name
- **Type**: String
- **Default**: yafva-jar
- **Description**: The name of the Spring Boot application.

### spring.mvc.problemdetails.enabled
- **Type**: Boolean
- **Default**: true
- **Description**: Enables RFC 7807 Problem Details for HTTP APIs support.

## Management and Monitoring

### management.endpoints.web.exposure.include
- **Type**: String array
- **Default**: health,info,metrics,prometheus
- **Description**: Specifies which actuator endpoints to expose over HTTP.

### management.endpoint.health.show-details
- **Type**: String
- **Default**: when-authorized
- **Description**: Controls when health endpoint details are shown.

### management.health.readiness-state.enabled
- **Type**: Boolean
- **Default**: true
- **Description**: Enables readiness state health indicator.

### management.health.liveness-state.enabled
- **Type**: Boolean
- **Default**: true
- **Description**: Enables liveness state health indicator.

## Logging Configuration

### logging.pattern.console
- **Type**: String
- **Default**: '%d{yyyy-MM-dd HH:mm:ss} [%p] [%t] - %logger{36} - %msg%n'
- **Description**: Pattern for console log output formatting.

### logging.pattern.file
- **Type**: String
- **Description**: Pattern for file log output formatting (Windows configuration only).

### logging.file.name
- **Type**: String
- **Description**: File path for log output (Windows configuration only).

### logging.logback.rollingpolicy.max-file-size
- **Type**: String
- **Default**: 10MB
- **Description**: Maximum size of each log file before rolling (Windows configuration only).

### logging.logback.rollingpolicy.max-history
- **Type**: Integer
- **Default**: 30
- **Description**: Maximum number of log files to retain (Windows configuration only).

## FHIR Validator Configuration

### validator.sv
- **Type**: String
- **Required**: Yes
- **Default**: '4.0.1'
- **Description**: The FHIR version to use for validation. This is a mandatory field.
- **Valid Values**: '4.0.1' (R4), '5.0.0' (R5), etc.

### validator.ig
- **Type**: String array
- **Required**: No
- **Description**: The Implementation Guides (IGs) to load for validation. This is an optional field.
- **Example Values**: 
  - 'hl7.fhir.r4.examples#4.0.1'
  - 'il.core.fhir.r4#0.17.5'
  - 'hl7.fhir.us.core#6.1.0'

### validator.tx-server
- **Type**: String
- **Required**: No
- **Description**: The terminology server URL to use for validation. This is an optional field.
- **Example Values**: 'https://tx.fhir.org/r4'
- **Note**: Leave empty or comment out to disable external terminology validation.

### validator.tx-log
- **Type**: String
- **Required**: No
- **Description**: The terminology server log file to use for validation. This is an optional field.

### validator.settings-file-path
- **Type**: String
- **Required**: No
- **Description**: The path to the settings file for FHIR settings. This is an optional field. The settings file allows you to configure custom package servers, authentication, and other FHIR-specific settings.
- **Example**: '/path/to/fhir-settings.json'
- **Reference**: For detailed information about fhir-settings.json format and options, see: https://confluence.hl7.org/spaces/FHIR/pages/161072808/Using+fhir-settings.json
- **Sample fhir-settings.json**:
  ```json
  {
    "ignoreDefaultPackageServers": true,
    "servers": [
      {
        "url": "http://mypackageserver:4873",
        "type": "fhir-package",
        "authenticationType" : "none"
      }
    ]
  }
  ```

### validator.locale
- **Type**: String
- **Default**: en
- **Description**: Specifies the locale/language of the validation result messages.

### validator.remove-text
- **Type**: Boolean
- **Default**: true
- **Description**: Removes OperationOutcome text from the validation result. This is an optional field.

### validator.any-extensions-allowed
- **Type**: Boolean
- **Default**: false
- **Description**: Allow all unknown extensions. By default, unknown extensions are prohibited.

### validator.extension-domains
- **Type**: String array
- **Required**: No
- **Description**: Extension domains to allow. Extensions from specified domains are allowed by matching the URL.
- **Example**: 
  ```yaml
  extension-domains:
    - "http://example.org/extensions"
  ```

### validator.unknown-code-systems-cause-errors
- **Type**: Boolean
- **Default**: false
- **Description**: When an unknown code system is encountered, determines whether this causes an error or warning.

### validator.allow-example-urls
- **Type**: Boolean
- **Default**: false
- **Description**: Allow URLs from example.org. By default, references to example.org are marked as errors.

### validator.display-warnings
- **Type**: Boolean
- **Default**: false
- **Description**: When the validator encounters a coding where the display value isn't consistent with the display(s) defined by the code systems, this controls whether it's treated as an error or warning.

### validator.want-invariant-in-message
- **Type**: Boolean
- **Default**: false
- **Description**: Control whether the FHIRPath for invariants is included in the message.

### validator.level
- **Type**: String
- **Default**: warnings
- **Description**: Set the minimum level for validation messages.
- **Valid Values**: hints, warnings, errors

### validator.best-practice-level
- **Type**: String
- **Default**: warning
- **Description**: Controls how best practice rules are handled.
- **Valid Values**: hint, warning, error, ignore

### validator.verbose
- **Type**: Boolean
- **Default**: false
- **Description**: When verbose is set, the validator will create hints against the resources.

### validator.show-times
- **Type**: Boolean
- **Default**: false
- **Description**: When show-times is set, the validator will produce a line in the output.

## Configuration Examples

### Basic Configuration
```yaml
server:
  port: 8080

validator:
  sv: '4.0.1'
  ig:
    - 'hl7.fhir.r4.examples#4.0.1'
```

### Production Configuration with Terminology Server
```yaml
server:
  port: 8080
  tomcat:
    threads:
      min-spare: 9
      max: 15

validator:
  sv: '4.0.1'
  ig:
    - 'hl7.fhir.us.core#6.1.0'
    - 'il.core.fhir.r4#0.17.5'
  tx-server: 'https://tx.fhir.org/r4'
  settings-file-path: '/config/fhir-settings.json'
```

### Development Configuration
```yaml
server:
  port: 3500

validator:
  sv: '4.0.1'
  ig:
    - 'hl7.fhir.r4.examples#4.0.1'
  verbose: true
  show-times: true
  level: warnings
```

### Configuration with Custom Package Server
```yaml
server:
  port: 8080

validator:
  sv: '4.0.1'
  ig:
    - 'hl7.fhir.us.core#6.1.0'
    - 'custom.implementation.guide#1.0.0'
  settings-file-path: '/config/fhir-settings.json'
  tx-server: 'https://tx.fhir.org/r4'
```

Example `fhir-settings.json` for the above configuration:
```json
{
  "ignoreDefaultPackageServers": true,
  "servers": [
    {
      "url": "http://mypackageserver:4873",
      "type": "fhir-package",
      "authenticationType" : "none"
    }
  ]
}
```

## Command Line Override

All configuration options can be overridden using command line arguments when starting the JAR:

```bash
java -jar yafva.jar \
  --server.port=3500 \
  --validator.tx-server= \
  --validator.sv=4.0.1 \
  --server.tomcat.threads.min-spare=9 \
  --server.tomcat.threads.max=15 \
  --validator.ig[0]=hl7.fhir.us.core#6.1.0
```

## JVM Options

JVM options (memory, proxy, system properties) are passed through the `JAVA_TOOL_OPTIONS` environment variable in all deployment modes. The JVM reads it on startup, so no wrapper script or launcher changes are needed.

| Mode | Where to set it |
|------|-----------------|
| Linux (systemd) | `export JAVA_TOOL_OPTIONS=...` in `yafvajar.sh` |
| Windows (NSSM) | `JAVA_TOOL_OPTIONS` in `install-service.bat`, or `nssm set yafvajar AppEnvironmentExtra JAVA_TOOL_OPTIONS=...` |
| Docker Compose | `environment` in `docker-compose.yml` |
| Kubernetes | `env` in the Deployment |

Example:
```bash
JAVA_TOOL_OPTIONS="-Xms512m -Xmx2g"
```

> **Note:** The Docker image ships with a default `JAVA_TOOL_OPTIONS` (`-XX:MaxRAMPercentage=75.0` and friends). Setting the variable replaces that default.

### Proxy Configuration

Proxy settings are JVM system properties, not `application.yaml` options. Outbound HTTP calls (package downloads, terminology server) honor them automatically.

| Property | Description |
|----------|-------------|
| `http.proxyHost` / `http.proxyPort` | Proxy for HTTP requests |
| `https.proxyHost` / `https.proxyPort` | Proxy for HTTPS requests |
| `http.nonProxyHosts` | Hosts to bypass, separated by `\|`, wildcards allowed (applies to HTTPS too) |

Via `JAVA_TOOL_OPTIONS` (all deployment modes):
```bash
JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=8080 -Dhttp.nonProxyHosts=localhost|127.*|*.internal"
```

Direct `java -jar`:
```bash
java \
  -Dhttp.proxyHost=proxy.example.com \
  -Dhttp.proxyPort=8080 \
  -Dhttps.proxyHost=proxy.example.com \
  -Dhttps.proxyPort=8080 \
  -Dhttp.nonProxyHosts="localhost|127.*|*.internal" \
  -jar yafva.jar
```

Kubernetes:
```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: >-
      -Xmx2g
      -Xms512m
      -Dhttp.proxyHost=proxy.example.com
      -Dhttp.proxyPort=8080
      -Dhttps.proxyHost=proxy.example.com
      -Dhttps.proxyPort=8080
      -Dhttp.nonProxyHosts=localhost|127.*|*.internal
```

## Performance Tuning Recommendations

### Thread Configuration
- **min-spare**: ~0.75 × CPU cores
- **max**: ~1.25 × CPU cores
- Example: For a 12-core machine, use min-spare: 9, max: 15

### Memory Settings
Set heap limits through `JAVA_TOOL_OPTIONS` (see [JVM Options](#jvm-options)):
```bash
JAVA_TOOL_OPTIONS="-Xms512m -Xmx2g"
```

### Validation Level
- Use `level: warnings` or `level: errors` in production to reduce noise
- Use `level: hints` in development for comprehensive validation feedback
