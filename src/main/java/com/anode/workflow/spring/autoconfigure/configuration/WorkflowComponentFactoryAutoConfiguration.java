package com.anode.workflow.spring.autoconfigure.configuration;

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.registry.WorkflowComponentFactoryRegistrar;
import com.anode.workflow.spring.autoconfigure.runtime.FluentWorkflowBuilderFactory;
import com.anode.workflow.spring.autoconfigure.runtime.WorkflowEngine;
import com.anode.workflow.spring.autoconfigure.scanner.TaskScanner;
import com.anode.workflow.spring.autoconfigure.storage.FileStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.JpaStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;

@Configuration
@Import(WorkflowComponentFactoryRegistrar.class)
@ConditionalOnProperty(
    prefix = "workflow.factory",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@AutoConfigureAfter({
        JpaStorageConfiguration.class,
        MemoryStorageConfiguration.class,
        FileStorageConfiguration.class
})
public class WorkflowComponentFactoryAutoConfiguration {

    @Bean
    public TaskScanner taskScanner(ApplicationContext ctx) {
        return new TaskScanner(ctx);
    }
    
    /**
     * WorkflowEngine bean that manages workflow execution.
     * Automatically uses all RuntimeService beans in the context.
     */
    @Bean
    public WorkflowEngine workflowEngine(
            Map<String, RuntimeService> runtimeServices,
            TaskScanner taskScanner
    ) {
        return new WorkflowEngine(runtimeServices, taskScanner);
    }

    /**
     * Provides a fluent workflow builder for building and starting workflows.
     */
    @Bean
    public FluentWorkflowBuilderFactory fluentWorkflowBuilderFactory(
            WorkflowEngine workflowEngine
    ) {
        return new FluentWorkflowBuilderFactory(workflowEngine);
    }
}