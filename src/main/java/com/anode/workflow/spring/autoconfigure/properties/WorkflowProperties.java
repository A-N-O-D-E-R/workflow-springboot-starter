package com.anode.workflow.spring.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import lombok.Data;

/**
 * Main configuration properties for the Workflow Spring Boot Starter.
 *
 * <p>Configuration example:
 * <pre>
 * workflow:
 *   storage:
 *     type: jpa
 *     file-path: ./workflow-data
 *   jpa:
 *     enabled: true
 *     entity-manager-factory-ref: entityManagerFactory
 *   sla:
 *     enabled: true
 *   event:
 *     enabled: true
 *   factory:
 *     enabled: true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {

    /**
     * Storage configuration.
     */
    @NestedConfigurationProperty
    private StorageProperties storage = new StorageProperties();

    /**
     * JPA configuration.
     */
    @NestedConfigurationProperty
    private JpaProperties jpa = new JpaProperties();

    /**
     * SLA configuration.
     */
    @NestedConfigurationProperty
    private SlaProperties sla = new SlaProperties();

    /**
     * Event handler configuration.
     */
    @NestedConfigurationProperty
    private EventHandlerProperties event = new EventHandlerProperties();

    /**
     * Factory configuration.
     */
    @NestedConfigurationProperty
    private FactoryProperties factory = new FactoryProperties();

    /**
     * Factory configuration properties.
     */
    @Data
    public static class FactoryProperties {
        /**
         * Enable component factory auto-configuration.
         */
        private boolean enabled = true;
    }
}
