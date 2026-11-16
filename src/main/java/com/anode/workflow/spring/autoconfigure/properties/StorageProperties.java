package com.anode.workflow.spring.autoconfigure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** Where an engine persists the state of its cases. */
@Data
public class StorageProperties {

    /** Storage backends. */
    public enum StorageType {
        /** Durable: one JSON document per key in a relational table (PostgreSQL). */
        JDBC,
        /** In-memory, lost on restart: tests and demos only. */
        MEMORY,
        /** One JSON file per key under {@link #filePath}. */
        FILE,
        /** A {@code CommonService} bean of your own, named by {@link #customBeanName}. */
        CUSTOM
    }

    /** Storage backend. */
    private StorageType type = StorageType.MEMORY;

    /** Directory of the FILE storage. */
    private String filePath = "./workflow-data";

    /** Bean name of the CUSTOM storage. */
    private String customBeanName;

    /** JDBC storage settings. */
    @NestedConfigurationProperty private JdbcStorageProperties jdbc = new JdbcStorageProperties();
}
