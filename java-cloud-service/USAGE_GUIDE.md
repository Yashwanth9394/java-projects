# Java Cloud Service - Quick Usage Guide

## What Was Built

A production-ready **Key Management Service (KMS)** library that can be packaged as a JAR and used across multiple projects or hosts.

## Build Results

Successfully created:
- `java-cloud-service-1.0.0.jar` (17 KB) - Core library
- `java-cloud-service-1.0.0-jar-with-dependencies.jar` (7.7 MB) - Fat JAR with all dependencies

All **14 comprehensive tests passed** successfully!

## Quick Start

### 1. Build the JAR

```bash
cd java-cloud-service
mvn clean package
```

### 2. Use in Another Project

#### Option A: Add to local Maven repository
```bash
mvn install
```

Then in your project's `pom.xml`:
```xml
<dependency>
    <groupId>com.cloudservice</groupId>
    <artifactId>java-cloud-service</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Option B: Use the JAR directly
```bash
# Copy the JAR to your project
cp target/java-cloud-service-1.0.0-jar-with-dependencies.jar /path/to/your/project/lib/

# Run your application with the JAR
java -cp lib/java-cloud-service-1.0.0-jar-with-dependencies.jar:your-app.jar com.yourapp.Main
```

### 3. Use on Another Host

```bash
# Copy JAR to another server
scp target/java-cloud-service-1.0.0-jar-with-dependencies.jar user@remote-host:/opt/libs/

# On the remote host
java -cp /opt/libs/java-cloud-service-1.0.0-jar-with-dependencies.jar:app.jar com.app.Main
```

## Example Usage

```java
import com.cloudservice.CloudServiceFactory;
import com.cloudservice.kms.KMSService;

// Create factory
CloudServiceFactory factory = CloudServiceFactory.builder()
    .withRegion("us-east-1")
    .build();

// Get KMS service
KMSService kms = factory.createKMSService();

// Create encryption key
String keyId = kms.createKey("my-app-key", KMSService.KeyType.SYMMETRIC);

// Encrypt data
byte[] encrypted = kms.encrypt(keyId, "sensitive-data".getBytes());

// Decrypt data
byte[] decrypted = kms.decrypt(keyId, encrypted);
```

## Running the Example

```bash
mvn exec:java -Dexec.mainClass="com.cloudservice.examples.KMSExample"
```

## Running Tests

```bash
mvn test
```

Test coverage includes:
- Key creation (symmetric and asymmetric)
- Encryption/decryption operations
- Key lifecycle management (enable, disable, delete)
- Key rotation
- Data key generation
- Error handling and edge cases
- Concurrent operations
- Large data handling

## KMS Service Features

### Available Operations

| Operation | Description |
|-----------|-------------|
| `createKey(alias, type)` | Create a new encryption key |
| `encrypt(keyId, data)` | Encrypt data using a key |
| `decrypt(keyId, data)` | Decrypt data using a key |
| `generateDataKey(keyId, length)` | Generate a data key for envelope encryption |
| `rotateKey(keyId)` | Rotate an encryption key |
| `deleteKey(keyId)` | Mark key for deletion |
| `describeKey(keyId)` | Get key metadata |
| `listKeys()` | List all keys |
| `enableKey(keyId)` | Enable a disabled key |
| `disableKey(keyId)` | Disable a key |

### Supported Key Types

- `SYMMETRIC` - AES-256 encryption
- `ASYMMETRIC_RSA_2048` - RSA 2048-bit
- `ASYMMETRIC_RSA_4096` - RSA 4096-bit

## Distributing Your Service

### 1. Maven Repository
Upload to a private Maven repository:
```bash
mvn deploy
```

### 2. JAR Distribution
Distribute the fat JAR directly to teams or servers.

### 3. Docker Container
```dockerfile
FROM openjdk:11-jre-slim
COPY target/java-cloud-service-1.0.0-jar-with-dependencies.jar /app/lib/
```

## Next Steps

The architecture is extensible. To add more services:
1. Create a new service interface in `com.cloudservice.<service-name>`
2. Implement the service
3. Add factory method in `CloudServiceFactory`
4. Write tests
5. Rebuild the JAR

## Robustness Demonstrated

- All tests passed (14/14)
- Example ran successfully
- Proper error handling with custom exceptions
- Thread-safe concurrent operations
- Comprehensive test coverage
- Production-ready packaging
