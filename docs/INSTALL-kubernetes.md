# YAFVA.JAR Installation Manual – Kubernetes

This guide explains how to deploy the **YAFVA.JAR** validator in a Kubernetes cluster using a single YAML manifest.

---

## Prerequisites

- A running Kubernetes cluster (v1.22+ recommended)
- `kubectl` configured to access the cluster
- Access to pull the container image `outburnltd/yafva.jar:latest`

---

## Full Manifest

Save the following as `yafva-all.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: yafva-jar-config
data:
  application.yaml: |
    server:
      port: 8080
      tomcat:
        threads:
          max: 50
      servlet:
        context-path: /

    spring:
      application:
        name: yafva-jar
      mvc:
        problemdetails:
          enabled: true

    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: when-authorized
      health:
        readiness-state:
          enabled: true
        liveness-state:
          enabled: true

    logging:
      pattern:
        console: '%d{yyyy-MM-dd HH:mm:ss} [%p] [%t] - %logger{36} - %msg%n'

    validator:
      sv: '4.0.1'
      ig:
        - 'il.core.fhir.r4'
        - 'fume.outburn.r4'
      tx-server: 'https://tx.fhir.org/r4'
      tx-log:
      locale: en
      remove-text: true
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: yafva-jar
  labels:
    app: yafva-jar
spec:
  replicas: 2
  selector:
    matchLabels:
      app: yafva-jar
  template:
    metadata:
      labels:
        app: yafva-jar
    spec:
      securityContext:
        runAsUser: 10001
        runAsGroup: 10001
        fsGroup: 10001
      tolerations:
        - key: "iris"
          operator: "Equal"
          value: "fhir-dev"
          effect: "NoSchedule"
      containers:
      - name: yafva-jar
        image: outburnltd/yafva.jar:latest
        ports:
        - containerPort: 8080
          protocol: TCP
        env:
        - name: JAVA_OPTS
          value: "-Xmx2g -Xms512m"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "3Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 400
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: fhir-cache
          mountPath: /home/app/.fhir
        - name: config-volume
          mountPath: /app/application.yaml
          subPath: application.yaml
      volumes:
      - name: fhir-cache
        emptyDir: {}
      - name: config-volume
        configMap:
          name: yafva-jar-config
          items:
          - key: application.yaml
            path: application.yaml
---
apiVersion: v1
kind: Service
metadata:
  name: yafva-jar-service
  labels:
    app: yafva-jar
spec:
  selector:
    app: yafva-jar
  ports:
  - name: http
    port: 8080
    targetPort: 8080
    protocol: TCP
  type: ClusterIP
```
---

## Deployment

Apply the manifest:

```sh
  kubectl apply -f yafva-all.yaml
```

This will create:

- **ConfigMap** – holds the `application.yaml` configuration
- **Deployment** – runs the validator pods
- **Service** – exposes the validator inside the cluster

---

## Verification

Check pod status:

```sh
  kubectl get pods -l app=yafva-jar
```

Check logs:

```sh
  kubectl logs -l app=yafva-jar -f
```

Verify readiness:

```sh
  kubectl run test --rm -it --image=curlimages/curl -- curl http://yafva-jar-service:8080/info | jq .
```

---

## Probes

The deployment defines two health probes:

- **Liveness Probe** – checks `/actuator/health/liveness` every 30 seconds.
- **Readiness Probe** – checks `/actuator/health/readiness`.

⚠️ **Note on readiness delay:**  
The initial readiness delay is set to **400 seconds**. This value is intentionally high to ensure the validator has enough time to **download and cache required FHIR packages** during the very first startup.

Subsequent restarts of the pods (where the cache already exists on the persistent volume) will usually be much faster. In such cases, you may safely lower the `initialDelaySeconds` to around **150 seconds** for a better balance between availability and startup speed.

---

## Scaling

Increase replicas:

```sh
  kubectl scale deployment yafva-jar --replicas=4
```

---

## Cleanup

Remove all resources:

```sh
  kubectl delete -f yafva-all.yaml
```

---

✅ With this single manifest you get a **fully functional YAFVA.JAR validator** in Kubernetes: config, persistent cache, probes, scaling, and service exposure.