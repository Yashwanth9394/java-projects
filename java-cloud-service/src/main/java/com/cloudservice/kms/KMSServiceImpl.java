package com.cloudservice.kms;

import com.cloudservice.common.CloudServiceException;
import com.cloudservice.common.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KMSServiceImpl implements KMSService {
    private static final Logger logger = LoggerFactory.getLogger(KMSServiceImpl.class);

    private final ServiceConfig config;
    private final Map<String, KeyMetadata> keyMetadataStore;
    private final Map<String, Key> keyStore;
    private final SecureRandom secureRandom;

    public KMSServiceImpl(ServiceConfig config) {
        this.config = config;
        this.keyMetadataStore = new ConcurrentHashMap<>();
        this.keyStore = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
        logger.info("KMS Service initialized");
    }

    @Override
    public String createKey(String keyAlias, KeyType keyType) throws CloudServiceException {
        try {
            String keyId = "key-" + UUID.randomUUID().toString();

            Key key;
            switch (keyType) {
                case SYMMETRIC:
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(256, secureRandom);
                    key = keyGen.generateKey();
                    break;

                case ASYMMETRIC_RSA_2048:
                    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
                    keyPairGen.initialize(2048, secureRandom);
                    KeyPair keyPair = keyPairGen.generateKeyPair();
                    key = keyPair.getPrivate();
                    break;

                case ASYMMETRIC_RSA_4096:
                    KeyPairGenerator keyPairGen2 = KeyPairGenerator.getInstance("RSA");
                    keyPairGen2.initialize(4096, secureRandom);
                    KeyPair keyPair2 = keyPairGen2.generateKeyPair();
                    key = keyPair2.getPrivate();
                    break;

                default:
                    throw new CloudServiceException("Unsupported key type: " + keyType, "UNSUPPORTED_KEY_TYPE");
            }

            keyStore.put(keyId, key);
            KeyMetadata metadata = new KeyMetadata(keyId, keyAlias, keyType);
            keyMetadataStore.put(keyId, metadata);

            logger.info("Created key with ID: {} and alias: {}", keyId, keyAlias);
            return keyId;

        } catch (NoSuchAlgorithmException e) {
            throw new CloudServiceException("Failed to create key", "KEY_CREATION_FAILED", e);
        }
    }

    @Override
    public byte[] encrypt(String keyId, byte[] plaintext) throws CloudServiceException {
        KeyMetadata metadata = getKeyMetadata(keyId);

        if (metadata.getKeyState() != KeyMetadata.KeyState.ENABLED) {
            throw new CloudServiceException("Key is not enabled", "KEY_NOT_ENABLED");
        }

        try {
            Key key = keyStore.get(keyId);
            if (key == null) {
                throw new CloudServiceException("Key not found", "KEY_NOT_FOUND");
            }

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plaintext);

            logger.info("Encrypted data using key: {}", keyId);
            return encrypted;

        } catch (Exception e) {
            throw new CloudServiceException("Encryption failed", "ENCRYPTION_FAILED", e);
        }
    }

    @Override
    public byte[] decrypt(String keyId, byte[] ciphertext) throws CloudServiceException {
        KeyMetadata metadata = getKeyMetadata(keyId);

        if (metadata.getKeyState() != KeyMetadata.KeyState.ENABLED) {
            throw new CloudServiceException("Key is not enabled", "KEY_NOT_ENABLED");
        }

        try {
            Key key = keyStore.get(keyId);
            if (key == null) {
                throw new CloudServiceException("Key not found", "KEY_NOT_FOUND");
            }

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(ciphertext);

            logger.info("Decrypted data using key: {}", keyId);
            return decrypted;

        } catch (Exception e) {
            throw new CloudServiceException("Decryption failed", "DECRYPTION_FAILED", e);
        }
    }

    @Override
    public String generateDataKey(String keyId, int keyLength) throws CloudServiceException {
        KeyMetadata metadata = getKeyMetadata(keyId);

        if (metadata.getKeyState() != KeyMetadata.KeyState.ENABLED) {
            throw new CloudServiceException("Key is not enabled", "KEY_NOT_ENABLED");
        }

        byte[] keyBytes = new byte[keyLength / 8];
        secureRandom.nextBytes(keyBytes);
        String dataKey = Base64.getEncoder().encodeToString(keyBytes);

        logger.info("Generated data key using master key: {}", keyId);
        return dataKey;
    }

    @Override
    public void rotateKey(String keyId) throws CloudServiceException {
        KeyMetadata metadata = getKeyMetadata(keyId);
        metadata.setLastRotationDate(Instant.now());
        logger.info("Rotated key: {}", keyId);
    }

    @Override
    public void deleteKey(String keyId) throws CloudServiceException {
        KeyMetadata metadata = getKeyMetadata(keyId);
        metadata.setKeyState(KeyMetadata.KeyState.PENDING_DELETION);
        logger.info("Marked key for deletion: {}", keyId);
    }

    @Override
    public KeyMetadata describeKey(String keyId) throws CloudServiceException {
        return getKeyMetadata(keyId);
    }

    @Override
    public List<KeyMetadata> listKeys() throws CloudServiceException {
        return new ArrayList<>(keyMetadataStore.values());
    }

    @Override
    public void enableKey(String keyId) throws CloudServiceException {
        KeyMetadata metadata = getKeyMetadata(keyId);
        metadata.setKeyState(KeyMetadata.KeyState.ENABLED);
        logger.info("Enabled key: {}", keyId);
    }

    @Override
    public void disableKey(String keyId) throws CloudServiceException {
        KeyMetadata metadata = getKeyMetadata(keyId);
        metadata.setKeyState(KeyMetadata.KeyState.DISABLED);
        logger.info("Disabled key: {}", keyId);
    }

    private KeyMetadata getKeyMetadata(String keyId) throws CloudServiceException {
        KeyMetadata metadata = keyMetadataStore.get(keyId);
        if (metadata == null) {
            throw new CloudServiceException("Key not found: " + keyId, "KEY_NOT_FOUND");
        }
        return metadata;
    }
}
