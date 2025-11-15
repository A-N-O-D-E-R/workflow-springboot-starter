package com.anode.workflow.spring.autoconfigure.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.anode.workflow.spring.autoconfigure.registry.SlaQueueManagerRegistrar;
import com.anode.workflow.spring.autoconfigure.storage.FileStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.JpaStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;

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
