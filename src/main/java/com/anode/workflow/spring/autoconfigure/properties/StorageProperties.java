package com.anode.workflow.spring.autoconfigure.properties;

import com.anode.workflow.spring.autoconfigure.properties.JpaProperties.StorageType;

import lombok.Data;

/**
 * Storage configuration properties.
 */
@Data
public class StorageProperties {

    /**
     * Storage type: jpa, memory, file, or custom.
     */
    private StorageType type = StorageType.JPA;

    /**
     * File path for file-based storage.
     */
    private String filePath = "./workflow-data";

    /**
     * Custom storage bean name (if type is CUSTOM).
     */
    private String customBeanName;
}
