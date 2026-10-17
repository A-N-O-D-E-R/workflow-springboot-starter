package com.anode.workflow.spring.autoconfigure.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.anode.tool.service.CommonService;
import com.anode.workflow.WorkflowService;
import com.anode.workflow.service.EventHandler;
import com.anode.workflow.service.SlaQueueManager;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.properties.RuntimeProperties;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;
import com.anode.workflow.spring.autoconfigure.runtime.WorkflowRuntimes;
import com.anode.workflow.spring.autoconfigure.storage.FileStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.JdbcStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.impl.DefaultWorkflowComponentFactory;
import com.anode.workflow.spring.autoconfigure.impl.NoOpsEventHandler;
import com.anode.workflow.spring.autoconfigure.impl.NoOpsSlaQueueManager;

/**
 * Spring Boot auto-configuration for the workflow engine.
 *
 * <p>This configuration class automatically sets up all necessary beans for the workflow
 * engine during application startup. It configures runtime services, storage, event handlers,
 * and SLA queue managers based on the properties defined in {@link WorkflowEnginesProperties}.
 *
 * <p><b>Configuration properties:</b>
 * Use {@code workflow.engines[].name}, {@code workflow.engines[].storage.type}, etc.
 * to configure different runtime services with different storage backends.
 *
 * <p><b>Provides beans:</b>
 * <ul>
 *   <li>{@link WorkflowService} - the main workflow service singleton</li>
 *   <li>{@link WorkflowRuntimes} - the configured engines; a new RuntimeService per operation</li>
 * </ul>
 *
 * @see WorkflowEnginesProperties
 * @see WorkflowService
 * @see RuntimeService
 */
@AutoConfiguration
@EnableConfigurationProperties(WorkflowEnginesProperties.class)
@AutoConfigureAfter({
        JdbcStorageConfiguration.class,
        MemoryStorageConfiguration.class,
        FileStorageConfiguration.class
})
public class WorkflowAutoConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WorkflowAutoConfiguration.class);

    /**
     * The engine is process-wide (thread pool, key separator): initialized from {@code
     * workflow.runtime.*} when the context starts, closed when it stops.
     */
    @Bean(destroyMethod = "")
    public WorkflowService workflowService(WorkflowEnginesProperties properties) {
        RuntimeProperties runtime = properties.getRuntime();
        WorkflowService.init(
                runtime.getThreads(),
                Math.toIntExact(runtime.getIdleTimeout().toMillis()),
                runtime.getKeySeparator(),
                runtime.getErrorWorkbasket());
        return WorkflowService.instance();
    }

    @Bean
    public DisposableBean workflowServiceShutdown(WorkflowService workflowService) {
        return WorkflowService::close;
    }

    @Bean
    public WorkflowRuntimes workflowRuntimes(
            WorkflowEnginesProperties enginesProperties,
            ApplicationContext context,
            WorkflowService workflowService) {
        Map<String, Supplier<RuntimeService>> engines = new LinkedHashMap<>();
        for (WorkflowEnginesProperties.EngineConfig engine : enginesProperties.getEngines()) {
            log.info("Configuring workflow engine '{}' with {} storage", engine.getName(), engine.getStorage().getType());
            CommonService storage = resolveStorage(context, engine);
            WorkflowComponantFactory factory = resolveOrDefault(context, WorkflowComponantFactory.class);
            EventHandler eventHandler = resolveOrDefault(context, EventHandler.class);
            SlaQueueManager slaQueueManager =
                    engine.getSla().isEnabled() ? resolveOrDefault(context, SlaQueueManager.class) : null;
            if (engines.putIfAbsent(engine.getName(), () -> workflowService.getRunTimeService(
                    storage, factory, eventHandler, slaQueueManager)) != null) {
                throw new IllegalStateException("Duplicate workflow engine name: " + engine.getName());
            }
        }
        return new WorkflowRuntimes(engines);
    }

    private <T> T resolveOrDefault(ApplicationContext ctx, Class<T> type) {
        try {
            return ctx.getBean(type);
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            log.debug("No bean of type {} found, using default implementation", type.getSimpleName());
            return createDefault(type, ctx);
        }
    }

    /**
     * Resolve the storage service based on engine configuration.
     *
     * @param ctx the application context
     * @param engine the engine configuration
     * @return the appropriate CommonService implementation
     */
    private CommonService resolveStorage(
            ApplicationContext ctx,
            WorkflowEnginesProperties.EngineConfig engine
    ) {
        var storageType = engine.getStorage().getType();

        log.debug("Resolving storage for engine '{}' with type: {}",
                  engine.getName(), storageType);

        switch (storageType) {
            case JDBC:
                log.debug("Using JDBC storage");
                return ctx.getBean("jdbcCommonService", CommonService.class);

            case MEMORY:
                log.debug("Using in-memory storage");
                return ctx.getBean("memoryCommonService", CommonService.class);

            case FILE:
                log.debug("Using file-based storage");
                return ctx.getBean("fileCommonService", CommonService.class);

            case CUSTOM:
                String customBeanName = engine.getStorage().getCustomBeanName();
                if (customBeanName == null || customBeanName.isEmpty()) {
                    throw new IllegalStateException(
                        "Custom storage type specified but no customBeanName provided for engine: "
                        + engine.getName());
                }
                log.debug("Using custom storage bean: {}", customBeanName);
                return ctx.getBean(customBeanName, CommonService.class);

            default:
                throw new IllegalArgumentException(
                    "Unknown storage type: " + storageType + " for engine: " + engine.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createDefault(Class<T> type, ApplicationContext ctx) {
        if (type.equals(EventHandler.class)) return (T) new NoOpsEventHandler();
        if (type.equals(SlaQueueManager.class)) return (T) new NoOpsSlaQueueManager();
        if (type.equals(WorkflowComponantFactory.class)) {
            return (T) new DefaultWorkflowComponentFactory(ctx);
        }
        throw new IllegalArgumentException("Unknown default for " + type);
    }
}
