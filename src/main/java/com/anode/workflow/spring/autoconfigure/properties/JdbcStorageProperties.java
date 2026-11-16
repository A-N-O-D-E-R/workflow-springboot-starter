package com.anode.workflow.spring.autoconfigure.properties;

import lombok.Data;

/** JDBC storage settings. */
@Data
public class JdbcStorageProperties {

    /** Table holding one JSON document per key. */
    private String documentTable = "workflow_document";

    /** Table holding the engine's counters (audit-log sequence numbers). */
    private String counterTable = "workflow_counter";

    /**
     * Create the two tables at startup if they do not exist. Leave off when the schema is managed
     * by a migration tool (Liquibase, Flyway): the DDL is {@code
     * classpath:com/anode/workflow/spring/storage/schema-postgresql.sql}.
     */
    private boolean initializeSchema = false;
}
