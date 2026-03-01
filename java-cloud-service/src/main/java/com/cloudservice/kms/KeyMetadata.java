package com.cloudservice.kms;

import java.time.Instant;

public class KeyMetadata {
    private String keyId;
    private String keyAlias;
    private KMSService.KeyType keyType;
    private KeyState keyState;
    private Instant creationDate;
    private Instant lastRotationDate;
    private String description;

    public KeyMetadata(String keyId, String keyAlias, KMSService.KeyType keyType) {
        this.keyId = keyId;
        this.keyAlias = keyAlias;
        this.keyType = keyType;
        this.keyState = KeyState.ENABLED;
        this.creationDate = Instant.now();
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public KMSService.KeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(KMSService.KeyType keyType) {
        this.keyType = keyType;
    }

    public KeyState getKeyState() {
        return keyState;
    }

    public void setKeyState(KeyState keyState) {
        this.keyState = keyState;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Instant getLastRotationDate() {
        return lastRotationDate;
    }

    public void setLastRotationDate(Instant lastRotationDate) {
        this.lastRotationDate = lastRotationDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public enum KeyState {
        ENABLED,
        DISABLED,
        PENDING_DELETION,
        DELETED
    }

    @Override
    public String toString() {
        return "KeyMetadata{" +
                "keyId='" + keyId + '\'' +
                ", keyAlias='" + keyAlias + '\'' +
                ", keyType=" + keyType +
                ", keyState=" + keyState +
                ", creationDate=" + creationDate +
                '}';
    }
}
