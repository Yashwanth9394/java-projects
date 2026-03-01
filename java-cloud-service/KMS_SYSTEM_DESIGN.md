# Key Management Service (KMS) - System Design Document

## Table of Contents
1. [Overview](#overview)
2. [Functional Requirements](#functional-requirements)
3. [Non-Functional Requirements](#non-functional-requirements)
4. [High-Level Architecture](#high-level-architecture)
5. [API Specifications](#api-specifications)
6. [Deep Dives](#deep-dives)
7. [Data Models](#data-models)
8. [Security Considerations](#security-considerations)
9. [Scalability & Performance](#scalability--performance)
10. [Monitoring & Observability](#monitoring--observability)

---

## Overview

### What is KMS?
Key Management Service (KMS) is a managed cryptographic service that enables users to create, manage, and control encryption keys used to protect their data. It provides a centralized platform for key lifecycle management, cryptographic operations, and access control.

### Core Capabilities
- **Key Generation**: Create symmetric and asymmetric Customer Master Keys (CMKs)
- **Envelope Encryption**: Generate data encryption keys (DEKs) wrapped by CMKs
- **Cryptographic Operations**: Encrypt, decrypt, sign, and verify operations
- **Key Rotation**: Automatic annual key rotation with backwards compatibility
- **Access Control**: Fine-grained IAM policies and key policies
- **Audit Trail**: Complete audit logging of all key usage
- **HSM Integration**: Hardware Security Module backing for key storage

---

## Functional Requirements

### 1. Key Management
- **FR1.1**: Create symmetric (AES-256) and asymmetric (RSA, ECC) Customer Master Keys
- **FR1.2**: Import external key material (BYOK - Bring Your Own Key)
- **FR1.3**: List all keys and aliases with pagination support
- **FR1.4**: Enable/disable keys (soft deletion)
- **FR1.5**: Schedule key deletion (7-30 day waiting period)
- **FR1.6**: Create and manage key aliases for easier reference
- **FR1.7**: Tag keys for organization and cost tracking

### 2. Cryptographic Operations
- **FR2.1**: Encrypt data up to 4 KB directly with a CMK
- **FR2.2**: Decrypt data encrypted by KMS
- **FR2.3**: Generate data encryption keys (DEKs) for envelope encryption
- **FR2.4**: Generate cryptographically secure random bytes
- **FR2.5**: Sign messages using asymmetric private keys
- **FR2.6**: Verify signatures using asymmetric public keys
- **FR2.7**: Support encryption context for authenticated encryption

### 3. Key Rotation
- **FR3.1**: Enable automatic annual key rotation
- **FR3.2**: Maintain old key material for decryption of existing ciphertext
- **FR3.3**: Use new key material for all new encryption operations
- **FR3.4**: Support manual key rotation via key aliases

### 4. Access Control
- **FR4.1**: Define key policies specifying who can use and manage keys
- **FR4.2**: Integrate with IAM for identity-based access control
- **FR4.3**: Support grant-based temporary access delegation
- **FR4.4**: Enforce encryption context requirements in policies

### 5. Audit & Compliance
- **FR5.1**: Log all API calls to audit service (CloudTrail equivalent)
- **FR5.2**: Track key usage across all services
- **FR5.3**: Support compliance certifications (FIPS 140-2, etc.)
- **FR5.4**: Provide key usage metrics and analytics

---

## Non-Functional Requirements

### 1. Security
- **NFR1.1**: Keys stored in FIPS 140-2 Level 2 validated HSMs
- **NFR1.2**: Master keys never leave HSM in plaintext
- **NFR1.3**: All API calls use TLS 1.2+ encryption
- **NFR1.4**: Support multi-region key replication with separate backing keys
- **NFR1.5**: Implement defense-in-depth with multiple security layers
- **NFR1.6**: Zero-trust architecture with mutual TLS between services

### 2. Availability
- **NFR2.1**: 99.99% availability SLA for cryptographic operations
- **NFR2.2**: Multi-AZ deployment for regional resilience
- **NFR2.3**: Automatic failover to standby HSMs within seconds
- **NFR2.4**: Graceful degradation during partial outages
- **NFR2.5**: No single point of failure in the architecture

### 3. Performance
- **NFR3.1**: P99 latency < 100ms for Encrypt/Decrypt operations
- **NFR3.2**: P99 latency < 150ms for GenerateDataKey operations
- **NFR3.3**: Support 10,000+ requests per second per region
- **NFR3.4**: Horizontal scaling for increased throughput
- **NFR3.5**: Request throttling with exponential backoff guidance

### 4. Durability & Reliability
- **NFR4.1**: 99.999999999% (11 nines) durability for key metadata
- **NFR4.2**: Automatic backup of HSM key material to encrypted storage
- **NFR4.3**: Cross-region backup for disaster recovery
- **NFR4.4**: Point-in-time recovery for key metadata

### 5. Compliance
- **NFR5.1**: FIPS 140-2 Level 2 compliance
- **NFR5.2**: PCI DSS compliance
- **NFR5.3**: SOC 2 Type II compliance
- **NFR5.4**: HIPAA compliance
- **NFR5.5**: GDPR compliance

### 6. Scalability
- **NFR6.1**: Support millions of keys per region
- **NFR6.2**: Handle burst traffic 3x normal capacity
- **NFR6.3**: Auto-scaling based on demand
- **NFR6.4**: Regional isolation for independent scaling

### 7. Maintainability
- **NFR7.1**: Zero-downtime deployments
- **NFR7.2**: Automated rollback on deployment failures
- **NFR7.3**: Canary deployments for gradual rollout
- **NFR7.4**: Comprehensive monitoring and alerting

---

## High-Level Architecture

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Applications                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                    ┌───────▼───────┐
                    │   API Gateway  │
                    │   (TLS/Auth)   │
                    └───────┬───────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
    ┌───────▼──────┐ ┌─────▼─────┐ ┌──────▼──────┐
    │ Key Mgmt     │ │ Crypto    │ │  Audit      │
    │ Service      │ │ Service   │ │  Service    │
    └───────┬──────┘ └─────┬─────┘ └──────┬──────┘
            │               │               │
            │       ┌───────▼───────┐       │
            │       │  HSM Cluster  │       │
            │       │  (Multi-AZ)   │       │
            │       └───────────────┘       │
            │                               │
    ┌───────▼──────┐              ┌────────▼────────┐
    │  Metadata DB │              │   Audit Logs    │
    │  (Multi-AZ)  │              │   (Immutable)   │
    └───────┬──────┘              └─────────────────┘
            │
    ┌───────▼──────┐
    │  Backup      │
    │  Storage     │
    └──────────────┘
```

### Components

#### 1. API Gateway Layer
- **Purpose**: Entry point for all KMS requests
- **Responsibilities**:
  - TLS termination
  - Authentication (IAM signature verification)
  - Request validation
  - Rate limiting and throttling
  - Request routing
  - DDoS protection

#### 2. Key Management Service
- **Purpose**: Manages key lifecycle and metadata
- **Responsibilities**:
  - Key creation and deletion
  - Key policy management
  - Alias management
  - Key rotation scheduling
  - Grant management
  - Metadata CRUD operations

#### 3. Crypto Service
- **Purpose**: Performs cryptographic operations
- **Responsibilities**:
  - Encrypt/Decrypt operations
  - Data key generation
  - Digital signatures
  - Signature verification
  - Random number generation
  - HSM communication

#### 4. HSM Cluster
- **Purpose**: Secure key storage and cryptographic operations
- **Responsibilities**:
  - Store master keys in tamper-resistant hardware
  - Perform cryptographic operations
  - Generate cryptographically secure random numbers
  - Key wrapping/unwrapping
  - FIPS 140-2 compliance

#### 5. Metadata Database
- **Purpose**: Store key metadata and policies
- **Responsibilities**:
  - Key metadata (ARN, creation date, state, etc.)
  - Key policies and grants
  - Alias mappings
  - Rotation configuration
  - Tags and metadata

#### 6. Audit Service
- **Purpose**: Log all KMS operations
- **Responsibilities**:
  - Log API calls with full context
  - Track key usage patterns
  - Integration with compliance tools
  - Immutable audit trail

#### 7. Backup Storage
- **Purpose**: Disaster recovery and key material backup
- **Responsibilities**:
  - Encrypted HSM backups
  - Cross-region replication
  - Point-in-time recovery
  - Metadata snapshots

---

## API Specifications

### API Coverage Matrix

| Operation | Implemented | Notes |
|-----------|-------------|-------|
| CreateKey | ✅ | Symmetric & Asymmetric |
| ListKeys | ✅ | With pagination |
| ListAliases | ✅ | With pagination |
| Encrypt | ✅ | ≤ 4 KB payload |
| Decrypt | ✅ | Full support |
| GenerateDataKey | ✅ | Envelope encryption |
| GenerateDataKeyWithoutPlaintext | ✅ | Enhanced security |
| GenerateRandom | ✅ | HSM-backed RNG |
| Sign | ✅ | Asymmetric signing |
| Verify | ✅ | Signature verification |
| GetPublicKey | ✅ | Asymmetric public key export |
| EnableKeyRotation | ✅ | Annual rotation |
| DisableKeyRotation | ✅ | Turn off rotation |

---

### 1. CreateKey

**Purpose**: Create a new Customer Master Key (symmetric or asymmetric).

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeySpec": "SYMMETRIC_DEFAULT | RSA_2048 | RSA_3072 | RSA_4096 | ECC_NIST_P256 | ECC_NIST_P384 | ECC_NIST_P521",
  "KeyUsage": "ENCRYPT_DECRYPT | SIGN_VERIFY",
  "Origin": "AWS_KMS | EXTERNAL | AWS_CLOUDHSM",
  "Description": "string (max 8192 chars)",
  "Policy": "JSON string - key policy",
  "Tags": [
    {
      "TagKey": "string",
      "TagValue": "string"
    }
  ],
  "MultiRegion": "boolean",
  "BypassPolicyLockoutSafetyCheck": "boolean"
}
```

**Response**:
```json
{
  "KeyMetadata": {
    "KeyId": "uuid",
    "Arn": "arn:aws:kms:region:account-id:key/key-id",
    "CreationDate": "timestamp",
    "Enabled": "boolean",
    "Description": "string",
    "KeyUsage": "ENCRYPT_DECRYPT | SIGN_VERIFY",
    "KeyState": "Enabled | Disabled | PendingDeletion | PendingImport",
    "Origin": "AWS_KMS | EXTERNAL | AWS_CLOUDHSM",
    "CloudHsmClusterId": "string",
    "KeySpec": "string",
    "EncryptionAlgorithms": ["array of supported algorithms"],
    "SigningAlgorithms": ["array of supported algorithms"],
    "MultiRegion": "boolean"
  }
}
```

**Key Validations**:
- `KeyUsage` must match `KeySpec` (e.g., RSA keys can be for ENCRYPT_DECRYPT or SIGN_VERIFY)
- `Policy` must be valid JSON and follow IAM policy syntax
- Symmetric keys (`SYMMETRIC_DEFAULT`) can only be used for `ENCRYPT_DECRYPT`
- ECC keys can only be used for `SIGN_VERIFY`

**Error Codes**:
- `InvalidParameterException`: Invalid parameter combination
- `LimitExceededException`: Exceeded key quota
- `MalformedPolicyDocumentException`: Invalid policy JSON

---

### 2. ListKeys

**Purpose**: Enumerate all Customer Master Keys in the account/region.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "Limit": "integer (1-1000, default: 100)",
  "Marker": "string (pagination token)"
}
```

**Response**:
```json
{
  "Keys": [
    {
      "KeyId": "uuid",
      "KeyArn": "arn:aws:kms:region:account-id:key/key-id"
    }
  ],
  "NextMarker": "string (present if more results available)",
  "Truncated": "boolean"
}
```

---

### 3. ListAliases

**Purpose**: List all key aliases in the account/region.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "string (optional - filter by key)",
  "Limit": "integer (1-100, default: 100)",
  "Marker": "string (pagination token)"
}
```

**Response**:
```json
{
  "Aliases": [
    {
      "AliasName": "alias/my-key",
      "AliasArn": "arn:aws:kms:region:account-id:alias/my-key",
      "TargetKeyId": "uuid",
      "CreationDate": "timestamp",
      "LastUpdatedDate": "timestamp"
    }
  ],
  "NextMarker": "string",
  "Truncated": "boolean"
}
```

---

### 4. Encrypt

**Purpose**: Encrypt up to 4 KB of data directly using a CMK.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN",
  "Plaintext": "blob (base64, max 4096 bytes)",
  "EncryptionContext": {
    "key": "value"
  },
  "EncryptionAlgorithm": "SYMMETRIC_DEFAULT | RSAES_OAEP_SHA_1 | RSAES_OAEP_SHA_256"
}
```

**Response**:
```json
{
  "CiphertextBlob": "blob (base64)",
  "KeyId": "arn:aws:kms:region:account-id:key/key-id",
  "EncryptionAlgorithm": "string"
}
```

**Implementation Notes**:
- Encryption context is AAD (Additional Authenticated Data) for AES-GCM
- Ciphertext includes key ID, algorithm, IV, and auth tag
- Size limit enforced to prevent misuse (envelope encryption preferred for large data)

**Error Codes**:
- `KeyUnavailableException`: Key not in `Enabled` state
- `InvalidKeyUsageException`: Key not configured for encryption
- `InvalidCiphertextException`: Malformed plaintext

---

### 5. Decrypt

**Purpose**: Decrypt data encrypted by KMS.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "CiphertextBlob": "blob (base64)",
  "EncryptionContext": {
    "key": "value"
  },
  "KeyId": "string (optional - for validation)",
  "EncryptionAlgorithm": "SYMMETRIC_DEFAULT | RSAES_OAEP_SHA_1 | RSAES_OAEP_SHA_256"
}
```

**Response**:
```json
{
  "Plaintext": "blob (base64)",
  "KeyId": "arn:aws:kms:region:account-id:key/key-id",
  "EncryptionAlgorithm": "string"
}
```

**Implementation Notes**:
- Key ID extracted from ciphertext blob
- Encryption context must match what was used during encryption
- Supports decryption with rotated keys (uses key version from ciphertext)

**Error Codes**:
- `InvalidCiphertextException`: Corrupted or invalid ciphertext
- `IncorrectKeyException`: Specified key ID doesn't match ciphertext
- `KeyUnavailableException`: Key disabled or pending deletion

---

### 6. GenerateDataKey

**Purpose**: Generate a data encryption key for envelope encryption.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN",
  "KeySpec": "AES_256 | AES_128",
  "NumberOfBytes": "integer (1-1024, alternative to KeySpec)",
  "EncryptionContext": {
    "key": "value"
  }
}
```

**Response**:
```json
{
  "CiphertextBlob": "blob (base64) - encrypted DEK",
  "Plaintext": "blob (base64) - plaintext DEK",
  "KeyId": "arn:aws:kms:region:account-id:key/key-id"
}
```

**Workflow**:
1. Generate random bytes in HSM
2. Return plaintext DEK to caller
3. Encrypt DEK with CMK
4. Return both plaintext and encrypted DEK
5. Caller uses plaintext DEK to encrypt data, stores encrypted DEK with data
6. Caller must securely wipe plaintext DEK from memory after use

**Security Best Practices**:
- Always use encryption context for binding DEK to data
- Wipe plaintext DEK from memory immediately after use
- Store encrypted DEK alongside encrypted data

---

### 7. GenerateDataKeyWithoutPlaintext

**Purpose**: Generate encrypted data key without returning plaintext (enhanced security).

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN",
  "KeySpec": "AES_256 | AES_128",
  "NumberOfBytes": "integer (1-1024)",
  "EncryptionContext": {
    "key": "value"
  }
}
```

**Response**:
```json
{
  "CiphertextBlob": "blob (base64) - encrypted DEK",
  "KeyId": "arn:aws:kms:region:account-id:key/key-id"
}
```

**Use Cases**:
- Pre-generate DEKs for future use
- Store encrypted DEKs in advance
- Reduce exposure of plaintext key material
- Batch key generation for performance

---

### 8. GenerateRandom

**Purpose**: Generate cryptographically secure random bytes from HSM.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "NumberOfBytes": "integer (1-1024)"
}
```

**Response**:
```json
{
  "Plaintext": "blob (base64) - random bytes"
}
```

**Implementation**:
- Uses HSM's True Random Number Generator (TRNG)
- FIPS 140-2 compliant random number generation
- No key required (doesn't use any CMK)

**Use Cases**:
- Generate IVs (Initialization Vectors)
- Generate salts for password hashing
- Generate session tokens
- Any use case requiring high-quality randomness

---

### 9. Sign

**Purpose**: Digitally sign a message using an asymmetric CMK.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN",
  "Message": "blob (base64, max 4096 bytes)",
  "MessageType": "RAW | DIGEST",
  "SigningAlgorithm": "RSASSA_PSS_SHA_256 | RSASSA_PSS_SHA_384 | RSASSA_PSS_SHA_512 | RSASSA_PKCS1_V1_5_SHA_256 | RSASSA_PKCS1_V1_5_SHA_384 | RSASSA_PKCS1_V1_5_SHA_512 | ECDSA_SHA_256 | ECDSA_SHA_384 | ECDSA_SHA_512"
}
```

**Response**:
```json
{
  "Signature": "blob (base64)",
  "KeyId": "arn:aws:kms:region:account-id:key/key-id",
  "SigningAlgorithm": "string"
}
```

**Message Types**:
- `RAW`: KMS hashes the message using algorithm's hash function
- `DIGEST`: Message is already hashed (client-side hashing)

**Supported Key Specs**:
- RSA keys: RSA_2048, RSA_3072, RSA_4096
- ECC keys: ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521

**Error Codes**:
- `InvalidKeyUsageException`: Key not configured for signing
- `UnsupportedOperationException`: Algorithm not supported for key spec

---

### 10. Verify

**Purpose**: Verify a digital signature using an asymmetric CMK.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN",
  "Message": "blob (base64)",
  "MessageType": "RAW | DIGEST",
  "Signature": "blob (base64)",
  "SigningAlgorithm": "string (same as used for signing)"
}
```

**Response**:
```json
{
  "SignatureValid": "boolean",
  "KeyId": "arn:aws:kms:region:account-id:key/key-id",
  "SigningAlgorithm": "string"
}
```

**Implementation Notes**:
- Verification uses public key (no private key access needed)
- Message and algorithm must match what was used during signing
- Returns `SignatureValid: false` for invalid signatures (doesn't throw error)

---

### 11. GetPublicKey

**Purpose**: Retrieve the public key portion of an asymmetric CMK.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN"
}
```

**Response**:
```json
{
  "KeyId": "arn:aws:kms:region:account-id:key/key-id",
  "PublicKey": "blob (base64, DER-encoded X.509 SubjectPublicKeyInfo)",
  "KeyUsage": "ENCRYPT_DECRYPT | SIGN_VERIFY",
  "KeySpec": "RSA_2048 | RSA_3072 | RSA_4096 | ECC_NIST_P256 | ECC_NIST_P384 | ECC_NIST_P521",
  "SigningAlgorithms": ["array of supported algorithms"],
  "EncryptionAlgorithms": ["array of supported algorithms"]
}
```

**Use Cases**:
- Client-side encryption/verification outside KMS
- Share public key with external parties
- Offline signature verification
- Integration with third-party crypto libraries

**Security Note**:
- Public key can be freely distributed (not sensitive)
- Private key always remains in HSM

---

### 12. EnableKeyRotation

**Purpose**: Enable automatic annual key rotation for a CMK.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN"
}
```

**Response**:
```json
{}
```

**Implementation**:
- Creates new backing key material annually
- New encryptions use new material
- Old ciphertext can still be decrypted with old material
- KMS tracks which version encrypted each ciphertext
- Only supported for symmetric keys with `Origin: AWS_KMS`

**Error Codes**:
- `UnsupportedOperationException`: Key not eligible for rotation (imported, asymmetric, etc.)

---

### 13. DisableKeyRotation

**Purpose**: Disable automatic key rotation.

**Endpoint**: `POST /`

**Request Parameters**:
```json
{
  "KeyId": "key-id | alias/alias-name | ARN"
}
```

**Response**:
```json
{}
```

**Note**: Disabling rotation doesn't affect existing key versions; they remain available for decryption.

---

## Deep Dives

### 1. Envelope Encryption Architecture

**Why Envelope Encryption?**
- CMKs can only encrypt up to 4 KB directly
- Network overhead for large data
- Performance bottleneck at HSM
- Better security model (defense in depth)

**How It Works**:

```
┌─────────────────────────────────────────────────────────────┐
│                    ENVELOPE ENCRYPTION                       │
└─────────────────────────────────────────────────────────────┘

Step 1: Generate DEK
─────────────────────
Client → KMS: GenerateDataKey(CMK)
KMS → HSM: Generate random bytes
HSM → KMS: Random DEK
KMS → HSM: Encrypt(DEK, CMK)
HSM → KMS: Encrypted DEK
KMS → Client: {Plaintext DEK, Encrypted DEK}

Step 2: Encrypt Data Locally
─────────────────────────────
Client: AES-256-GCM(Data, Plaintext DEK) → Encrypted Data
Client: Wipe Plaintext DEK from memory
Client: Store {Encrypted Data, Encrypted DEK}

Step 3: Decrypt Data (Later)
─────────────────────────────
Client: Retrieve {Encrypted Data, Encrypted DEK}
Client → KMS: Decrypt(Encrypted DEK)
KMS → HSM: Decrypt(Encrypted DEK, CMK)
HSM → KMS: Plaintext DEK
KMS → Client: Plaintext DEK
Client: AES-256-GCM-Decrypt(Encrypted Data, Plaintext DEK)
Client: Wipe Plaintext DEK from memory
```

**Benefits**:
- Encrypt unlimited data size
- Reduce KMS API calls (one per file vs. one per 4KB)
- Better performance (local AES vs. network + HSM)
- Lower cost
- Defense in depth (need both CMK access and encrypted DEK)

---

### 2. HSM Integration Deep Dive

**HSM Architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│                      HSM CLUSTER                             │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │   HSM-1    │  │   HSM-2    │  │   HSM-3    │            │
│  │   (AZ-A)   │  │   (AZ-B)   │  │   (AZ-C)   │            │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘            │
│        │                │                │                   │
│        └────────────────┼────────────────┘                   │
│                         │                                    │
│                 ┌───────▼───────┐                            │
│                 │  Key Sync &   │                            │
│                 │  Replication  │                            │
│                 └───────────────┘                            │
└─────────────────────────────────────────────────────────────┘
```

**HSM Responsibilities**:
1. **Key Generation**: Generate cryptographically secure keys
2. **Key Storage**: Store keys in tamper-resistant hardware
3. **Crypto Operations**: Encrypt, decrypt, sign, verify
4. **Key Wrapping**: Wrap DEKs with CMKs
5. **Random Generation**: FIPS-compliant TRNG

**HSM Communication Protocol**:
```
Crypto Service ←→ HSM Protocol ←→ HSM

1. Mutual TLS authentication
2. Request serialization (protobuf)
3. Command execution in HSM
4. Response serialization
5. Audit logging at HSM level
```

**Key Storage in HSM**:
- Keys identified by UUID
- Key material encrypted at rest in HSM
- Master key stored in Hardware Key Storage
- Keys never leave HSM in plaintext
- Backup keys encrypted with HSM master key

**High Availability**:
- Active-active across AZs
- Automatic failover (<1 second)
- Session affinity not required (stateless operations)
- Load balancing across HSM cluster

---

### 3. Key Rotation Mechanism

**Automatic Rotation**:

```
Timeline: Key Creation and Rotation
────────────────────────────────────

Day 0: Create Key
└─ CMK (v1) created
   └─ All operations use v1

Day 365: First Automatic Rotation
└─ CMK (v2) created
   ├─ New encryptions use v2
   ├─ v1 still available for decryption
   └─ Rotation marked in metadata

Day 730: Second Automatic Rotation
└─ CMK (v3) created
   ├─ New encryptions use v3
   ├─ v1, v2 still available for decryption
   └─ All versions maintained indefinitely
```

**Implementation**:

```java
class KeyRotation {
    // Check rotation schedule (runs daily)
    if (key.rotationEnabled &&
        daysSince(key.lastRotation) >= 365) {

        // Generate new key version in HSM
        KeyVersion newVersion = hsm.generateKeyVersion(key.id);

        // Update metadata
        key.currentVersion = newVersion.id;
        key.lastRotation = now();
        key.versions.add(newVersion);

        // All new encryptions use new version
        // Old versions remain for decryption
    }
}

// Encryption (always uses current version)
function encrypt(keyId, plaintext) {
    key = getKey(keyId);
    version = key.currentVersion;
    return hsm.encrypt(version, plaintext);
}

// Decryption (extracts version from ciphertext)
function decrypt(ciphertext) {
    version = extractVersion(ciphertext);
    return hsm.decrypt(version, ciphertext);
}
```

**Rotation Benefits**:
- Limits blast radius of key compromise
- Compliance with security policies
- No downtime or re-encryption needed
- Transparent to applications

**Limitations**:
- Only for symmetric keys with `Origin: AWS_KMS`
- Not supported for imported keys
- Not supported for asymmetric keys
- Annual rotation only (not customizable)

---

### 4. Access Control Model

**Three-Layer Authorization**:

```
┌─────────────────────────────────────────────────────────────┐
│                    REQUEST AUTHORIZATION                     │
└─────────────────────────────────────────────────────────────┘

Layer 1: IAM Policy (Identity-based)
─────────────────────────────────────
{
  "Effect": "Allow",
  "Action": "kms:Encrypt",
  "Resource": "arn:aws:kms:*:*:key/*",
  "Condition": {
    "StringEquals": {
      "kms:EncryptionContext:Department": "Finance"
    }
  }
}

Layer 2: Key Policy (Resource-based)
─────────────────────────────────────
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::123456789012:role/AppRole"
  },
  "Action": ["kms:Encrypt", "kms:Decrypt"],
  "Resource": "*",
  "Condition": {
    "StringEquals": {
      "kms:EncryptionContext:AppId": "myapp"
    }
  }
}

Layer 3: Grants (Temporary delegation)
───────────────────────────────────────
{
  "GrantId": "grant-id",
  "GranteePrincipal": "arn:aws:iam::123456789012:role/TempRole",
  "Operations": ["Decrypt"],
  "Constraints": {
    "EncryptionContextSubset": {
      "Department": "Finance"
    }
  },
  "ExpirationTime": "2024-12-31T23:59:59Z"
}

Authorization Decision: ALLOW if (IAM Policy AND Key Policy) OR Grant
```

**Encryption Context Binding**:
- Key-value pairs for authenticated encryption
- Part of AAD in AES-GCM
- Must be provided on decrypt (authentication fails otherwise)
- Used for access control and audit

**Example**:
```json
{
  "Department": "Finance",
  "DocumentId": "doc-12345",
  "Classification": "Confidential"
}
```

**Benefits**:
- Bind encrypted data to context
- Prevent ciphertext substitution attacks
- Fine-grained access control
- Better audit trail

---

### 5. Multi-Region Keys

**Architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│              MULTI-REGION KEY ARCHITECTURE                   │
└─────────────────────────────────────────────────────────────┘

Primary Region: us-east-1
┌──────────────────────────────┐
│  Primary Key                 │
│  ├─ Key ID: mrk-xxx         │
│  ├─ Key Material: K1        │
│  └─ Metadata: {primary}     │
└──────────────────────────────┘
         │
         │ Replicate
         │
    ┌────┴─────┐
    ▼          ▼
Replica 1      Replica 2
us-west-2      eu-west-1
┌──────────┐   ┌──────────┐
│  Replica │   │  Replica │
│  Key ID  │   │  Key ID  │
│  mrk-xxx │   │  mrk-xxx │
│  Material│   │  Material│
│  K2      │   │  K3      │
└──────────┘   └──────────┘
```

**Key Properties**:
- Same key ID across regions (`mrk-` prefix)
- Different key material in each region
- Encrypt in one region, decrypt in another
- Ciphertext includes region info
- Automatic cross-region routing

**Use Cases**:
- Global applications
- Disaster recovery
- Low-latency access
- Data sovereignty compliance

**Security Model**:
- Each region has separate HSM-backed key material
- Compromise in one region doesn't affect others
- Independent key policies per region
- Separate audit logs

---

### 6. Ciphertext Structure

**Encrypted Data Format**:

```
┌─────────────────────────────────────────────────────────────┐
│                    KMS CIPHERTEXT BLOB                       │
└─────────────────────────────────────────────────────────────┘

Header (Cleartext)
──────────────────
├─ Version: 1 byte (0x01)
├─ Type: 1 byte (0x80 = symmetric)
├─ Key ID: 16 bytes (UUID)
├─ Key Version: 4 bytes
├─ Algorithm: 2 bytes
├─ IV: 12 bytes (for AES-GCM)
└─ Context Length: 2 bytes

Encryption Context (Cleartext)
───────────────────────────────
└─ Serialized JSON (if present)

Encrypted Data
──────────────
└─ Ciphertext: Variable length

Authentication Tag
──────────────────
└─ GCM Tag: 16 bytes

Total: ~60 bytes overhead + context + ciphertext
```

**Why Include Metadata?**
- Self-describing ciphertext
- Key ID for routing decrypt requests
- Version for rotation support
- Algorithm for forward compatibility
- IV for proper decryption

**Security**:
- Metadata is authenticated (part of AAD)
- Tampering detected by GCM tag verification
- Context must match for successful decryption

---

### 7. Rate Limiting and Throttling

**Request Quotas**:

| Operation | Shared Quota | Request Rate |
|-----------|--------------|--------------|
| Encrypt | Yes | 10,000/sec |
| Decrypt | Yes | 10,000/sec |
| GenerateDataKey | Yes | 10,000/sec |
| GenerateRandom | No | 1,000/sec |
| Sign | Yes | 1,000/sec |
| Verify | No | 5,000/sec |
| CreateKey | No | 5/sec |
| ListKeys | No | 100/sec |

**Throttling Algorithm**:
```
Token Bucket Algorithm
──────────────────────

Bucket Capacity: Request quota per second
Refill Rate: 1 token per (1 second / quota)

Request Processing:
1. Check if token available
2. If yes, consume token and process
3. If no, return ThrottlingException
4. Tokens refill continuously

Example (Encrypt at 10,000/sec):
- Bucket size: 10,000 tokens
- Refill: 10,000 tokens per second
- Burst: Can use 10,000 immediately
- Sustained: 10,000 per second average
```

**Client Retry Strategy**:
```python
def kms_operation_with_retry(operation, **kwargs):
    max_retries = 10
    base_delay = 0.1  # 100ms

    for attempt in range(max_retries):
        try:
            return operation(**kwargs)
        except ThrottlingException:
            if attempt == max_retries - 1:
                raise

            # Exponential backoff with jitter
            delay = min(base_delay * (2 ** attempt), 60)
            jitter = random.uniform(0, delay * 0.1)
            time.sleep(delay + jitter)
```

**Quota Management**:
- Per-account, per-region quotas
- Request quota increase through support
- Monitor CloudWatch metrics for throttling
- Use caching to reduce API calls

---

### 8. Audit and Compliance

**Audit Trail Components**:

```
┌─────────────────────────────────────────────────────────────┐
│                    AUDIT LOG ENTRY                           │
└─────────────────────────────────────────────────────────────┘

{
  "eventTime": "2024-01-15T10:30:00Z",
  "eventSource": "kms.amazonaws.com",
  "eventName": "Decrypt",
  "awsRegion": "us-east-1",
  "sourceIPAddress": "203.0.113.12",
  "userAgent": "aws-sdk-java/2.17.100",
  "requestParameters": {
    "encryptionAlgorithm": "SYMMETRIC_DEFAULT",
    "keyId": "arn:aws:kms:us-east-1:123456789012:key/xxx"
  },
  "responseElements": null,  // Sensitive data redacted
  "requestID": "req-xxx",
  "eventID": "evt-xxx",
  "readOnly": false,
  "resources": [
    {
      "accountId": "123456789012",
      "type": "AWS::KMS::Key",
      "ARN": "arn:aws:kms:us-east-1:123456789012:key/xxx"
    }
  ],
  "eventType": "AwsApiCall",
  "recipientAccountId": "123456789012",
  "sharedEventID": "shared-xxx",
  "encryptionContext": {
    "Department": "Finance"  // Logged for audit
  }
}
```

**What Gets Logged**:
- ✅ All API calls (management and cryptographic)
- ✅ Who made the request (IAM principal)
- ✅ When the request was made
- ✅ Source IP address
- ✅ Request parameters (except sensitive data)
- ✅ Whether request succeeded or failed
- ✅ Error codes for failures
- ✅ Encryption context (for audit trail)

**What Doesn't Get Logged**:
- ❌ Plaintext data
- ❌ Plaintext DEKs
- ❌ Private keys
- ❌ Signatures (too large)

**Compliance Reports**:
```sql
-- Example: Keys used by department
SELECT
  encryptionContext.Department,
  COUNT(*) as operation_count,
  COUNT(DISTINCT resources.ARN) as unique_keys
FROM audit_logs
WHERE eventName IN ('Encrypt', 'Decrypt')
  AND eventTime >= '2024-01-01'
GROUP BY encryptionContext.Department;

-- Example: Unauthorized access attempts
SELECT
  userIdentity.principalId,
  eventName,
  errorCode,
  COUNT(*) as failure_count
FROM audit_logs
WHERE errorCode IN ('AccessDenied', 'UnauthorizedException')
  AND eventTime >= NOW() - INTERVAL '24 hours'
GROUP BY userIdentity.principalId, eventName, errorCode
HAVING COUNT(*) > 10;
```

---

### 9. Disaster Recovery

**Backup Strategy**:

```
┌─────────────────────────────────────────────────────────────┐
│              DISASTER RECOVERY ARCHITECTURE                  │
└─────────────────────────────────────────────────────────────┘

Primary Region
──────────────
┌─────────────────┐
│  HSM Cluster    │  ──┐
│  Key Material   │    │
└─────────────────┘    │
                       │ Real-time Replication
┌─────────────────┐    │
│  Metadata DB    │  ──┤
│  (RDS/DynamoDB) │    │
└─────────────────┘    │
                       │
                       ▼
            ┌──────────────────┐
            │  Backup Storage  │
            │  (S3 + Glacier)  │
            └──────────────────┘
                       │
                       │ Cross-Region Replication
                       ▼
DR Region
─────────
┌─────────────────┐
│  HSM Cluster    │  ← Restore from backup
│  (Standby)      │
└─────────────────┘

┌─────────────────┐
│  Metadata DB    │  ← Read replica / Point-in-time restore
│  (Standby)      │
└─────────────────┘
```

**Backup Components**:

1. **HSM Key Material**:
   - Encrypted backup to S3 every hour
   - HSM master key used for encryption
   - Cross-region replication to DR region
   - Retention: 90 days

2. **Metadata Database**:
   - Continuous replication to standby
   - Point-in-time recovery (35 days)
   - Cross-region read replicas
   - Automated snapshots every 24 hours

3. **Audit Logs**:
   - Immutable storage in S3
   - Cross-region replication
   - Glacier archival after 90 days
   - 7-year retention for compliance

**Recovery Procedures**:

**Scenario 1: Single HSM Failure**
```
RTO: <1 second
RPO: 0 (no data loss)

Steps:
1. Load balancer detects failure
2. Routes traffic to healthy HSMs
3. Alert operations team
4. Replace failed HSM
5. Restore key material from cluster
```

**Scenario 2: AZ Failure**
```
RTO: <5 seconds
RPO: 0 (no data loss)

Steps:
1. Health checks fail for AZ
2. Traffic routed to other AZs
3. Operations notified
4. Monitor for AZ recovery
5. Gradual traffic restoration
```

**Scenario 3: Region Failure**
```
RTO: <1 hour
RPO: <15 minutes

Steps:
1. Declare regional disaster
2. Promote DR region HSMs
3. Restore latest metadata snapshot
4. Update DNS to DR region
5. Verify all systems operational
6. Monitor for issues
```

---

## Data Models

### Key Metadata Schema

```sql
CREATE TABLE customer_master_keys (
    key_id UUID PRIMARY KEY,
    account_id VARCHAR(12) NOT NULL,
    region VARCHAR(20) NOT NULL,
    arn VARCHAR(255) NOT NULL UNIQUE,

    -- Key Properties
    key_spec VARCHAR(50) NOT NULL,  -- SYMMETRIC_DEFAULT, RSA_2048, etc.
    key_usage VARCHAR(20) NOT NULL,  -- ENCRYPT_DECRYPT, SIGN_VERIFY
    origin VARCHAR(20) NOT NULL,     -- AWS_KMS, EXTERNAL, AWS_CLOUDHSM
    key_state VARCHAR(20) NOT NULL,  -- Enabled, Disabled, PendingDeletion

    -- Descriptive Info
    description TEXT,

    -- Timestamps
    creation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    deletion_date TIMESTAMP,

    -- Rotation
    rotation_enabled BOOLEAN DEFAULT FALSE,
    last_rotation_date TIMESTAMP,
    next_rotation_date TIMESTAMP,

    -- Multi-Region
    multi_region BOOLEAN DEFAULT FALSE,
    primary_key_id UUID,  -- NULL for single-region, self for primary, other for replica

    -- HSM Reference
    hsm_cluster_id VARCHAR(50),
    current_key_version INTEGER DEFAULT 1,

    -- Indexes
    INDEX idx_account_region (account_id, region),
    INDEX idx_key_state (key_state),
    INDEX idx_deletion_date (deletion_date) WHERE deletion_date IS NOT NULL
);

CREATE TABLE key_versions (
    version_id SERIAL PRIMARY KEY,
    key_id UUID NOT NULL REFERENCES customer_master_keys(key_id),
    version_number INTEGER NOT NULL,

    -- HSM reference for this version's key material
    hsm_key_handle VARCHAR(255) NOT NULL,

    creation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    enabled BOOLEAN DEFAULT TRUE,

    UNIQUE (key_id, version_number)
);

CREATE TABLE key_policies (
    key_id UUID PRIMARY KEY REFERENCES customer_master_keys(key_id),
    policy_document JSONB NOT NULL,
    version VARCHAR(20) DEFAULT '2012-10-17',
    last_updated TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE key_aliases (
    alias_name VARCHAR(256) PRIMARY KEY,
    alias_arn VARCHAR(512) NOT NULL UNIQUE,
    target_key_id UUID NOT NULL REFERENCES customer_master_keys(key_id),
    account_id VARCHAR(12) NOT NULL,
    region VARCHAR(20) NOT NULL,
    creation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated_date TIMESTAMP NOT NULL DEFAULT NOW(),

    INDEX idx_target_key (target_key_id)
);

CREATE TABLE key_grants (
    grant_id UUID PRIMARY KEY,
    key_id UUID NOT NULL REFERENCES customer_master_keys(key_id),
    grantee_principal VARCHAR(512) NOT NULL,

    operations TEXT[] NOT NULL,  -- Array of allowed operations
    constraints JSONB,            -- Encryption context constraints

    creation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    expiration_date TIMESTAMP,
    retiring_principal VARCHAR(512),

    INDEX idx_key_grantee (key_id, grantee_principal),
    INDEX idx_expiration (expiration_date) WHERE expiration_date IS NOT NULL
);

CREATE TABLE key_tags (
    key_id UUID NOT NULL REFERENCES customer_master_keys(key_id),
    tag_key VARCHAR(128) NOT NULL,
    tag_value VARCHAR(256) NOT NULL,

    PRIMARY KEY (key_id, tag_key)
);
```

---

## Security Considerations

### 1. Threat Model

**Threats Mitigated**:
- ✅ Key theft (keys in HSM, never in plaintext outside)
- ✅ Unauthorized access (IAM + key policies + grants)
- ✅ Insider threats (separation of duties, audit logs)
- ✅ Network interception (TLS 1.2+, certificate pinning)
- ✅ Replay attacks (nonce/IV in encryption)
- ✅ Key compromise (rotation, time-limited grants)

**Attack Scenarios**:

**1. Compromised Application Credentials**
```
Attacker gains: IAM credentials with kms:Decrypt permission

Mitigations:
- Encryption context binding (attacker needs correct context)
- Key policies with condition keys
- Grant expiration
- Audit alerts on unusual usage patterns
- Rate limiting prevents bulk decryption
```

**2. HSM Physical Access**
```
Attacker gains: Physical access to HSM

Mitigations:
- FIPS 140-2 Level 2 tamper-resistant hardware
- Tamper detection triggers key zeroization
- Multi-person access controls in data center
- Video surveillance
- HSM authentication required
```

**3. Insider Threat (AWS Employee)**
```
Attacker: Malicious AWS employee

Mitigations:
- Separation of duties (no single person has full access)
- All access logged and monitored
- Anomaly detection for unusual access
- Regular access audits
- Background checks and training
- Keys encrypted even within AWS systems
```

### 2. Cryptographic Standards

**Algorithms**:

| Operation | Algorithm | Key Size | Standard |
|-----------|-----------|----------|----------|
| Symmetric Encryption | AES-GCM | 256-bit | FIPS 197 |
| Asymmetric Encryption | RSA-OAEP | 2048-4096 bit | PKCS#1 v2.2 |
| Digital Signature (RSA) | RSA-PSS, PKCS1 v1.5 | 2048-4096 bit | FIPS 186-4 |
| Digital Signature (ECC) | ECDSA | P-256, P-384, P-521 | FIPS 186-4 |
| Key Derivation | HKDF | 256-bit | RFC 5869 |
| Random Generation | DRBG | - | NIST SP 800-90A |

**Key Lengths**:
- Symmetric: 256-bit (AES-256) - 128-bit security level
- RSA: Minimum 2048-bit - equivalent to ~112-bit symmetric
- ECC: P-256 (128-bit security), P-384 (192-bit), P-521 (256-bit)

**Why These Choices**:
- FIPS 140-2 compliance required
- NIST-approved algorithms only
- Post-quantum resistance not yet standard (watching NIST PQC)
- Balance of security, performance, compatibility

---

## Scalability & Performance

### 1. Scalability Design

**Horizontal Scaling**:

```
┌─────────────────────────────────────────────────────────────┐
│                    SCALING ARCHITECTURE                      │
└─────────────────────────────────────────────────────────────┘

                    Load Balancer
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌────▼────┐      ┌────▼────┐     ┌────▼────┐
   │  API    │      │  API    │     │  API    │
   │  Node 1 │      │  Node 2 │ ... │  Node N │
   └────┬────┘      └────┬────┘     └────┬────┘
        │                │                │
        └────────────────┼────────────────┘
                         │
                    ┌────▼────┐
                    │  Shared │
                    │  State  │
                    │  (none) │
                    └─────────┘

Scaling Triggers:
- CPU > 70% for 5 minutes → Add node
- Request latency P99 > 200ms → Add node
- HSM queue depth > 100 → Add HSM
- Active connections > 10,000 per node → Add node
```

**Auto-Scaling Configuration**:
```yaml
auto_scaling:
  min_instances: 10
  max_instances: 1000
  target_cpu: 60%
  target_latency_p99: 100ms
  scale_up_cooldown: 60s
  scale_down_cooldown: 300s

  scaling_policies:
    - metric: cpu_utilization
      threshold: 70
      action: scale_up
      adjustment: +20%

    - metric: request_latency_p99
      threshold: 150ms
      action: scale_up
      adjustment: +30%
```

### 2. Performance Optimization

**Caching Strategy**:

```
┌─────────────────────────────────────────────────────────────┐
│                    CACHING LAYERS                            │
└─────────────────────────────────────────────────────────────┘

L1: In-Memory Cache (per node)
───────────────────────────────
Cache: Key metadata, policies
TTL: 60 seconds
Size: 10,000 entries
Eviction: LRU
Hit Rate: ~80%

L2: Distributed Cache (Redis)
──────────────────────────────
Cache: Key metadata, public keys
TTL: 300 seconds
Size: 1M entries
Hit Rate: ~95%

L3: Database Read Replicas
───────────────────────────
Replicas: 5 per region
Lag: <1 second
Read traffic: 90% of queries
```

**Connection Pooling**:
```java
// HSM connection pool
HSMConnectionPool pool = HSMConnectionPool.builder()
    .minConnections(50)
    .maxConnections(200)
    .connectionTimeout(5000)  // 5 seconds
    .idleTimeout(300000)      // 5 minutes
    .healthCheckInterval(30000)
    .build();

// Database connection pool
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(100);
config.setMinimumIdle(20);
config.setConnectionTimeout(10000);
config.setIdleTimeout(600000);
```

**Performance Benchmarks**:

| Operation | P50 | P99 | P99.9 | Throughput |
|-----------|-----|-----|-------|------------|
| Encrypt | 15ms | 45ms | 80ms | 15,000/sec/node |
| Decrypt | 18ms | 50ms | 90ms | 12,000/sec/node |
| GenerateDataKey | 20ms | 60ms | 100ms | 10,000/sec/node |
| Sign (RSA-2048) | 25ms | 70ms | 120ms | 8,000/sec/node |
| Verify (RSA-2048) | 12ms | 35ms | 60ms | 20,000/sec/node |
| CreateKey | 100ms | 300ms | 500ms | 100/sec/node |

---

## Monitoring & Observability

### 1. Metrics

**System Metrics**:
```
# Request metrics
kms.requests.count{operation, status, region}
kms.requests.latency{operation, percentile}
kms.requests.errors{operation, error_code}

# Key metrics
kms.keys.count{state, key_spec}
kms.keys.operations{key_id, operation}
kms.keys.rotation{status}

# HSM metrics
kms.hsm.queue_depth
kms.hsm.operation_latency{operation}
kms.hsm.errors{error_type}
kms.hsm.availability{hsm_id}

# Throttling metrics
kms.throttling.events{operation}
kms.throttling.rate

# Database metrics
kms.db.query_latency{query_type}
kms.db.connection_pool{state}
kms.db.replication_lag
```

### 2. Alerting

**Critical Alerts**:
```yaml
- name: HSM_Unavailable
  condition: hsm.availability < 100%
  duration: 30s
  severity: P0
  action: Page on-call, auto-failover

- name: High_Error_Rate
  condition: error_rate > 1%
  duration: 2m
  severity: P1
  action: Page on-call

- name: Increased_Latency
  condition: p99_latency > 200ms
  duration: 5m
  severity: P2
  action: Auto-scale, notify team

- name: Replication_Lag
  condition: db_replication_lag > 10s
  duration: 1m
  severity: P1
  action: Notify DBA team
```

### 3. Dashboards

**Operations Dashboard**:
- Requests per second (by operation)
- Error rate (by operation and error code)
- P50, P99, P99.9 latency
- HSM queue depth
- Active keys by state
- Throttling events

**Capacity Dashboard**:
- CPU, memory, disk usage
- Connection pool stats
- Auto-scaling events
- Request queue depth
- Database IOPS

**Security Dashboard**:
- Failed authorization attempts
- Keys accessed by principal
- Encryption context violations
- Unusual access patterns
- Grant expirations

---

## Conclusion

This system design provides a comprehensive, secure, and scalable Key Management Service that:

✅ **Meets Functional Requirements**: All core KMS operations implemented
✅ **Achieves Non-Functional Requirements**: High availability, performance, security
✅ **Provides Complete API Coverage**: All 13 operations specified
✅ **Ensures Security**: HSM-backed, multi-layer authorization, comprehensive audit
✅ **Scales Effectively**: Horizontal scaling, caching, connection pooling
✅ **Maintains Compliance**: FIPS 140-2, PCI DSS, HIPAA, SOC 2, GDPR

The architecture leverages industry best practices including envelope encryption, defense in depth, zero-trust security, and immutable audit logging to provide a world-class key management service.
