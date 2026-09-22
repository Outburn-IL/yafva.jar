# YAFVA.JAR Docker Deployment

This directory contains the Docker configuration for deploying YAFVA.JAR (Yet Another FHIR Validator Java Wrapper) as a containerized application suitable for Kubernetes deployment.

## Architecture Changes

The architecture has been updated to:
- Remove HAProxy dependency
- Create a standalone, production-ready Docker image
- Add Spring Boot Actuator for health checks
- Optimize for Kubernetes deployment
- Include proper security practices (non-root user)

## Building the Image

### Windows
```cmd
docker\build-image.bat
```

### Linux/macOS
```bash
chmod +x docker/build-image.sh
./docker/build-image.sh
```

### Manual Build
```bash
docker build -f docker/Dockerfile -t yafva-jar:latest .
```

## Running Locally

### With Docker Compose
```bash
cd docker
docker-compose up
```

### With Docker Run
```bash
docker run -p 8080:8080 yafva-jar:latest
```

## Kubernetes Deployment

The application is now optimized for Kubernetes deployment with:
- Health checks (liveness and readiness probes)
- Resource limits and requests
- ConfigMap for configuration
- Service for network access

### Deploy to Kubernetes
```bash
kubectl apply -f docker/k8s-deployment.yaml
```

### Verify Deployment
```bash
kubectl get pods -l app=yafva-jar
kubectl get svc yafva-jar-service
```

### Port Forward for Testing
```bash
kubectl port-forward svc/yafva-jar-service 8080:8080
```

## Health Checks

The application exposes the following health endpoints:
- `/actuator/health` - General health status
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

## Configuration

Configuration can be customized through:
1. Environment variables
2. ConfigMap (in Kubernetes)
3. Volume-mounted configuration files

### Key Environment Variables
- `JAVA_TOOL_OPTIONS` - JVM options (memory, proxy, ...), e.g. `-Xmx2g -Xms512m -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080`. See [JVM Options](../docs/CONFIGURATION.md#jvm-options).

## Security Features

- Non-root user execution
- Minimal base image (JRE instead of JDK)
- Resource limits in Kubernetes
- Health check endpoints for monitoring

## Image Details

- Base Image: `openjdk:21-jre-slim`
- Exposed Port: 8080
- Working Directory: `/app`
- User: `yafva` (non-root)
- Health Check: Built-in via Spring Boot Actuator

## Monitoring

The application includes metrics endpoints that can be scraped by Prometheus:
- `/actuator/metrics`
- `/actuator/prometheus` (if Micrometer Prometheus is added)
