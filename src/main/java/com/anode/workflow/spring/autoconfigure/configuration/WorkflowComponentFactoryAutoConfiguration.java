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

/**
 * Spring configuration for the workflow component factory and task scanning.
 *
 * <p>This configuration enables automatic discovery of workflow components (tasks, routes, etc.)
 * through classpath scanning. It sets up the task scanner, workflow engine, and fluent builder factory.
 *
 * <p><b>Enabled by:</b> Set {@code workflow.factory.enabled=true} (default: true) to enable this configuration.
 *
 * <p><b>Provides beans:</b>
 * <ul>
 *   <li>{@link TaskScanner} - discovers and registers @Task annotated classes</li>
 *   <li>{@link WorkflowEngine} - provides workflow execution operations</li>
 *   <li>{@link FluentWorkflowBuilderFactory} - factory for creating workflow builders</li>
 * </ul>
 *
 * @see TaskScanner
 * @see WorkflowEngine
 * @see FluentWorkflowBuilderFactory
 */
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

    /**
     * Creates a task scanner for discovering @Task annotated classes.
     *
     * @param ctx the application context
     * @return the configured task scanner
     */
    @Bean
    public TaskScanner taskScanner(ApplicationContext ctx) {
        return new TaskScanner(ctx);
    }

    /**
     * Creates the workflow engine bean.
     *
     * <p>The engine manages workflow execution and uses all available runtime services.
     *
     * @param runtimeServices map of configured runtime services
     * @param taskScanner the task scanner
     * @param applicationContext the Spring application context
     * @return the configured workflow engine
     */
    @Bean
    public WorkflowEngine workflowEngine(
            Map<String, RuntimeService> runtimeServices,
            TaskScanner taskScanner,
            ApplicationContext applicationContext
    ) {
        WorkflowEngine engine = new WorkflowEngine(runtimeServices, taskScanner);
        engine.setApplicationContext(applicationContext);
        return engine;
    }

    /**
     * Creates the fluent workflow builder factory bean.
     *
     * @param workflowEngine the workflow engine
     * @return the configured builder factory
     */
    @Bean
    public FluentWorkflowBuilderFactory fluentWorkflowBuilderFactory(
            WorkflowEngine workflowEngine
    ) {
        return new FluentWorkflowBuilderFactory(workflowEngine);
    }
}