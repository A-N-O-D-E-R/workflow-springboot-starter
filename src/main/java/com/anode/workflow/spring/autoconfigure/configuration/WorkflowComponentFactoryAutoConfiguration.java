package com.anode.workflow.spring.autoconfigure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.anode.workflow.spring.autoconfigure.registry.WorkflowComponentFactoryRegistrar;

@Configuration
@Import(WorkflowComponentFactoryRegistrar.class)
@ConditionalOnProperty(
    prefix = "workflow.factory",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class WorkflowComponentFactoryAutoConfiguration {
}