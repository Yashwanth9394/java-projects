package com.cloudservice.kms;

import com.cloudservice.common.CloudServiceException;

import java.util.List;

public interface KMSService {

    String createKey(String keyAlias, KeyType keyType) throws CloudServiceException;

    byte[] encrypt(String keyId, byte[] plaintext) throws CloudServiceException;

    byte[] decrypt(String keyId, byte[] ciphertext) throws CloudServiceException;

    String generateDataKey(String keyId, int keyLength) throws CloudServiceException;

    void rotateKey(String keyId) throws CloudServiceException;

    void deleteKey(String keyId) throws CloudServiceException;

    KeyMetadata describeKey(String keyId) throws CloudServiceException;

    List<KeyMetadata> listKeys() throws CloudServiceException;

    void enableKey(String keyId) throws CloudServiceException;

    void disableKey(String keyId) throws CloudServiceException;

    enum KeyType {
        SYMMETRIC,
        ASYMMETRIC_RSA_2048,
        ASYMMETRIC_RSA_4096,
        ASYMMETRIC_ECC
    }
}
