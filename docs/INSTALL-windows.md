# Installation Guide for Windows

## Prerequisites

### Required Downloads
1. Download the `yafva.jar` file from the [latest release](https://github.com/Outburn-IL/yafva.jar/releases)
2. Download NSSM (Non-Sucking Service Manager) from [https://nssm.cc/download](https://nssm.cc/download)
3. Download the Windows service scripts from the [windows folder](../windows) in the project repository:
   - `install-service.bat`
   - `uninstall-service.bat`
   - `application.yaml`
   - `bin/README.md`

### System Requirements
- Windows 7 or later (64-bit recommended)
- Java 21 or later installed and available in PATH
- Administrator privileges for service installation

> **Configuration:** For detailed information about configuration options in `application.yaml`, refer to the [Configuration Reference](./CONFIGURATION.md).

## Installation Steps

### 1. Prepare the Installation Directory

Create a directory for YAFVA.JAR (e.g., `C:\yafva`) and organize the files as follows:

```
C:\yafva\
├── bin\
│   ├── nssm.exe              <- Extract from NSSM download
│   └── README.md
├── install-service.bat
├── uninstall-service.bat
├── application.yaml
└── yafva.jar
```

### 2. Extract NSSM

1. Extract the downloaded NSSM archive
2. Copy `nssm.exe` from the appropriate architecture folder:
   - **For 64-bit Windows**: Use `win64\nssm.exe`
   - **For 32-bit Windows**: Use `win32\nssm.exe`
3. Place `nssm.exe` in the `bin\` subdirectory

### 3. Verify Java Installation

Open a command prompt and run:
```cmd
java -version
```

Ensure Java 21 or later is installed and available in your PATH.

### 4. Configure the Application (Optional)

Edit `application.yaml` to customize:
- FHIR version (`validator.sv`)
- Implementation Guides (`validator.ig[]`)
- Server port (`server.port`)

See [CONFIGURATION.md](./CONFIGURATION.md) for all available options.

### 5. Install the Service

1. Right-click `install-service.bat` and select **"Run as administrator"**
2. The script will:
   - Verify all prerequisites (NSSM, Java, JAR file)
   - Install the Windows service named `yafvajar`
   - Configure automatic startup and restart on failure
   - Set up log rotation
3. Wait for the success message

### 6. Start the Service

Use one of these methods:

**Option A: Using Command Line**
```cmd
net start yafvajar
```

**Option B: Using Windows Services**
1. Press `Win + R`, type `services.msc`, and press Enter
2. Find "YafvaJar - FHIR Validator" in the list
3. Right-click and select "Start"

### 7. Verify the Service is Running

Open a web browser and navigate to:
```
http://localhost:8080
```

You should see the YAFVA.JAR home page.

## Service Management

### Check Service Status
```cmd
sc query yafvajar
```

### Stop the Service
```cmd
net stop yafvajar
```

### Restart the Service
```cmd
net stop yafvajar && net start yafvajar
```

### Uninstall the Service

1. Stop the service if running
2. Right-click `uninstall-service.bat` and select **"Run as administrator"**
3. Wait for the success message

## Logs

Service logs are written to the `logs\` directory in your installation folder:
- `service-stdout.log` - Application output (Spring Boot logs, validation results)
- `service-stderr.log` - Error output (stack traces, startup errors)

Log files are automatically rotated when they reach 10 MB.

## Troubleshooting

### Service fails to start

1. Check Java installation: `java -version`
2. Verify `yafva.jar` exists in the installation directory
3. Review logs in the `logs\` directory
4. Check Windows Event Viewer (Application logs) for service-related errors

### Port 8080 already in use

Edit `application.yaml` and change the port:
```yaml
server:
  port: 3500  # or any available port
```

Then restart the service.

### Service doesn't auto-start after reboot

1. Open Services (`services.msc`)
2. Find "YafvaJar - FHIR Validator"
3. Right-click → Properties
4. Ensure "Startup type" is set to "Automatic"

### NSSM not found error

Ensure you've downloaded NSSM and placed `nssm.exe` in the `bin\` folder. See the `bin\README.md` file for detailed instructions.

## Advanced Configuration

### Custom Java Options

JVM arguments (memory settings, proxy, system properties) are passed through the `JAVA_TOOL_OPTIONS` environment variable. See [JVM Options](./CONFIGURATION.md#jvm-options).

Before installing, set `JAVA_TOOL_OPTIONS` at the top of `install-service.bat`. To change it on an installed service:

1. Open Command Prompt as administrator
2. Update the service environment:
```cmd
bin\nssm.exe set yafvajar AppEnvironmentExtra "JAVA_TOOL_OPTIONS=-Xms512m -Xmx2g -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080"
```
3. Restart the service

### Change Service Account

By default, the service runs under the Local System account. To use a different account:

1. Open Services (`services.msc`)
2. Right-click "YafvaJar - FHIR Validator" → Properties
3. Go to the "Log On" tab
4. Select "This account" and provide credentials
5. Click OK and restart the service

## Additional Resources

- **NSSM Documentation**: [https://nssm.cc/usage](https://nssm.cc/usage)
- **YAFVA.JAR Configuration**: [CONFIGURATION.md](./CONFIGURATION.md)
- **Project Repository**: [https://github.com/Outburn-IL/yafva.jar](https://github.com/Outburn-IL/yafva.jar)
