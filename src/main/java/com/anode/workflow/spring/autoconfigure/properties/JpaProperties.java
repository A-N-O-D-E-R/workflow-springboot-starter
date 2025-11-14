package com.anode.workflow.spring.autoconfigure.properties;

import lombok.Data;

/**
 * JPA configuration properties.
 */
@Data
public class JpaProperties {

    /**
     * Enable JPA-based storage.
     */
    private boolean enabled = true;

    /**
     * EntityManagerFactory bean name.
     */
    private String entityManagerFactoryRef = "entityManagerFactory";

    /**
     * Enable automatic schema creation.
     */
    private boolean autoCreateSchema = false;

    /**
     * Storage type enumeration.
     */
    public enum StorageType {
        /**
         * JPA/Hibernate-based storage (recommended for production).
         */
        JPA,

        /**
         * In-memory storage (for testing).
         */
        MEMORY,

        /**
         * File-based JSON storage (for development/testing).
         */
        FILE,

        /**
         * Custom storage implementation.
         */
        CUSTOM
    }
}

