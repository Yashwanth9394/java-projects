package com.cloudservice.examples;

import com.cloudservice.CloudServiceFactory;
import com.cloudservice.common.CloudServiceException;
import com.cloudservice.kms.KMSService;
import com.cloudservice.kms.KeyMetadata;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class KMSExample {

    public static void main(String[] args) {
        try {
            CloudServiceFactory factory = CloudServiceFactory.builder()
                    .withRegion("us-east-1")
                    .withCredential("accessKey", "YOUR_ACCESS_KEY")
                    .withCredential("secretKey", "YOUR_SECRET_KEY")
                    .build();

            KMSService kmsService = factory.createKMSService();

            System.out.println("=== Java Cloud Service - KMS Example ===\n");

            String keyId = kmsService.createKey("my-encryption-key", KMSService.KeyType.SYMMETRIC);
            System.out.println("1. Created encryption key: " + keyId);

            String sensitiveData = "Credit Card: 1234-5678-9012-3456";
            byte[] plaintext = sensitiveData.getBytes(StandardCharsets.UTF_8);

            byte[] encrypted = kmsService.encrypt(keyId, plaintext);
            System.out.println("2. Encrypted sensitive data");
            System.out.println("   Encrypted bytes length: " + encrypted.length);

            byte[] decrypted = kmsService.decrypt(keyId, encrypted);
            String decryptedData = new String(decrypted, StandardCharsets.UTF_8);
            System.out.println("3. Decrypted data: " + decryptedData);

            String dataKey = kmsService.generateDataKey(keyId, 256);
            System.out.println("4. Generated 256-bit data key (first 20 chars): " +
                    dataKey.substring(0, Math.min(20, dataKey.length())) + "...");

            kmsService.rotateKey(keyId);
            System.out.println("5. Rotated encryption key");

            KeyMetadata metadata = kmsService.describeKey(keyId);
            System.out.println("6. Key metadata:");
            System.out.println("   - Key ID: " + metadata.getKeyId());
            System.out.println("   - Alias: " + metadata.getKeyAlias());
            System.out.println("   - Type: " + metadata.getKeyType());
            System.out.println("   - State: " + metadata.getKeyState());
            System.out.println("   - Created: " + metadata.getCreationDate());
            System.out.println("   - Last Rotated: " + metadata.getLastRotationDate());

            String asymKeyId = kmsService.createKey("rsa-key", KMSService.KeyType.ASYMMETRIC_RSA_2048);
            System.out.println("7. Created RSA 2048 asymmetric key: " + asymKeyId);

            List<KeyMetadata> allKeys = kmsService.listKeys();
            System.out.println("8. Total keys in KMS: " + allKeys.size());

            kmsService.disableKey(keyId);
            System.out.println("9. Disabled key: " + keyId);

            kmsService.enableKey(keyId);
            System.out.println("10. Re-enabled key: " + keyId);

            System.out.println("\n=== Example completed successfully! ===");

        } catch (CloudServiceException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
    }
}
