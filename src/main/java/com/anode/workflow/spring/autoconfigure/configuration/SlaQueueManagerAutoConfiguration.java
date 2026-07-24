package com.anode.workflow.spring.autoconfigure.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.anode.workflow.spring.autoconfigure.registry.SlaQueueManagerRegistrar;
import com.anode.workflow.spring.autoconfigure.storage.FileStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.JpaStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;

/**
 * Spring configuration for SLA queue manager auto-registration.
 *
 * <p>This configuration enables automatic discovery of SLA queue manager implementations
 * through classpath scanning. SLA managers handle priority-based task queuing and ensure
 * workflow tasks are processed within specified time constraints.
 *
 * <p><b>Enabled by:</b> Set {@code workflow.sla.enabled=true} (default: true) to enable this configuration.
 *
 * @see com.anode.workflow.spring.autoconfigure.annotations.SlaQueueManagerComponent
 * @see SlaQueueManagerRegistrar
 */
@Configuration
@Import(SlaQueueManagerRegistrar.class)
@ConditionalOnProperty(
    prefix = "workflow.sla",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@AutoConfigureAfter({
        JpaStorageConfiguration.class,
        MemoryStorageConfiguration.class,
        FileStorageConfiguration.class
})
public class SlaQueueManagerAutoConfiguration {
}
