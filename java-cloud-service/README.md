# Java Cloud Service Library

A comprehensive, production-ready cloud service library for Java applications. Currently featuring a robust Key Management Service (KMS) with plans for additional cloud services.

## Features

### KMS (Key Management Service)
- **Key Creation**: Support for symmetric and asymmetric keys (RSA 2048/4096)
- **Encryption/Decryption**: AES-256 encryption for data protection
- **Key Rotation**: Automated key rotation support
- **Key Lifecycle Management**: Enable, disable, and delete keys
- **Data Key Generation**: Generate data keys for envelope encryption
- **Metadata Tracking**: Complete audit trail with creation and rotation dates
- **Thread-Safe**: Concurrent operation support

## Installation

### Build from Source

```bash
cd java-cloud-service
mvn clean install
```

This will generate two JAR files in the `target/` directory:
- `java-cloud-service-1.0.0.jar` - Main library
- `java-cloud-service-1.0.0-jar-with-dependencies.jar` - Fat JAR with all dependencies

### Using in Your Project

#### Maven
```xml
<dependency>
    <groupId>com.cloudservice</groupId>
    <artifactId>java-cloud-service</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'com.cloudservice:java-cloud-service:1.0.0'
```

#### Manual JAR
Download the JAR and add to your project classpath:
```bash
java -cp java-cloud-service-1.0.0-jar-with-dependencies.jar:your-app.jar com.yourapp.Main
```

## Quick Start

### Basic KMS Usage

```java
import com.cloudservice.CloudServiceFactory;
import com.cloudservice.kms.KMSService;

// Create factory
CloudServiceFactory factory = CloudServiceFactory.builder()
    .withRegion("us-east-1")
    .build();

// Get KMS service
KMSService kms = factory.createKMSService();

// Create an encryption key
String keyId = kms.createKey("my-app-key", KMSService.KeyType.SYMMETRIC);

// Encrypt sensitive data
String secret = "my-secret-data";
byte[] encrypted = kms.encrypt(keyId, secret.getBytes());

// Decrypt data
byte[] decrypted = kms.decrypt(keyId, encrypted);
String original = new String(decrypted);
```

### Advanced KMS Features

```java
// Create asymmetric key
String rsaKeyId = kms.createKey("rsa-key", KMSService.KeyType.ASYMMETRIC_RSA_2048);

// Generate data key for envelope encryption
String dataKey = kms.generateDataKey(keyId, 256);

// Rotate key
kms.rotateKey(keyId);

// Get key metadata
KeyMetadata metadata = kms.describeKey(keyId);
System.out.println("Key created: " + metadata.getCreationDate());
System.out.println("Last rotated: " + metadata.getLastRotationDate());

// List all keys
List<KeyMetadata> allKeys = kms.listKeys();

// Disable key temporarily
kms.disableKey(keyId);

// Re-enable key
kms.enableKey(keyId);

// Mark for deletion
kms.deleteKey(keyId);
```

### Using Factory Builder

```java
CloudServiceFactory factory = CloudServiceFactory.builder()
    .withRegion("us-west-2")
    .withEndpoint("https://custom-endpoint.example.com")
    .withCredential("accessKey", "YOUR_ACCESS_KEY")
    .withCredential("secretKey", "YOUR_SECRET_KEY")
    .withProperty("timeout", 5000)
    .withProperty("retryAttempts", 3)
    .build();

KMSService kms = factory.createKMSService();
```

## Running Examples

```bash
# Compile and run the example
mvn compile exec:java -Dexec.mainClass="com.cloudservice.examples.KMSExample"
```

## Testing

Run the comprehensive test suite:

```bash
mvn test
```

The test suite includes:
- Unit tests for all KMS operations
- Encryption/decryption validation
- Key lifecycle management tests
- Error handling and edge cases
- Concurrent operation tests
- Large data handling tests

## Architecture

```
com.cloudservice/
├── common/
│   ├── CloudServiceException.java    # Custom exception handling
│   └── ServiceConfig.java            # Configuration management
├── kms/
│   ├── KMSService.java               # KMS interface
│   ├── KMSServiceImpl.java           # KMS implementation
│   └── KeyMetadata.java              # Key metadata model
├── CloudServiceFactory.java          # Service factory
└── examples/
    └── KMSExample.java               # Usage examples
```

## Key Types Supported

| Key Type | Algorithm | Use Case |
|----------|-----------|----------|
| SYMMETRIC | AES-256 | Fast encryption/decryption |
| ASYMMETRIC_RSA_2048 | RSA 2048-bit | Digital signatures, asymmetric encryption |
| ASYMMETRIC_RSA_4096 | RSA 4096-bit | High-security asymmetric operations |

## Error Handling

All operations throw `CloudServiceException` with specific error codes:

```java
try {
    kms.encrypt(keyId, data);
} catch (CloudServiceException e) {
    switch (e.getErrorCode()) {
        case "KEY_NOT_FOUND":
            // Handle missing key
            break;
        case "KEY_NOT_ENABLED":
            // Handle disabled key
            break;
        case "ENCRYPTION_FAILED":
            // Handle encryption error
            break;
        default:
            // Handle other errors
    }
}
```

### Error Codes

- `KEY_NOT_FOUND` - Key does not exist
- `KEY_NOT_ENABLED` - Key is disabled or marked for deletion
- `ENCRYPTION_FAILED` - Encryption operation failed
- `DECRYPTION_FAILED` - Decryption operation failed
- `KEY_CREATION_FAILED` - Key creation failed
- `UNSUPPORTED_KEY_TYPE` - Invalid key type specified
- `GENERAL_ERROR` - Generic error

## Best Practices

1. **Key Management**
   - Rotate keys regularly
   - Disable keys instead of deleting immediately
   - Use appropriate key types for your use case

2. **Security**
   - Store key IDs securely
   - Never log sensitive data
   - Use envelope encryption for large data

3. **Performance**
   - Reuse KMSService instances
   - Cache data keys when using envelope encryption
   - Use symmetric keys for bulk encryption

## Roadmap

Future services planned:
- Secret Manager Service
- Storage Service
- Queue Service
- Cache Service
- Config Service
- Notification Service

## Requirements

- Java 11 or higher
- Maven 3.6+ (for building)

## Dependencies

- SLF4J 2.0.7 - Logging
- Jackson 2.15.2 - JSON processing
- BouncyCastle 1.70 - Cryptography
- JUnit 5.9.3 - Testing

## License

This is a demonstration library for educational and development purposes.

## Contributing

This library is designed to be extensible. To add new services:

1. Create service interface in `com.cloudservice.<service-name>`
2. Implement the interface with `<ServiceName>ServiceImpl`
3. Add factory method in `CloudServiceFactory`
4. Write comprehensive tests
5. Update documentation

## Support

For issues, questions, or contributions, please use the issue tracker.

## Changelog

### Version 1.0.0
- Initial release
- KMS Service with full encryption/decryption support
- Factory pattern for service instantiation
- Comprehensive test suite
- Complete documentation
