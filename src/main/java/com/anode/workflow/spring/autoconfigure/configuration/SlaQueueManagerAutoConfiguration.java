package com.anode.workflow.spring.autoconfigure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.anode.workflow.spring.autoconfigure.registry.SlaQueueManagerRegistrar;

@Configuration
@Import(SlaQueueManagerRegistrar.class)
@ConditionalOnProperty(
    prefix = "workflow.sla",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SlaQueueManagerAutoConfiguration {
}
