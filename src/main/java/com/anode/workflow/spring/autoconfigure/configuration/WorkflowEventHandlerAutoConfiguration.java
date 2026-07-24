package com.anode.workflow.spring.autoconfigure.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.anode.workflow.spring.autoconfigure.registry.WorkflowEventHandlerRegistrar;
import com.anode.workflow.spring.autoconfigure.storage.FileStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.JpaStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;

/**
 * Spring configuration for workflow event handler auto-registration.
 *
 * <p>This configuration enables automatic discovery of workflow event handlers
 * through classpath scanning. Event handlers are registered and made available
 * to the workflow engine for listening to workflow lifecycle events.
 *
 * <p><b>Enabled by:</b> Set {@code workflow.event.enabled=true} (default: true) to enable this configuration.
 *
 * @see com.anode.workflow.spring.autoconfigure.annotations.WorkflowEventHandler
 * @see WorkflowEventHandlerRegistrar
 */
@Configuration
@Import(WorkflowEventHandlerRegistrar.class)
@ConditionalOnProperty(
    prefix = "workflow.event",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@AutoConfigureAfter({
        JpaStorageConfiguration.class,
        MemoryStorageConfiguration.class,
        FileStorageConfiguration.class
})
public class WorkflowEventHandlerAutoConfiguration {
}

