package com.cloudservice.common;

public class CloudServiceException extends Exception {
    private final String errorCode;

    public CloudServiceException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
    }

    public CloudServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public CloudServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_ERROR";
    }

    public CloudServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
