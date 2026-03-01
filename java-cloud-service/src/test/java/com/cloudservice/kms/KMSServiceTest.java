package com.cloudservice.kms;

import com.cloudservice.CloudServiceFactory;
import com.cloudservice.common.CloudServiceException;
import com.cloudservice.common.ServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KMS Service Tests")
class KMSServiceTest {

    private KMSService kmsService;
    private ServiceConfig config;

    @BeforeEach
    void setUp() {
        config = new ServiceConfig();
        config.setRegion("us-east-1");
        kmsService = new KMSServiceImpl(config);
    }

    @Test
    @DisplayName("Should create symmetric key successfully")
    void testCreateSymmetricKey() throws CloudServiceException {
        String keyId = kmsService.createKey("test-symmetric-key", KMSService.KeyType.SYMMETRIC);

        assertNotNull(keyId);
        assertTrue(keyId.startsWith("key-"));

        KeyMetadata metadata = kmsService.describeKey(keyId);
        assertEquals("test-symmetric-key", metadata.getKeyAlias());
        assertEquals(KMSService.KeyType.SYMMETRIC, metadata.getKeyType());
        assertEquals(KeyMetadata.KeyState.ENABLED, metadata.getKeyState());
    }

    @Test
    @DisplayName("Should create asymmetric RSA 2048 key successfully")
    void testCreateAsymmetricKey() throws CloudServiceException {
        String keyId = kmsService.createKey("test-rsa-key", KMSService.KeyType.ASYMMETRIC_RSA_2048);

        assertNotNull(keyId);
        KeyMetadata metadata = kmsService.describeKey(keyId);
        assertEquals(KMSService.KeyType.ASYMMETRIC_RSA_2048, metadata.getKeyType());
    }

    @Test
    @DisplayName("Should encrypt and decrypt data successfully")
    void testEncryptDecrypt() throws CloudServiceException {
        String keyId = kmsService.createKey("encryption-key", KMSService.KeyType.SYMMETRIC);

        String plaintext = "Hello, Cloud Service!";
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = kmsService.encrypt(keyId, plaintextBytes);
        assertNotNull(encrypted);
        assertNotEquals(0, encrypted.length);

        byte[] decrypted = kmsService.decrypt(keyId, encrypted);
        assertNotNull(decrypted);

        String decryptedText = new String(decrypted, StandardCharsets.UTF_8);
        assertEquals(plaintext, decryptedText);
    }

