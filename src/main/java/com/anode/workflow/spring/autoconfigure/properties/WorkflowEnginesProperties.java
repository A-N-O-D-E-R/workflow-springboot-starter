package com.anode.workflow.spring.autoconfigure.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import com.anode.workflow.spring.autoconfigure.properties.WorkflowProperties.FactoryProperties;

import lombok.Data;

/**
 * Configuration properties for multiple workflow engines.
 *
 * <p>This allows configuring multiple workflow engines with different configurations.
 * <p>Configuration example:
 * <pre>
 * workflow:
 *   engines:
 *     - name: engine1
 *       storage:
 *         type: jpa
 *       sla:
 *         enabled: true
 *     - name: engine2
 *       storage:
 *         type: memory
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "workflow")
public class WorkflowEnginesProperties {

    private List<EngineConfig> engines = new ArrayList<>();

    @Data
    public static class EngineConfig {
        private String name;

        @NestedConfigurationProperty
        private StorageProperties storage = new StorageProperties();

        @NestedConfigurationProperty
        private SlaProperties sla = new SlaProperties();

        @NestedConfigurationProperty
        private JpaProperties jpa = new JpaProperties();

        @NestedConfigurationProperty
        private EventHandlerProperties event = new EventHandlerProperties();
        
        @NestedConfigurationProperty
        private FactoryProperties factory = new FactoryProperties();
    }
}
