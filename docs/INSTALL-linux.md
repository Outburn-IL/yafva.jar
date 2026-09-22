# YAFVA.JAR Installation Manual – Linux

This manual provides step-by-step instructions for installing and configuring the `YAFVA.JAR` validator to run as a `systemd` service on a Linux machine.

---

### Prerequisites

* Superuser shell access (`sudo su`)
* Java Runtime Environment (JRE), version 21 or higher  
  Install using:
  ```sh
  sudo apt install openjdk-21-jdk
  ```
* Java runtime path  
  A typical path is: `/usr/lib/jvm/java-21-openjdk-amd64`  
  To determine the installed path:
  ```sh
  java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home'
  ```
  If multiple versions are installed:
  ```sh
  update-alternatives --config java
  ```
* [`jq`](https://stedolan.github.io/jq/) – JSON processor  
  Install using:
  ```sh
  sudo apt-get install jq
  ```
> **Note:** If you are not running as root, prefix all commands with `sudo`.

---

### Installation Steps

#### 1. Download the Validator

```sh
# Create the installation directory
cd /
mkdir -p /opt/yafvajar/logs
cd /opt/yafvajar

# Retrieve the latest release URL from GitHub
url=$(curl -s https://api.github.com/repos/Outburn-IL/yafva.jar/releases/latest \
| jq -r '.assets[] | select(.name | startswith("yafva-") and endswith(".jar")) | .browser_download_url')

# Confirm the URL was found
echo $url

# Download the JAR file
wget -O yafva.jar "$url"

# Verify the file has been downloaded
ls -l
```

---

#### 2. Create the Configuration File

Create `application.yaml`:
```sh
nano application.yaml
```

Paste the following content:
```yaml
server:
  port: 8080
  tomcat:
    threads:
      min-spare: 10
      max: 200
  servlet:
    context-path: /

spring:
  application:
    name: jafva.jar
  mvc:
    problemdetails:
      enabled: true

logging:
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} [%p] [%t] - %logger{36} - %msg%n'
    file: '%d{yyyy-MM-dd HH:mm:ss} [%p] [%t] - %logger{36} - %msg%n'
  file:
    name: 'logs/log.log'
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30

validator:
  sv: '4.0.1'
  ig:
    - 'il.core.fhir.r4#0.17.1'
  tx-server:
  tx-log:
  locale: en
  remove-text: true
```

> **Note:** For detailed information about all configuration options, refer to the [Configuration Reference](./CONFIGURATION.md).

---

#### 3. Create the Startup Script

Create `yafvajar.sh`:
```sh
nano yafvajar.sh
```

Paste the following:
```sh
#!/bin/bash

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

WORKDIR=/opt/yafvajar/
JAVA_TOOL_OPTIONS=  # example: "-Xms256m -Xmx512m -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080"
APP_OPTIONS=        # example: "--spring.config.location=application.yaml"

cd $WORKDIR
[ -n "$JAVA_TOOL_OPTIONS" ] && export JAVA_TOOL_OPTIONS
"${JAVA_HOME}/bin/java" -jar yafva.jar $APP_OPTIONS
```

> **Note:** JVM options (memory, proxy) go in `JAVA_TOOL_OPTIONS`. See [JVM Options](./CONFIGURATION.md#jvm-options).

---

#### 4. Create the Service File

Create the `systemd` unit:
```sh
nano /etc/systemd/system/yafvajar.service
```

Paste:
```ini
[Unit]
Description=YAFVA.JAR - Yet Another FHIR VAlidator JAva wRapper
After=syslog.target network.target

[Service]
User=root
Type=simple
Restart=on-failure
RestartSec=10

ExecStart=/opt/yafvajar/yafvajar.sh
ExecStop=/bin/kill -15 $MAINPID

[Install]
WantedBy=multi-user.target
```

---

#### 5. Set File Permissions

```sh
chmod 744 /opt/yafvajar/yafvajar.sh
chmod 644 /etc/systemd/system/yafvajar.service
```

---

#### 6. Register and Start the Service

```sh
systemctl daemon-reload              # Reload unit files
systemctl enable yafvajar.service    # Enable service at boot
systemctl start yafvajar.service     # Start the service
```

---

### Verification and Monitoring

After a few seconds, verify the service status:
```sh
# Print current status and exit
systemctl status yafvajar.service

# Stream logs live (Ctrl+C to exit)
journalctl -u yafvajar.service -f
```

To verify the validator is up and responding:
```sh
curl http://localhost:8080/info | jq .
```

To restart the service:
```sh
systemctl restart yafvajar.service
```

---

### Log Location

The application logs by default to:
```
/opt/yafvajar/logs/log.log
```

To view the log:
```sh
cat /opt/yafvajar/logs/log.log
```

---

### Firewall Configuration

If needed, allow traffic to port 8080:
```sh
# Confirm port is unused
netstat -na | grep :8080

# Open the port
ufw allow 8080
```