    @Test
    @DisplayName("Should fail to encrypt with disabled key")
    void testEncryptWithDisabledKey() throws CloudServiceException {
        String keyId = kmsService.createKey("disabled-key", KMSService.KeyType.SYMMETRIC);
        kmsService.disableKey(keyId);

        byte[] plaintext = "test".getBytes(StandardCharsets.UTF_8);

        CloudServiceException exception = assertThrows(CloudServiceException.class, () -> {
            kmsService.encrypt(keyId, plaintext);
        });

        assertEquals("KEY_NOT_ENABLED", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should fail to decrypt with disabled key")
    void testDecryptWithDisabledKey() throws CloudServiceException {
        String keyId = kmsService.createKey("decrypt-key", KMSService.KeyType.SYMMETRIC);

        byte[] plaintext = "test".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = kmsService.encrypt(keyId, plaintext);

        kmsService.disableKey(keyId);

        CloudServiceException exception = assertThrows(CloudServiceException.class, () -> {
            kmsService.decrypt(keyId, encrypted);
        });

        assertEquals("KEY_NOT_ENABLED", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should generate data key successfully")
    void testGenerateDataKey() throws CloudServiceException {
        String keyId = kmsService.createKey("master-key", KMSService.KeyType.SYMMETRIC);

        String dataKey256 = kmsService.generateDataKey(keyId, 256);
        assertNotNull(dataKey256);

        String dataKey128 = kmsService.generateDataKey(keyId, 128);
        assertNotNull(dataKey128);

        assertNotEquals(dataKey256, dataKey128);
    }

    @Test
    @DisplayName("Should rotate key successfully")
    void testRotateKey() throws CloudServiceException {
        String keyId = kmsService.createKey("rotation-key", KMSService.KeyType.SYMMETRIC);

        KeyMetadata before = kmsService.describeKey(keyId);
        assertNull(before.getLastRotationDate());

        kmsService.rotateKey(keyId);

        KeyMetadata after = kmsService.describeKey(keyId);
        assertNotNull(after.getLastRotationDate());
    }

    @Test
    @DisplayName("Should list all keys")
    void testListKeys() throws CloudServiceException {
        kmsService.createKey("key-1", KMSService.KeyType.SYMMETRIC);
        kmsService.createKey("key-2", KMSService.KeyType.SYMMETRIC);
        kmsService.createKey("key-3", KMSService.KeyType.ASYMMETRIC_RSA_2048);

        List<KeyMetadata> keys = kmsService.listKeys();
        assertTrue(keys.size() >= 3);
    }

    @Test
    @DisplayName("Should enable and disable key")
    void testEnableDisableKey() throws CloudServiceException {
        String keyId = kmsService.createKey("toggle-key", KMSService.KeyType.SYMMETRIC);

        KeyMetadata metadata = kmsService.describeKey(keyId);
        assertEquals(KeyMetadata.KeyState.ENABLED, metadata.getKeyState());

        kmsService.disableKey(keyId);
        metadata = kmsService.describeKey(keyId);
        assertEquals(KeyMetadata.KeyState.DISABLED, metadata.getKeyState());

        kmsService.enableKey(keyId);
        metadata = kmsService.describeKey(keyId);
        assertEquals(KeyMetadata.KeyState.ENABLED, metadata.getKeyState());
    }

    @Test
    @DisplayName("Should delete key")
    void testDeleteKey() throws CloudServiceException {
        String keyId = kmsService.createKey("delete-key", KMSService.KeyType.SYMMETRIC);

        kmsService.deleteKey(keyId);

        KeyMetadata metadata = kmsService.describeKey(keyId);
        assertEquals(KeyMetadata.KeyState.PENDING_DELETION, metadata.getKeyState());
    }

    @Test
    @DisplayName("Should throw exception for non-existent key")
    void testNonExistentKey() {
        CloudServiceException exception = assertThrows(CloudServiceException.class, () -> {
            kmsService.describeKey("non-existent-key");
        });

        assertEquals("KEY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle large data encryption")
    void testLargeDataEncryption() throws CloudServiceException {
        String keyId = kmsService.createKey("large-data-key", KMSService.KeyType.SYMMETRIC);

        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeText.append("This is a test message. ");
        }

        byte[] plaintext = largeText.toString().getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = kmsService.encrypt(keyId, plaintext);
        byte[] decrypted = kmsService.decrypt(keyId, encrypted);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Should handle multiple encryption operations concurrently")
    void testConcurrentOperations() throws CloudServiceException {
        String keyId = kmsService.createKey("concurrent-key", KMSService.KeyType.SYMMETRIC);

        for (int i = 0; i < 10; i++) {
            String message = "Message " + i;
            byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = kmsService.encrypt(keyId, plaintext);
            byte[] decrypted = kmsService.decrypt(keyId, encrypted);

            assertEquals(message, new String(decrypted, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("Should work with factory pattern")
    void testFactoryPattern() throws CloudServiceException {
        CloudServiceFactory factory = CloudServiceFactory.builder()
                .withRegion("us-west-2")
                .withCredential("accessKey", "test-key")
                .build();

        KMSService factoryKms = factory.createKMSService();
        String keyId = factoryKms.createKey("factory-key", KMSService.KeyType.SYMMETRIC);

        assertNotNull(keyId);
        KeyMetadata metadata = factoryKms.describeKey(keyId);
        assertEquals("factory-key", metadata.getKeyAlias());
    }
}
