# YAFVA.JAR Installation Manual – Kubernetes

This guide explains how to deploy the **YAFVA.JAR** validator in a Kubernetes cluster using a single YAML manifest.

---

## Prerequisites

- A running Kubernetes cluster (v1.22+ recommended)  
- `kubectl` configured to access the cluster  
- Access to pull the container image `outburnltd/yafva.jar:latest`  
- A StorageClass available for PVCs (adjust `storageClassName` as needed)  
- Optional: image pull secrets if the registry is private  

---

## Deployment

Apply the manifest:

```sh
  kubectl apply -f yafva-all.yaml
```

This will create:

- **ConfigMap** – holds the `application.yaml` configuration  
- **PersistentVolumeClaim (PVC)** – provides storage for the validator cache  
- **Deployment** – runs the validator pods  
- **Service** – exposes the validator inside the cluster  

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
        - 'il.fhir.r4.dgmc'
      tx-server: 'https://tx.fhir.org/r4'
      tx-log:
      locale: en
      remove-text: true
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: fhir-cache-pvc
spec:
  accessModes:
    - ReadWriteOnce   # Or ReadWriteMany if multi-pod sharing is needed
  resources:
    requests:
      storage: 12Gi
  storageClassName: gp3-encrypted # Adjust to your cluster's StorageClass
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
      imagePullSecrets:
        - name: gitlab-secrets
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
          value: "-Xms3g -Xmx7g"
        resources:
          requests:
            memory: "4Gi"
            cpu: "3"
          limits:
            memory: "8Gi"
            cpu: "4"
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
        persistentVolumeClaim:
          claimName: fhir-cache-pvc
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

## Persistent Storage

The deployment uses a **PersistentVolumeClaim (PVC)** named `fhir-cache-pvc` to store the validator’s **FHIR package cache**.  

- The current request is set to **12 GiB** of storage.  
- This cache may **grow over time** as more packages are downloaded and validated.  

🔎 **Recommendation:**  
- Monitor the PVC usage regularly to avoid running out of space.  
- For production setups, consider initially allocating a **larger size** (e.g., 20–50 GiB) depending on expected workloads.  
- If storage fills up, the validator will fail to download additional packages.

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
