# YAFVA.JAR
![A Yafva Jar!](yafva-jar.png)
## (Yet Another) FHIR VAlidator JAva wRapper  
### Wrapped. Lit. Ready to validate.
---
Just another JAR. But faster.

YAFVA.JAR is a production-grade HTTP wrapper around the official HL7/HAPI FHIR Validator. It exposes the validation engine as a lightweight, high-performance HTTP service—built into a single JAR file you can drop into your infrastructure.

Unlike the official FHIR Validator Wrapper, this project is optimized for speed and reliability in persistent runtime environments. It avoids the overhead of loading FHIR packages from disk for every user session by maintaining a preloaded, memory-resident validation context, which can be cloned for multithreaded use.  
The client does not need to worry about user session initialization times, sesssion ID's or session TTL.

### ⚙️ Configuration Options
Configurations can be set using application.yaml or by passing command line arguments when calling the JAR:  
`java -jar yafva.jar --server.port=3500 --validator.tx-server= --validator.sv=4.0.1 --server.tomcat.threads.min-spare=9 --server.tomcat.threads.max=15 --validator.ig[0]=hl7.fhir.us.core#6.1.0`  
The above command will start the server using port 3500, FHIR version R4, with no terminology server, 9-15 concurrent threads and the package `hl7.fhir.us.core#6.1.0` in context.

This same configuration in `application.yaml` will look like this:
```
server:
  port: 3500
  tomcat:
    threads:
      min-spare: 9
      max: 15

validator:
  sv: '4.0.1'
  ig:
    - 'hl7.fhir.us.core#6.1.0'
  tx-server:
```

For detailed information about all available configuration options, see the [Configuration Reference](./docs/CONFIGURATION.md).

### ⚙️ Recommended server.tomcat.threads configuration
As a baseline it is recommended to start with:  
`min-spare`: ~0.75 * CPU's  
`max`: ~1.25 * CPU's  
For example, the configurations supplied above of 9-15 is for a 12-core machine.

---

## HTTP Endpoints
### GET / (Root)
Home page (HTML) with information about the deployed validator server.

### GET /swagger-ui/index.html
API documentation page (Swagger)

### /validate
Validate a single resource.  
*optional URL parameters*: 
- profiles
- format

### /validateBundle
Validate a bath Bundle and recieve the results as a Bundle of OperationOutcomes.

---

## 📦 Installation Guides

- [Linux Installation](./docs/INSTALL-linux.md)
- [Windows Installation](./docs/INSTALL-windows.md)
- [Configuration Reference](./docs/CONFIGURATION.md)

---

## 🔍 License

This project is licensed under the **Apache License 2.0**.

### 📜 Dependencies & Their Licenses:

- **FHIR Validator (Apache 2.0)** - [HL7 FHIR Validator](https://github.com/hapifhir/org.hl7.fhir.validator-wrapper)

---
