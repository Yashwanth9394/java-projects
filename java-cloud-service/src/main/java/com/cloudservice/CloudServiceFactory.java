package com.cloudservice;

import com.cloudservice.common.ServiceConfig;
import com.cloudservice.kms.KMSService;
import com.cloudservice.kms.KMSServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(CloudServiceFactory.class);

    private final ServiceConfig config;

    private CloudServiceFactory(Builder builder) {
        this.config = builder.config;
    }

    public KMSService createKMSService() {
        logger.info("Creating KMS Service instance");
        return new KMSServiceImpl(config);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ServiceConfig config;

        public Builder() {
            this.config = new ServiceConfig();
        }

        public Builder withConfig(ServiceConfig config) {
            this.config = config;
            return this;
        }

        public Builder withRegion(String region) {
            this.config.setRegion(region);
            return this;
        }

        public Builder withEndpoint(String endpoint) {
            this.config.setEndpoint(endpoint);
            return this;
        }

        public Builder withCredential(String key, String value) {
            this.config.addCredential(key, value);
            return this;
        }

        public Builder withProperty(String key, Object value) {
            this.config.addProperty(key, value);
            return this;
        }

        public CloudServiceFactory build() {
            return new CloudServiceFactory(this);
        }
    }
}
