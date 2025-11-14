package com.anode.workflow.spring.autoconfigure.configuration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.anode.tool.service.CommonService;
import com.anode.workflow.WorkflowService;
import com.anode.workflow.service.EventHandler;
import com.anode.workflow.service.SlaQueueManager;
import com.anode.workflow.service.WorkflowComponantFactory;
import com.anode.workflow.service.runtime.RuntimeService;
import com.anode.workflow.spring.autoconfigure.properties.WorkflowEnginesProperties;
import com.anode.workflow.spring.autoconfigure.impl.*;


@AutoConfiguration
@EnableConfigurationProperties(WorkflowEnginesProperties.class)
public class WorkflowAutoConfiguration {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WorkflowAutoConfiguration.class);

    @Bean
    public List<RuntimeService> runtimeServices(
            WorkflowEnginesProperties enginesProperties,
            ApplicationContext context,
            WorkflowService workflowService
    ) {

        List<RuntimeService> services = new ArrayList<>();

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

            services.add(runtime);
        }

        return services;
    }

    private <T> T resolveOrDefault(ApplicationContext ctx, Class<T> type) {
        try {
            return ctx.getBean(type);
        } catch (Exception e) {
            return createDefault(type);
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
    private <T> T createDefault(Class<T> type) {
        if (type.equals(EventHandler.class)) return (T) new NoOpsEventHandler();
        if (type.equals(SlaQueueManager.class)) return (T) new NoOpsSlaQueueManager();
        if (type.equals(WorkflowComponantFactory.class)) return (T) new DefaultWorkflowComponentFactory();
        throw new IllegalArgumentException("Unknown default for " + type);
    }
}
