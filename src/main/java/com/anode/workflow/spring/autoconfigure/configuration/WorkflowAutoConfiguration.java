package com.anode.workflow.spring.autoconfigure.configuration;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.anode.tool.service.CommonService;
import com.anode.workflow.WorkflowService;
import com.anode.workflow.entities.steps.InvokableRoute;
import com.anode.workflow.entities.steps.InvokableTask;
import com.anode.workflow.service.EventHandler;
import com.anode.workflow.service.SlaQueueManager;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;
import com.anode.workflow.spring.autoconfigure.storage.FileStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.JpaStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.storage.MemoryStorageConfiguration;
import com.anode.workflow.spring.autoconfigure.impl.*;


@AutoConfiguration
@EnableConfigurationProperties(WorkflowEnginesProperties.class)
@AutoConfigureAfter({
        JpaStorageConfiguration.class,
        MemoryStorageConfiguration.class,
        FileStorageConfiguration.class
})
public class WorkflowAutoConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WorkflowAutoConfiguration.class);

    @Bean
    public WorkflowService workflowService() {
        // Use the static singleton instance
        return WorkflowService.instance();
    }

    @Bean
    public Map<String,RuntimeService> runtimeServices(
            WorkflowEnginesProperties enginesProperties,
            ApplicationContext context,
            WorkflowService workflowService
    ) {

        Map<String, RuntimeService> services = new HashMap<>();

        for (WorkflowEnginesProperties.EngineConfig engine : enginesProperties.getEngines()) {

            log.info("Creating RuntimeService for engine: {}", engine.getName());

            WorkflowComponantFactory factory =
                    resolveOrDefault(context, WorkflowComponantFactory.class);

            EventHandler eventHandler =
                    resolveOrDefault(context, EventHandler.class);

            SlaQueueManager slaQueueManager =
                    engine.getSla().isEnabled()
                            ? resolveOrDefault(context, SlaQueueManager.class)
                            : null;

            CommonService storage = resolveStorage(context, engine);

            RuntimeService runtime = workflowService.getRunTimeService(
                    storage,
                    factory,
                    eventHandler,
                    slaQueueManager
            );

            services.put(engine.getName(),runtime);
        }

        return services;
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
            case JPA:
                log.debug("Using JPA storage");
                return ctx.getBean("jpaCommonService", CommonService.class);

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
            // Get the ObjectProviders for InvokableTask and InvokableRoute from the context
            ObjectProvider<InvokableTask> taskProvider = ctx.getBeanProvider(InvokableTask.class);
            ObjectProvider<InvokableRoute> routeProvider = ctx.getBeanProvider(InvokableRoute.class);
            return (T) new DefaultWorkflowComponentFactory(taskProvider, routeProvider);
        }
        throw new IllegalArgumentException("Unknown default for " + type);
    }
}
